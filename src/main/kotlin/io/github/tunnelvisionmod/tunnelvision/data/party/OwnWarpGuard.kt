package io.github.tunnelvisionmod.tunnelvision.data.party

/**
 * The `!w` you send to be warped into a shared mineshaft comes back to you in party chat, where
 * it must not be answered as if somebody else had asked for a warp.
 */
object OwnWarpGuard {
	private const val IGNORE_MS = 5_000L

	private var ignoreUntil = 0L

	/** You are about to send `!w` yourself. */
	fun arm() {
		ignoreUntil = System.currentTimeMillis() + IGNORE_MS
	}

	/** True once for your own `!w` echoed back within the window. */
	fun consume(author: String, ownName: String): Boolean {
		if (!author.equals(ownName, ignoreCase = true) || System.currentTimeMillis() >= ignoreUntil) return false
		ignoreUntil = 0L
		return true
	}
}
