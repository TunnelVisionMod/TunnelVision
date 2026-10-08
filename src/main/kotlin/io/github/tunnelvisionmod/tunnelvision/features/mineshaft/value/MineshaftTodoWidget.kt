package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.value

import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudManager
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudPosition
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudWidget
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftTodoState
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.Todo
import io.github.tunnelvisionmod.tunnelvision.data.value.ColdResistance
import io.github.tunnelvisionmod.tunnelvision.data.value.ShaftVerdict
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

/** The to-dos of the shaft you are in, under the [ShaftVerdict]. Part of Mineshaft Value. */
object MineshaftTodoWidget : Feature {
	private val config get() = ConfigManager.config.mineshaft.gemstones.mineshaftValue

	override fun init() {
		HudManager.register(Widget)
	}

	private fun CorpseType.color(): ChatFormatting = when (this) {
		CorpseType.LAPIS -> ChatFormatting.BLUE
		CorpseType.UMBER -> ChatFormatting.GOLD
		CorpseType.TUNGSTEN -> ChatFormatting.GRAY
		CorpseType.VANGUARD -> ChatFormatting.AQUA
	}

	private fun header(shouldMine: Boolean?): Component {
		val header = Component.literal("Mineshaft To-Dos").withStyle(ChatFormatting.GOLD)
		shouldMine ?: return header
		header.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
		return if (shouldMine) {
			header.append(Component.literal("MINE").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
		} else {
			header.append(Component.literal("DON'T MINE").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
		}
	}

	private val hotmHint: Component = Component.literal("open /hotm for crystals").withStyle(ChatFormatting.GRAY)
	private val coldHint: Component = Component.literal("add Cold Resistance to the Stats widget (/widget)").withStyle(ChatFormatting.GRAY)

	private fun line(todo: Todo): Component = when (todo) {
		is Todo.Corpse -> {
			val line = Component.literal("${todo.type.tabName} ×${todo.count}").withStyle(todo.type.color())
			if (todo.hasKey) line else line.append(Component.literal(" · no ${todo.type.keyName}").withStyle(ChatFormatting.RED))
		}
		Todo.Fossil -> Component.literal("Mine the Fossil").withStyle(ChatFormatting.LIGHT_PURPLE)
		is Todo.GrabCrystal -> Component.literal("Grab ${todo.crystal.displayName} Crystal").withStyle(todo.crystal.color)
	}

	object Widget : HudWidget("corpses_to_loot", "Mineshaft To-Dos", HudPosition(0.02f, 0.7f)) {
		override val isEnabled get() = config.enabled && config.todoWidget

		override fun getLines(): List<Component> {
			val state = MineshaftTodoState.current ?: return emptyList()
			val needsHotm = MineshaftTodoState.needsHotm
			val needsCold = ColdResistance.missing
			if (!needsHotm && !needsCold && (state.done || state.todos.isEmpty())) return emptyList()
			val lines = mutableListOf(header(ShaftVerdict.current()?.shouldMine))
			lines += state.todos.map { line(it) }
			if (needsHotm) lines += hotmHint
			if (needsCold) lines += coldHint
			return lines
		}

		override fun getExampleLines() = listOf(
			header(shouldMine = true),
			line(Todo.Corpse(CorpseType.LAPIS, 2, hasKey = true)),
			line(Todo.Corpse(CorpseType.UMBER, 1, hasKey = false)),
			line(Todo.Fossil),
			line(Todo.GrabCrystal(CrystalType.JASPER)),
		)
	}
}
