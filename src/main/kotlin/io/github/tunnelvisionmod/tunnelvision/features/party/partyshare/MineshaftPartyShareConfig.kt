package io.github.tunnelvisionmod.tunnelvision.features.party.partyshare

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MineshaftPartyShareConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Send the mineshaft type and corpses to party chat.")
	@ConfigEditorBoolean
	var enabled = false
}
