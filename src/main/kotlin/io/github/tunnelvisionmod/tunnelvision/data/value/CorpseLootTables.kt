package io.github.tunnelvisionmod.tunnelvision.data.value

import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType

/** One line of a corpse loot table: [weight] out of the table's total picks [amount] of [item]. */
data class CorpseDrop(val item: CorpseDropItem, val amount: Double, val weight: Int)

/** What a [CorpseDrop] is worth is looked up differently depending on where the item trades. */
sealed interface CorpseDropItem {
	/** Sold on the Bazaar under [productId]. */
	data class Bazaar(val productId: String) : CorpseDropItem

	/** A Gemstone Crystal, worth the Perfect it builds minus the Fine gems that Perfect would cost. */
	data class Crystal(val crystal: CrystalType) : CorpseDropItem

	/** Auction-house only, priced at its lowest BIN, with [fallback] until that feed has loaded. */
	data class Auction(val itemId: String, val fallback: Double) : CorpseDropItem
}

/**
 * A corpse's loot table. Hypixel picks a weighted entry [rolls] times per corpse, so one corpse
 * yields `rolls * weight / totalWeight * amount` of an entry's item.
 *
 * Weights are kept rather than folded into expected amounts because entries can drop out: a crystal
 * you already carry cannot drop, and that roll is taken again over what is left, which makes every
 * remaining entry correspondingly more likely.
 */
data class CorpseTable(val rolls: Double, val meterXp: Double, val drops: List<CorpseDrop>) {
	val totalWeight: Int get() = drops.sumOf { it.weight }
}

/**
 * Expected drops per looted corpse, from the loot tables on the Frozen Corpses wiki page at the
 * maximum roll count, which is what we always assume. Validated against the wiki's own Vanguard
 * profit calculator: every one of its eighteen expected-amount terms matches to twelve digits.
 *
 * Only the 5 crystals listed here drop from corpses; Jasper and Ruby come from crystal mineshafts.
 */
object CorpseLootTables {
	/**
	 * The two auction-only drops, named once so the loot tables and the drop-name lookup cannot
	 * drift apart on the id or the fallback.
	 */
	val CAGED_WISP = CorpseDropItem.Auction("CAGED_WISP", 40_000_000.0)
	val SHATTERED_LOCKET = CorpseDropItem.Auction(CorpseValue.LOCKET_ID, CorpseValue.LOCKET_FALLBACK)

