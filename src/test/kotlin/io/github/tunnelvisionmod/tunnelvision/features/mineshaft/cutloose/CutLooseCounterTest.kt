package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.cutloose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CutLooseCounterTest {
	private val counter = CutLooseCounter()

	@Test
	fun `counts mob you hit before it died`() {
		counter.onHit(entityId = 1)
		assertTrue(counter.onDeath(entityId = 1))
		assertEquals(1, counter.kills)
	}

	@Test
	fun `counts mob you hit long before it died`() {
		counter.onHit(entityId = 1)
		counter.onHit(entityId = 2)
		counter.onDeath(entityId = 2)
		assertTrue(counter.onDeath(entityId = 1))
	}

	@Test
	fun `ignores mob you never hit`() {
		assertFalse(counter.onDeath(entityId = 1))
		assertEquals(0, counter.kills)
	}

	@Test
	fun `counts each death only once`() {
		counter.onHit(entityId = 1)
		counter.onDeath(entityId = 1)
		assertFalse(counter.onDeath(entityId = 1))
		assertEquals(1, counter.kills)
	}

	@Test
	fun `stops at max stacks`() {
		repeat(12) {
			counter.onHit(entityId = it)
			counter.onDeath(entityId = it)
		}
		assertEquals(CutLooseCounter.MAX_KILLS, counter.kills)
	}

	@Test
	fun `reset clears kills and hits`() {
		counter.onHit(entityId = 1)
		counter.onDeath(entityId = 1)
		counter.onHit(entityId = 2)
		counter.reset()
		assertEquals(0, counter.kills)
		assertFalse(counter.onDeath(entityId = 2))
	}
}
