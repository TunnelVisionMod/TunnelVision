package io.github.tunnelvisionmod.tunnelvision.data.crystals

import net.minecraft.ChatFormatting

/**
 * The crystals that can be waiting to be forged.
 *
 * Colours match the matching MineshaftType entries, so a Jasper crystal reads the same colour here
 * as a Jasper crystal mineshaft does. [fromCorpses] is false for Jasper and Ruby, which only come
 * from their crystal mineshafts and never from a corpse.
 */
enum class CrystalType(val displayName: String, val color: ChatFormatting, val fromCorpses: Boolean) {
	JASPER("Jasper", ChatFormatting.LIGHT_PURPLE, false),
	OPAL("Opal", ChatFormatting.WHITE, true),
	AQUAMARINE("Aquamarine", ChatFormatting.DARK_BLUE, true),
	CITRINE("Citrine", ChatFormatting.YELLOW, true),
	PERIDOT("Peridot", ChatFormatting.DARK_GREEN, true),
	ONYX("Onyx", ChatFormatting.DARK_GRAY, true),
	RUBY("Ruby", ChatFormatting.RED, false),
	;

	companion object {
		val CORPSE_DROPS: Set<CrystalType> = entries.filter { it.fromCorpses }.toSet()

		private val byName = entries.associateBy { it.displayName.lowercase() }

		/** Accepts `Jasper` as well as `Jasper Crystal`, with any decoration stripped. */
		fun byDisplayName(name: String): CrystalType? {
			val cleaned = name.trim().lowercase().removeSuffix("crystal").trim()
			return byName[cleaned]
		}

	}
}
