package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.cutloose.CutLooseTrackerConfig
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.type.MineshaftTypeConfig

class MineshaftConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Mineshaft Type", desc = "Show the mineshaft type when you enter.")
	@Accordion
	var mineshaftType = MineshaftTypeConfig()

	@Expose
	@JvmField
	@ConfigOption(name = "Cut Loose Tracker", desc = "Track your kills for the Cut Loose perk.")
	@Accordion
	var cutLooseTracker = CutLooseTrackerConfig()

	@Expose
	@JvmField
	@Category(name = "Corpses", desc = "Corpse loot, waypoints and to-dos")
	var corpses = CorpsesConfig()

	@Expose
	@JvmField
	@Category(name = "Gemstones", desc = "Mineshaft value and gemstone routes")
	var gemstones = GemstonesConfig()
}
