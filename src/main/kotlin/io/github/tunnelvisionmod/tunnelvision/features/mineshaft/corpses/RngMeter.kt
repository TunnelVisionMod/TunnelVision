package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import kotlin.math.max
import kotlin.math.min

/**
 * How far the Frozen Corpse RNG meter has come since its last payout.
 *
 * Every corpse grants meter XP, and the tracker credits that XP as coins as it goes - a corpse is
 * worth its slice of the Shattered Pendant it is working towards. When the meter finally pays out,
 * the Pendant itself must therefore not be counted again, or it is paid for twice.
 *
 * Knowing which Pendant came from the meter is the whole reason this exists. One can also drop on
 * its own, and at roughly the same rate the meter fills, so the drop alone cannot be told apart
 * from the payout - only the progress can.
 *
 * The count starts at zero on a profile we have never seen, which is wrong for anyone who already
 * had progress; `/tv corpses meter <xp>` sets it straight.
 */
class RngMeter(private val needed: Double) {
	var progress: Double = 0.0
		private set

	val isFull: Boolean get() = progress >= needed

	/** How full the meter is, 0 to 1. */
	val fraction: Double get() = if (needed <= 0) 0.0 else min(1.0, progress / needed)

	fun gain(xp: Double) {
		progress = max(0.0, progress + xp)
	}

	fun set(xp: Double) {
		progress = max(0.0, xp)
	}

	/**
	 * Takes one payout if the meter has filled, leaving any overshoot behind, and reports whether
	 * it did. A false answer means a Pendant that turned up here dropped on its own.
	 */
	fun claim(): Boolean {
		if (!isFull) return false
		progress -= needed
		return true
	}
}
