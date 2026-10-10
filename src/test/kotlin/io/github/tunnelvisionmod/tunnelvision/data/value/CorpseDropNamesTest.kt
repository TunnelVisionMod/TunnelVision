package io.github.tunnelvisionmod.tunnelvision.data.value

import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseLootParser
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CorpseDropNamesTest {
	/**
	 * The invariant that keeps the chat message honest: a corpse cannot drop something we have no
	 * name for, because the loot message names it and we would then show it unpriced.
	 */
	@Test
	fun `every item in every loot table has a name`() {
		val nameable = CorpseDropNames.entries.values.toSet()
		val missing = CorpseType.entries
			.flatMap { CorpseValue.table(it).drops }
			.map { it.item }
			.distinct()
			.filterNot { it in nameable }
		assertTrue(missing.isEmpty(), "No display name for: $missing")
	}

	@Test
	fun `gemstones are named by tier and gem`() {
		assertEquals(CorpseDropItem.Bazaar("FLAWED_PERIDOT_GEM"), CorpseDropNames.itemFor("Flawed Peridot Gemstone"))
		assertEquals(CorpseDropItem.Bazaar("FINE_ONYX_GEM"), CorpseDropNames.itemFor("Fine Onyx Gemstone"))
		assertEquals(CorpseDropItem.Bazaar("FLAWLESS_AQUAMARINE_GEM"), CorpseDropNames.itemFor("Flawless Aquamarine Gemstone"))
		assertEquals(CorpseDropItem.Bazaar("PERFECT_JASPER_GEM"), CorpseDropNames.itemFor("Perfect Jasper Gemstone"))
	}

	@Test
	fun `crystals map to the crystal they are`() {
		assertEquals(CorpseDropItem.Crystal(CrystalType.ONYX), CorpseDropNames.itemFor("Onyx Crystal"))
		assertEquals(CorpseDropItem.Crystal(CrystalType.OPAL), CorpseDropNames.itemFor("Opal Crystal"))
	}

	@Test
	fun `auction only drops carry their estimate`() {
		assertEquals(CorpseLootTables.SHATTERED_LOCKET, CorpseDropNames.itemFor("Shattered Locket"))
		assertEquals(CorpseLootTables.SHATTERED_LOCKET, CorpseDropNames.itemFor("Shattered Pendant"))
		assertEquals(CorpseLootTables.CAGED_WISP, CorpseDropNames.itemFor("Caged Wisp"))
	}

	@Test
	fun `an enchanted book out of a corpse is always Ice Cold`() {
		val iceCold = CorpseDropItem.Bazaar("ENCHANTMENT_ICE_COLD_1")
		assertEquals(iceCold, CorpseDropNames.itemFor("Enchanted Book"))
		assertEquals(iceCold, CorpseDropNames.itemFor("Ice Cold I"))
	}

	@Test
	fun `the one armour drop is priced off the auction feed`() {
		assertEquals(CorpseDropItem.Auction("DEAD_MANS_CHESTPLATE", 300_000.0), CorpseDropNames.itemFor("Dead Man's Chestplate"))
	}

	@Test
	fun `the armour a corpse wears is not a drop`() {
		// The helmet identifies the corpse; the set is not loot, so naming it would invent a drop.
		// No corpse drops armour of its own kind, Vanguard included - the one armour drop is the
		// Dead Man's Chestplate above.
		assertNull(CorpseDropNames.itemFor("Mineral Chestplate"))
		assertNull(CorpseDropNames.itemFor("Yog Boots"))
		assertNull(CorpseDropNames.itemFor("Lapis Armor Leggings"))
		assertNull(CorpseDropNames.itemFor("Vanguard Chestplate"))
	}

	@Test
	fun `names are matched loosely on case and spacing`() {
		assertEquals(CorpseDropItem.Bazaar("GLACITE_JEWEL"), CorpseDropNames.itemFor("glacite jewel"))
		assertEquals(CorpseDropItem.Bazaar("GLACITE_JEWEL"), CorpseDropNames.itemFor("  Glacite   Jewel  "))
	}

	@Test
	fun `powder drops but buys nothing`() {
		assertTrue(CorpseDropNames.isWorthless("Glacite Powder"))
		assertTrue(CorpseDropNames.isWorthless("glacite powder"))
		assertEquals(false, CorpseDropNames.isWorthless("Glacite Jewel"))
	}

	/**
	 * The lines below are copied from real loot blocks. They caught the bug where the gem symbol
	 * stayed glued to the name, which left every gemstone in the message unpriced.
	 */
	@Test
	fun `every drop in a real loot block resolves to something priceable`() {
		val block = listOf(
			" ✦ Fine Peridot Gemstone ×12",
			"Suspicious Scrap ×4",
			" ✖ Fine Onyx Gemstone ×4",
			"60x ✖ Flawed Onyx Gemstone",
			"20x ⍟ Flawed Aquamarine Gemstone",
			"40x ☘ Flawed Citrine Gemstone",
			"20x ⚶ Flawed Peridot Gemstone",
			"Dead Man's Chestplate",
			"Caged Wisp",
		)
		val drops = block.mapNotNull { CorpseLootParser.item(it) }
		assertEquals(block.size, drops.size, "some lines did not read as a drop")
		val unnameable = drops.filter { CorpseDropNames.itemFor(it.name) == null }.map { it.name }
		assertTrue(unnameable.isEmpty(), "no price lookup for: $unnameable")
	}

	@Test
	fun `an unknown item has no name`() {
		assertNull(CorpseDropNames.itemFor("Some Item Hypixel Added Today"))
	}
}
