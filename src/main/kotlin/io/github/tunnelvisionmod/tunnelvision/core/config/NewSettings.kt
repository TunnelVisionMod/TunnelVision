package io.github.tunnelvisionmod.tunnelvision.core.config

import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.Storage
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent

/**
 * Tells the player about settings an update added, since every feature starts off and would
 * otherwise go unnoticed.
 *
 * The settings seen so far are kept in storage by path. A fresh install records them silently;
 * after that, anything new is announced once in chat with a button that opens the menu filtered to
 * it, through the [SEARCH_TAG] the menu's search understands. `/tv new` shows the last batch again.
 */
object NewSettings {
	const val SEARCH_TAG = "@new"
	private const val MAX_LINES = 8
	private const val DEV_CATEGORY = "dev"

	private var checked = false
	private var newPaths: Set<String> = emptySet()

	fun init() {
		newPaths = Storage.data.newSettings.toSet()
		EventBus.on<ClientTickEvent> { if (!checked && mc.player != null) check() }
	}

	/** The menu's search, with [SEARCH_TAG] narrowing it to the settings an update added. */
	fun fulfillsSearch(editor: GuiOptionEditor, word: String): Boolean =
		if (word.equals(SEARCH_TAG, ignoreCase = true)) editor.option.path in newPaths else editor.fulfillsSearch(word)

	private fun check() {
		checked = true
		val options = ConfigManager.createEditor().allOptions.filterNot { it.category.identifier == DEV_CATEGORY }
		val paths = options.map { it.path }
		val known = Storage.data.knownSettings
		Debug.log { "NewSettings: ${paths.size} settings, e.g. ${paths.take(3)}" }
		if (known == null) {
			Storage.data.knownSettings = paths.toMutableList()
			Storage.save()
			return
		}
		val added = paths.filter { it !in known }
		if (added.isEmpty()) return
		Storage.data.knownSettings = (known + added).toMutableList()
		Storage.data.newSettings = added.toMutableList()
		Storage.save()
		newPaths = added.toSet()
		announce()
	}

	/** `/tv new`. */
	fun announce() {
		val editor = ConfigManager.createEditor()
		val options = editor.allOptions.filter { it.path in newPaths }
		if (options.isEmpty()) {
			ChatUtils.send(Component.literal("No new settings since the last update.").withStyle(ChatFormatting.GRAY))
			return
		}
		val shown = topLevel(options)
		ChatUtils.send(
			Component.literal("${options.size} new setting${if (options.size == 1) "" else "s"}. ").withStyle(ChatFormatting.YELLOW)
				.append(button("[Show]", "/tv new show", "Open the settings with only the new ones")),
		)
		shown.take(MAX_LINES).forEach { option ->
			ChatUtils.sendRaw(
				Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
					.append(Component.literal(label(editor, option) + " ").withStyle(ChatFormatting.GRAY))
					.append(button("[Go]", "/tv new go ${option.path}", "Open the settings at this one")),
			)
		}
		if (shown.size > MAX_LINES) {
			ChatUtils.sendRaw(Component.literal(" • ${shown.size - MAX_LINES} more under [Show]").withStyle(ChatFormatting.DARK_GRAY))
		}
	}

	fun show() = ConfigManager.openScreen { it.search(SEARCH_TAG) }

	fun goTo(path: String) = ConfigManager.openScreen { editor ->
		val option = editor.allOptions.firstOrNull { it.path == path } ?: return@openScreen
		if (!editor.goToOption(option)) editor.search(SEARCH_TAG)
	}

	/** A whole new accordion is listed once, not once per option inside it. */
	private fun topLevel(options: List<ProcessedOption>): List<ProcessedOption> =
		options.filter { option -> parent(option)?.path !in newPaths }

	private fun parent(option: ProcessedOption): ProcessedOption? =
		option.accordionId.takeIf { it >= 0 }?.let { option.category.accordionAnchors[it] }

	/** `Mineshaft → Corpses → Loot → RNG Meter Bonus`, the way the menu nests it. */
	private fun label(editor: MoulConfigEditor<*>, option: ProcessedOption): String {
		val names = ArrayDeque<String>()
		names.addFirst(option.name.text)
		var accordion = parent(option)
		while (accordion != null) {
			names.addFirst(accordion.name.text)
			accordion = parent(accordion)
		}
		var category = option.category
		while (true) {
			names.addFirst(category.displayName.text)
			category = category.parentCategoryId?.let { editor.allCategories[it] } ?: break
		}
		return names.joinToString(" → ")
	}

	private fun button(label: String, command: String, hover: String): MutableComponent =
		Component.literal(label).withStyle { style ->
			style.withColor(ChatFormatting.AQUA).withBold(true)
				.withClickEvent(ClickEvent.RunCommand(command))
				.withHoverEvent(HoverEvent.ShowText(Component.literal(hover)))
		}
}
