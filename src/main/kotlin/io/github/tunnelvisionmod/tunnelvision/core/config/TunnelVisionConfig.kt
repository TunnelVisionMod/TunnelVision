package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.common.text.StructuredText

class TunnelVisionConfig : Config() {
	override fun getTitle(): StructuredText = StructuredText.of("TunnelVision")

	@Expose
	@JvmField
	@Category(name = "General", desc = "HUD and prices")
	var general = GeneralConfig()

	@Expose
	@JvmField
	@Category(name = "Mining", desc = "Mining features anywhere in the mines")
	var mining = MiningConfig()

	@Expose
	@JvmField
	@Category(name = "Forge", desc = "The Forge and your crystals")
	var forge = ForgeConfig()

	@Expose
	@JvmField
	@Category(name = "Party", desc = "Party commands and sharing mineshafts")
	var party = PartyConfig()

	@Expose
	@JvmField
	@Category(name = "Mineshaft", desc = "Features inside of Mineshafts")
	var mineshaft = MineshaftConfig()

	@Expose
	@JvmField
	@Category(name = "Dev", desc = "Don't touch")
	var dev = DevConfig()
}
