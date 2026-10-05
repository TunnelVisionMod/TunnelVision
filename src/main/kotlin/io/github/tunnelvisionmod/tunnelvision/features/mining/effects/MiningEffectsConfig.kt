package io.github.tunnelvisionmod.tunnelvision.features.mining.effects

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MiningEffectsConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Show a HUD widget with the remaining time of Cold Resistance IV and Filet O' Fortune on mining islands.")
	@ConfigEditorBoolean
	var enabled = false

	@Expose
	@JvmField
	@ConfigOption(name = "Potion Affinity", desc = "Your Potion Affinity accessory.")
	@ConfigEditorDropdown
	var potionAffinity = PotionAffinity.NONE

	@Expose
	@JvmField
	@ConfigOption(name = "Expired Title", desc = "Show a title when one of the effects runs out while you are on a mining island.")
	@ConfigEditorBoolean
	var showExpiredTitle = true
}
