package io.github.tunnelvisionmod.tunnelvision.core.sound

import io.github.notenoughupdates.moulconfig.managed.ManagedDataFile
import io.github.tunnelvisionmod.tunnelvision.TunnelVision
import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import java.nio.file.Files
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Util
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs

/**
 * The sound every alert plays, set per [TitleSound] in the Title Sounds menu and kept in
 * `sounds.json`. A custom file that cannot be played falls back to the default sound and says so
 * once in chat, so a broken file is never just silence.
 */
object TitleSounds {
	const val MAX_VOLUME = 2f

	private lateinit var data: ManagedDataFile<SoundData>
	private val warned = mutableSetOf<String>()

	val folder: Path get() = FabricLoader.getInstance().configDir.resolve("${TunnelVision.MOD_ID}/sounds")

	fun load() {
		val file = FabricLoader.getInstance().configDir.resolve("${TunnelVision.MOD_ID}/sounds.json").toFile()
		data = ManagedDataFile.create(file, SoundData::class.java) {
			loadFailed = { _, e -> TunnelVision.logger.error("Failed to load title sounds", e) }
			saveFailed = { _, e -> TunnelVision.logger.error("Failed to save title sounds", e) }
		}
	}

	fun save() = data.saveToFile()

	fun settingOf(sound: TitleSound): SoundSetting =
		data.instance.sounds.getOrPut(sound.name) { SoundSetting(enabled = sound.enabledByDefault) }

	/** Plays [sound] if it is switched on. */
	fun play(sound: TitleSound) {
		if (settingOf(sound).enabled) test(sound)
	}

	/** Plays [sound] whether or not it is switched on - the menu's test button. */
	fun test(sound: TitleSound) {
		val setting = settingOf(sound)
		val file = setting.file
		if (file != null) {
			val master = mc.options.getFinalSoundSourceVolume(SoundSource.MASTER)
			if (OggPlayer.play(folder.resolve(file), setting.volume * master)) return
			warnOnce(file)
		}
		playDefault(sound.defaultSound, setting.volume)
	}

	fun reset(sound: TitleSound) {
		settingOf(sound).file?.let { OggPlayer.forget(folder.resolve(it)) }
		data.instance.sounds[sound.name] = SoundSetting(enabled = sound.enabledByDefault)
		save()
	}

	/**
	 * Opens the system file picker for an .ogg file and, once one is picked, copies it into the
	 * sounds folder and sets it for [sound]. The picker blocks until closed, so it runs off the
	 * render thread and [onDone] is called back on it.
	 */
	fun choose(sound: TitleSound, onDone: (Component) -> Unit) {
		Thread({
			val picked = pickFile()
			mc.execute { onDone(picked?.let { assign(sound, it) } ?: return@execute) }
		}, "TunnelVision sound picker").apply { isDaemon = true }.start()
	}

	fun openFolder() {
		Files.createDirectories(folder)
		Util.getPlatform().openPath(folder)
	}

	private fun assign(sound: TitleSound, source: Path): Component {
		if (!SoundFiles.isOgg(source)) {
			return Component.literal("Only .ogg files work - convert it with an online MP3 to OGG converter first.").withStyle(ChatFormatting.RED)
		}
		val name = try {
			SoundFiles.import(source, folder)
		} catch (e: Exception) {
			TunnelVision.logger.warn("Failed to copy sound $source", e)
			return Component.literal("Could not copy ${source.fileName}: ${e.message}").withStyle(ChatFormatting.RED)
		}
		OggPlayer.forget(folder.resolve(name))
		warned -= name
		val setting = settingOf(sound)
		setting.file = name
		setting.enabled = true
		save()
		test(sound)
		return Component.literal("${sound.displayName} now plays $name").withStyle(ChatFormatting.GREEN)
	}

	private fun pickFile(): Path? = MemoryStack.stackPush().use { stack ->
		val filters = stack.mallocPointer(1)
		filters.put(stack.UTF8("*.${SoundFiles.EXTENSION}"))
		filters.flip()
		TinyFileDialogs.tinyfd_openFileDialog("Choose a sound (.ogg)", null, filters, "OGG sound", false)?.let { Path.of(it) }
	}

	private fun warnOnce(file: String) {
		if (!warned.add(file)) return
		ChatUtils.send(Component.literal("Could not play sound $file - playing the default instead. Is it a valid .ogg file?").withStyle(ChatFormatting.RED))
	}

	private fun playDefault(sound: DefaultSound, volume: Float) {
		val instance = when (sound) {
			DefaultSound.NONE -> return
			DefaultSound.PLING -> SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1f, volume)
			DefaultSound.VILLAGER_NO -> SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1f, volume)
			DefaultSound.XP_ORB -> SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, volume)
		}
		mc.soundManager.play(instance)
	}

	fun openScreen() {
		mc.schedule { Compat.setScreen(TitleSoundsScreen(Compat.screen)) }
	}
}
