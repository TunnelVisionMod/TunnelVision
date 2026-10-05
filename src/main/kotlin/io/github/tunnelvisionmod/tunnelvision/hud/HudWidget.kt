package io.github.tunnelvisionmod.tunnelvision.hud

import net.minecraft.network.chat.Component

/** [text], drawn on [line] from [fromX] to [toX] in unscaled widget pixels, runs [onClick] while a screen is open and is underlined on hover. */
data class HudClickable(val line: Int, val fromX: Int, val toX: Int, val text: Component, val onClick: () -> Unit)

abstract class HudWidget(val id: String, val displayName: String, val defaultPosition: HudPosition) {
	abstract val isEnabled: Boolean

	abstract fun getLines(): List<Component>

	abstract fun getExampleLines(): List<Component>

	/** The clickable parts of the lines last returned by [getLines]. */
	open val clickables: List<HudClickable> get() = emptyList()
}
