package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.core.events.ChatReceivedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.DisconnectEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.LocationTracker
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock

/**
 * Which side of a shared mineshaft you are on: you warped someone in, someone warped you in, or
 * you are mining alone.
 *
 * The warp key is pressed before the teleport lands, so a warp counts for [WARP_IN_WINDOW_MS]
 * and arriving in a mineshaft within that window reads as [WarpRole.WARPED_IN].
 *
 * Hypixel's own arrival announcement backs both shared roles up, so they survive a warp the mod
 * never saw: another mod answering `!w`, a `/p warp` typed by hand, or a host warping you long
 * after the key prompt timed out.
 */
object MineshaftRole {
	private const val MINESHAFT_ISLAND = "mineshaft"
	private const val WARP_IN_WINDOW_MS = 60_000L

	private val location = LocationTracker()
	private var warpRequestedAt = 0L

	var role = WarpRole.SOLO
		private set

	/** The fossil and the crystal belong to whoever opened the mineshaft, not to the warped-in player. */
	val isWarpedIn: Boolean get() = role == WarpRole.WARPED_IN

	fun init() {
		EventBus.on<LocationChangedEvent> { onLocationChanged(it) }
		EventBus.on<ChatReceivedEvent> { onChat(it) }
		EventBus.on<DisconnectEvent> {
			location.forget()
			warpRequestedAt = 0L
			role = WarpRole.SOLO
		}
	}

	/** You asked a party member to warp you into the mineshaft they shared. */
	fun onWarpRequested() {
		warpRequestedAt = System.currentTimeMillis()
	}

	/** A party member joined the mineshaft you opened. */
	fun onPartyWarped() = becomeShared(WarpRole.WARPED_SOMEONE, "warped someone in")

	/**
	 * Hypixel announces your own arrival only for a mineshaft someone else opened, never for one
	 * you walked into yourself, so it is the one signal that catches a missed warp window.
	 */
	private fun onOwnArrival() = becomeShared(WarpRole.WARPED_IN, "warped in, window missed")

	/** The arrival lines land a tick or two after the teleport, so only an unclaimed shaft changes side. */
	private fun becomeShared(shared: WarpRole, reason: String) {
		if (!SkyBlock.isInMineshaft || role != WarpRole.SOLO) return
		role = shared
		Debug.log { "MineshaftRole: $reason" }
	}

	private fun onChat(event: ChatReceivedEvent) {
		val player = MineshaftEntryParser.playerEntered(event.text) ?: return
		if (player.equals(mc.user.name, ignoreCase = true)) onOwnArrival() else onPartyWarped()
	}

	private fun onLocationChanged(event: LocationChangedEvent) {
		if (!location.isNewLocation(event)) return
		val warpedIn = event.island == MINESHAFT_ISLAND && System.currentTimeMillis() - warpRequestedAt < WARP_IN_WINDOW_MS
		role = if (warpedIn) WarpRole.WARPED_IN else WarpRole.SOLO
		Debug.log { "MineshaftRole: $role on ${event.server}" }
	}
}
