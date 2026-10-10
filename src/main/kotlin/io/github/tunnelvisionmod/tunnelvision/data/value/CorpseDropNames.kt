package io.github.tunnelvisionmod.tunnelvision.data.value

import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType

/**
 * The display name Hypixel prints for a drop, mapped to the entry that prices it.
 *
 * A loot message names items the way the player sees them - `Flawed Peridot Gemstone` - while every
 * price source is keyed by SkyBlock id, so a looted line has to be translated before it can be
 * valued. Gemstones are generated from their tier and gem because they are regular and numerous;
 * everything else is listed.
 *
 * `CorpseDropNamesTest` holds this to the loot tables: every item a corpse can drop has to be
 * nameable here, so a table entry added without a name fails the build rather than quietly going
 * unpriced in chat.
 */
object CorpseDropNames {
	private val gemTiers = mapOf("Rough" to "ROUGH", "Flawed" to "FLAWED", "Fine" to "FINE", "Flawless" to "FLAWLESS", "Perfect" to "PERFECT")

	private val gems = listOf(
		"Jasper", "Ruby", "Opal", "Amber", "Jade", "Sapphire",
		"Amethyst", "Topaz", "Onyx", "Aquamarine", "Citrine", "Peridot",
	)

	/** Powder is a currency, not an item: it really drops, and it buys nothing on any market. */
	private val worthless = setOf("glacite powder", "mithril powder", "gemstone powder")

	private val named: Map<String, CorpseDropItem> = buildMap {
		for ((tier, tierId) in gemTiers) {
			for (gem in gems) put(key("$tier $gem Gemstone"), bazaar("${tierId}_${gem.uppercase()}_GEM"))
		}
		for (crystal in CrystalType.entries) put(key("${crystal.displayName} Crystal"), CorpseDropItem.Crystal(crystal))
		putAll(
			listOf(
				"Goblin Egg" to bazaar("GOBLIN_EGG"),
				"Green Goblin Egg" to bazaar("GOBLIN_EGG_GREEN"),
				"Red Goblin Egg" to bazaar("GOBLIN_EGG_RED"),
				"Yellow Goblin Egg" to bazaar("GOBLIN_EGG_YELLOW"),
				"Blue Goblin Egg" to bazaar("GOBLIN_EGG_BLUE"),
				"Enchanted Glacite" to bazaar("ENCHANTED_GLACITE"),
				"Enchanted Tungsten" to bazaar("ENCHANTED_TUNGSTEN"),
				"Enchanted Umber" to bazaar("ENCHANTED_UMBER"),
				"Glacite Jewel" to bazaar("GLACITE_JEWEL"),
				"Glacite Amalgamation" to bazaar("GLACITE_AMALGAMATION"),
				"Suspicious Scrap" to bazaar("SUSPICIOUS_SCRAP"),
				"Bejeweled Handle" to bazaar("BEJEWELED_HANDLE"),
				"Refined Tungsten" to bazaar("REFINED_TUNGSTEN"),
				"Refined Umber" to bazaar("REFINED_UMBER"),
				"Tungsten Plate" to bazaar("TUNGSTEN_PLATE"),
				"Umber Plate" to bazaar("UMBER_PLATE"),
				"Mithril Plate" to bazaar("MITHRIL_PLATE"),
				"Tungsten Key" to bazaar("TUNGSTEN_KEY"),
				"Umber Key" to bazaar("UMBER_KEY"),
				"Skeleton Key" to bazaar("SKELETON_KEY"),
				"Frozen Scute" to bazaar("FROZEN_SCUTE"),
				"Ice Cold I" to bazaar("ENCHANTMENT_ICE_COLD_1"),
				// A corpse only ever drops the one book, and the message just says "Enchanted Book".
				"Enchanted Book" to bazaar("ENCHANTMENT_ICE_COLD_1"),
				"Ice Cold 1" to bazaar("ENCHANTMENT_ICE_COLD_1"),
				"Metallic Minis" to bazaar("DWARVEN_OS_METALLIC_MINIS"),
				// The only armour a corpse drops. It is nothing to do with the set the corpse wears -
				// that is only how the corpse is recognised - so no other piece belongs here.
				"Dead Man's Chestplate" to auction("DEAD_MANS_CHESTPLATE", 300_000.0),
				"Caged Wisp" to CorpseLootTables.CAGED_WISP,
				// The id says Pendant and the wiki says Locket, so both names resolve to it.
				"Shattered Pendant" to CorpseLootTables.SHATTERED_LOCKET,
				"Shattered Locket" to CorpseLootTables.SHATTERED_LOCKET,
			).associate { (name, item) -> key(name) to item }
		)
	}

	/** Every name we can price, for the test that holds this to the loot tables. */
	val entries: Map<String, CorpseDropItem> get() = named

	/** What prices [name], or null when we have never heard of it. */
	fun itemFor(name: String): CorpseDropItem? = named[key(name)]

	/** True for a drop that is real but worth no coins, which is not the same as one we cannot price. */
	fun isWorthless(name: String): Boolean = key(name) in worthless

	private fun bazaar(productId: String): CorpseDropItem = CorpseDropItem.Bazaar(productId)

	/** [fallback] is only read until the auction feed loads, so it is the BIN at the time of writing. */
	private fun auction(itemId: String, fallback: Double): CorpseDropItem = CorpseDropItem.Auction(itemId, fallback)

	/** Names are compared case-insensitively with runs of whitespace collapsed. */
	private fun key(name: String): String = name.trim().lowercase().replace(Regex("""\s+"""), " ")
}
