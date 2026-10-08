package io.github.tunnelvisionmod.tunnelvision.data.value

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

class ShaftTimeTest {
	/** Measured in a mineshaft at 138 Cold Resistance without taking a hit. */
	@Test
	fun `matches the cold measured in game`() {
		val measured = mapOf(150.0 to 13, 300.0 to 28, 450.0 to 47, 600.0 to 69)
		for ((seconds, cold) in measured) {
			assertEquals(cold, ShaftTime.coldAfter(seconds, 138.0).roundToInt(), "after $seconds s")
		}
	}

	@Test
	fun `freezing is where the cold reaches one hundred`() {
		for (resistance in listOf(0.0, 50.0, 138.0, 148.0, 300.0)) {
			val seconds = ShaftTime.secondsToFreeze(resistance)
			assertEquals(ShaftTime.FREEZE_COLD, ShaftTime.coldAfter(seconds, resistance), 1e-9, "at $resistance")
		}
	}

	@Test
	fun `about thirteen minutes at 138`() {
		assertEquals(13.0, ShaftTime.secondsToFreeze(138.0) / 60, 0.1)
	}

	@Test
	fun `more cold resistance means a longer stay`() {
		assertTrue(ShaftTime.secondsToFreeze(148.0) > ShaftTime.secondsToFreeze(138.0))
	}
}
