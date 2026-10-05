package io.github.tunnelvisionmod.tunnelvision.features.mining.pickaxe

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ChatReceivedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.DisconnectEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudManager
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudPosition
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudWidget
import io.github.tunnelvisionmod.tunnelvision.features.mining.pickaxe.CooldownSync.TICKS_PER_SECOND
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.TabList
import io.github.tunnelvisionmod.tunnelvision.utils.loreLines
import net.minecraft.ChatFormatting
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents

object PickaxeAbility : Feature {
	private val config get() = ConfigManager.config.mining.pickaxeAbility

	private var ability: String? = null
	private var ticksLeft = 0
	private var waitingTicks = 0
	private var lastMiningToolLore: List<String>? = null

	override fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<ChatReceivedEvent> { onChat(it) }
		EventBus.on<DisconnectEvent> { reset() }
		EventBus.on<LocationChangedEvent> { stop() }
		HudManager.register(Widget)
	}

	private fun onTick() {
		if (!config.enabled || !SkyBlock.isOnSkyBlock) return
		rememberHeldMiningTool()
		val tab = PickaxeAbilityParser.parseTab(TabList.lines)
		tab?.let { syncWithTab(it) }
		countDown(tab?.secondsLeft)
	}

	private fun countDown(tabSeconds: Int?) {
		if (ticksLeft <= 0) return
		if (tabSeconds != null && CooldownSync.isAheadOfServer(ticksLeft, tabSeconds)) return
		if (ticksLeft > 1) {
			ticksLeft--
			waitingTicks = 0
			return
		}
		if (++waitingTicks < CooldownSync.READY_GRACE_TICKS) return
		Debug.log { "PickaxeAbility: no available message from the server, ready anyway" }
		ready()
	}

	private fun syncWithTab(tab: TabAbility) {
		ability = tab.name
		val seconds = tab.secondsLeft
		if (seconds == null) {
			if (ticksLeft > 0) {
				val fire = CooldownSync.firesOnAvailable(ticksLeft)
				Debug.log { "PickaxeAbility: ${tab.name} available via tab, ${if (fire) "ready" else "reset silently"}" }
				if (fire) ready() else stop()
			}
			return
		}
		val synced = CooldownSync.resync(ticksLeft, seconds) ?: return
		Debug.log { "PickaxeAbility: Cooldown(${tab.name}, ${seconds}s) via tab" }
		ticksLeft = synced
	}

	private fun onChat(event: ChatReceivedEvent) {
		if (!config.enabled || !SkyBlock.isOnSkyBlock) return
		PickaxeAbilityParser.parseAvailableMessage(event.text)?.let { name ->
			if (ticksLeft > 0 && (ability == null || name == ability)) {
				Debug.log { "PickaxeAbility: $name available via chat" }
				ready()
			}
			return
		}
		val name = PickaxeAbilityParser.parseUsedMessage(event.text) ?: return
		ability = name
		if (PickaxeAbilityParser.parseTab(TabList.lines) != null) return
		val lore = heldMiningToolLore() ?: lastMiningToolLore ?: return
		val seconds = PickaxeAbilityParser.parseLoreCooldown(lore) ?: return
		Debug.log { "PickaxeAbility: Cooldown($name, ${seconds}s) via lore" }
		ticksLeft = seconds * TICKS_PER_SECOND
	}

	private fun stop() {
		ticksLeft = 0
		waitingTicks = 0
	}

	private fun ready() {
		stop()
		onReady()
	}

	private fun onReady() {
		val name = ability ?: return
		Debug.log { "PickaxeAbility: $name ready" }
		if (config.showTitle) {
			Compat.setTitleTimes(0, 50, 10)
			Compat.setTitle(Component.literal("${name.uppercase()}!").withStyle(ChatFormatting.GOLD))
		}
		if (config.playSound) {
			mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1f))
		}
	}

	private fun heldMiningToolLore(): List<String>? =
		mc.player?.mainHandItem?.loreLines()?.takeIf { PickaxeAbilityParser.isMiningTool(it) }

	private fun rememberHeldMiningTool() {
		heldMiningToolLore()?.let { lastMiningToolLore = it }
	}

	private fun reset() {
		ability = null
		stop()
		lastMiningToolLore = null
	}

	private fun cooldownLine(name: String, seconds: Int): Component =
		Component.literal("$name: ").withStyle(ChatFormatting.GOLD)
			.append(Component.literal("${seconds}s").withStyle(ChatFormatting.YELLOW))

	object Widget : HudWidget("pickaxe_ability_timer", "Pickaxe Ability Timer", HudPosition(0.02f, 0.4f)) {
		override val isEnabled get() = config.enabled && config.showWidget

		override fun getLines(): List<Component> {
			val name = ability ?: return emptyList()
			if (ticksLeft <= 0) return emptyList()
			return listOf(cooldownLine(name, (ticksLeft + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND))
		}

		override fun getExampleLines() = listOf(cooldownLine("Mining Speed Boost", 42))
	}
}
