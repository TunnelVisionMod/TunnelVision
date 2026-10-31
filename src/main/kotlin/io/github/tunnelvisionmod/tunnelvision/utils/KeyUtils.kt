package io.github.tunnelvisionmod.tunnelvision.utils

import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.tunnelvisionmod.tunnelvision.compat.Compat

object KeyUtils {
	const val NONE = -1

	private val heldKeys = mutableSetOf<Int>()

	fun isKeyDown(keyCode: Int): Boolean = keyCode != NONE && Compat.isKeyDown(keyCode)

	fun wasClicked(keyCode: Int): Boolean {
		if (isKeyDown(keyCode)) return heldKeys.add(keyCode)
		heldKeys.remove(keyCode)
		return false
	}

	fun keyName(keyCode: Int): String = IMinecraft.INSTANCE.getKeyName(keyCode).text
}
