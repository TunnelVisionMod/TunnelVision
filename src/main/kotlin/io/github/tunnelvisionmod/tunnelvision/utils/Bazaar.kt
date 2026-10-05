package io.github.tunnelvisionmod.tunnelvision.utils

import com.google.gson.JsonParser
import io.github.tunnelvisionmod.tunnelvision.TunnelVision
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

data class BazaarPrices(val sellOffer: Double, val instantSell: Double)

object Bazaar {
	private const val URL = "https://api.hypixel.net/v2/skyblock/bazaar"
	private const val REFRESH_MS = 5 * 60 * 1000L
	private const val RETRY_MS = 60 * 1000L

	private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
	private val fetching = AtomicBoolean(false)

	@Volatile
	private var prices: Map<String, BazaarPrices> = emptyMap()

	@Volatile
	private var nextFetch = 0L

	/** Bumped on every successful load, so callers can cache work derived from prices. */
	@Volatile
	var generation: Int = 0
		private set

	fun price(productId: String): BazaarPrices? = prices[productId]

	fun refreshIfStale() {
		if (System.currentTimeMillis() < nextFetch || !fetching.compareAndSet(false, true)) return
		val request = HttpRequest.newBuilder(URI.create(URL))
			.timeout(Duration.ofSeconds(15))
			.header("User-Agent", TunnelVision.MOD_ID)
			.GET()
			.build()
		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept { response ->
				val parsed = parse(response.body())
				if (parsed.isNotEmpty()) prices = parsed
				nextFetch = System.currentTimeMillis() + if (parsed.isNotEmpty()) REFRESH_MS else RETRY_MS
				Debug.log { "Bazaar: loaded ${parsed.size} products" }
			}
			.exceptionally { error ->
				TunnelVision.logger.warn("Failed to load bazaar prices: ${error.message}")
				nextFetch = System.currentTimeMillis() + RETRY_MS
				null
			}
			.whenComplete { _, _ -> fetching.set(false) }
	}

	fun parse(json: String): Map<String, BazaarPrices> {
		val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return emptyMap()
		if (root.get("success")?.asBoolean != true) return emptyMap()
		return root.getAsJsonObject("products").entrySet().mapNotNull { (id, product) ->
			val status = product.asJsonObject.getAsJsonObject("quick_status") ?: return@mapNotNull null
			id to BazaarPrices(sellOffer = status.get("buyPrice").asDouble, instantSell = status.get("sellPrice").asDouble)
		}.toMap()
	}
}
