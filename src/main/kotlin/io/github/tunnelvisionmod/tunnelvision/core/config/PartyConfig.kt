package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.tunnelvisionmod.tunnelvision.features.party.commands.PartyCommandsConfig
import io.github.tunnelvisionmod.tunnelvision.features.party.partyshare.MineshaftPartyShareConfig
import io.github.tunnelvisionmod.tunnelvision.features.party.sharedwarp.SharedMineshaftWarpConfig

class PartyConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Party Commands", desc = "Run !ptme and !warp from party chat.")
	@Accordion
	var partyCommands = PartyCommandsConfig()

	@Expose
	@JvmField
	@ConfigOption(name = "Shared Mineshaft Warp", desc = "Warp into a shared mineshaft with one key press.")
	@Accordion
	var sharedMineshaftWarp = SharedMineshaftWarpConfig()

	@Expose
	@JvmField
	@ConfigOption(name = "Party Share", desc = "Share the mineshaft type and corpses with your party.")
	@Accordion
	var partyShare = MineshaftPartyShareConfig()
}
