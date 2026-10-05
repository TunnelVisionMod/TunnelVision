package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.cutloose

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CutLooseTrackerConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Show a HUD widget with your Cut Loose kills in this mineshaft.")
	@ConfigEditorBoolean
	var enabled = false
}
