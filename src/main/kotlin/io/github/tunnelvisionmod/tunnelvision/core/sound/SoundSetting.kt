package io.github.tunnelvisionmod.tunnelvision.core.sound

import com.google.gson.annotations.Expose

/** [file] is a name inside the sounds folder, or null for the alert's default sound. */
data class SoundSetting(
	@field:Expose var enabled: Boolean = true,
	@field:Expose var file: String? = null,
	@field:Expose var volume: Float = 1f,
)

class SoundData {
	@field:Expose
	var sounds: MutableMap<String, SoundSetting> = mutableMapOf()
}
