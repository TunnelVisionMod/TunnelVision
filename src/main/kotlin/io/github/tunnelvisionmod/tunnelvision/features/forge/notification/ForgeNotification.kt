package io.github.tunnelvisionmod.tunnelvision.features.forge.notification

import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.DisconnectEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSound
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSounds
import io.github.tunnelvisionmod.tunnelvision.data.forge.ForgeParser
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.TabList
import io.github.tunnelvisionmod.tunnelvision.utils.Titles
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object ForgeNotification : Feature {
	private const val MISSING_WIDGET_HINT_TICKS = 200

	private val config get() = ConfigManager.config.forge.forgeNotification
	private val tracker = ForgeTracker()

	private var ticksWithoutWidget = 0
	private var hintShown = false

	override fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<LocationChangedEvent> {
			tracker.reset()
			ticksWithoutWidget = 0
		}
		EventBus.on<DisconnectEvent> { hintShown = false }
	}

	private fun onTick() {
		if (!config.enabled || !SkyBlock.isInDwarvenMines) return
		val slots = ForgeParser.parseTab(TabList.lines)
		if (slots == null) {
			if (!hintShown && ++ticksWithoutWidget >= MISSING_WIDGET_HINT_TICKS) {
				hintShown = true
				ChatUtils.send(Component.literal("Enable the Forges widget in /widget to get Forge notifications.").withStyle(ChatFormatting.YELLOW))
			}
			return
		}
		val ready = tracker.newlyReady(slots)
		if (ready.isNotEmpty()) notify(ready.map { it.item })
	}

	private fun notify(items: List<String>) {
		Debug.log { "ForgeNotification: ready $items" }
		if (config.showTitle) {
			val subtitle = Component.literal(ForgeSummary.subtitle(items)).withStyle(ChatFormatting.YELLOW)
			Titles.show(Component.literal("FORGE READY!").withStyle(ChatFormatting.GOLD), TitleSound.FORGE_READY, 0, 50, 10, subtitle)
		} else {
			TitleSounds.play(TitleSound.FORGE_READY)
		}
		if (config.sendChat) {
			val verb = if (items.size == 1) "is" else "are"
			ChatUtils.send(
				Component.literal(ForgeSummary.grouped(items).joinToString(", ")).withStyle(ChatFormatting.YELLOW)
					.append(Component.literal(" $verb ready in the Forge!").withStyle(ChatFormatting.GREEN))
			)
		}
	}
}
