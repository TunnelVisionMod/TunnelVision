package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.cutloose

import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.AttackEntityEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EntityDeathEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudManager
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudPosition
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudWidget
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import net.minecraft.ChatFormatting
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player

object CutLooseTracker : Feature {
	private const val NPC_UUID_VERSION = 2

	private val config get() = ConfigManager.config.mineshaft.cutLooseTracker
	private val counter = CutLooseCounter()

	override fun init() {
		EventBus.on<AttackEntityEvent> { onAttack(it) }
		EventBus.on<EntityDeathEvent> { onDeath(it) }
		EventBus.on<LocationChangedEvent> { counter.reset() }
		HudManager.register(Widget)
	}

	private fun onAttack(event: AttackEntityEvent) {
		if (!config.enabled || !SkyBlock.isInMineshaft || !event.entity.isMob()) return
		counter.onHit(event.entity.id)
	}

	private fun onDeath(event: EntityDeathEvent) {
		if (!config.enabled || !SkyBlock.isInMineshaft) return
		if (counter.onDeath(event.entity.id)) {
			Debug.log { "CutLooseTracker: kill ${counter.kills}/${CutLooseCounter.MAX_KILLS} (${event.entity.name.string})" }
		}
	}

	private fun Entity.isMob(): Boolean = when (this) {
		is ArmorStand, is LocalPlayer -> false
		is Player -> uuid.version() == NPC_UUID_VERSION
		else -> this is LivingEntity
	}

	private fun line(kills: Int): Component {
		val color = if (kills >= CutLooseCounter.MAX_KILLS) ChatFormatting.GREEN else ChatFormatting.YELLOW
		return Component.literal("Cut Loose: ").withStyle(ChatFormatting.GOLD)
			.append(Component.literal("$kills/${CutLooseCounter.MAX_KILLS}").withStyle(color))
	}

	object Widget : HudWidget("cut_loose_tracker", "Cut Loose Tracker", HudPosition(0.02f, 0.5f)) {
		override val isEnabled get() = config.enabled

		override fun getLines(): List<Component> =
			if (SkyBlock.isInMineshaft) listOf(line(counter.kills)) else emptyList()

		override fun getExampleLines() = listOf(line(3))
	}
}
