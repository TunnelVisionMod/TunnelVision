package io.github.tunnelvisionmod.tunnelvision.features.party.commands

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PartyCommandsConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "React to commands in party chat. §bRequires being party leader.")
	@ConfigEditorBoolean
	var enabled = false

	@Expose
	@JvmField
	@ConfigOption(name = "Party Transfer", desc = "Transfer the party to whoever types !pt or !ptme.")
	@ConfigEditorBoolean
	var transfer = true

	@Expose
	@JvmField
	@ConfigOption(name = "Party Warp", desc = "Warp the party when someone types !w or !warp.")
	@ConfigEditorBoolean
	var warp = true
}
