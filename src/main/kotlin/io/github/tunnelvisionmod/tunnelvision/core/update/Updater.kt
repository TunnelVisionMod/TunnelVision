package io.github.tunnelvisionmod.tunnelvision.core.update

import io.github.tunnelvisionmod.tunnelvision.TunnelVision
import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent

/**
 * Tells you when a new release is out, with links to where it is downloaded. Nothing is downloaded
 * or installed by the mod itself.
 *
 * The automatic check only runs with General → Check for Updates on; `/tv update` works either way.
 * Messages wait until there is a chat to show them in.
 */
object Updater {
	private const val USER_AGENT = "TunnelVision-Updater"

	private const val MODRINTH_URL = "https://modrinth.com/mod/tunnelvision-isingularity"

	private val client = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.connectTimeout(Duration.ofSeconds(10))
		.build()

	private val pending = mutableListOf<Component>()
	private var busy = false

	private val container get() = FabricLoader.getInstance().getModContainer(TunnelVision.MOD_ID).orElseThrow()
	private val installedVersion: String get() = container.metadata.version.friendlyString
	private val minecraftVersion: String
		get() = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().metadata.version.friendlyString

	/** What the self-installing updater of 1.3.4 to 1.4.1 left behind. */
	private val leftovers: List<Path>
		get() = listOf("update", "update-result.txt").map { FabricLoader.getInstance().configDir.resolve("${TunnelVision.MOD_ID}/$it") }

	fun init() {
		removeLeftovers()
		EventBus.on<ClientTickEvent> { flushMessages() }
		if (ConfigManager.config.general.checkForUpdates) checkForUpdate(announceNothingNew = false)
	}

	/** `/tv update`. */
	fun command() = checkForUpdate(announceNothingNew = true)

	private fun checkForUpdate(announceNothingNew: Boolean) {
		if (busy) return
		busy = true
		val request = HttpRequest.newBuilder(URI.create(UpdateChecker.LATEST_URL))
			.timeout(Duration.ofSeconds(15))
			.header("Accept", "application/vnd.github+json")
			.header("User-Agent", USER_AGENT)
			.GET()
			.build()
		client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete { response, error ->
			mc.execute {
				busy = false
				if (error != null || response.statusCode() != 200) {
					TunnelVision.logger.warn("Update check failed: ${error?.message ?: "HTTP ${response.statusCode()}"}")
					if (announceNothingNew) say(Component.literal("Could not check for updates.").withStyle(ChatFormatting.RED))
					return@execute
				}
				val release = UpdateChecker.newer(UpdateChecker.parseLatest(response.body(), minecraftVersion), Version.parse(installedVersion))
				if (release == null) {
					if (announceNothingNew) say(Component.literal("TunnelVision is up to date.").withStyle(ChatFormatting.GREEN))
					return@execute
				}
				val message = Component.literal("TunnelVision ${release.version} is available (you have $installedVersion). ")
					.withStyle(ChatFormatting.YELLOW)
				say(message.append(link("Modrinth", MODRINTH_URL)).append(Component.literal(" ")).append(link("GitHub", release.pageUrl)))
			}
		}
	}

	private fun removeLeftovers() {
		for (path in leftovers) {
			if (!Files.exists(path)) continue
			runCatching { path.toFile().deleteRecursively() }
				.onFailure { TunnelVision.logger.warn("Could not remove the old updater's $path: ${it.message}") }
		}
	}

	private fun say(message: Component) {
		pending += message
		flushMessages()
	}

	private fun flushMessages() {
		if (pending.isEmpty() || mc.player == null) return
		pending.forEach { ChatUtils.send(it) }
		pending.clear()
	}

	private fun link(label: String, url: String): MutableComponent =
		Component.literal("[$label]").withStyle { style ->
			style.withColor(ChatFormatting.AQUA).withBold(true)
				.withClickEvent(ClickEvent.OpenUrl(URI.create(url)))
				.withHoverEvent(HoverEvent.ShowText(Component.literal(url)))
		}
}
