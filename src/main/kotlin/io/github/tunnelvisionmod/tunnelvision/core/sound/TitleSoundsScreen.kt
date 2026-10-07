package io.github.tunnelvisionmod.tunnelvision.core.sound

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import kotlin.math.roundToInt
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * One row per [TitleSound]: on/off, the chosen file, choose, test, reset and a volume slider.
 * The rows are built from the enum, so a new alert shows up here without touching this screen.
 */
class TitleSoundsScreen(private val parent: Screen?) : Screen(Component.literal("Title Sounds")) {
	private var scroll = 0
	private var status: Component? = null
	private var statusUntil = 0L

	private val listTop get() = HEADER_HEIGHT
	private val listBottom get() = height - FOOTER_HEIGHT - STATUS_HEIGHT
	private val visibleRows get() = ((listBottom - listTop) / ROW_HEIGHT).coerceAtLeast(1)
	private val maxScroll get() = (TitleSound.entries.size - visibleRows).coerceAtLeast(0)
	private val rowLeft get() = (width - ROW_WIDTH) / 2

	override fun init() {
		scroll = scroll.coerceIn(0, maxScroll)
		addRenderableWidget(
			Button.builder(Component.literal("Open Sounds Folder")) { TitleSounds.openFolder() }
				.bounds(width / 2 - 60, 30, 120, 18)
				.build(),
		)
		TitleSound.entries.drop(scroll).take(visibleRows).forEachIndexed { index, sound ->
			addRow(sound, listTop + index * ROW_HEIGHT)
		}
		addRenderableWidget(
			Button.builder(Component.literal("Done")) { onClose() }
				.bounds(width / 2 - 50, height - FOOTER_HEIGHT + 6, 100, 20)
				.build(),
		)
	}

	private fun addRow(sound: TitleSound, y: Int) {
		val setting = TitleSounds.settingOf(sound)
		var x = rowLeft + NAME_WIDTH
		addRenderableWidget(
			CycleButton.onOffBuilder(setting.enabled).displayOnlyValue()
				.create(x, y, TOGGLE_WIDTH, BUTTON_HEIGHT, Component.literal(sound.displayName)) { _, on ->
					setting.enabled = on
					TitleSounds.save()
				},
		)
		x += TOGGLE_WIDTH + GAP
		addRenderableWidget(
			Button.builder(Component.literal("Choose…")) {
				TitleSounds.choose(sound) { message ->
					showStatus(message)
					rebuildWidgets()
				}
			}
				.bounds(x, y, CHOOSE_WIDTH, BUTTON_HEIGHT)
				.tooltip(Tooltip.create(Component.literal("Pick an .ogg file. It is copied into the sounds folder.")))
				.build(),
		)
		x += CHOOSE_WIDTH + GAP
		addRenderableWidget(
			Button.builder(Component.literal("▶")) { TitleSounds.test(sound) }
				.bounds(x, y, ICON_WIDTH, BUTTON_HEIGHT)
				.tooltip(Tooltip.create(Component.literal("Test")))
				.build(),
		)
		x += ICON_WIDTH + GAP
		addRenderableWidget(
			Button.builder(Component.literal("↺")) {
				TitleSounds.reset(sound)
				rebuildWidgets()
			}
				.bounds(x, y, ICON_WIDTH, BUTTON_HEIGHT)
				.tooltip(Tooltip.create(Component.literal("Back to the default sound and volume")))
				.build(),
		)
		x += ICON_WIDTH + GAP
		addRenderableWidget(VolumeSlider(x, y, setting))
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		graphics.text(font, title, (width - font.width(title)) / 2, 6, -1, true)
		graphics.text(font, HINT, (width - font.width(HINT)) / 2, 18, -1, true)
		TitleSound.entries.drop(scroll).take(visibleRows).forEachIndexed { index, sound ->
			val y = listTop + index * ROW_HEIGHT
			graphics.text(font, Component.literal(sound.displayName), rowLeft, y + 1, -1, true)
			graphics.text(font, fileLabel(sound), rowLeft, y + 11, -1, true)
		}
		val line = status?.takeIf { System.currentTimeMillis() < statusUntil } ?: if (maxScroll > 0) {
			Component.literal("Scroll for more (${scroll + 1}-${scroll + visibleRows} of ${TitleSound.entries.size})")
				.withStyle(ChatFormatting.DARK_GRAY)
		} else {
			null
		}
		line?.let { graphics.text(font, it, (width - font.width(it)) / 2, listBottom + 2, -1, true) }
	}

	private fun fileLabel(sound: TitleSound): Component {
		val file = TitleSounds.settingOf(sound).file
		val text = when {
			file != null -> font.plainSubstrByWidth(file, NAME_WIDTH - GAP)
			sound.defaultSound == DefaultSound.NONE -> "no sound"
			else -> "default"
		}
		return Component.literal(text).withStyle(ChatFormatting.GRAY)
	}

	override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
		val next = (scroll - scrollY.toInt()).coerceIn(0, maxScroll)
		if (next != scroll) {
			scroll = next
			rebuildWidgets()
		}
		return true
	}

	private fun showStatus(message: Component) {
		status = message
		statusUntil = System.currentTimeMillis() + STATUS_MS
	}

	override fun onClose() {
		TitleSounds.save()
		mc.schedule { Compat.setScreen(parent) }
	}

	private class VolumeSlider(x: Int, y: Int, private val setting: SoundSetting) :
		AbstractSliderButton(x, y, SLIDER_WIDTH, BUTTON_HEIGHT, Component.empty(), (setting.volume / TitleSounds.MAX_VOLUME).toDouble()) {
		init {
			updateMessage()
		}

		override fun updateMessage() {
			message = Component.literal("${(setting.volume * 100).roundToInt()}%")
		}

		override fun applyValue() {
			setting.volume = ((value * TitleSounds.MAX_VOLUME * 20).roundToInt() / 20.0).toFloat()
		}
	}

	private companion object {
		const val HEADER_HEIGHT = 56
		const val FOOTER_HEIGHT = 32
		const val STATUS_HEIGHT = 12
		const val ROW_HEIGHT = 24
		const val BUTTON_HEIGHT = 20
		const val GAP = 4
		const val NAME_WIDTH = 130
		const val TOGGLE_WIDTH = 36
		const val CHOOSE_WIDTH = 56
		const val ICON_WIDTH = 20
		const val SLIDER_WIDTH = 90
		const val ROW_WIDTH = NAME_WIDTH + TOGGLE_WIDTH + CHOOSE_WIDTH + 2 * ICON_WIDTH + SLIDER_WIDTH + 4 * GAP
		const val STATUS_MS = 4000L
		val HINT: Component = Component.literal("Only .ogg files work - convert MP3s with any online MP3 to OGG converter first.")
			.withStyle(ChatFormatting.YELLOW)
	}
}
