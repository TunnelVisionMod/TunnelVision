package io.github.tunnelvisionmod.tunnelvision.data.value

import io.github.tunnelvisionmod.tunnelvision.core.config.BazaarPriceType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.LootedItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * No Bazaar prices are loaded in a test, so every Bazaar-priced drop reads as unpriced here. That is
 * deliberate: it is also what happens in game before the first price fetch lands, and these tests
 * pin down that a missing price never silently becomes a zero.
 */
class CorpseProfitTest {
	private fun of(type: CorpseType, vararg items: LootedItem, includeMeter: Boolean = true) =
		CorpseProfit.of(type, items.toList(), BazaarPriceType.SELL_OFFER, includeMeter)

	@Test
	fun `the rng meter is worth its share of a locket`() {
		assertEquals(100_000.0, CorpseValue.meterValue(CorpseType.LAPIS))
		assertEquals(500_000.0, CorpseValue.meterValue(CorpseType.UMBER))
		assertEquals(500_000.0, CorpseValue.meterValue(CorpseType.TUNGSTEN))
		assertEquals(5_000_000.0, CorpseValue.meterValue(CorpseType.VANGUARD))
	}

	@Test
	fun `the rng meter attribute raises the xp and the coins it is worth`() {
		CorpseValue.meterBonusPercent = { 10 }
		try {
			assertEquals(550.0, CorpseValue.meterXp(CorpseType.LAPIS), 1e-9)
			assertEquals(27_500.0, CorpseValue.meterXp(CorpseType.VANGUARD), 1e-9)
			assertEquals(5_500_000.0, CorpseValue.meterValue(CorpseType.VANGUARD), 1e-6)
		} finally {
			CorpseValue.meterBonusPercent = { 0 }
		}
	}

	@Test
	fun `a lapis corpse needs no key, so its meter alone is profit`() {
		val breakdown = of(CorpseType.LAPIS)
		assertEquals(0.0, breakdown.keyCoins)
		assertEquals(100_000.0, breakdown.net)
	}

	@Test
	fun `auction estimates are priced per stack`() {
		val breakdown = of(CorpseType.LAPIS, LootedItem("Caged Wisp", 2))
		assertEquals(2 * CorpseLootTables.CAGED_WISP.fallback, breakdown.lootCoins)
		assertEquals(2 * CorpseLootTables.CAGED_WISP.fallback + 100_000.0, breakdown.net)
		assertEquals(0, breakdown.unpriced)
	}

	@Test
	fun `powder is left out of the message entirely`() {
		// It only buys HotM upgrades, so a line for it is noise in a message about coins.
		val breakdown = of(CorpseType.LAPIS, LootedItem("Glacite Powder", 15_759), LootedItem("Caged Wisp", 1))
		assertEquals(listOf("Caged Wisp"), breakdown.lines.map { it.item.name })
		assertEquals(0, breakdown.unpriced)
	}

	@Test
	fun `a known item resolves to the entry that prices it`() {
		val breakdown = of(CorpseType.LAPIS, LootedItem("Fine Onyx Gemstone", 4))
		assertEquals(CorpseDropItem.Bazaar("FINE_ONYX_GEM"), breakdown.lines.single().drop)
	}

	@Test
	fun `an item we cannot price is reported, not counted`() {
		val breakdown = of(CorpseType.LAPIS, LootedItem("Some Item Hypixel Added Today", 1), LootedItem("Caged Wisp", 1))
		assertEquals(LootPrice.Unknown, breakdown.lines.first().price)
		assertEquals(1, breakdown.unpriced)
		assertEquals(CorpseLootTables.CAGED_WISP.fallback, breakdown.lootCoins)
	}

	@Test
	fun `a corpse whose key price is missing has no net at all`() {
		// A Skeleton Key costs more than most Vanguard loot, so guessing zero would be wrong by millions.
		val breakdown = of(CorpseType.VANGUARD, LootedItem("Caged Wisp", 1))
		assertNull(breakdown.keyCoins)
		assertNull(breakdown.net)
	}

	@Test
	fun `hiding the meter takes it out of the total too`() {
		val breakdown = of(CorpseType.LAPIS, LootedItem("Caged Wisp", 1), includeMeter = false)
		assertEquals(0.0, breakdown.meterCoins)
		assertEquals(CorpseLootTables.CAGED_WISP.fallback, breakdown.net)
	}

	/**
	 * The meter is credited corpse by corpse, so the Pendant it finally hands over must not be
	 * counted as well - that would pay for it twice.
	 */
	@Test
	fun `the meter payout is shown but not counted`() {
		val breakdown = CorpseProfit.of(
			CorpseType.LAPIS,
			listOf(LootedItem("Shattered Pendant", 1)),
			BazaarPriceType.SELL_OFFER,
			meterPayout = true,
		)
		val line = breakdown.lines.single()
		assertTrue(line.fromMeter)
		assertEquals(LootPrice.Coins(CorpseValue.LOCKET_FALLBACK, CorpseValue.LOCKET_FALLBACK), line.price)
		assertEquals(0.0, breakdown.lootCoins, "the payout adds nothing to the loot")
		assertEquals(100_000.0, breakdown.net, "only this corpse's own meter slice counts")
	}

	@Test
	fun `a pendant that did not come from the meter is counted`() {
		val breakdown = CorpseProfit.of(
			CorpseType.LAPIS,
			listOf(LootedItem("Shattered Pendant", 1)),
			BazaarPriceType.SELL_OFFER,
			meterPayout = false,
		)
		assertEquals(false, breakdown.lines.single().fromMeter)
		assertEquals(CorpseValue.LOCKET_FALLBACK, breakdown.lootCoins)
	}

	@Test
	fun `only the payout item is held back, not the rest of the loot`() {
		val breakdown = CorpseProfit.of(
			CorpseType.LAPIS,
			listOf(LootedItem("Shattered Pendant", 1), LootedItem("Caged Wisp", 1)),
			BazaarPriceType.SELL_OFFER,
			meterPayout = true,
		)
		assertEquals(CorpseLootTables.CAGED_WISP.fallback, breakdown.lootCoins)
	}

	@Test
	fun `drops keep the order the message listed them in`() {
		val breakdown = of(
			CorpseType.LAPIS,
			LootedItem("Suspicious Scrap", 4),
			LootedItem("Caged Wisp", 1),
			LootedItem("Glacite Jewel", 2),
		)
		assertEquals(listOf("Suspicious Scrap", "Caged Wisp", "Glacite Jewel"), breakdown.lines.map { it.item.name })
	}
}
