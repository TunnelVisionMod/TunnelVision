package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.routes

import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudManager
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudPosition
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudWidget
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftRole
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftState
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftTodoState
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.Todo
import io.github.tunnelvisionmod.tunnelvision.data.value.ShaftVerdict
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.LocationTracker
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object GemstoneRoutes : Feature {
	private val config get() = ConfigManager.config.mineshaft.gemstones.gemstoneRoutes

	private var running: RouteChoice? = null
	private val location = LocationTracker()

	/** Why a route that exists for this shaft is not showing yet. */
	private sealed interface Waiting {
		data object Value : Waiting
		data object NotWorthMining : Waiting
		data class Todos(val left: Int) : Waiting
	}

	override fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<LocationChangedEvent> { onLocationChanged(it) }
		HudManager.register(Widget)
	}

	private fun onLocationChanged(event: LocationChangedEvent) {
		if (!location.isNewLocation(event)) return
		if (running != null) RouteRunner.stop()
		running = null
	}

	private fun onTick() {
		if (!config.enabled || !SkyBlock.isInMineshaft) return
		val type = MineshaftState.type ?: return
		val choice = choice() ?: return
		if (choice == running) return
		if (running == null && waiting() != null) return
		val route = RouteLibrary.load(choice.name) ?: return
		running = choice
		RouteRunner.start(route.part(choice.part)) { config.enabled && SkyBlock.isInMineshaft }
		Debug.log { "GemstoneRoutes: ${choice.name} ${choice.part} for ${type.code} (${config.start})" }
	}

	private fun choice(): RouteChoice? {
		val type = MineshaftState.type ?: return null
		return RouteChoice.of(type.code, MineshaftRole.role, RouteLibrary::exists)
	}

	private fun waiting(): Waiting? {
		if (config.start == RouteStart.ON_ENTRY) return null
		val verdict = ShaftVerdict.current() ?: return Waiting.Value
		if (!verdict.shouldMine) return Waiting.NotWorthMining
		val todos = MineshaftTodoState.current ?: return Waiting.Value
		if (todos.done) return null
		return Waiting.Todos(todos.todos.sumOf { if (it is Todo.Corpse) (if (it.hasKey) it.count else 0) else 1 })
	}

	private fun line(waiting: Waiting): Component {
		val label = Component.literal("Route: ").withStyle(ChatFormatting.GREEN)
		return when (waiting) {
			Waiting.Value -> label.append(Component.literal("checking value...").withStyle(ChatFormatting.GRAY))
			Waiting.NotWorthMining -> label.append(Component.literal("not worth mining").withStyle(ChatFormatting.RED))
			is Waiting.Todos -> label.append(Component.literal("${waiting.left} to-do${if (waiting.left == 1) "" else "s"} left").withStyle(ChatFormatting.YELLOW))
		}
	}

	object Widget : HudWidget("route_status", "Route Status", HudPosition(0.02f, 0.65f)) {
		override val isEnabled get() = config.enabled && config.statusWidget && config.start == RouteStart.AFTER_TODOS

		override fun getLines(): List<Component> {
			if (!SkyBlock.isInMineshaft || running != null || choice() == null) return emptyList()
			return listOfNotNull(waiting()?.let { line(it) })
		}

		override fun getExampleLines() = listOf(line(Waiting.Todos(2)))
	}
}
