package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.tunnelvisionmod.tunnelvision.features.mining.pristine.HidePristineConfig
import io.github.tunnelvisionmod.tunnelvision.features.mining.pristine.WrongGearConfig

class PristineConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Wrong Gear", desc = "Warn when a Pristine proc is lower than your gear should give.")
	@Accordion
	var wrongGear = WrongGearConfig()

	@Expose
	@JvmField
	@ConfigOption(name = "Hide Pristine Messages", desc = "Hide the \"PRISTINE! You found ...\" spam from chat.")
	@Accordion
	var hidePristineMessages = HidePristineConfig()
}
