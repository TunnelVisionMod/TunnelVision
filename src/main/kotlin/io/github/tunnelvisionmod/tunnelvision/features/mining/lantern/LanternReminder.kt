package io.github.tunnelvisionmod.tunnelvision.features.mining.lantern

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ChatReceivedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.MineshaftEnteredEvent
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSound
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.Titles
import io.github.tunnelvisionmod.tunnelvision.utils.plainName
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

object LanternReminder : Feature {
	private val config get() = ConfigManager.config.mining.lanternReminder

	override fun init() {
		EventBus.on<MineshaftEnteredEvent> { onMineshaftEntered() }
		EventBus.on<ChatReceivedEvent> { onChat(it) }
	}

	private fun onMineshaftEntered() {
		if (!config.enabled || !config.entryReminder) return
		val inventory = mc.player?.inventory ?: return
		val lantern = Lantern.strongest((0 until Inventory.INVENTORY_SIZE).map { inventory.getItem(it).plainName() }) ?: return
		Debug.log { "LanternReminder: reminding to place $lantern" }
		val text = Component.literal("Place your ${lantern.itemName}!").withStyle(ChatFormatting.YELLOW)
		ChatUtils.send(text)
		Titles.addSubtitle(text)
	}

	private fun onChat(event: ChatReceivedEvent) {
		if (!config.enabled || !config.expiredAlert || !SkyBlock.isOnMiningIsland) return
		val lantern = Lantern.fromDespawnMessage(event.text) ?: return
		Debug.log { "LanternReminder: $lantern despawned" }
		val text = Component.literal("${lantern.itemName} expired!").withStyle(ChatFormatting.YELLOW)
		Titles.show(text, TitleSound.LANTERN_EXPIRED, 0, 40, 10)
	}
}
