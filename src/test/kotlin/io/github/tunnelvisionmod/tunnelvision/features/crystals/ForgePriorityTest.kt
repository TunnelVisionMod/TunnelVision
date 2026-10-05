package io.github.tunnelvisionmod.tunnelvision.features.crystals

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ForgePriorityTest {
	/** Roughly the real spread: Onyx and Peridot dear, Aquamarine and Citrine cheap. */
	private val prices = mapOf(
		CrystalType.ONYX to 11_900_000.0,
		CrystalType.PERIDOT to 10_300_000.0,
		CrystalType.JASPER to 5_300_000.0,
		CrystalType.OPAL to 4_500_000.0,
		CrystalType.RUBY to 2_300_000.0,
		CrystalType.CITRINE to 2_100_000.0,
		CrystalType.AQUAMARINE to 1_900_000.0,
	)

	private fun value(crystal: CrystalType): Double? = prices[crystal]

	@Test
	fun `ranks the most valuable crystal first`() {
		val ranked = ForgePriority.rank(CrystalType.entries, openSlots = 0, value = ::value)
		assertEquals(
			listOf(
				CrystalType.ONYX,
				CrystalType.PERIDOT,
				CrystalType.JASPER,
				CrystalType.OPAL,
				CrystalType.RUBY,
				CrystalType.CITRINE,
				CrystalType.AQUAMARINE,
			),
			ranked.map { it.crystal },
		)
	}

	@Test
	fun `one free slot takes the most valuable crystal`() {
		val carried = listOf(CrystalType.AQUAMARINE, CrystalType.ONYX, CrystalType.CITRINE)
		assertEquals(listOf(CrystalType.ONYX), ForgePriority.toForge(carried, openSlots = 1, value = ::value))
	}

	@Test
	fun `three free slots take the three most valuable`() {
		val carried = listOf(CrystalType.AQUAMARINE, CrystalType.ONYX, CrystalType.CITRINE, CrystalType.PERIDOT)
		assertEquals(
			listOf(CrystalType.ONYX, CrystalType.PERIDOT, CrystalType.CITRINE),
			ForgePriority.toForge(carried, openSlots = 3, value = ::value),
		)
	}

	@Test
	fun `more slots than crystals forges everything carried`() {
		val carried = listOf(CrystalType.AQUAMARINE, CrystalType.ONYX)
		assertEquals(listOf(CrystalType.ONYX, CrystalType.AQUAMARINE), ForgePriority.toForge(carried, 7, ::value))
	}

	/** A full forge means nothing to do, which must not read as "forge the best one anyway". */
	@Test
	fun `a full forge marks nothing`() {
		val ranked = ForgePriority.rank(CrystalType.entries, openSlots = 0, value = ::value)
		assertTrue(ranked.none { it.forgeNow })
		assertTrue(ranked.all { it.value != null })
	}

	@Test
	fun `carrying nothing gives nothing to forge`() {
		assertEquals(emptyList<CrystalType>(), ForgePriority.toForge(emptySet(), openSlots = 7, value = ::value))
	}

	/** Guessing with a missing price would be worse than staying quiet, so unpriced sorts last. */
	@Test
	fun `a crystal with no price is never picked and sorts last`() {
		val carried = listOf(CrystalType.ONYX, CrystalType.JASPER)
		val ranked = ForgePriority.rank(carried, openSlots = 2) { if (it == CrystalType.ONYX) null else 5_300_000.0 }
		assertEquals(listOf(CrystalType.JASPER, CrystalType.ONYX), ranked.map { it.crystal })
		assertTrue(ranked.first { it.crystal == CrystalType.JASPER }.forgeNow)
		assertFalse(ranked.first { it.crystal == CrystalType.ONYX }.forgeNow)
	}

	@Test
	fun `a repeated crystal is only listed once`() {
		val ranked = ForgePriority.rank(listOf(CrystalType.ONYX, CrystalType.ONYX), openSlots = 2, value = ::value)
		assertEquals(1, ranked.size)
	}

	@Test
	fun `negative slots are treated as none`() {
		assertEquals(emptyList<CrystalType>(), ForgePriority.toForge(CrystalType.entries, openSlots = -1, value = ::value))
	}
}
