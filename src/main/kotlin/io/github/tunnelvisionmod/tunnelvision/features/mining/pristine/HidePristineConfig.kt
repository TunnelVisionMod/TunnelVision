package io.github.tunnelvisionmod.tunnelvision.features.mining.pristine

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HidePristineConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Hide Pristine messages from chat.")
	@ConfigEditorBoolean
	var enabled = false
}
