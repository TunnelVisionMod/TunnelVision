package io.github.tunnelvisionmod.tunnelvision.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CrystalNotificationsConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Notify you about crystals waiting to be forged.")
	@ConfigEditorBoolean
	var enabled = false

	@Expose
	@JvmField
	@ConfigOption(name = "Available Title", desc = "Show a title when you enter the Dwarven Mines with a crystal to forge.")
	@ConfigEditorBoolean
	var availableTitle = true

	@Expose
	@JvmField
	@ConfigOption(name = "Crystals Full Message", desc = "Tell you in chat when you carry every crystal and the forge is full.")
	@ConfigEditorBoolean
	var fullMessage = true

	@Expose
	@JvmField
	@ConfigOption(name = "Widget", desc = "Show a HUD widget with the crystals you carry.")
	@ConfigEditorBoolean
	var widget = true

	@Expose
	@JvmField
	@ConfigOption(
		name = "Forge Priority",
		desc = "In the widget, order the crystals you carry by what forging them is worth and mark the ones to put in your free forge slots.",
	)
	@ConfigEditorBoolean
	var forgePriority = true

	@Expose
	@JvmField
	@ConfigOption(
		name = "Forge Gems",
		desc = "Next to each crystal in the widget, show the cheaper gems to forge its Perfect with: 400x Fine on a buy order or 5x Flawless insta-bought. §bRequires §eForge Priority§b.",
	)
	@ConfigEditorBoolean
	var forgeGems = true

	@Expose
	@JvmField
	@ConfigOption(name = "Sound", desc = "Play a sound with these notifications.")
	@ConfigEditorBoolean
	var playSound = true
}
