package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CorpseTrackerConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Price every item a corpse drops and track what it made.")
	@ConfigEditorBoolean
	var enabled = false

	@Expose
	@JvmField
	@ConfigOption(
		name = "Replace Loot Message",
		desc = "Replace the corpse loot message with the same list priced per item, the RNG meter and the profit after the key. §7Uses §eBazaar Price§7.",
	)
	@ConfigEditorBoolean
	var replaceChat = true

	@Expose
	@JvmField
	@ConfigOption(name = "Widget", desc = "Show a HUD widget with the profit per corpse type and overall. Reset it with §e/tv corpses reset§7.")
	@ConfigEditorBoolean
	var widget = true

	@Expose
	@JvmField
	@ConfigOption(name = "RNG Meter Line", desc = "Count the corpse's RNG meter progress towards a Shattered Locket as part of the profit.")
	@ConfigEditorBoolean
	var showMeter = true
}
