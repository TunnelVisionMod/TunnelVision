package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import net.minecraft.ChatFormatting

/**
 * The Glacite Mineshaft variants. [code] is the tag Hypixel puts into the server id at the bottom
 * of the scoreboard, e.g. `09/29/25 mTOPA_1x`.
 */
enum class MineshaftType(val code: String, val displayName: String, val color: ChatFormatting) {
	TOPAZ_1("TOPA_1", "Topaz 1", ChatFormatting.YELLOW),
	TOPAZ_2("TOPA_2", "Topaz 2", ChatFormatting.YELLOW),
	SAPPHIRE_1("SAPP_1", "Sapphire 1", ChatFormatting.BLUE),
	SAPPHIRE_2("SAPP_2", "Sapphire 2", ChatFormatting.BLUE),
	AMETHYST_1("AMET_1", "Amethyst 1", ChatFormatting.DARK_PURPLE),
	AMETHYST_2("AMET_2", "Amethyst 2", ChatFormatting.DARK_PURPLE),
	AMBER_1("AMBE_1", "Amber 1", ChatFormatting.GOLD),
	AMBER_2("AMBE_2", "Amber 2", ChatFormatting.GOLD),
	JADE_1("JADE_1", "Jade 1", ChatFormatting.GREEN),
	JADE_2("JADE_2", "Jade 2", ChatFormatting.GREEN),
	TITANIUM("TITA_1", "Titanium", ChatFormatting.GRAY),
	UMBER("UMBE_1", "Umber", ChatFormatting.GOLD),
	TUNGSTEN("TUNG_1", "Tungsten", ChatFormatting.DARK_GRAY),
	VANGUARD("FAIR_1", "Vanguard", ChatFormatting.WHITE),
	RUBY_1("RUBY_1", "Ruby 1", ChatFormatting.RED),
	RUBY_2("RUBY_2", "Ruby 2", ChatFormatting.RED),
	RUBY_CRYSTAL("RUBY_C", "Ruby Crystal", ChatFormatting.RED),
	ONYX_1("ONYX_1", "Onyx 1", ChatFormatting.DARK_GRAY),
	ONYX_2("ONYX_2", "Onyx 2", ChatFormatting.DARK_GRAY),
	ONYX_CRYSTAL("ONYX_C", "Onyx Crystal", ChatFormatting.DARK_GRAY),
	AQUAMARINE_1("AQUA_1", "Aquamarine 1", ChatFormatting.DARK_BLUE),
	AQUAMARINE_2("AQUA_2", "Aquamarine 2", ChatFormatting.DARK_BLUE),
	AQUAMARINE_CRYSTAL("AQUA_C", "Aquamarine Crystal", ChatFormatting.DARK_BLUE),
	CITRINE_1("CITR_1", "Citrine 1", ChatFormatting.YELLOW),
	CITRINE_2("CITR_2", "Citrine 2", ChatFormatting.YELLOW),
	CITRINE_CRYSTAL("CITR_C", "Citrine Crystal", ChatFormatting.YELLOW),
	PERIDOT_1("PERI_1", "Peridot 1", ChatFormatting.DARK_GREEN),
	PERIDOT_2("PERI_2", "Peridot 2", ChatFormatting.DARK_GREEN),
	PERIDOT_CRYSTAL("PERI_C", "Peridot Crystal", ChatFormatting.DARK_GREEN),
	JASPER("JASP_1", "Jasper", ChatFormatting.LIGHT_PURPLE),
	JASPER_CRYSTAL("JASP_C", "Jasper Crystal", ChatFormatting.LIGHT_PURPLE),
	OPAL("OPAL_1", "Opal", ChatFormatting.WHITE),
	OPAL_CRYSTAL("OPAL_C", "Opal Crystal", ChatFormatting.WHITE),
	LITTLEFOOTS_DEN("LITT_L", "Littlefoot's Den", ChatFormatting.AQUA),
}
