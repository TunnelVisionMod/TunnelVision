package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import net.minecraft.ChatFormatting

enum class CorpseType(
	private val helmetId: String,
	private val helmetName: String,
	val tabName: String,
	val keyName: String?,
	val keyProductId: String?,
	val color: ChatFormatting,
) {
	LAPIS("LAPIS_ARMOR_HELMET", "Lapis Armor Helmet", "Lapis", null, null, ChatFormatting.BLUE),
	TUNGSTEN("MINERAL_HELMET", "Mineral Helmet", "Tungsten", "Tungsten Key", "TUNGSTEN_KEY", ChatFormatting.GRAY),
	UMBER("ARMOR_OF_YOG_HELMET", "Yog Helmet", "Umber", "Umber Key", "UMBER_KEY", ChatFormatting.GOLD),
	VANGUARD("VANGUARD_HELMET", "Vanguard Helmet", "Vanguard", "Skeleton Key", "SKELETON_KEY", ChatFormatting.AQUA);

	companion object {
		fun fromHelmet(skyblockId: String?, name: String): CorpseType? =
			entries.firstOrNull { it.helmetId == skyblockId || it.helmetName == name }

		fun fromTabName(name: String): CorpseType? = entries.firstOrNull { it.tabName == name }
	}
}
