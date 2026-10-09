package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses.BlueCheeseCorpseLockConfig
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses.CorpseTrackerConfig
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses.MineshaftWaypointsConfig

class CorpsesConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Loot", desc = "Which corpses are worth looting. Used by §eMineshaft Value§7, §eMineshaft Waypoints §7and §eGemstone Routes§7.")
	@Accordion
	var loot = LootConfig()

	@Expose
	@JvmField
	@ConfigOption(name = "Blue Cheese Corpse Lock", desc = "Only allow looting corpses with your Blue Cheese drill.")
	@Accordion
	var blueCheeseCorpseLock = BlueCheeseCorpseLockConfig()

	@Expose
	@JvmField
	@ConfigOption(name = "Mineshaft Waypoints", desc = "Waypoints for corpses and the fossil.")
	@Accordion
	var mineshaftWaypoints = MineshaftWaypointsConfig()

	@Expose
	@JvmField
	@ConfigOption(name = "Corpse Tracker", desc = "Price what a corpse dropped and track the profit per corpse type.")
	@Accordion
	var corpseTracker = CorpseTrackerConfig()
}
