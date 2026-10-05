package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

enum class CorpseType(
	private val helmetId: String,
	private val helmetName: String,
	val tabName: String,
	val keyName: String?,
	val keyProductId: String?,
) {
	LAPIS("LAPIS_ARMOR_HELMET", "Lapis Armor Helmet", "Lapis", null, null),
	TUNGSTEN("MINERAL_HELMET", "Mineral Helmet", "Tungsten", "Tungsten Key", "TUNGSTEN_KEY"),
	UMBER("ARMOR_OF_YOG_HELMET", "Yog Helmet", "Umber", "Umber Key", "UMBER_KEY"),
	VANGUARD("VANGUARD_HELMET", "Vanguard Helmet", "Vanguard", "Skeleton Key", "SKELETON_KEY");

	companion object {
		fun fromHelmet(skyblockId: String?, name: String): CorpseType? =
			entries.firstOrNull { it.helmetId == skyblockId || it.helmetName == name }

		fun fromTabName(name: String): CorpseType? = entries.firstOrNull { it.tabName == name }
	}
}
