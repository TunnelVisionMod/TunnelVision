package io.github.tunnelvisionmod.tunnelvision.core.hud

import com.mojang.blaze3d.platform.cursor.CursorTypes
import io.github.notenoughupdates.moulconfig.managed.ManagedDataFile
import io.github.tunnelvisionmod.tunnelvision.TunnelVision
import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import kotlin.math.roundToInt
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

data class HudBounds(val x: Int, val y: Int, val width: Int, val height: Int) {
	fun contains(mouseX: Double, mouseY: Double) = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
}

object HudManager {
	const val MIN_SCALE = 0.5f
	const val MAX_SCALE = 3f
	private const val LINE_SPACING = 1

	private val widgets = mutableListOf<HudWidget>()
	private lateinit var data: ManagedDataFile<HudData>

	val enabledWidgets: List<HudWidget> get() = widgets.filter { it.isEnabled }

	fun load() {
		val file = FabricLoader.getInstance().configDir.resolve("${TunnelVision.MOD_ID}/hud.json").toFile()
		data = ManagedDataFile.create(file, HudData::class.java) {
			loadFailed = { _, e -> TunnelVision.logger.error("Failed to load HUD positions", e) }
			saveFailed = { _, e -> TunnelVision.logger.error("Failed to save HUD positions", e) }
		}
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(TunnelVision.MOD_ID, "hud")) { graphics, _ -> render(graphics) }
		ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
			if (!isClickScreen(screen)) return@register
			ScreenMouseEvents.allowMouseClick(screen).register { _, event -> !click(event.x(), event.y()) }
		}
	}

	private fun isClickScreen(screen: Screen?) = screen is AbstractContainerScreen<*> || screen is ChatScreen

	private fun click(mouseX: Double, mouseY: Double): Boolean {
		if (!SkyBlock.isOnSkyBlock) return false
		val window = mc.window
		for (widget in enabledWidgets.asReversed()) {
			val lines = widget.getLines()
			if (lines.isEmpty()) continue
			val bounds = bounds(widget, lines, window.guiScaledWidth, window.guiScaledHeight)
			val clickable = clickableAt(widget, bounds, mouseX, mouseY) ?: continue
			clickable.onClick()
			return true
		}
		return false
	}

	private fun clickableAt(widget: HudWidget, bounds: HudBounds, mouseX: Double, mouseY: Double): HudClickable? {
		if (!bounds.contains(mouseX, mouseY)) return null
		val scale = positionOf(widget).scale
		val line = ((mouseY - bounds.y) / scale / (mc.font.lineHeight + LINE_SPACING)).toInt()
		val x = (mouseX - bounds.x) / scale
		return widget.clickables.firstOrNull { it.line == line && x >= it.fromX && x < it.toX }
	}

	fun register(widget: HudWidget) {
		widgets += widget
	}

	fun positionOf(widget: HudWidget): HudPosition =
		data.instance.positions.getOrPut(widget.id) { widget.defaultPosition.copy() }

	fun reset(widget: HudWidget) {
		data.instance.positions[widget.id] = widget.defaultPosition.copy()
	}

	fun save() = data.saveToFile()

	fun openEditor() {
		mc.schedule { Compat.setScreen(HudEditorScreen()) }
	}

	fun bounds(widget: HudWidget, lines: List<Component>, screenWidth: Int, screenHeight: Int): HudBounds {
		val position = positionOf(widget)
		val contentWidth = lines.maxOfOrNull { mc.font.width(it) } ?: 0
		val contentHeight = lines.size * (mc.font.lineHeight + LINE_SPACING) - LINE_SPACING
		val width = (contentWidth * position.scale).roundToInt()
		val height = (contentHeight * position.scale).roundToInt()
		val x = (position.x.coerceIn(0f, 1f) * (screenWidth - width).coerceAtLeast(0)).roundToInt()
		val y = (position.y.coerceIn(0f, 1f) * (screenHeight - height).coerceAtLeast(0)).roundToInt()
		return HudBounds(x, y, width, height)
	}

	fun draw(graphics: GuiGraphicsExtractor, widget: HudWidget, lines: List<Component>) {
		if (lines.isEmpty()) return
		val bounds = bounds(widget, lines, graphics.guiWidth(), graphics.guiHeight())
		val hovered = if (isClickScreen(Compat.screen)) {
			val window = mc.window
			clickableAt(widget, bounds, mc.mouseHandler.getScaledXPos(window), mc.mouseHandler.getScaledYPos(window))
		} else {
			null
		}
		val scale = positionOf(widget).scale
		val pose = graphics.pose()
		pose.pushMatrix()
		pose.translate(bounds.x.toFloat(), bounds.y.toFloat())
		pose.scale(scale, scale)
		lines.forEachIndexed { index, line ->
			graphics.text(mc.font, line, 0, index * (mc.font.lineHeight + LINE_SPACING), -1, true)
		}
		if (hovered != null) {
			val underlined = hovered.text.copy().withStyle(ChatFormatting.UNDERLINE)
			graphics.text(mc.font, underlined, hovered.fromX, hovered.line * (mc.font.lineHeight + LINE_SPACING), -1, true)
			graphics.requestCursor(CursorTypes.POINTING_HAND)
		}
		pose.popMatrix()
	}

	private fun render(graphics: GuiGraphicsExtractor) {
		if (!SkyBlock.isOnSkyBlock || Compat.screen is HudEditorScreen) return
		enabledWidgets.forEach { draw(graphics, it, it.getLines()) }
	}
}
