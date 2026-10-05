package io.github.tunnelvisionmod.tunnelvision.utils

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.core.events.DisconnectEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.post
import net.hypixel.data.type.GameType
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket

object SkyBlock {
	private const val MINESHAFT_ISLAND = "mineshaft"
	private const val DWARVEN_MINES_ISLAND = "mining_3"
	private val MINING_ISLANDS = setOf(DWARVEN_MINES_ISLAND, "crystal_hollows", MINESHAFT_ISLAND)

	var isOnSkyBlock = false
		private set
	var island: String? = null
		private set

	/** The server instance you are on, e.g. `mini123A`; every mineshaft is its own instance. */
	var server: String? = null
		private set

	val isInMineshaft: Boolean get() = isOnSkyBlock && island == MINESHAFT_ISLAND
	val isInDwarvenMines: Boolean get() = isOnSkyBlock && island == DWARVEN_MINES_ISLAND
	val isOnMiningIsland: Boolean get() = isOnSkyBlock && island in MINING_ISLANDS

	fun register() {
		val api = HypixelModAPI.getInstance()
		api.subscribeToEventPacket(ClientboundLocationPacket::class.java)
		api.createHandler(ClientboundLocationPacket::class.java) { packet ->
			mc.execute {
				val onSkyBlock = packet.serverType.orElse(null) == GameType.SKYBLOCK
				update(onSkyBlock, if (onSkyBlock) packet.mode.orElse(null) else null, packet.serverName)
			}
		}
		EventBus.on<DisconnectEvent> { update(false, null, null) }
	}

	private fun update(onSkyBlock: Boolean, newIsland: String?, newServer: String?) {
		isOnSkyBlock = onSkyBlock
		island = newIsland
		server = newServer
		Debug.log { "Location: onSkyBlock=$onSkyBlock island=$newIsland server=$newServer" }
		LocationChangedEvent(onSkyBlock, newIsland, newServer).post()
	}
}
