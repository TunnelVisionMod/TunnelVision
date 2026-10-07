package io.github.tunnelvisionmod.tunnelvision.core.sound

import java.nio.file.Path
import org.lwjgl.openal.AL10
import org.lwjgl.openal.ALC10
import org.lwjgl.stb.STBVorbis
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.libc.LibCStdlib

/**
 * Plays .ogg files from disk through Minecraft's own OpenAL context.
 *
 * Minecraft only loads sounds from resource packs, so custom files are decoded with the stb_vorbis
 * and played with the OpenAL that ship with the game. Decoded files stay in OpenAL buffers until
 * [forget] or until the sound engine restarts - a restart destroys the context, and with it every
 * buffer and source, which the context check below notices. Render thread only.
 */
object OggPlayer {
	private const val MAX_GAIN = 2f

	private var context = 0L
	private val buffers = HashMap<Path, Int>()
	private val sources = mutableListOf<Int>()

	/** False if the file could not be decoded or there is no sound device. */
	fun play(path: Path, gain: Float): Boolean {
		val current = ALC10.alcGetCurrentContext()
		if (current == 0L) return false
		if (current != context) {
			context = current
			buffers.clear()
			sources.clear()
		}
		releaseFinished()
		// The error flag is shared with Minecraft's own calls, so clear whatever it left behind first.
		AL10.alGetError()
		val buffer = buffers[path] ?: load(path)?.also { buffers[path] = it } ?: return false
		val source = AL10.alGenSources()
		if (AL10.alGetError() != AL10.AL_NO_ERROR) return false
		AL10.alSourcei(source, AL10.AL_BUFFER, buffer)
		AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE)
		AL10.alSource3f(source, AL10.AL_POSITION, 0f, 0f, 0f)
		AL10.alSourcef(source, AL10.AL_MAX_GAIN, MAX_GAIN)
		AL10.alSourcef(source, AL10.AL_GAIN, gain.coerceIn(0f, MAX_GAIN))
		AL10.alSourcePlay(source)
		sources += source
		return AL10.alGetError() == AL10.AL_NO_ERROR
	}

	/** Drops a decoded file, so the next play reads it from disk again. */
	fun forget(path: Path) {
		if (ALC10.alcGetCurrentContext() != context) return
		val buffer = buffers.remove(path) ?: return
		val playing = sources.filter { AL10.alGetSourcei(it, AL10.AL_BUFFER) == buffer }
		playing.forEach { AL10.alSourceStop(it) }
		releaseFinished()
		AL10.alDeleteBuffers(buffer)
	}

	private fun releaseFinished() {
		val done = sources.filter { AL10.alGetSourcei(it, AL10.AL_SOURCE_STATE) == AL10.AL_STOPPED }
		done.forEach { AL10.alDeleteSources(it) }
		sources -= done.toSet()
	}

	private fun load(path: Path): Int? = MemoryStack.stackPush().use { stack ->
		val channels = stack.mallocInt(1)
		val sampleRate = stack.mallocInt(1)
		val pcm = STBVorbis.stb_vorbis_decode_filename(path.toString(), channels, sampleRate) ?: return null
		try {
			val format = when (channels[0]) {
				1 -> AL10.AL_FORMAT_MONO16
				2 -> AL10.AL_FORMAT_STEREO16
				else -> return null
			}
			val buffer = AL10.alGenBuffers()
			AL10.alBufferData(buffer, format, pcm, sampleRate[0])
			if (AL10.alGetError() != AL10.AL_NO_ERROR) {
				AL10.alDeleteBuffers(buffer)
				return null
			}
			buffer
		} finally {
			LibCStdlib.free(pcm)
		}
	}
}
