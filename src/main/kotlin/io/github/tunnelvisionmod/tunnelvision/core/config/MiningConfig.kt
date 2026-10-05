package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.tunnelvisionmod.tunnelvision.features.mining.effects.MiningEffectsConfig
import io.github.tunnelvisionmod.tunnelvision.features.mining.lantern.LanternReminderConfig
import io.github.tunnelvisionmod.tunnelvision.features.mining.pickaxe.PickaxeAbilityConfig
import io.github.tunnelvisionmod.tunnelvision.features.mining.stats.MiningStatsConfig

class MiningConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Pickaxe Ability", desc = "Notifications and timer for your pickaxe ability cooldown.")
	@Accordion
	var pickaxeAbility = PickaxeAbilityConfig()

	@Expose
	@JvmField
	@ConfigOption(name = "Mining Effects", desc = "Show the remaining time of Cold Resistance IV and Filet O' Fortune while mining.")
	@Accordion
	var miningEffects = MiningEffectsConfig()

	@Expose
	@JvmField
	@ConfigOption(name = "Mining Stats", desc = "Your current mining buffs at a glance.")
	@Accordion
	var miningStats = MiningStatsConfig()

	@Expose
	@JvmField
	@ConfigOption(name = "Lantern Reminder", desc = "Remind you to place your lantern and warn when it expires.")
	@Accordion
	var lanternReminder = LanternReminderConfig()

	@Expose
	@JvmField
	@Category(name = "Pristine", desc = "Pristine procs")
	var pristine = PristineConfig()
}
