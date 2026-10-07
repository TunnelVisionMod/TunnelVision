package io.github.tunnelvisionmod.tunnelvision.core.sound

/** The vanilla sound an alert plays until a custom one is chosen. */
enum class DefaultSound {
	NONE,
	PLING,
	VILLAGER_NO,
	XP_ORB,
}

/**
 * Every alert the mod can play a sound for, one row each in the Title Sounds menu.
 *
 * A new title is one new entry here: [io.github.tunnelvisionmod.tunnelvision.utils.Titles.show]
 * asks for one, and the menu lists whatever is in this enum. The entry name is the key in
 * `sounds.json`, so renaming one resets that row.
 */
enum class TitleSound(val displayName: String, val defaultSound: DefaultSound, val enabledByDefault: Boolean) {
	FORGE_READY("Forge Ready", DefaultSound.PLING, true),
	FORGE_CRYSTAL("Forge Crystal", DefaultSound.PLING, true),
	CRYSTALS_FULL("Crystals Full", DefaultSound.PLING, true),
	PICKAXE_ABILITY("Pickaxe Ability Ready", DefaultSound.XP_ORB, true),
	MINING_EFFECT_EXPIRED("Mining Effect Expired", DefaultSound.NONE, false),
	LANTERN_EXPIRED("Lantern Expired", DefaultSound.PLING, true),
	WRONG_GEAR("Wrong Gear", DefaultSound.VILLAGER_NO, true),
	MINESHAFT_TYPE("Mineshaft Type", DefaultSound.NONE, false),
	MINESHAFT_VERDICT("Mineshaft Verdict", DefaultSound.NONE, false),
	BLUE_CHEESE("Blue Cheese", DefaultSound.VILLAGER_NO, true),
	SHARED_MINESHAFT("Shared Mineshaft", DefaultSound.PLING, true),
}
