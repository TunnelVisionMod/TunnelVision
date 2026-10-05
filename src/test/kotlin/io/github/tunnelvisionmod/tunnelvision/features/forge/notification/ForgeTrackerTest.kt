package io.github.tunnelvisionmod.tunnelvision.features.forge.notification

import io.github.tunnelvisionmod.tunnelvision.data.forge.ForgeSlot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ForgeTrackerTest {
	private val tracker = ForgeTracker()
	private val diamondRunning = ForgeSlot(1, "Refined Diamond", false)
	private val diamondReady = ForgeSlot(1, "Refined Diamond", true)
	private val rubyReady = ForgeSlot(2, "Perfect Ruby Gemstone", true)

	@Test
	fun `reports everything ready on first update`() {
		assertEquals(listOf(diamondReady, rubyReady), tracker.newlyReady(listOf(diamondReady, rubyReady)))
	}

	@Test
	fun `reports slot when it becomes ready`() {
		assertEquals(emptyList<ForgeSlot>(), tracker.newlyReady(listOf(diamondRunning)))
		assertEquals(listOf(diamondReady), tracker.newlyReady(listOf(diamondReady)))
	}

	@Test
	fun `reports each ready item only once`() {
		tracker.newlyReady(listOf(diamondReady))
		assertEquals(emptyList<ForgeSlot>(), tracker.newlyReady(listOf(diamondReady)))
	}

	@Test
	fun `reports again after slot was collected and refilled`() {
		tracker.newlyReady(listOf(diamondReady))
		tracker.newlyReady(emptyList())
		tracker.newlyReady(listOf(diamondRunning))
		assertEquals(listOf(diamondReady), tracker.newlyReady(listOf(diamondReady)))
	}

	@Test
	fun `reports again after reset`() {
		tracker.newlyReady(listOf(diamondReady))
		tracker.reset()
		assertEquals(listOf(diamondReady), tracker.newlyReady(listOf(diamondReady)))
	}
}
