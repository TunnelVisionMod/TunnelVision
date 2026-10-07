package io.github.tunnelvisionmod.tunnelvision.features.mining.pickaxe

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PickaxeAbilityConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Track your pickaxe ability cooldown and notify you when it is ready again.")
	@ConfigEditorBoolean
	var enabled = false

	@Expose
	@JvmField
	@ConfigOption(name = "Ready Title", desc = "Show a title when your pickaxe ability is ready.")
	@ConfigEditorBoolean
	var showTitle = true


	@Expose
	@JvmField
	@ConfigOption(name = "Cooldown Timer", desc = "Show a HUD widget with the remaining cooldown.")
	@ConfigEditorBoolean
	var showWidget = true
}
