package io.github.tunnelvisionmod.tunnelvision.utils

import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocationTrackerTest {
	private fun at(island: String?, server: String?) = LocationChangedEvent(island != null, island, server)

	@Test
	fun `first location is new`() {
		assertTrue(LocationTracker().isNewLocation(at("mineshaft", "mini1A")))
	}

	@Test
	fun `repeated packet for the same place is not a move`() {
		val tracker = LocationTracker()
		tracker.isNewLocation(at("mineshaft", "mini1A"))
		assertFalse(tracker.isNewLocation(at("mineshaft", "mini1A")))
	}

	@Test
	fun `warping into another mineshaft is a move`() {
		val tracker = LocationTracker()
		tracker.isNewLocation(at("mineshaft", "mini1A"))
		assertTrue(tracker.isNewLocation(at("mineshaft", "mini2B")))
	}

	@Test
	fun `changing island is a move`() {
		val tracker = LocationTracker()
		tracker.isNewLocation(at("mining_3", "mini1A"))
		assertTrue(tracker.isNewLocation(at("mineshaft", "mini1A")))
	}

	@Test
	fun `forgetting makes the same place new again`() {
		val tracker = LocationTracker()
		tracker.isNewLocation(at("mineshaft", "mini1A"))
		tracker.forget()
		assertTrue(tracker.isNewLocation(at("mineshaft", "mini1A")))
	}
}
