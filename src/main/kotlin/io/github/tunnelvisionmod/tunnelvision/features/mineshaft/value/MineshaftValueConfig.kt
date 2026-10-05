package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.value

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MineshaftValueConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Tell you if the gemstones are worth mining when you enter, and what to do first. §7Uses §eLoot §7and §eBazaar Price§7.")
	@ConfigEditorBoolean
	var enabled = false

	@Expose
	@JvmField
	@ConfigOption(name = "Verdict Title", desc = "Show MINE or DON'T MINE as a title when you enter.")
	@ConfigEditorBoolean
	var verdictTitle = true

	@Expose
	@JvmField
	@ConfigOption(name = "Chat Message", desc = "Also send the verdict with the Fine gem price to chat.")
	@ConfigEditorBoolean
	var sendChat = true

	@Expose
	@JvmField
	@ConfigOption(
		name = "To-Do Widget",
		desc = "Show the verdict with what is left to do: corpses worth looting, the fossil and the crystal of a crystal shaft. Corpses without a key don't count. §bThe crystal needs /hotm opened once.",
	)
	@ConfigEditorBoolean
	var todoWidget = true
}
