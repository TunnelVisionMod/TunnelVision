package io.github.tunnelvisionmod.tunnelvision.features.party.sharedwarp

class WarpWindow {
	private var openedAt: Long? = null

	fun open(now: Long) {
		openedAt = now
	}

	fun close() {
		openedAt = null
	}

	fun isOpen(now: Long, windowSeconds: Int): Boolean {
		val opened = openedAt ?: return false
		return windowSeconds == 0 || now - opened < windowSeconds * 1000L
	}
}
