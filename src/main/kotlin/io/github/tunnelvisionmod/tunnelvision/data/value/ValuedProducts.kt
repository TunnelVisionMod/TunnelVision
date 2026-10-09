package io.github.tunnelvisionmod.tunnelvision.data.value

import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType

/**
 * Every Bazaar product the verdict sells, the biggest levers first: Fine gems, then the Perfect a
 * crystal forges, then corpse loot from the richest table down. Costs are priced live, so keys and
 * forge inputs need no median.
 */
object ValuedProducts {
	val inPriority: List<String> = buildList {
		GemstoneShaft.entries.forEach { add(it.fineGemId) }
		CrystalType.entries.forEach { add("PERFECT_${it.displayName.uppercase()}_GEM") }
		listOf(CorpseLootTables.VANGUARD, CorpseLootTables.UMBER_TUNGSTEN, CorpseLootTables.LAPIS).forEach { table ->
			table.drops.forEach { drop -> (drop.item as? CorpseDropItem.Bazaar)?.let { add(it.productId) } }
		}
	}.distinct()
}
