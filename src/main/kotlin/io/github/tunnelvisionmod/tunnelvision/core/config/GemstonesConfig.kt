package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.routes.GemstoneRoutesConfig
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.value.MineshaftValueConfig

class GemstonesConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Mineshaft Value", desc = "Tell you if the gemstones are worth mining, and what to do before mining them.")
	@Accordion
	var mineshaftValue = MineshaftValueConfig()

	@Expose
	@JvmField
	@ConfigOption(name = "Gemstone Routes", desc = "Route through the best gemstone veins of the mineshaft.")
	@Accordion
	var gemstoneRoutes = GemstoneRoutesConfig()
}
