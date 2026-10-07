package io.github.tunnelvisionmod.tunnelvision.features.mining.effects

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ChatReceivedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.DisconnectEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.RightClickEvent
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudManager
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudPosition
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudWidget
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSound
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.TabList
import io.github.tunnelvisionmod.tunnelvision.utils.Titles
import io.github.tunnelvisionmod.tunnelvision.utils.formatDuration
import io.github.tunnelvisionmod.tunnelvision.utils.loreLines
import io.github.tunnelvisionmod.tunnelvision.utils.plainName
import io.github.tunnelvisionmod.tunnelvision.utils.removeFormatting
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

object MiningEffects : Feature {
	private const val CONSUME_CHECK_TICKS = 20
	private const val POTION_MESSAGE_WINDOW_MS = 5_000L

	private val config get() = ConfigManager.config.mining.miningEffects
	private val timers = EffectTimers()

	private var pendingConsume: PendingConsume? = null
	private var lastPotion: HeldPotion? = null

	private class PendingConsume(val effect: MiningEffect, val slot: Int, val count: Int, var ticksLeft: Int)

	private class HeldPotion(val effect: MiningEffect, val seconds: Int, val time: Long)

	override fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<ChatReceivedEvent> { onChat(it) }
		EventBus.on<RightClickEvent> { onRightClick() }
		EventBus.on<DisconnectEvent> {
			timers.clear()
			pendingConsume = null
			lastPotion = null
		}
		HudManager.register(Widget)
	}

	private fun onTick() {
		if (!config.enabled || !SkyBlock.isOnSkyBlock) return
		val now = System.currentTimeMillis()
		for (tab in EffectParser.parseTab(TabList.lines + TabList.footerLines)) {
			if (timers.sync(tab, now)) Debug.log { "MiningEffects: ${tab.effect.displayName} ${tab.seconds}s via tab" }
		}
		readEffectsMenu(now)
		checkPendingConsume(now)
		timers.expire(now).forEach { onExpired(it) }
	}

	private fun readEffectsMenu(now: Long) {
		val screen = Compat.screen as? AbstractContainerScreen<*> ?: return
		if (!EffectParser.isEffectsMenu(screen.title.string.removeFormatting().trim())) return
		for (slot in screen.menu.slots) {
			val item = slot.item
			if (item.isEmpty) continue
			val reading = EffectParser.parseMenuItem(item.plainName(), item.loreLines()) ?: continue
			if (timers.sync(reading, now)) Debug.log { "MiningEffects: ${reading.effect.displayName} ${reading.seconds}s via /effects" }
		}
	}

	private fun onExpired(effect: MiningEffect) {
		Debug.log { "MiningEffects: ${effect.displayName} expired" }
		if (!config.showExpiredTitle || !SkyBlock.isOnMiningIsland) return
		Titles.show(Component.literal("${effect.displayName} expired!").withStyle(ChatFormatting.RED), TitleSound.MINING_EFFECT_EXPIRED, 0, 50, 10)
	}

	private fun onChat(event: ChatReceivedEvent) {
		if (!config.enabled || !SkyBlock.isOnSkyBlock) return
		val effect = EffectParser.parseGainedMessage(event.text) ?: return
		val now = System.currentTimeMillis()
		val potionSeconds = lastPotion?.takeIf { it.effect == effect && now - it.time <= POTION_MESSAGE_WINDOW_MS }?.seconds
		val duration = effect.durationSeconds(potionSeconds, config.potionAffinity.bonusPercent)
		Debug.log { "MiningEffects: gained ${effect.displayName} via chat, potion ${potionSeconds}s -> ${duration}s" }
		timers.start(effect, now, duration)
		lastPotion = null
	}

	private fun onRightClick() {
		if (!config.enabled || !SkyBlock.isOnSkyBlock) return
		val inventory = mc.player?.inventory ?: return
		val held = inventory.selectedItem
		EffectParser.parsePotionLore(held.loreLines())?.let { (potion, seconds) ->
			lastPotion = HeldPotion(potion, seconds, System.currentTimeMillis())
		}
		val effect = held.consumableEffect() ?: return
		pendingConsume = PendingConsume(effect, inventory.selectedSlot, held.count, CONSUME_CHECK_TICKS)
	}

	private fun checkPendingConsume(now: Long) {
		val pending = pendingConsume ?: return
		val stack = mc.player?.inventory?.getItem(pending.slot) ?: return
		if (stack.consumableEffect() != pending.effect || stack.count < pending.count) {
			Debug.log { "MiningEffects: consumed ${pending.effect.displayName}" }
			timers.start(pending.effect, now, pending.effect.durationSeconds(null, 0))
			pendingConsume = null
		} else if (--pending.ticksLeft <= 0) {
			pendingConsume = null
		}
	}

	private fun ItemStack.consumableEffect(): MiningEffect? {
		if (isEmpty) return null
		val name = plainName()
		return MiningEffect.entries.firstOrNull { it.itemName != null && name == it.itemName }
	}

	private fun line(effect: MiningEffect, remaining: Long?): Component {
		val color = when (effect) {
			MiningEffect.COLD_RESISTANCE -> ChatFormatting.AQUA
			MiningEffect.FILET_O_FORTUNE -> ChatFormatting.GOLD
		}
		val time = remaining?.let { Component.literal(formatDuration(it)).withStyle(ChatFormatting.WHITE) }
			?: Component.literal("open /effects").withStyle(ChatFormatting.GRAY)
		return Component.literal("${effect.displayName}: ").withStyle(color).append(time)
	}

	object Widget : HudWidget("mining_effects", "Mining Effects", HudPosition(0.02f, 0.6f)) {
		override val isEnabled get() = config.enabled

		override fun getLines(): List<Component> {
			if (!SkyBlock.isOnMiningIsland) return emptyList()
			return timers.remaining(System.currentTimeMillis()).map { (effect, remaining) -> line(effect, remaining) }
		}

		override fun getExampleLines() = listOf(
			line(MiningEffect.COLD_RESISTANCE, 750_000),
			line(MiningEffect.FILET_O_FORTUNE, 3_484_000),
		)
	}
}
