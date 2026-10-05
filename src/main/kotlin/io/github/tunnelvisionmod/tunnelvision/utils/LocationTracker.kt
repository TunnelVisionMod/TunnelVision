package io.github.tunnelvisionmod.tunnelvision.utils

import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent

/**
 * Tells an actual move apart from Hypixel repeating the location packet for the place you are in.
 *
 * Comparing the island alone misses a party warp from one mineshaft into another - both are the
 * `mineshaft` island, so the move would look like a repeat and features would keep the data of the
 * mineshaft you just left. The server instance changes on every warp, so it catches those too.
 */
class LocationTracker {
	private var island: String? = null
	private var server: String? = null

	fun isNewLocation(event: LocationChangedEvent): Boolean {
		if (event.island == island && event.server == server) return false
		island = event.island
		server = event.server
		return true
	}

	fun forget() {
		island = null
		server = null
	}
}
