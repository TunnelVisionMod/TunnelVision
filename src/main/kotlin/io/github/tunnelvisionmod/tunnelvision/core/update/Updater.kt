package io.github.tunnelvisionmod.tunnelvision.core.update

import io.github.tunnelvisionmod.tunnelvision.TunnelVision
import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.update.TunnelVisionUpdateHelper
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent

/**
 * Finds, downloads, verifies and installs new releases.
 *
 * The automatic check only runs with General → Check for Updates on; `/tv update` works either way.
 * Nothing is downloaded without a click, nothing is installed unless [UpdateVerifier] accepts its
 * signature, and the jar is only swapped after the game has exited, by [HELPER_CLASS] in its own
 * process. Messages wait until there is a chat to show them in.
 */
object Updater {
	private const val HELPER_CLASS = "io.github.tunnelvisionmod.tunnelvision.update.TunnelVisionUpdateHelper"
	private const val USER_AGENT = "TunnelVision-Updater"

	private val client = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.connectTimeout(Duration.ofSeconds(10))
		.build()

	private val pending = mutableListOf<Component>()
	private var latest: Release? = null
	private var downloaded: Path? = null
	private var downloadedHash: String? = null
	private var busy = false
	private var armed = false

	private val container get() = FabricLoader.getInstance().getModContainer(TunnelVision.MOD_ID).orElseThrow()
	private val currentVersion: Version? get() = Version.parse(container.metadata.version.friendlyString)
	private val minecraftVersion: String
		get() = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().metadata.version.friendlyString

	/** The jar we were loaded from, or null in a dev environment where there is nothing to replace. */
	private val ownJar: Path?
		get() = container.origin.paths.singleOrNull()?.takeIf { Files.isRegularFile(it) && it.toString().endsWith(".jar") }

	private val resultFile: Path get() = FabricLoader.getInstance().configDir.resolve("${TunnelVision.MOD_ID}/update-result.txt")
	// Inside our own config folder rather than the shared temp folder, so no other user can swap the
	// verified jar or the helper before the install.
	private val workDir: Path get() = FabricLoader.getInstance().configDir.resolve("${TunnelVision.MOD_ID}/update")

	fun init() {
		reportLastResult()
		EventBus.on<ClientTickEvent> { flushMessages() }
		if (ConfigManager.config.general.checkForUpdates) checkForUpdate(announceNothingNew = false)
	}

	/** `/tv update`: offers the update, or downloads it if one is already known. */
	fun command() {
		when {
			armed -> say(Component.literal("Update ${latest?.version} is already set to install when the game closes.").withStyle(ChatFormatting.YELLOW))
			downloaded != null -> offerInstall()
			latest != null -> download()
			else -> checkForUpdate(announceNothingNew = true)
		}
	}

	fun installNow() = arm(closeGame = true)

