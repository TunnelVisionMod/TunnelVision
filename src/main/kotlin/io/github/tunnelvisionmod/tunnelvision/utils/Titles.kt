package io.github.tunnelvisionmod.tunnelvision.utils

import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import net.minecraft.network.chat.Component

/**
 * Titles shown by the mod, remembered so another feature can add a subtitle under one that is on
 * screen. A subtitle set with no title showing would wait and attach itself to the next title,
 * whoever shows it, so [addSubtitle] does nothing then.
 */
object Titles {
	private const val MS_PER_TICK = 50L

	private var shownUntil = 0L

	fun show(title: Component, fadeIn: Int, stay: Int, fadeOut: Int) {
		Compat.setTitleTimes(fadeIn, stay, fadeOut)
		Compat.setTitle(title)
		shownUntil = System.currentTimeMillis() + (fadeIn + stay) * MS_PER_TICK
	}

	fun addSubtitle(subtitle: Component) {
		if (System.currentTimeMillis() < shownUntil) Compat.setSubtitle(subtitle)
	}
}
