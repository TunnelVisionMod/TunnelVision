package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType

class CorpseTotalsTest {
	@Test
	fun `adds up per type and overall`() {
		val totals = CorpseTotals()
		totals.add(CorpseType.LAPIS, 120_000.0)
		totals.add(CorpseType.LAPIS, 80_000.0)
		totals.add(CorpseType.VANGUARD, -29_000_000.0)

		assertEquals(CorpseTotal(2, 200_000.0), totals.total(CorpseType.LAPIS))
		assertEquals(CorpseTotal(1, -29_000_000.0), totals.total(CorpseType.VANGUARD))
		assertEquals(CorpseTotal(3, -28_800_000.0), totals.overall)
	}

	@Test
	fun `a type never opened is left out`() {
		val totals = CorpseTotals()
		totals.add(CorpseType.UMBER, 500_000.0)

		assertEquals(listOf(CorpseType.UMBER), totals.perType.map { it.first })
		assertEquals(CorpseTotal.NONE, totals.total(CorpseType.LAPIS))
	}

	@Test
	fun `per type keeps enum order`() {
		val totals = CorpseTotals()
		totals.add(CorpseType.VANGUARD, 1.0)
		totals.add(CorpseType.LAPIS, 1.0)
		totals.add(CorpseType.UMBER, 1.0)

		assertEquals(listOf(CorpseType.LAPIS, CorpseType.UMBER, CorpseType.VANGUARD), totals.perType.map { it.first })
	}

	@Test
	fun `nothing looted is an empty total, not a null`() {
		assertEquals(CorpseTotal.NONE, CorpseTotals().overall)
		assertTrue(CorpseTotals().perType.isEmpty())
	}

	@Test
	fun `survives a round trip through storage`() {
		val totals = CorpseTotals()
		totals.add(CorpseType.LAPIS, 200_000.0)
		totals.add(CorpseType.TUNGSTEN, -1_500_000.0)

		val coins = mutableMapOf<String, Double>()
		val corpses = mutableMapOf<String, Int>()
		totals.saveInto(coins, corpses)

		val loaded = CorpseTotals()
		loaded.load(coins, corpses)
		assertEquals(totals.overall, loaded.overall)
		assertEquals(totals.total(CorpseType.LAPIS), loaded.total(CorpseType.LAPIS))
		assertEquals(totals.total(CorpseType.TUNGSTEN), loaded.total(CorpseType.TUNGSTEN))
	}

	@Test
	fun `stored totals are keyed by name, so a reordered enum keeps its coins`() {
		val loaded = CorpseTotals()
		loaded.load(mapOf("VANGUARD" to -29_000_000.0), mapOf("VANGUARD" to 1))
		assertEquals(CorpseTotal(1, -29_000_000.0), loaded.total(CorpseType.VANGUARD))
	}

	@Test
	fun `a name that is no longer a corpse type is ignored`() {
		val loaded = CorpseTotals()
		loaded.load(mapOf("GLACITE" to 5.0, "LAPIS" to 7.0), mapOf("GLACITE" to 1, "LAPIS" to 1))
		assertEquals(CorpseTotal(1, 7.0), loaded.overall)
	}

	@Test
	fun `reset clears everything, saving an empty state`() {
		val totals = CorpseTotals()
		totals.add(CorpseType.LAPIS, 200_000.0)
		totals.reset()

		val coins = mutableMapOf("stale" to 1.0)
		val corpses = mutableMapOf("stale" to 1)
		totals.saveInto(coins, corpses)
		assertEquals(CorpseTotal.NONE, totals.overall)
		assertTrue(coins.isEmpty())
		assertTrue(corpses.isEmpty())
	}
}