	fun installOnExit() = arm(closeGame = false)

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
				val release = UpdateChecker.newer(UpdateChecker.parseLatest(response.body(), minecraftVersion), currentVersion)
				if (release == null) {
					if (announceNothingNew) say(Component.literal("TunnelVision is up to date.").withStyle(ChatFormatting.GREEN))
					return@execute
				}
				latest = release
				say(
					Component.literal("TunnelVision ${release.version} is available (you have ${container.metadata.version.friendlyString}). ")
						.withStyle(ChatFormatting.YELLOW)
						.append(button("Update", "/tv update", "Download and verify ${release.jarName}")),
				)
			}
		}
	}

	private fun download() {
		val release = latest ?: return
		if (busy) return
		if (ownJar == null) {
			say(Component.literal("This TunnelVision is not running from a jar, so it cannot update itself.").withStyle(ChatFormatting.RED))
			return
		}
		busy = true
		say(Component.literal("Downloading TunnelVision ${release.version}...").withStyle(ChatFormatting.GRAY))
		val jar = fetch(release.jarUrl, HttpResponse.BodyHandlers.ofByteArray())
		val signature = fetch(release.signatureUrl, HttpResponse.BodyHandlers.ofString())
		jar.thenCombine(signature) { jarResponse, signatureResponse -> jarResponse to signatureResponse }.whenComplete { result, error ->
			val outcome = runCatching {
				if (error != null) throw error
				val (jarResponse, signatureResponse) = result
				check(jarResponse.statusCode() == 200 && signatureResponse.statusCode() == 200) {
					"HTTP ${jarResponse.statusCode()}/${signatureResponse.statusCode()}"
				}
				if (!UpdateVerifier.verify(jarResponse.body(), signatureResponse.body())) return@runCatching null
				Files.createDirectories(workDir)
				val path = workDir.resolve(release.jarName).also { Files.write(it, jarResponse.body()) }
				path to TunnelVisionUpdateHelper.sha256(jarResponse.body())
			}
			mc.execute {
				busy = false
				outcome.onFailure {
					TunnelVision.logger.warn("Update download failed", it)
					say(Component.literal("Downloading the update failed: ${it.message}").withStyle(ChatFormatting.RED))
				}.onSuccess { verified ->
					if (verified == null) {
						say(Component.literal("Update ${release.version} could not be verified and was not installed.").withStyle(ChatFormatting.RED))
					} else {
						downloaded = verified.first
						downloadedHash = verified.second
						offerInstall()
					}
				}
			}
		}
	}

	private fun <T> fetch(url: String, handler: HttpResponse.BodyHandler<T>) = client.sendAsync(
		HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(2)).header("User-Agent", USER_AGENT).GET().build(),
		handler,
	)

	private fun offerInstall() {
		val version = latest?.version ?: return
		say(
			Component.literal("Update $version is verified and ready. ").withStyle(ChatFormatting.GREEN)
				.append(button("Close & install", "/tv update now", "Closes the game and installs the update - start it again afterwards"))
				.append(Component.literal(" "))
				.append(button("Install on exit", "/tv update onexit", "Keeps playing; the update is installed when you close the game")),
		)
	}

	private fun arm(closeGame: Boolean) {
		val release = latest ?: return
		val newJar = downloaded ?: run {
			say(Component.literal("No verified update is downloaded yet - use /tv update first.").withStyle(ChatFormatting.RED))
			return
		}
		val oldJar = ownJar ?: return
		if (!armed) {
			armed = true
			val target = oldJar.resolveSibling(release.jarName)
			val hash = downloadedHash ?: return
			Runtime.getRuntime().addShutdownHook(Thread({ launchHelper(newJar, hash, oldJar, target, release.version) }, "TunnelVision updater"))
		}
		if (closeGame) {
			mc.stop()
		} else {
			say(Component.literal("Update ${release.version} will be installed when you close the game.").withStyle(ChatFormatting.GREEN))
		}
	}

	private fun launchHelper(newJar: Path, hash: String, oldJar: Path, target: Path, version: Version) {
		try {
			val classPath = workDir.resolve("helper")
			val classFile = classPath.resolve(HELPER_CLASS.replace('.', '/') + ".class")
			Files.createDirectories(classFile.parent)
			Updater::class.java.getResourceAsStream("/" + HELPER_CLASS.replace('.', '/') + ".class")!!.use {
				Files.copy(it, classFile, StandardCopyOption.REPLACE_EXISTING)
			}
			val java = ProcessHandle.current().info().command().orElse("java")
			ProcessBuilder(
				java, "-cp", classPath.toString(), HELPER_CLASS,
				ProcessHandle.current().pid().toString(), newJar.toString(), oldJar.toString(), target.toString(),
				resultFile.toString(), version.toString(), hash,
			).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start()
		} catch (e: Exception) {
			Files.createDirectories(resultFile.parent)
			Files.writeString(resultFile, "failed $version could not start the installer ($e)")
		}
	}

	private fun reportLastResult() {
		val text = runCatching { Files.readString(resultFile) }.getOrNull() ?: return
		runCatching { Files.delete(resultFile) }
		val (status, version, reason) = (text.trim().split(' ', limit = 3) + listOf("", "")).take(3)
		val message = if (status == "ok") {
			Component.literal("Updated to TunnelVision $version.").withStyle(ChatFormatting.GREEN)
		} else {
			Component.literal("Update to $version failed: $reason. The old version is still installed.").withStyle(ChatFormatting.RED)
		}
		pending += message
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

	private fun button(label: String, command: String, hover: String): MutableComponent =
		Component.literal("[$label]").withStyle { style ->
			style.withColor(ChatFormatting.AQUA).withBold(true)
				.withClickEvent(ClickEvent.RunCommand(command))
				.withHoverEvent(HoverEvent.ShowText(Component.literal(hover)))
		}
}
