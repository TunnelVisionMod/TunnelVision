package io.github.tunnelvisionmod.tunnelvision.features.mining.pickaxe

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CooldownSyncTest {
	@Test
	fun `starts countdown from tab when idle`() {
		assertEquals(42 * 20, CooldownSync.resync(ticksLeft = 0, tabSeconds = 42))
	}

	@Test
	fun `ignores short tab cooldown when idle`() {
		assertNull(CooldownSync.resync(ticksLeft = 0, tabSeconds = 1))
	}

	@Test
	fun `never counts up when tab lags behind`() {
		assertNull(CooldownSync.resync(ticksLeft = 40 * 20, tabSeconds = 42))
		assertNull(CooldownSync.resync(ticksLeft = 10 * 20, tabSeconds = 60))
	}

	@Test
	fun `ignores tab rounding`() {
		assertNull(CooldownSync.resync(ticksLeft = 42 * 20 + 15, tabSeconds = 42))
	}

	@Test
	fun `corrects down when timer is too slow`() {
		assertEquals(30 * 20, CooldownSync.resync(ticksLeft = 45 * 20, tabSeconds = 30))
	}

	@Test
	fun `available near the end still fires ready`() {
		assertTrue(CooldownSync.firesOnAvailable(ticksLeft = 2 * 20))
		assertTrue(CooldownSync.firesOnAvailable(ticksLeft = 1))
	}

	@Test
	fun `available long before the end resets silently`() {
		assertFalse(CooldownSync.firesOnAvailable(ticksLeft = 16 * 20))
	}

	@Test
	fun `available while idle stays idle`() {
		assertFalse(CooldownSync.firesOnAvailable(ticksLeft = 0))
	}

	@Test
	fun `waits for a lagging server instead of counting up`() {
		assertTrue(CooldownSync.isAheadOfServer(ticksLeft = 10 * 20, tabSeconds = 12))
		assertTrue(CooldownSync.isAheadOfServer(ticksLeft = 1, tabSeconds = 2))
	}

	@Test
	fun `keeps counting within tab rounding`() {
		assertFalse(CooldownSync.isAheadOfServer(ticksLeft = 10 * 20, tabSeconds = 11))
		assertFalse(CooldownSync.isAheadOfServer(ticksLeft = 1, tabSeconds = 1))
		assertFalse(CooldownSync.isAheadOfServer(ticksLeft = 30 * 20, tabSeconds = 20))
	}
}
