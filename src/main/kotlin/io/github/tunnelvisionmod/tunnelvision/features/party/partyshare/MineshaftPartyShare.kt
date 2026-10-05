package io.github.tunnelvisionmod.tunnelvision.features.party.partyshare

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.DisconnectEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftParser
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftRole
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftState
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftType
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.LocationTracker
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.TabList

/**
 * Announces the mineshaft you just entered to your party, e.g.
 * `!ptme Mineshafttype: JASP_1, Corpses: Lapis 2, Tungsten 1`.
 *
 * Sent once per mineshaft, and only once the corpse widget has stopped changing - the party
 * cannot un-see a message sent off a half-filled widget.
 */
object MineshaftPartyShare : Feature {
	/** How long the corpse list has to hold steady before it is safe to broadcast. */
	private const val SETTLE_TICKS = 20

	private val config get() = ConfigManager.config.party.partyShare

	private var sent = false
	private var lastCorpses: Map<String, Int>? = null
	private var stableTicks = 0
	private val location = LocationTracker()

	override fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<LocationChangedEvent> { onLocationChanged(it) }
		EventBus.on<DisconnectEvent> {
			location.forget()
			reset()
		}
	}

	private fun onLocationChanged(event: LocationChangedEvent) {
		if (!location.isNewLocation(event)) return
		reset()
	}

	private fun onTick() {
		if (!config.enabled || !SkyBlock.isInMineshaft) return
		// Announcing a mineshaft you were warped into would hand party lead back to you and prompt
		// the host to warp into the shaft they are already standing in.
		if (MineshaftRole.isWarpedIn) return

		val corpses = MineshaftParser.parseCorpses(TabList.lines)
		if (corpses != lastCorpses) stableTicks = 0 else stableTicks++
		lastCorpses = corpses

		if (sent) return
		val type = MineshaftState.type ?: return
		// No corpse data means nothing worth sending yet; an empty widget is not yet filled in.
		if (corpses.isNullOrEmpty() || stableTicks < SETTLE_TICKS) return

		sent = true
		val message = buildMessage(type, corpses)
		Debug.log { "MineshaftPartyShare: /pc $message" }
		mc.connection?.sendCommand("pc $message")
	}

	/** e.g. `!ptme Mineshafttype: JASP_1, Corpses: Lapis 2, Tungsten 1, Umber 1` */
	fun buildMessage(type: MineshaftType, corpses: Map<String, Int>): String {
		val breakdown = corpses.entries.joinToString(", ") { (name, count) -> "$name $count" }
		return "!ptme Mineshafttype: ${type.code}, Corpses: $breakdown"
	}

	private fun reset() {
		sent = false
		lastCorpses = null
		stableTicks = 0
	}
}
