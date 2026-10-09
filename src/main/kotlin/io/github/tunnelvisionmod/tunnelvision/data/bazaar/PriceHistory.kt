package io.github.tunnelvisionmod.tunnelvision.data.bazaar

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import io.github.tunnelvisionmod.tunnelvision.TunnelVision
import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

data class MedianPrices(val sellOffer: Double, val instantSell: Double, val fetchedAt: Long)

/**
 * 7-day median Bazaar prices from EliteSkyblock, so a bought-out product cannot inflate a verdict.
 *
 * Hypixel has no price history, and one request covers one product, so every game start walks
 * [products] in priority order, one request every [STEP_MS], under the 20 per 15 s limit that other
 * mods share with us, with a toast showing progress since it mostly runs on the title screen.
 * Medians are saved and stay usable for [MAX_AGE_MS] if a refresh fails.
 */
object PriceHistory {
	private const val URL = "https://api.eliteskyblock.com/resources/bazaar/%s/history"
	private const val STEP_MS = 1_000L
	private const val MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L
	private const val MIN_POINTS = 24
	private const val GIVE_UP_AFTER = 3

	private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
	private val gson = Gson()
	private val running = AtomicBoolean(false)
	private val pending = mutableListOf<Component>()
	private val toast = SystemToast.SystemToastId(5_000L)
	private var products: List<String> = emptyList()

	@Volatile
	private var medians: Map<String, MedianPrices> = emptyMap()

	private val file: Path get() = FabricLoader.getInstance().configDir.resolve("${TunnelVision.MOD_ID}/bazaar-medians.json")

	fun init(products: List<String>) {
		this.products = products
		medians = load()
		EventBus.on<ClientTickEvent> { flushMessages() }
		refresh()
	}

	/** The saved median for [productId], or null if there is none or it is older than 7 days. */
	fun median(productId: String, now: Long = System.currentTimeMillis()): MedianPrices? =
		medians[productId]?.takeIf { now - it.fetchedAt <= MAX_AGE_MS }

	/** Fetches every median again; [announce] also reports success, for `/tv prices`. */
	fun refresh(announce: Boolean = false) {
		if (!running.compareAndSet(false, true)) {
			say(Component.literal("Bazaar price history is already loading.").withStyle(ChatFormatting.YELLOW))
			return
		}
		Thread.ofPlatform().daemon().name("TunnelVision-PriceHistory").start {
			try {
				run(announce)
			} finally {
				running.set(false)
			}
		}
	}

	private fun run(announce: Boolean) {
		var failed = 0
		var inARow = 0
		for ((index, product) in products.withIndex()) {
			progress("Loading 7-day Bazaar prices ${index + 1}/${products.size}")
			if (inARow >= GIVE_UP_AFTER) {
				failed += products.size - index
				break
			}
			if (index > 0) Thread.sleep(STEP_MS)
			val result = runCatching { fetch(product) }
			result.onFailure { TunnelVision.logger.warn("Failed to load price history for $product: ${it.message}") }
			val prices = result.getOrNull()
			if (result.isFailure) {
				failed++
				inARow++
				continue
			}
			inARow = 0
			if (prices == null) continue
			medians = medians + (product to prices)
			save()
			Bazaar.pricesChanged()
			Debug.log { "PriceHistory: $product median ${prices.sellOffer} / ${prices.instantSell}" }
		}
		progress(if (failed == 0) "Loaded 7-day Bazaar prices" else "$failed of ${products.size} Bazaar prices failed")
		if (failed == 0 && announce) {
			mc.execute { say(Component.literal("Loaded the 7-day Bazaar prices.").withStyle(ChatFormatting.GREEN)) }
		} else if (failed > 0) {
			mc.execute {
				say(
					Component.literal("Could not load the 7-day Bazaar prices for $failed of ${products.size} items. Saved prices up to 7 days old are used, live prices otherwise. ")
						.withStyle(ChatFormatting.RED)
						.append(retryButton()),
				)
			}
		}
	}

	/** The median for [product], null when EliteSkyblock has too little history for it. */
	private fun fetch(product: String): MedianPrices? {
		val request = HttpRequest.newBuilder(URI.create(URL.format(product)))
			.timeout(Duration.ofSeconds(15))
			.header("User-Agent", TunnelVision.MOD_ID)
			.GET()
			.build()
		var response = client.send(request, HttpResponse.BodyHandlers.ofString())
		if (response.statusCode() == 429) {
			val reset = response.headers().firstValue("x-ratelimit-reset").map { it.toLongOrNull() }.orElse(null)
			val waitMs = reset?.let { it * 1000 - System.currentTimeMillis() }?.coerceIn(1_000L, 60_000L) ?: 15_000L
			Thread.sleep(waitMs + 1_000L)
			response = client.send(request, HttpResponse.BodyHandlers.ofString())
		}
		check(response.statusCode() == 200) { "HTTP ${response.statusCode()}" }
		return parse(response.body(), System.currentTimeMillis())
	}

	fun parse(json: String, now: Long): MedianPrices? {
		val root = JsonParser.parseString(json).asJsonObject
		val history = root.getAsJsonArray("history") ?: return null
		val points = history.mapNotNull { it.takeIf { it.isJsonObject }?.asJsonObject }
		fun prices(field: String) = points.mapNotNull { point ->
			point.get(field)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble?.takeIf { it > 0 }
		}
		val sellOffer = prices("instaBuyPrice")
		val instantSell = prices("instaSellPrice")
		if (sellOffer.size < MIN_POINTS || instantSell.size < MIN_POINTS) return null
		return MedianPrices(median(sellOffer), median(instantSell), now)
	}

	fun median(values: List<Double>): Double {
		val sorted = values.sorted()
		val middle = sorted.size / 2
		return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
	}

	private fun load(): Map<String, MedianPrices> {
		if (!Files.exists(file)) return emptyMap()
		return runCatching {
			val type = object : TypeToken<Map<String, MedianPrices>>() {}.type
			gson.fromJson<Map<String, MedianPrices>>(Files.readString(file), type) ?: emptyMap()
		}.getOrElse {
			TunnelVision.logger.warn("Failed to load saved Bazaar medians: ${it.message}")
			emptyMap()
		}
	}

	private fun save() {
		runCatching {
			Files.createDirectories(file.parent)
			val temp = file.resolveSibling("${file.fileName}.tmp")
			Files.writeString(temp, gson.toJson(medians))
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
		}.onFailure { TunnelVision.logger.warn("Failed to save Bazaar medians: ${it.message}") }
	}

	private fun progress(text: String) = mc.execute {
		SystemToast.addOrUpdate(Compat.toasts, toast, Component.literal("TunnelVision"), Component.literal(text))
	}

	private fun say(message: Component) {
		pending += message
		flushMessages()
	}

	private fun flushMessages() {
		if (pending.isEmpty() || mc.player == null || !SkyBlock.isOnSkyBlock) return
		pending.forEach { ChatUtils.send(it) }
		pending.clear()
	}

	private fun retryButton() = Component.literal("[Retry]").withStyle { style ->
		style.withColor(ChatFormatting.AQUA).withBold(true)
			.withClickEvent(ClickEvent.RunCommand("/tv prices"))
			.withHoverEvent(HoverEvent.ShowText(Component.literal("Load the 7-day Bazaar prices again")))
	}
}
