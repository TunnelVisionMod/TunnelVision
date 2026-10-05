package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

object BlueCheese {
	private const val UPGRADE_MODULE_ID = "goblin_omelette_blue_cheese"

	fun isUpgradeModule(value: String?): Boolean = value.equals(UPGRADE_MODULE_ID, ignoreCase = true)
}
