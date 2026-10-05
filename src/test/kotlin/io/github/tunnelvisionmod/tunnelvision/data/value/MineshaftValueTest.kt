package io.github.tunnelvisionmod.tunnelvision.data.value

import io.github.tunnelvisionmod.tunnelvision.core.config.BazaarPriceType
import io.github.tunnelvisionmod.tunnelvision.core.config.LootMode
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.LootRule
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MineshaftValueTest {
	@Test
	fun `maps mineshaft types to gemstones`() {
		assertEquals(GemstoneShaft.RUBY, GemstoneShaft.of(MineshaftType.RUBY_2))
		assertEquals(GemstoneShaft.RUBY, GemstoneShaft.of(MineshaftType.RUBY_CRYSTAL))
		assertEquals(GemstoneShaft.JADE, GemstoneShaft.of(MineshaftType.JADE_1))
		assertEquals(GemstoneShaft.JASPER, GemstoneShaft.of(MineshaftType.JASPER_CRYSTAL))
		assertEquals(GemstoneShaft.PERIDOT, GemstoneShaft.of(MineshaftType.PERIDOT_2))
		assertEquals(GemstoneShaft.CITRINE, GemstoneShaft.of(MineshaftType.CITRINE_CRYSTAL))
		assertEquals(GemstoneShaft.ONYX, GemstoneShaft.of(MineshaftType.ONYX_1))
		assertEquals(GemstoneShaft.AQUAMARINE, GemstoneShaft.of(MineshaftType.AQUAMARINE_2))
	}

	@Test
	fun `non gemstone mineshafts are not rated`() {
		assertNull(GemstoneShaft.of(MineshaftType.UMBER))
		assertNull(GemstoneShaft.of(MineshaftType.TITANIUM))
		assertNull(GemstoneShaft.of(MineshaftType.TUNGSTEN))
		assertNull(GemstoneShaft.of(MineshaftType.VANGUARD))
		assertNull(GemstoneShaft.of(MineshaftType.LITTLEFOOTS_DEN))
	}

	@Test
	fun `bazaar ids`() {
		assertEquals("FINE_RUBY_GEM", GemstoneShaft.RUBY.fineGemId)
		assertEquals("FINE_AMETHYST_GEM", GemstoneShaft.AMETHYST.fineGemId)
	}

	@Test
	fun `gemstones are grouped by break time`() {
		assertEquals(GemstoneGroup.RUBY, GemstoneShaft.RUBY.group)
		assertEquals(GemstoneGroup.TOPAZ, GemstoneShaft.TOPAZ.group)
		assertEquals(GemstoneGroup.JASPER, GemstoneShaft.JASPER.group)
		for (shaft in listOf(GemstoneShaft.JADE, GemstoneShaft.AMBER, GemstoneShaft.SAPPHIRE, GemstoneShaft.AMETHYST, GemstoneShaft.OPAL)) {
			assertEquals(GemstoneGroup.SOFT, shaft.group, shaft.gemName)
		}
		for (shaft in listOf(GemstoneShaft.ONYX, GemstoneShaft.AQUAMARINE, GemstoneShaft.CITRINE, GemstoneShaft.PERIDOT)) {
			assertEquals(GemstoneGroup.HARD, shaft.group, shaft.gemName)
		}
	}

	/**
	 * Hypixel publishes the mining speed each tick count needs, per gemstone group. Every cell of
	 * that table has to come back out of [GemstoneGroup.ticksPerBlock], and two speed below the
	 * listed minimum has to cost a tick - the listed minimum is sometimes one high, because the
	 * exact boundary can land on a whole number.
	 */
	@Test
	fun `tick formula reproduces the published speed table`() {
		val groups = listOf(GemstoneGroup.RUBY, GemstoneGroup.SOFT, GemstoneGroup.TOPAZ, GemstoneGroup.JASPER, GemstoneGroup.HARD)
		for ((ticks, speeds) in SPEED_TABLE) {
			for ((index, speed) in speeds.withIndex()) {
				if (speed == null) continue
				val group = groups[index]
				assertEquals(ticks, group.ticksPerBlock(speed.toDouble()), "$group at $speed speed")
				if (ticks < SLOWEST_ROW) {
					assertEquals(ticks + 1, group.ticksPerBlock(speed - 2.0), "$group just below $speed speed")
				}
			}
		}
	}

	@Test
	fun `four ticks is the floor`() {
		for (group in GemstoneGroup.entries) {
			assertEquals(GemstoneGroup.MIN_TICKS, group.ticksPerBlock(1_000_000.0), group.name)
		}
	}

	@Test
	fun `rough per block follows the drop formula`() {
		val stats = MiningStats(miningFortune = 1000.0, gemstoneFortune = 100.0, pristine = 10.0, miningSpeed = 5000.0, gemstoneSpread = 0.0)
		// 4 * (1 + 10 * 0.79) * (1 + 1100 / 100) = 4 * 8.9 * 12 = 427.2
		assertEquals(427.2, GemstoneIncome.roughPerBlock(stats), 1e-9)
	}

	@Test
	fun `a looted corpse adds one pristine`() {
		val stats = MiningStats(miningFortune = 0.0, gemstoneFortune = 0.0, pristine = 0.0, miningSpeed = 5000.0, gemstoneSpread = 0.0)
		assertEquals(4.0, GemstoneIncome.roughPerBlock(stats), 1e-9)
		assertEquals(4.0 * (1 + 0.79), GemstoneIncome.roughPerBlock(stats, extraPristine = 1.0), 1e-9)
	}

	@Test
	fun `gemstone spread adds free blocks to the fine yield`() {
		val plain = MiningStats(miningFortune = 0.0, gemstoneFortune = 0.0, pristine = 0.0, miningSpeed = 5000.0, gemstoneSpread = 0.0)
		val spread = plain.copy(gemstoneSpread = 20.0)
		assertEquals(1.2, GemstoneIncome.finePerBlock(spread) / GemstoneIncome.finePerBlock(plain), 1e-9)
	}

	@Test
	fun `six thousand four hundred rough make a fine gem`() {
		assertEquals(6400, GemstoneIncome.ROUGH_PER_FINE)
	}

	/**
	 * The outside leg is driven by the `1 / (2000 - weight)` spawn roll and the Pickobulus rotation,
	 * so it is pinned to the same shape the published simulation has: a spawn inside a couple of
	 * minutes, well short of the pity cap, having mined a sane number of Jade blocks on the way.
	 */
	@Test
	fun `outside leg spawns a mineshaft within the pity cap`() {
		val leg = MineshaftSpawn.outsideLeg()
		assertTrue(leg.seconds > MineshaftSpawn.ENTER_SECONDS, "expected a wait, got ${leg.seconds}")
		assertTrue(leg.seconds < 120, "expected a spawn inside two minutes, got ${leg.seconds}")
		assertTrue(leg.jadeBlocks > 0, "expected Jade mined on the way, got ${leg.jadeBlocks}")
		assertTrue(leg.jadeBlocks < MineshaftSpawn.PITY_WEIGHT / MineshaftSpawn.JADE_QUALITY, "expected fewer blocks than the pity cap")
	}

	/**
	 * Pinned against an independent reference implementation of the same walk. The weight lands on
	 * 1999.6 during the third Pickobulus throw, one short of the cap, where an unclamped
	 * `1 / (2000 - weight)` reads 2.5 and drives the surviving probability negative - which silently
	 * shifted this to 58.8s and 108.6 blocks before [MineshaftSpawn] clamped it.
	 */
	@Test
	fun `outside leg matches the cross-checked expectation`() {
		val leg = MineshaftSpawn.outsideLeg()
		assertEquals(57.783, leg.seconds, 0.01)
		assertEquals(101.188, leg.jadeBlocks, 0.01)
	}

	@Test
	fun `slower mining speed means a later spawn`() {
		val fast = MineshaftSpawn.outsideLeg()
		val slow = MineshaftSpawn.outsideLeg(MiningProfile.outside.copy(miningSpeed = 4000.0))
		assertTrue(slow.seconds > fast.seconds, "${slow.seconds} should be later than ${fast.seconds}")
	}

	/**
	 * Expected amounts are validated against the Vanguard profit calculator on the wiki, which lists
	 * the same products to twelve digits.
	 */
	@Test
	fun `corpse loot tables match the published expected amounts`() {
		assertEquals(5.02677667140825, amount(CorpseLootTables.VANGUARD, "SUSPICIOUS_SCRAP"), 1e-9)
		assertEquals(0.942520625889047, amount(CorpseLootTables.VANGUARD, "GLACITE_AMALGAMATION"), 1e-9)
		assertEquals(0.628347083926031, amount(CorpseLootTables.VANGUARD, "FLAWLESS_ONYX_GEM"), 1e-9)
		assertEquals(0.471260312944524, amount(CorpseLootTables.VANGUARD, "TUNGSTEN_KEY"), 1e-9)
		assertEquals(0.117815078236131, amount(CorpseLootTables.VANGUARD, "MITHRIL_PLATE"), 1e-9)
		assertEquals(0.0196358463726885, amount(CorpseLootTables.VANGUARD, "SKELETON_KEY"), 1e-9)
		assertEquals(0.235630156472262, amount(CorpseLootTables.VANGUARD, CrystalType.ONYX), 1e-9)
	}

	@Test
	fun `only vanguard corpses can drop a shattered locket`() {
		assertTrue(CorpseLootTables.VANGUARD.drops.any { it.item == CorpseDropItem.Fixed(CorpseValue.LOCKET_COINS) })
		assertTrue(CorpseLootTables.LAPIS.drops.none { it.item is CorpseDropItem.Fixed })
	}

	@Test
	fun `lapis corpses drop no crystals at all`() {
		assertTrue(CorpseLootTables.LAPIS.drops.none { it.item is CorpseDropItem.Crystal })
	}

	/** Jasper and Ruby crystals come from crystal mineshafts, never from a corpse. */
	@Test
	fun `corpses drop five of the seven crystals`() {
		val fromCorpses = (CorpseLootTables.UMBER_TUNGSTEN.drops + CorpseLootTables.VANGUARD.drops)
			.mapNotNull { (it.item as? CorpseDropItem.Crystal)?.crystal }
			.toSet()
		assertEquals(
			setOf(CrystalType.OPAL, CrystalType.ONYX, CrystalType.PERIDOT, CrystalType.CITRINE, CrystalType.AQUAMARINE),
			fromCorpses,
		)
	}

	/**
	 * A crystal already carried cannot drop, and Hypixel takes that roll again over the rest of the
	 * table - so its weight moves to the other entries rather than vanishing. Locking one crystal
	 * must therefore make every surviving entry strictly more likely.
	 */
	@Test
	fun `a locked crystal redistributes its weight to the rest of the table`() {
		val table = CorpseLootTables.VANGUARD
		val locked = setOf(CrystalType.ONYX)
		val before = expected(table, "SUSPICIOUS_SCRAP", emptySet())
		val after = expected(table, "SUSPICIOUS_SCRAP", locked)
		assertTrue(after > before, "$after should exceed $before")
		// the Onyx entry carried 120 of 3515 weight, so the rest share it out
		assertEquals(before * table.totalWeight / (table.totalWeight - 120), after, 1e-9)
	}

	@Test
	fun `locking every corpse crystal still leaves the table normalised`() {
		val table = CorpseLootTables.VANGUARD
		val all = CrystalType.entries.toSet()
		val usable = table.drops.filterNot { (it.item as? CorpseDropItem.Crystal)?.crystal in all }
		assertEquals(table.totalWeight - 600, usable.sumOf { it.weight })
		assertTrue(expected(table, "SUSPICIOUS_SCRAP", all) > expected(table, "SUSPICIOUS_SCRAP", emptySet()))
	}

	@Test
	fun `the rng meter is always valued as a shattered locket`() {
		assertEquals(CorpseValue.LOCKET_COINS / 5000, CorpseValue.meterValue(CorpseType.LAPIS), 1e-6)
		assertEquals(CorpseValue.LOCKET_COINS / 1000, CorpseValue.meterValue(CorpseType.UMBER), 1e-6)
		assertEquals(CorpseValue.LOCKET_COINS / 1000, CorpseValue.meterValue(CorpseType.TUNGSTEN), 1e-6)
		assertEquals(CorpseValue.LOCKET_COINS / 100, CorpseValue.meterValue(CorpseType.VANGUARD), 1e-6)
	}

	@Test
	fun `lapis corpses need no key`() {
		assertEquals(0.0, CorpseValue.keyCost(CorpseType.LAPIS))
	}

	@Test
	fun `skipping loots lapis unless greedy is still filling crystals`() {
		assertEquals(LootRule.LAPIS, MineshaftValue.skipRule(LootMode.LAPIS_ONLY, crystalsFull = false, openVanguards = true))
		assertEquals(LootRule.ALL, MineshaftValue.skipRule(LootMode.GREEDY, crystalsFull = false, openVanguards = true))
		assertEquals(LootRule.LAPIS_AND_VANGUARD, MineshaftValue.skipRule(LootMode.NORMAL, crystalsFull = false, openVanguards = true))
	}

	/**
	 * With Vanguards off, Normal collapses onto Lapis Only: the two rules can only ever differ over a
	 * Vanguard corpse. That is what closes the gap between those two modes, since the Fairy mineshaft
	 * in the respawn pool holds nothing else.
	 */
	@Test
	fun `without vanguards normal skips exactly what lapis only skips`() {
		for (crystalsFull in listOf(false, true)) {
			assertEquals(
				MineshaftValue.skipRule(LootMode.LAPIS_ONLY, crystalsFull, openVanguards = false),
				MineshaftValue.skipRule(LootMode.NORMAL, crystalsFull, openVanguards = false),
			)
		}
	}

	@Test
	fun `mining a shaft never loots a vanguard we do not open`() {
		assertTrue(MineshaftValue.mineRule(LootMode.NORMAL, crystalsFull = false, openVanguards = true).includes(CorpseType.VANGUARD))
		assertFalse(MineshaftValue.mineRule(LootMode.NORMAL, crystalsFull = false, openVanguards = false).includes(CorpseType.VANGUARD))
	}

	/** Lapis Only opens nothing but Lapis, in a shaft worth mining as much as in one we leave. */
	@Test
	fun `lapis only never opens another corpse even in a shaft worth mining`() {
		for (crystalsFull in listOf(false, true)) {
			for (openVanguards in listOf(false, true)) {
				val rule = MineshaftValue.mineRule(LootMode.LAPIS_ONLY, crystalsFull, openVanguards)
				assertEquals(LootRule.LAPIS, rule)
				for (type in listOf(CorpseType.UMBER, CorpseType.TUNGSTEN, CorpseType.VANGUARD)) {
					assertFalse(rule.includes(type), "$type with crystalsFull=$crystalsFull vanguards=$openVanguards")
				}
			}
		}
	}

	@Test
	fun `greedy still opens everything in a shaft worth mining`() {
		assertEquals(LootRule.ALL, MineshaftValue.mineRule(LootMode.GREEDY, crystalsFull = false, openVanguards = true))
	}

	@Test
	fun `corpse counts cover one to four and sum to one`() {
		assertEquals(1.0, MineshaftValue.corpseCountChances.values.sum(), 1e-9)
		assertEquals(setOf(1, 2, 3, 4), MineshaftValue.corpseCountChances.keys)
	}

	/**
	 * The needed price is the decision restated as a Bazaar price, so it has to be the exact
	 * break-even: feed it back into the margin and you must land on the long-run rate.
	 */
	@Test
	fun `needed price is the exact break even`() {
		val group = GemstoneGroup.SOFT
		val rate = 65_000_000.0 / 3600
		val corpseGain = 1_500_000.0
		val price = MineshaftValue.breakEvenPrice(group, looted = 3, rate = rate, corpseGain = corpseGain)!!
		val income = MineshaftValue.SHAFT_SECONDS * MineshaftValue.finePerSecond(group, 3) * price
		val margin = (income + corpseGain) / (MineshaftValue.SHAFT_SECONDS - MineshaftValue.SKIP_SECONDS)
		assertEquals(rate, margin, 1e-9)
	}

	@Test
	fun `a harder gemstone needs a higher price`() {
		val rate = 65_000_000.0 / 3600
		val soft = MineshaftValue.breakEvenPrice(GemstoneGroup.SOFT, 3, rate, 0.0)!!
		val hard = MineshaftValue.breakEvenPrice(GemstoneGroup.HARD, 3, rate, 0.0)!!
		assertTrue(hard > soft, "$hard should exceed $soft")
	}

	@Test
	fun `more corpses lower the price needed`() {
		val rate = 65_000_000.0 / 3600
		val one = MineshaftValue.breakEvenPrice(GemstoneGroup.SOFT, 1, rate, 500_000.0)!!
		val four = MineshaftValue.breakEvenPrice(GemstoneGroup.SOFT, 4, rate, 2_000_000.0)!!
		assertTrue(four < one, "$four should be below $one")
	}

	@Test
	fun `no price is needed once the corpses clear the bar on their own`() {
		assertNull(MineshaftValue.breakEvenPrice(GemstoneGroup.SOFT, 4, 65_000_000.0 / 3600, 1e12))
	}

	@Test
	fun `no verdict for other mineshafts`() {
		assertNull(
			MineshaftValue.evaluate(
				type = MineshaftType.UMBER,
				corpses = mapOf(CorpseType.LAPIS to 3),
				priceType = BazaarPriceType.SELL_OFFER,
				mode = LootMode.LAPIS_ONLY,
				crystalsFull = false,
				openVanguards = true,
				lockedCrystals = emptySet(),
			),
		)
	}

	@Test
	fun `no verdict without bazaar prices`() {
		assertNull(
			MineshaftValue.evaluate(
				type = MineshaftType.JASPER,
				corpses = mapOf(CorpseType.LAPIS to 3),
				priceType = BazaarPriceType.SELL_OFFER,
				mode = LootMode.LAPIS_ONLY,
				crystalsFull = false,
				openVanguards = true,
				lockedCrystals = emptySet(),
			),
		)
	}

	/** Expected amount of a product per corpse, the way the wiki's calculator states it. */
	private fun amount(table: CorpseTable, productId: String): Double =
		table.drops.filter { (it.item as? CorpseDropItem.Bazaar)?.productId == productId }
			.sumOf { table.rolls * it.weight / table.totalWeight * it.amount }

	private fun amount(table: CorpseTable, crystal: CrystalType): Double =
		table.drops.filter { (it.item as? CorpseDropItem.Crystal)?.crystal == crystal }
			.sumOf { table.rolls * it.weight / table.totalWeight * it.amount }

	/** Expected amount once [locked] crystals are taken out and their weight redistributed. */
	private fun expected(table: CorpseTable, productId: String, locked: Set<CrystalType>): Double {
		val usable = table.drops.filterNot { (it.item as? CorpseDropItem.Crystal)?.crystal in locked }
		val weight = usable.sumOf { it.weight }
		return usable.filter { (it.item as? CorpseDropItem.Bazaar)?.productId == productId }
			.sumOf { table.rolls * it.weight / weight * it.amount }
	}

	private companion object {
		const val SLOWEST_ROW = 25

		/** Ticks to the minimum mining speed for Ruby, the soft group, Topaz, Jasper, the hard group. */
		val SPEED_TABLE = listOf(
			25 to listOf(2706, 3530, 4471, 5648, null),
			24 to listOf(2817, 3674, 4654, 5878, 6368),
			23 to listOf(2937, 3830, 4852, 6128, 6639),
			22 to listOf(3067, 4000, 5067, 6400, 6934),
			21 to listOf(3210, 4187, 5303, 6698, 7256),
			20 to listOf(3366, 4391, 5561, 7025, 7610),
			19 to listOf(3539, 4616, 5847, 7385, 8001),
			18 to listOf(3730, 4865, 6163, 7784, 8433),
			17 to listOf(3943, 5143, 6515, 8229, 8915),
			16 to listOf(4182, 5455, 6910, 8728, 9455),
			15 to listOf(4452, 5807, 7355, 9291, 10065),
			14 to listOf(4759, 6207, 7863, 9932, 10759),
			13 to listOf(5112, 6667, 8445, 10667, 11556),
			12 to listOf(5520, 7200, 9120, 11520, 12480),
			11 to listOf(6001, 7827, 9914, 12522, 13566),
			10 to listOf(6572, 8572, 10858, 13715, 14858),
			9 to listOf(7264, 9474, 12001, 15158, 16422),
			8 to listOf(8118, 10589, 13412, 16942, 18353),
			7 to listOf(9201, 12001, 15201, 19201, 20801),
			6 to listOf(10616, 13847, 17539, 22154, 24000),
			5 to listOf(12546, 16364, 20728, 26182, 28364),
			4 to listOf(15334, 20000, 25334, 32000, 34667),
		)
	}
}
