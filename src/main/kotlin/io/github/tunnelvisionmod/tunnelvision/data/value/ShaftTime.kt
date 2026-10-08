package io.github.tunnelvisionmod.tunnelvision.data.value

/**
 * How long you can stay in a mineshaft before the cold freezes you out at [FREEZE_COLD].
 *
 * You gain one Cold every `5 * (1 + coldResistance / 100)` seconds, and a mineshaft speeds that up
 * by another 25% for every full [RAMP_SECONDS] you have been inside: x1 for the first 150 seconds,
 * then x1.25, x1.5 and so on. Checked in-game at 138 Cold Resistance: 13, 28, 47 and 69 Cold after
 * 2:30, 5:00, 7:30 and 10:00. Mobs hitting you add Cold on top, which we leave out.
 */
object ShaftTime {
	const val FREEZE_COLD = 100.0
	const val BASE_SECONDS_PER_COLD = 5.0
	const val RAMP_SECONDS = 150.0
	const val RAMP_STEP = 0.25

	private fun secondsPerCold(coldResistance: Double): Double =
		BASE_SECONDS_PER_COLD * (1 + maxOf(coldResistance, 0.0) / 100)

	/** The Cold you have after [seconds] inside. */
	fun coldAfter(seconds: Double, coldResistance: Double): Double {
		val perSecond = 1 / secondsPerCold(coldResistance)
		var cold = 0.0
		var start = 0.0
		var multiplier = 1.0
		while (start < seconds) {
			cold += minOf(RAMP_SECONDS, seconds - start) * multiplier * perSecond
			start += RAMP_SECONDS
			multiplier += RAMP_STEP
		}
		return cold
	}

	/** The seconds until the cold reaches [FREEZE_COLD]. */
	fun secondsToFreeze(coldResistance: Double): Double {
		val perSecond = 1 / secondsPerCold(coldResistance)
		var cold = 0.0
		var start = 0.0
		var multiplier = 1.0
		while (true) {
			val gained = RAMP_SECONDS * multiplier * perSecond
			if (cold + gained >= FREEZE_COLD) return start + (FREEZE_COLD - cold) / (multiplier * perSecond)
			cold += gained
			start += RAMP_SECONDS
			multiplier += RAMP_STEP
		}
	}
}
