package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudManager

class GeneralConfig {
	@JvmField
	@ConfigOption(name = "HUD Editor", desc = "Move HUD widgets. Also available via §b/tv hud§7.")
	@ConfigEditorButton(buttonText = "Open")
	val openHudEditor = Runnable { HudManager.openEditor() }

	@Expose
	@JvmField
	@ConfigOption(name = "Bazaar Price", desc = "Which Bazaar price to value gemstones and corpse loot at.")
	@ConfigEditorDropdown
	var bazaarPrice = BazaarPriceType.SELL_OFFER
}
