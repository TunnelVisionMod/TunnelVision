package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudManager
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSounds

class GeneralConfig {
	@Expose
	@JvmField
	@ConfigOption(
		name = "Check for Updates",
		desc = "Check GitHub for a new TunnelVision release when the game starts and offer it in chat. Nothing is downloaded until you click §bUpdate§7, and only releases signed by the TunnelVision maintainer are installed. Also available via §b/tv update§7.",
	)
	@ConfigEditorBoolean
	var checkForUpdates = false

	@JvmField
	@ConfigOption(name = "HUD Editor", desc = "Move HUD widgets. Also available via §b/tv hud§7.")
	@ConfigEditorButton(buttonText = "Open")
	val openHudEditor = Runnable { HudManager.openEditor() }

	@JvmField
	@ConfigOption(
		name = "Title Sounds",
		desc = "Pick a sound and volume for every title and alert. Custom sounds must be §b.ogg§7 files - convert MP3s online first. Also available via §b/tv sounds§7.",
	)
	@ConfigEditorButton(buttonText = "Open")
	val openTitleSounds = Runnable { TitleSounds.openScreen() }

	@Expose
	@JvmField
	@ConfigOption(name = "Bazaar Price", desc = "Which Bazaar price to value gemstones and corpse loot at.")
	@ConfigEditorDropdown
	var bazaarPrice = BazaarPriceType.SELL_OFFER
}
