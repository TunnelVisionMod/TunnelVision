package io.github.tunnelvisionmod.tunnelvision.data.crystals

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CrystalTrackerTest {
	@Test
	fun `chat adds crystals`() {
		val tracker = CrystalTracker()
		assertTrue(tracker.gained(CrystalType.JASPER))
		assertFalse(tracker.gained(CrystalType.JASPER), "the same crystal twice is not news")
		assertEquals(setOf(CrystalType.JASPER), tracker.carried)
	}

	@Test
	fun `the widget can take a crystal away again`() {
		val tracker = CrystalTracker()
		tracker.gained(CrystalType.JASPER)
		tracker.apply(mapOf(CrystalType.JASPER to false))
		assertEquals(emptySet<CrystalType>(), tracker.carried)
	}

	@Test
	fun `the widget only speaks for the crystals it mentions`() {
		val tracker = CrystalTracker()
		tracker.gained(CrystalType.OPAL)
		tracker.apply(mapOf(CrystalType.JASPER to true))
		assertEquals(setOf(CrystalType.OPAL, CrystalType.JASPER), tracker.carried)
	}

	@Test
	fun `hasAll needs every crystal`() {
		val tracker = CrystalTracker()
		for (crystal in CrystalType.entries.dropLast(1)) tracker.gained(crystal)
		assertFalse(tracker.hasAll)
		tracker.gained(CrystalType.entries.last())
		assertTrue(tracker.hasAll)
	}

	@Test
	fun `reset clears everything`() {
		val tracker = CrystalTracker()
		tracker.gained(CrystalType.JASPER)
		tracker.reset()
		assertEquals(emptySet<CrystalType>(), tracker.carried)
		assertFalse(tracker.hasAll)
	}
}
