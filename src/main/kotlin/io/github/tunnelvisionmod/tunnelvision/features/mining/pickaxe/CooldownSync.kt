package io.github.tunnelvisionmod.tunnelvision.features.mining.pickaxe

object CooldownSync {
	const val TICKS_PER_SECOND = 20
	const val READY_GRACE_TICKS = 3 * TICKS_PER_SECOND
	private const val CORRECTION_THRESHOLD_TICKS = 30
	private const val MIN_START_SECONDS = 5
	private const val NATURAL_END_TICKS = 3 * TICKS_PER_SECOND

	fun resync(ticksLeft: Int, tabSeconds: Int): Int? {
		val tabTicks = tabSeconds * TICKS_PER_SECOND
		if (ticksLeft == 0) return tabTicks.takeIf { tabSeconds >= MIN_START_SECONDS }
		return tabTicks.takeIf { it < ticksLeft - CORRECTION_THRESHOLD_TICKS }
	}

	fun isAheadOfServer(ticksLeft: Int, tabSeconds: Int): Boolean =
		ticksLeft < tabSeconds * TICKS_PER_SECOND - CORRECTION_THRESHOLD_TICKS

	fun firesOnAvailable(ticksLeft: Int): Boolean = ticksLeft in 1..NATURAL_END_TICKS
}
