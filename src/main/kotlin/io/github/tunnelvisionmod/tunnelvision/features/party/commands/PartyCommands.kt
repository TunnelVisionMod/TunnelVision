package io.github.tunnelvisionmod.tunnelvision.features.party.commands

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ChatReceivedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftRole
import io.github.tunnelvisionmod.tunnelvision.data.party.OwnWarpGuard
import io.github.tunnelvisionmod.tunnelvision.data.party.PartyChatParser
import io.github.tunnelvisionmod.tunnelvision.utils.Cooldown
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock

object PartyCommands : Feature {
	private const val WARP_COOLDOWN_MS = 5_000L

	private val config get() = ConfigManager.config.party.partyCommands
	private val warpCooldown = Cooldown(WARP_COOLDOWN_MS)

	override fun init() {
		EventBus.on<ChatReceivedEvent> { onChat(it) }
	}

	private fun onChat(event: ChatReceivedEvent) {
		if (!config.enabled || !SkyBlock.isOnSkyBlock) return
		val message = PartyChatParser.parse(event.text) ?: return
		val command = PartyCommand.parse(message.message) ?: return
		if (!command.isEnabled() || !command.canBeUsedBy(message.author, mc.user.name)) return
		if (command == PartyCommand.WARP && (OwnWarpGuard.consume(message.author, mc.user.name) || !warpCooldown.tryUse(System.currentTimeMillis()))) return
		val hypixelCommand = command.hypixelCommand(message.author)
		Debug.log { "PartyCommands: ${message.author} used ${message.message} -> /$hypixelCommand" }
		mc.connection?.sendCommand(hypixelCommand)
		if (command == PartyCommand.WARP) MineshaftRole.onPartyWarped()
	}

	private fun PartyCommand.isEnabled(): Boolean = when (this) {
		PartyCommand.TRANSFER -> config.transfer
		PartyCommand.WARP -> config.warp
	}
}
