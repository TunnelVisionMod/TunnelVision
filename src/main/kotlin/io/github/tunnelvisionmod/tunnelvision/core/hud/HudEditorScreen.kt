package io.github.tunnelvisionmod.tunnelvision.core.hud

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class HudEditorScreen : Screen(Component.literal("TunnelVision HUD Editor")) {
	private var dragging: HudWidget? = null
	private var dragOffsetX = 0.0
	private var dragOffsetY = 0.0

	private fun boundsOf(widget: HudWidget) = HudManager.bounds(widget, widget.getExampleLines(), width, height)

	private fun widgetAt(mouseX: Double, mouseY: Double): HudWidget? =
		HudManager.enabledWidgets.lastOrNull { boundsOf(it).contains(mouseX, mouseY) }

	override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		if (mc.level == null) super.extractBackground(graphics, mouseX, mouseY, delta)
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		val hovered = dragging ?: widgetAt(mouseX.toDouble(), mouseY.toDouble())
		for (widget in HudManager.enabledWidgets) {
			val bounds = boundsOf(widget)
			val isHovered = widget == hovered
			graphics.fill(bounds.x - 2, bounds.y - 2, bounds.x + bounds.width + 2, bounds.y + bounds.height + 2, if (isHovered) HOVER_FILL else IDLE_FILL)
			graphics.outline(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4, if (isHovered) HOVER_OUTLINE else IDLE_OUTLINE)
			HudManager.draw(graphics, widget, widget.getExampleLines())
		}
		graphics.text(font, HINT, (width - font.width(HINT)) / 2, 6, -1, true)
		if (HudManager.enabledWidgets.isEmpty()) {
			graphics.text(font, EMPTY, (width - font.width(EMPTY)) / 2, height / 2, -1, true)
		}
	}

	override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
		val widget = widgetAt(event.x(), event.y()) ?: return super.mouseClicked(event, doubleClick)
		when (event.button()) {
			0 -> {
				val bounds = boundsOf(widget)
				dragging = widget
				dragOffsetX = event.x() - bounds.x
				dragOffsetY = event.y() - bounds.y
			}
			1 -> HudManager.reset(widget)
		}
		return true
	}

	override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
		val widget = dragging ?: return super.mouseDragged(event, dragX, dragY)
		val bounds = boundsOf(widget)
		val position = HudManager.positionOf(widget)
		position.x = fraction(event.x() - dragOffsetX, width - bounds.width)
		position.y = fraction(event.y() - dragOffsetY, height - bounds.height)
		return true
	}

	override fun mouseReleased(event: MouseButtonEvent): Boolean {
		dragging = null
		return super.mouseReleased(event)
	}

	override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
		val widget = widgetAt(mouseX, mouseY) ?: return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
		val position = HudManager.positionOf(widget)
		position.scale = (position.scale + scrollY.toFloat() * SCALE_STEP).coerceIn(HudManager.MIN_SCALE, HudManager.MAX_SCALE)
		return true
	}

	override fun removed() {
		super.removed()
		HudManager.save()
	}

	private fun fraction(offset: Double, space: Int): Float =
		if (space <= 0) 0f else (offset / space).toFloat().coerceIn(0f, 1f)

	private companion object {
		const val SCALE_STEP = 0.1f
		const val IDLE_FILL = 0x40000000
		const val HOVER_FILL = 0x40FFFFFF
		const val IDLE_OUTLINE = 0x80FFFFFF.toInt()
		const val HOVER_OUTLINE = -1
		val HINT: Component = Component.literal("Drag to move · Scroll to resize · Right-click to reset")
		val EMPTY: Component = Component.literal("No HUD widgets enabled")
	}
}
