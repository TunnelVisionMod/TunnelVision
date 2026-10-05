package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DevConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Debug Mode", desc = "Log extra information for development.")
	@ConfigEditorBoolean
	var debugMode = false
}
