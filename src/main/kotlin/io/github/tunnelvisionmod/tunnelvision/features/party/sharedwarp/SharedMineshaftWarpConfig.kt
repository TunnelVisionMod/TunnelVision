package io.github.tunnelvisionmod.tunnelvision.features.party.sharedwarp

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.tunnelvisionmod.tunnelvision.utils.KeyUtils

class SharedMineshaftWarpConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Press a key to warp into a mineshaft shared in party chat.")
	@ConfigEditorBoolean
	var enabled = false

	@Expose
	@JvmField
	@ConfigOption(name = "Warp Key", desc = "Key to warp into the shared mineshaft.")
	@ConfigEditorKeybind(defaultKey = KeyUtils.NONE)
	var warpKey = KeyUtils.NONE

	@Expose
	@JvmField
	@ConfigOption(name = "Time Window", desc = "Seconds the key works after a share. 0 = no limit.")
	@ConfigEditorSlider(minValue = 0f, maxValue = 20f, minStep = 1f)
	var windowSeconds = 10

	@Expose
	@JvmField
	@ConfigOption(name = "Title", desc = "Show a title with the mineshaft type and the key to press.")
	@ConfigEditorBoolean
	var showTitle = true

}
