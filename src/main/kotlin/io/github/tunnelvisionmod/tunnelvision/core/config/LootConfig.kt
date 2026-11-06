package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LootConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Loot Mode", desc = "§eLapis Only§7: only Lapis corpses. §eAdaptive§7: Lapis and Vanguard always, Umber and Tungsten only in a shaft worth mining. §eCrystal Hunter§7: Umber and Tungsten always, until you carry every crystal a corpse can drop, then like Adaptive. Vanguards are skipped throughout unless §eVanguard Worth It §7is on.")
	@ConfigEditorDropdown
	var lootMode = LootMode.LAPIS_ONLY

	@Expose
	@JvmField
	@ConfigOption(
		name = "Crystal Hunting Speed",
		desc = "§bRequires Loot Mode Crystal Hunter.\n§7How hard the verdict pushes for crystals while you still miss some. §eFast§7: more DON'T MINE, so you see more shafts and get crystals sooner. §eSlow§7: more MINE, crystals take longer.",
	)
	@ConfigEditorDropdown
	var crystalHuntingSpeed = CrystalHuntingSpeed.NORMAL

	@Expose
	@JvmField
	@ConfigOption(
		name = "Vanguard Worth It",
		desc = "Tells you to open Vanguard corpses while one pays for its Skeleton Key at current prices. Off never suggests them. The verdict never counts Vanguards either way.",
	)
	@ConfigEditorBoolean
	var openVanguards = true

	@Expose
	@JvmField
	@ConfigOption(
		name = "RNG Meter Bonus",
		desc = "Extra Frozen Corpse RNG meter XP from your attribute, in percent. Used by §eMineshaft Value §7and §eCorpse Tracker§7.",
	)
	@ConfigEditorSlider(minValue = 0f, maxValue = 10f, minStep = 1f)
	var rngMeterBonus = 0
}
