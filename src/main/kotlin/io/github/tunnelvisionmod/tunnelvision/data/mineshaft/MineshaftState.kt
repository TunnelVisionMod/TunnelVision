package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.DisconnectEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.MineshaftEnteredEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.post
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.LocationTracker
import io.github.tunnelvisionmod.tunnelvision.utils.Sidebar
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.TabList

/**
 * Tracks which Glacite Mineshaft you are in and what it still holds.
 *
 * The type comes off the scoreboard and [MineshaftEnteredEvent] is posted as soon as it is known. It is re-read for as
 * long as you are in the mineshaft so a warp that lands before the new scoreboard still ends up right. The corpse count
 * comes from a tab list widget that Hypixel fills in seconds later, so the entered event
 * deliberately does not wait for it - [corpseCount] is kept live for features that want it.
 */
object MineshaftState {
	private const val MINESHAFT_ISLAND = "mineshaft"

	/** The current mineshaft, or null when not in one (or not identified yet). */
	var type: MineshaftType? = null
		private set

	/** Corpses listed in the tab widget right now, or null while the widget is absent. */
	var corpseCount: Int? = null
		private set

	private val location = LocationTracker()

	fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<LocationChangedEvent> { onLocationChanged(it) }
		EventBus.on<DisconnectEvent> {
			location.forget()
			reset()
		}
	}

	// Only an actual move starts a new mineshaft; Hypixel may repeat the location packet.
	private fun onLocationChanged(event: LocationChangedEvent) {
		if (!location.isNewLocation(event)) return
		reset()
	}

	private fun onTick() {
		if (SkyBlock.island != MINESHAFT_ISLAND) return

		val count = MineshaftParser.parseCorpseCount(TabList.lines)
		if (count != corpseCount) Debug.log { "Mineshaft: corpses -> ${count ?: "widget absent"}" }
		corpseCount = count

		// Keep following the sidebar instead of latching the first read: right after a party warp it
		// can still name the mineshaft we just left, and the announcement is keyed on the type changing.
		val detected = MineshaftParser.parseType(Sidebar.lines) ?: return
		if (detected == type) return
		type?.let { Debug.log { "Mineshaft: type corrected ${it.code} -> ${detected.code}" } }
		type = detected
		Debug.log { "Mineshaft: entered ${detected.code}" }
		MineshaftEnteredEvent(detected).post()
	}

	private fun reset() {
		type = null
		corpseCount = null
	}
}
