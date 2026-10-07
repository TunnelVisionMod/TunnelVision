package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BlueCheeseCorpseLockConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Block looting corpses unless you hold your Blue Cheese drill. §bOnly active with a Blue Cheese drill in your inventory.")
	@ConfigEditorBoolean
	var enabled = false

	@Expose
	@JvmField
	@ConfigOption(name = "Warning Title", desc = "Show a red \"Blue Cheese!\" title when a click is blocked.")
	@ConfigEditorBoolean
	var showTitle = true

}
