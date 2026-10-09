package io.github.tunnelvisionmod.tunnelvision.data.bazaar

import com.google.gson.JsonParser
import io.github.tunnelvisionmod.tunnelvision.TunnelVision
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class BazaarPrices(val sellOffer: Double, val instantSell: Double)

/**
 * Bazaar prices from Hypixel, plus lowest BINs for the few auction-house items we value.
 *
 * Hypixel's own auction API is paged and far too heavy to poll, so lowest BINs come from the
 * EliteSkyblock feed Firmament also uses. Both refresh together.
 */
object Bazaar {
	private const val URL = "https://api.hypixel.net/v2/skyblock/bazaar"
	private const val AUCTION_URL = "https://api.eliteskyblock.com/resources/auctions/neu"
	private const val REFRESH_MS = 5 * 60 * 1000L
	private const val RETRY_MS = 60 * 1000L

	private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
	private val fetching = AtomicBoolean(false)

	@Volatile
	private var prices: Map<String, BazaarPrices> = emptyMap()

	@Volatile
	private var lowestBins: Map<String, Double> = emptyMap()

	@Volatile
	private var nextFetch = 0L

	private val loads = AtomicInteger()

	/** Bumped on every successful load, so callers can cache work derived from prices. */
	val generation: Int get() = loads.get()

	fun init() {
		EventBus.on<ClientTickEvent> { if (SkyBlock.isOnMiningIsland) refreshIfStale() }
	}

	/**
	 * Live prices capped at their 7-day median, so a bought-out product reads at its usual price.
	 * For what we sell; costs use [livePrice], since capping a cost would flatter the verdict.
	 */
	fun price(productId: String): BazaarPrices? {
		val live = prices[productId] ?: return null
		return capped(live, PriceHistory.median(productId))
	}

	fun livePrice(productId: String): BazaarPrices? = prices[productId]

	fun capped(live: BazaarPrices, median: MedianPrices?): BazaarPrices {
		if (median == null) return live
		return BazaarPrices(sellOffer = minOf(live.sellOffer, median.sellOffer), instantSell = minOf(live.instantSell, median.instantSell))
	}

	internal fun pricesChanged() {
		loads.incrementAndGet()
	}

	/** The lowest BIN for an auction-house item, or null until the feed has loaded or if it is not listed. */
	fun lowestBin(itemId: String): Double? = lowestBins[itemId]

	private fun refreshIfStale() {
		if (System.currentTimeMillis() < nextFetch || !fetching.compareAndSet(false, true)) return
		val bazaar = fetch(URL)
			.thenAccept { body ->
				val parsed = parse(body)
				if (parsed.isNotEmpty()) {
					prices = parsed
					loads.incrementAndGet()
				}
				nextFetch = System.currentTimeMillis() + if (parsed.isNotEmpty()) REFRESH_MS else RETRY_MS
				Debug.log { "Bazaar: loaded ${parsed.size} products" }
				Debug.log {
					val capped = parsed.mapNotNull { (id, live) ->
						val cap = capped(live, PriceHistory.median(id))
						if (cap == live) null else "$id ${live.sellOffer}/${live.instantSell} -> ${cap.sellOffer}/${cap.instantSell}"
					}
					"Bazaar: ${capped.size} capped at their 7-day median" + capped.joinToString(prefix = ": ").takeIf { capped.isNotEmpty() }.orEmpty()
				}
			}
			.exceptionally { error ->
				TunnelVision.logger.warn("Failed to load bazaar prices: ${error.message}")
				nextFetch = System.currentTimeMillis() + RETRY_MS
				null
			}
		val auctions = fetch(AUCTION_URL)
			.thenAccept { body ->
				val parsed = parseLowestBins(body)
				if (parsed.isNotEmpty()) {
					lowestBins = parsed
					loads.incrementAndGet()
				}
				Debug.log { "Bazaar: loaded ${parsed.size} lowest BINs" }
			}
			.exceptionally { error ->
				TunnelVision.logger.warn("Failed to load lowest BINs: ${error.message}")
				null
			}
		CompletableFuture.allOf(bazaar, auctions).whenComplete { _, _ -> fetching.set(false) }
	}

	private fun fetch(url: String): CompletableFuture<String> {
		val request = HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(15))
			.header("User-Agent", TunnelVision.MOD_ID)
			.GET()
			.build()
		return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply { it.body() }
	}

	fun parse(json: String): Map<String, BazaarPrices> {
		val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return emptyMap()
		if (root.get("success")?.asBoolean != true) return emptyMap()
		return root.getAsJsonObject("products").entrySet().mapNotNull { (id, product) ->
			val status = product.asJsonObject.getAsJsonObject("quick_status") ?: return@mapNotNull null
			id to BazaarPrices(sellOffer = status.get("buyPrice").asDouble, instantSell = status.get("sellPrice").asDouble)
		}.toMap()
	}

	fun parseLowestBins(json: String): Map<String, Double> {
		val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return emptyMap()
		return root.entrySet().mapNotNull { (id, price) ->
			price.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.let { id to it.asDouble }
		}.toMap()
	}
}
