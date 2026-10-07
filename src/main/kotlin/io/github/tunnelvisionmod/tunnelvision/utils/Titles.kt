package io.github.tunnelvisionmod.tunnelvision.utils

import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSound
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSounds
import net.minecraft.network.chat.Component

/**
 * The only way the mod shows a title, so every title comes with its [TitleSound] and shows up in
 * the Title Sounds menu.
 *
 * Titles are also remembered so another feature can add a subtitle under one that is on screen. A
 * subtitle set with no title showing would wait and attach itself to the next title, whoever shows
 * it, so [addSubtitle] does nothing then.
 */
object Titles {
	private const val MS_PER_TICK = 50L

	private var shownUntil = 0L

	fun show(title: Component, sound: TitleSound, fadeIn: Int, stay: Int, fadeOut: Int, subtitle: Component? = null) {
		Compat.setTitleTimes(fadeIn, stay, fadeOut)
		// Minecraft keeps the subtitle for the next title, so it has to be set before the title.
		subtitle?.let { Compat.setSubtitle(it) }
		Compat.setTitle(title)
		shownUntil = System.currentTimeMillis() + (fadeIn + stay) * MS_PER_TICK
		TitleSounds.play(sound)
	}

	fun addSubtitle(subtitle: Component) {
		if (System.currentTimeMillis() < shownUntil) Compat.setSubtitle(subtitle)
	}
}