	/** Lapis corpses: 3-6 rolls, 4.902 with maxed Gifts from the Departed, a Blue Cheese drill and Corpse Milestone 6. */
	val LAPIS = CorpseTable(
		rolls = 4.902,
		meterXp = 500.0,
		drops = listOf(
			CorpseDrop(CorpseDropItem.Bazaar("FLAWED_AQUAMARINE_GEM"), amount = 20.0, weight = 1000),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWED_CITRINE_GEM"), amount = 20.0, weight = 1000),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWED_ONYX_GEM"), amount = 20.0, weight = 1000),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWED_PERIDOT_GEM"), amount = 20.0, weight = 1000),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG"), amount = 1.0, weight = 800),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG_GREEN"), amount = 1.0, weight = 700),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG_RED"), amount = 1.0, weight = 700),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG_YELLOW"), amount = 1.0, weight = 700),
			CorpseDrop(CorpseDropItem.Bazaar("ENCHANTED_GLACITE"), amount = 1.0, weight = 500),
			CorpseDrop(CorpseDropItem.Bazaar("ENCHANTED_TUNGSTEN"), amount = 1.0, weight = 500),
			CorpseDrop(CorpseDropItem.Bazaar("ENCHANTED_UMBER"), amount = 1.0, weight = 500),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWED_AQUAMARINE_GEM"), amount = 40.0, weight = 500),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWED_CITRINE_GEM"), amount = 40.0, weight = 500),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWED_ONYX_GEM"), amount = 40.0, weight = 500),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWED_PERIDOT_GEM"), amount = 40.0, weight = 500),
			CorpseDrop(CorpseDropItem.Bazaar("GLACITE_JEWEL"), amount = 1.0, weight = 500),
			CorpseDrop(CorpseDropItem.Bazaar("SUSPICIOUS_SCRAP"), amount = 1.0, weight = 500),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG"), amount = 2.0, weight = 400),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG_GREEN"), amount = 2.0, weight = 350),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG_RED"), amount = 2.0, weight = 350),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG_YELLOW"), amount = 2.0, weight = 350),
			CorpseDrop(CorpseDropItem.Bazaar("ENCHANTED_GLACITE"), amount = 2.0, weight = 250),
			CorpseDrop(CorpseDropItem.Bazaar("ENCHANTED_TUNGSTEN"), amount = 2.0, weight = 250),
			CorpseDrop(CorpseDropItem.Bazaar("ENCHANTED_UMBER"), amount = 2.0, weight = 250),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_AQUAMARINE_GEM"), amount = 1.0, weight = 250),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_CITRINE_GEM"), amount = 1.0, weight = 250),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_ONYX_GEM"), amount = 1.0, weight = 250),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_PERIDOT_GEM"), amount = 1.0, weight = 250),
			CorpseDrop(CorpseDropItem.Bazaar("GLACITE_JEWEL"), amount = 2.0, weight = 250),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG"), amount = 4.0, weight = 200),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG_GREEN"), amount = 4.0, weight = 175),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG_RED"), amount = 4.0, weight = 175),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG_YELLOW"), amount = 4.0, weight = 175),
			CorpseDrop(CorpseDropItem.Bazaar("ENCHANTED_GLACITE"), amount = 4.0, weight = 125),
			CorpseDrop(CorpseDropItem.Bazaar("ENCHANTED_TUNGSTEN"), amount = 4.0, weight = 125),
			CorpseDrop(CorpseDropItem.Bazaar("ENCHANTED_UMBER"), amount = 4.0, weight = 125),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_AQUAMARINE_GEM"), amount = 2.0, weight = 125),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_CITRINE_GEM"), amount = 2.0, weight = 125),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_ONYX_GEM"), amount = 2.0, weight = 125),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_PERIDOT_GEM"), amount = 2.0, weight = 125),
			CorpseDrop(CorpseDropItem.Bazaar("BEJEWELED_HANDLE"), amount = 1.0, weight = 100),
			CorpseDrop(CorpseDropItem.Bazaar("BEJEWELED_HANDLE"), amount = 2.0, weight = 50),
			CorpseDrop(CorpseDropItem.Bazaar("BEJEWELED_HANDLE"), amount = 4.0, weight = 25),
			CorpseDrop(CorpseDropItem.Bazaar("GLACITE_AMALGAMATION"), amount = 1.0, weight = 25),
			CorpseDrop(CorpseDropItem.Bazaar("REFINED_TUNGSTEN"), amount = 1.0, weight = 25),
			CorpseDrop(CorpseDropItem.Bazaar("REFINED_UMBER"), amount = 1.0, weight = 25),
			CorpseDrop(CorpseDropItem.Bazaar("TUNGSTEN_KEY"), amount = 1.0, weight = 25),
			CorpseDrop(CorpseDropItem.Bazaar("UMBER_KEY"), amount = 1.0, weight = 25),
		),
	)

	/** Umber and Tungsten corpses: 4-7 rolls, 5.902 maxed. */
	val UMBER_TUNGSTEN = CorpseTable(
		rolls = 5.902,
		meterXp = 2_500.0,
		drops = listOf(
			CorpseDrop(CorpseDropItem.Bazaar("FINE_AQUAMARINE_GEM"), amount = 4.0, weight = 700),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_CITRINE_GEM"), amount = 4.0, weight = 700),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_ONYX_GEM"), amount = 4.0, weight = 700),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_PERIDOT_GEM"), amount = 4.0, weight = 700),
			CorpseDrop(CorpseDropItem.Bazaar("SUSPICIOUS_SCRAP"), amount = 2.0, weight = 700),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_AQUAMARINE_GEM"), amount = 8.0, weight = 350),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_CITRINE_GEM"), amount = 8.0, weight = 350),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_ONYX_GEM"), amount = 8.0, weight = 350),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_PERIDOT_GEM"), amount = 8.0, weight = 350),
			CorpseDrop(CorpseDropItem.Bazaar("SUSPICIOUS_SCRAP"), amount = 4.0, weight = 350),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_AQUAMARINE_GEM"), amount = 16.0, weight = 175),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_CITRINE_GEM"), amount = 16.0, weight = 175),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_ONYX_GEM"), amount = 16.0, weight = 175),
			CorpseDrop(CorpseDropItem.Bazaar("FINE_PERIDOT_GEM"), amount = 16.0, weight = 175),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG_BLUE"), amount = 1.0, weight = 80),
			CorpseDrop(CorpseDropItem.Bazaar("ENCHANTMENT_ICE_COLD_1"), amount = 1.0, weight = 80),
			CorpseDrop(CorpseDropItem.Crystal(CrystalType.AQUAMARINE), amount = 1.0, weight = 40),
			CorpseDrop(CorpseDropItem.Crystal(CrystalType.CITRINE), amount = 1.0, weight = 40),
			CorpseDrop(CorpseDropItem.Crystal(CrystalType.ONYX), amount = 1.0, weight = 40),
			CorpseDrop(CorpseDropItem.Crystal(CrystalType.OPAL), amount = 1.0, weight = 40),
			CorpseDrop(CorpseDropItem.Crystal(CrystalType.PERIDOT), amount = 1.0, weight = 40),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWLESS_AQUAMARINE_GEM"), amount = 1.0, weight = 25),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWLESS_CITRINE_GEM"), amount = 1.0, weight = 25),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWLESS_ONYX_GEM"), amount = 1.0, weight = 25),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWLESS_PERIDOT_GEM"), amount = 1.0, weight = 25),
			CorpseDrop(CorpseDropItem.Bazaar("DWARVEN_OS_METALLIC_MINIS"), amount = 1.0, weight = 20),
			CorpseDrop(CorpseDropItem.Bazaar("GLACITE_AMALGAMATION"), amount = 1.0, weight = 20),
			CorpseDrop(CorpseDropItem.Bazaar("REFINED_TUNGSTEN"), amount = 1.0, weight = 20),
			CorpseDrop(CorpseDropItem.Bazaar("REFINED_UMBER"), amount = 1.0, weight = 20),
			CorpseDrop(CorpseDropItem.Bazaar("MITHRIL_PLATE"), amount = 1.0, weight = 10),
			CorpseDrop(CorpseDropItem.Bazaar("TUNGSTEN_KEY"), amount = 1.0, weight = 10),
			CorpseDrop(CorpseDropItem.Bazaar("UMBER_KEY"), amount = 1.0, weight = 10),
			CorpseDrop(CorpseDropItem.Bazaar("TUNGSTEN_PLATE"), amount = 1.0, weight = 5),
			CorpseDrop(CorpseDropItem.Bazaar("UMBER_PLATE"), amount = 1.0, weight = 5),
			CorpseDrop(CorpseDropItem.Bazaar("FROZEN_SCUTE"), amount = 1.0, weight = 4),
			CorpseDrop(CorpseDropItem.Bazaar("SKELETON_KEY"), amount = 1.0, weight = 4),
			CorpseDrop(CAGED_WISP, amount = 1.0, weight = 2),
		),
	)

	/** Vanguard corpses: 5-8 rolls, 6.902 maxed. */
	val VANGUARD = CorpseTable(
		rolls = 6.902,
		meterXp = 25_000.0,
		drops = listOf(
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG_BLUE"), amount = 1.0, weight = 160),
			CorpseDrop(CorpseDropItem.Bazaar("ENCHANTMENT_ICE_COLD_1"), amount = 1.0, weight = 160),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWLESS_AQUAMARINE_GEM"), amount = 1.0, weight = 160),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWLESS_CITRINE_GEM"), amount = 1.0, weight = 160),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWLESS_ONYX_GEM"), amount = 1.0, weight = 160),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWLESS_PERIDOT_GEM"), amount = 1.0, weight = 160),
			CorpseDrop(CorpseDropItem.Bazaar("GLACITE_AMALGAMATION"), amount = 1.0, weight = 160),
			CorpseDrop(CorpseDropItem.Bazaar("REFINED_TUNGSTEN"), amount = 1.0, weight = 160),
			CorpseDrop(CorpseDropItem.Bazaar("REFINED_UMBER"), amount = 1.0, weight = 160),
			CorpseDrop(CorpseDropItem.Bazaar("SUSPICIOUS_SCRAP"), amount = 8.0, weight = 160),
			CorpseDrop(CorpseDropItem.Crystal(CrystalType.AQUAMARINE), amount = 1.0, weight = 120),
			CorpseDrop(CorpseDropItem.Crystal(CrystalType.CITRINE), amount = 1.0, weight = 120),
			CorpseDrop(CorpseDropItem.Bazaar("DWARVEN_OS_METALLIC_MINIS"), amount = 1.0, weight = 120),
			CorpseDrop(CorpseDropItem.Crystal(CrystalType.ONYX), amount = 1.0, weight = 120),
			CorpseDrop(CorpseDropItem.Crystal(CrystalType.OPAL), amount = 1.0, weight = 120),
			CorpseDrop(CorpseDropItem.Crystal(CrystalType.PERIDOT), amount = 1.0, weight = 120),
			CorpseDrop(CorpseDropItem.Bazaar("GOBLIN_EGG_BLUE"), amount = 2.0, weight = 80),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWLESS_AQUAMARINE_GEM"), amount = 2.0, weight = 80),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWLESS_CITRINE_GEM"), amount = 2.0, weight = 80),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWLESS_ONYX_GEM"), amount = 2.0, weight = 80),
			CorpseDrop(CorpseDropItem.Bazaar("FLAWLESS_PERIDOT_GEM"), amount = 2.0, weight = 80),
			CorpseDrop(CorpseDropItem.Bazaar("GLACITE_AMALGAMATION"), amount = 2.0, weight = 80),
			CorpseDrop(CorpseDropItem.Bazaar("REFINED_TUNGSTEN"), amount = 2.0, weight = 80),
			CorpseDrop(CorpseDropItem.Bazaar("REFINED_UMBER"), amount = 2.0, weight = 80),
			CorpseDrop(CorpseDropItem.Bazaar("SUSPICIOUS_SCRAP"), amount = 16.0, weight = 80),
			CorpseDrop(CorpseDropItem.Bazaar("TUNGSTEN_KEY"), amount = 1.0, weight = 80),
			CorpseDrop(CorpseDropItem.Bazaar("UMBER_KEY"), amount = 1.0, weight = 80),
			CorpseDrop(CorpseDropItem.Bazaar("MITHRIL_PLATE"), amount = 1.0, weight = 60),
			CorpseDrop(CorpseDropItem.Bazaar("GLACITE_AMALGAMATION"), amount = 4.0, weight = 40),
			CorpseDrop(CorpseDropItem.Bazaar("TUNGSTEN_KEY"), amount = 2.0, weight = 40),
			CorpseDrop(CorpseDropItem.Bazaar("UMBER_KEY"), amount = 2.0, weight = 40),
			CorpseDrop(CorpseDropItem.Bazaar("TUNGSTEN_PLATE"), amount = 1.0, weight = 30),
			CorpseDrop(CorpseDropItem.Bazaar("UMBER_PLATE"), amount = 1.0, weight = 30),
			CorpseDrop(CorpseDropItem.Bazaar("TUNGSTEN_KEY"), amount = 4.0, weight = 20),
			CorpseDrop(CorpseDropItem.Bazaar("UMBER_KEY"), amount = 4.0, weight = 20),
			CorpseDrop(CAGED_WISP, amount = 1.0, weight = 10),
			CorpseDrop(CorpseDropItem.Bazaar("FROZEN_SCUTE"), amount = 1.0, weight = 10),
			CorpseDrop(CorpseDropItem.Bazaar("SKELETON_KEY"), amount = 1.0, weight = 10),
			CorpseDrop(SHATTERED_LOCKET, amount = 1.0, weight = 5),
		),
	)

}
