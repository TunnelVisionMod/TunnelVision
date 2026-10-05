package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LootConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Loot Mode", desc = "§eLapis Only§7: only Lapis corpses. §eNormal§7: all corpses in a shaft worth mining, otherwise Lapis + Vanguard. §eGreedy§7: all corpses until crystals and forge are full, then like Normal. Vanguards are skipped throughout unless §eOpen Vanguard Corpses §7is on.")
	@ConfigEditorDropdown
	var lootMode = LootMode.LAPIS_ONLY

	@Expose
	@JvmField
	@ConfigOption(
		name = "Open Vanguard Corpses",
		desc = "Whether you buy Skeleton Keys to open Vanguard corpses. Off makes them worth nothing in every loot mode, Greedy included, and a Fairy mineshaft worthless.",
	)
	@ConfigEditorBoolean
	var openVanguards = true
}
