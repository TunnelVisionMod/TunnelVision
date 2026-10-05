package io.github.tunnelvisionmod.tunnelvision.features.mineshaft

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.features.crystals.CrystalNotifications
import io.github.tunnelvisionmod.tunnelvision.features.crystals.CrystalType
import io.github.tunnelvisionmod.tunnelvision.features.mining.MineshaftDetection
import io.github.tunnelvisionmod.tunnelvision.hud.HudManager
import io.github.tunnelvisionmod.tunnelvision.hud.HudPosition
import io.github.tunnelvisionmod.tunnelvision.hud.HudWidget
import io.github.tunnelvisionmod.tunnelvision.utils.Bazaar
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.TabList
import io.github.tunnelvisionmod.tunnelvision.utils.plainName
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

object CorpsesToLoot {
	private const val CRYSTAL_SUFFIX = "_C"

	private val config get() = ConfigManager.config.mineshaft.corpsesToLoot
	private val valueConfig get() = ConfigManager.config.mineshaft.mineshaftValue

	/** [corpsesKnown] is false while the Frozen Corpses tab widget is missing, so "unknown" never reads as "done". */
	class TodoState(val todos: List<Todo>, val corpsesKnown: Boolean) {
		val done: Boolean get() = corpsesKnown && MineshaftTodos.isDone(todos)
	}

	val state: TodoState?
		get() {
			if (!SkyBlock.isInMineshaft) return null
			val unlooted = CorpseLoot.parseUnlooted(TabList.lines)
			val todos = MineshaftTodos.list(
				unlooted?.let { CorpseLoot.toLoot(it, currentRule()) } ?: emptyMap(),
				carriedItemNames(),
				MineshaftWaypoints.hasPendingFossil,
				MineshaftTodos.crystalToGrab(crystalShaft(), CrystalNotifications.crystalsKnown, CrystalNotifications.carriedCrystals),
				warpedIn = MineshaftRole.isWarpedIn,
			)
			return TodoState(todos, corpsesKnown = unlooted != null)
		}

	val todosDone: Boolean get() = state?.done == true

	fun init() {
		EventBus.on<ClientTickEvent> { if (config.enabled && SkyBlock.isOnMiningIsland) Bazaar.refreshIfStale() }
		HudManager.register(Widget)
	}

	private fun currentRule(): LootRule = CorpseLoot.rule(
		mode = valueConfig.lootMode,
		crystalsFull = CrystalNotifications.crystalsAndForgeFull,
		shouldMine = MineshaftValueAlert.currentVerdict()?.shouldMine,
		openVanguards = valueConfig.openVanguards,
	)

	private fun crystalShaft(): CrystalType? =
		MineshaftDetection.type?.takeIf { it.code.endsWith(CRYSTAL_SUFFIX) }?.let { CrystalType.byDisplayName(it.displayName) }

	private fun carriedItemNames(): Set<String> {
		val inventory = mc.player?.inventory ?: return emptySet()
		return (0 until Inventory.INVENTORY_SIZE).map { inventory.getItem(it).plainName() }.toSet()
	}

	private fun CorpseType.color(): ChatFormatting = when (this) {
		CorpseType.LAPIS -> ChatFormatting.BLUE
		CorpseType.UMBER -> ChatFormatting.GOLD
		CorpseType.TUNGSTEN -> ChatFormatting.GRAY
		CorpseType.VANGUARD -> ChatFormatting.AQUA
	}

	private val header: Component = Component.literal("Mineshaft To-Dos").withStyle(ChatFormatting.GOLD)

	private fun line(todo: Todo): Component = when (todo) {
		is Todo.Corpse -> {
			val line = Component.literal("${todo.type.tabName} ×${todo.count}").withStyle(todo.type.color())
			if (todo.hasKey) line else line.append(Component.literal(" · no ${todo.type.keyName}").withStyle(ChatFormatting.RED))
		}
		Todo.Fossil -> Component.literal("Mine the Fossil").withStyle(ChatFormatting.LIGHT_PURPLE)
		is Todo.GrabCrystal -> Component.literal("Grab ${todo.crystal.displayName} Crystal").withStyle(todo.crystal.color)
	}

	object Widget : HudWidget("corpses_to_loot", "Mineshaft To-Dos", HudPosition(0.02f, 0.7f)) {
		override val isEnabled get() = config.enabled

		override fun getLines(): List<Component> {
			val state = state ?: return emptyList()
			if (state.done || state.todos.isEmpty()) return emptyList()
			return listOf(header) + state.todos.map { line(it) }
		}

		override fun getExampleLines() = listOf(
			header,
			line(Todo.Corpse(CorpseType.LAPIS, 2, hasKey = true)),
			line(Todo.Corpse(CorpseType.UMBER, 1, hasKey = false)),
			line(Todo.Fossil),
			line(Todo.GrabCrystal(CrystalType.JASPER)),
		)
	}
}
