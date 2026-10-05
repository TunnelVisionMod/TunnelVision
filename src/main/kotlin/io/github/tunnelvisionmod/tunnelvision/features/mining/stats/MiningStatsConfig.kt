package io.github.tunnelvisionmod.tunnelvision.features.mining.stats

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MiningStatsConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Show a HUD widget with mining stats for your current island.")
	@ConfigEditorBoolean
	var enabled = false

	@Expose
	@JvmField
	@ConfigOption(name = "Sky Mall", desc = "Your current Sky Mall buff. §bMay require opening /hotm once.")
	@ConfigEditorBoolean
	var skyMall = true

	@Expose
	@JvmField
	@ConfigOption(name = "Mineshaft Mayhem", desc = "Your Mineshaft Mayhem buff.")
	@ConfigEditorBoolean
	var mayhem = true

	@Expose
	@JvmField
	@ConfigOption(name = "Mining Event", desc = "Fortunate Freezing or Better Together bonus.")
	@ConfigEditorBoolean
	var miningEvent = true

	@Expose
	@JvmField
	@ConfigOption(name = "Cold Resistance", desc = "Your Cold Resistance. §bRequires Cold Resistance in the Stats tab widget (/widget).")
	@ConfigEditorBoolean
	var coldResistance = true
}
