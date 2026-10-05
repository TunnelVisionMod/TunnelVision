package io.github.tunnelvisionmod.tunnelvision.data.crystals

import net.minecraft.ChatFormatting

/**
 * The crystals that can be waiting to be forged.
 *
 * Colours match the matching MineshaftType entries, so a Jasper crystal reads the same colour here
 * as a Jasper crystal mineshaft does.
 */
enum class CrystalType(val displayName: String, val color: ChatFormatting) {
	JASPER("Jasper", ChatFormatting.LIGHT_PURPLE),
	OPAL("Opal", ChatFormatting.WHITE),
	AQUAMARINE("Aquamarine", ChatFormatting.DARK_BLUE),
	CITRINE("Citrine", ChatFormatting.YELLOW),
	PERIDOT("Peridot", ChatFormatting.DARK_GREEN),
	ONYX("Onyx", ChatFormatting.DARK_GRAY),
	RUBY("Ruby", ChatFormatting.RED),
	;

	companion object {
		private val byName = entries.associateBy { it.displayName.lowercase() }

		/** Accepts `Jasper` as well as `Jasper Crystal`, with any decoration stripped. */
		fun byDisplayName(name: String): CrystalType? {
			val cleaned = name.trim().lowercase().removeSuffix("crystal").trim()
			return byName[cleaned]
		}

	}
}
