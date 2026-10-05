package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.routes

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GemstoneRoutesConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Show a route through the best gemstone veins. Jasper splits the route when someone is warped in.")
	@ConfigEditorBoolean
	var enabled = false

	@Expose
	@JvmField
	@ConfigOption(
		name = "Start",
		desc = "§eOn Entry§7: as soon as you are in the mineshaft. §eAfter To-Dos§7: once your to-dos are done and the mineshaft is worth mining. §7Uses §eLoot §7and §eBazaar Price§7.",
	)
	@ConfigEditorDropdown
	var start = RouteStart.AFTER_TODOS

	@Expose
	@JvmField
	@ConfigOption(name = "Status Widget", desc = "While a route waits, show why: to-dos left or not worth mining. §bOnly with §eStart §bset to §eAfter To-Dos§b.")
	@ConfigEditorBoolean
	var statusWidget = true

	@Expose
	@JvmField
	@ConfigOption(name = "Advance Distance", desc = "How close you have to get to a vein in blocks before the route shows the next one.")
	@ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
	var advanceDistance = 2
}

enum class RouteStart(private val label: String) {
	ON_ENTRY("On Entry"),
	AFTER_TODOS("After To-Dos");

	override fun toString() = label
}
