package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RngMeterTest {
	private val needed = 2_500_000.0
	private val vanguard = 25_000.0

	@Test
	fun `fills from corpse xp`() {
		val meter = RngMeter(needed)
		repeat(99) { meter.gain(vanguard) }
		assertFalse(meter.isFull)
		meter.gain(vanguard)
		assertTrue(meter.isFull, "100 Vanguard corpses is 2.5M xp")
	}

	@Test
	fun `a claim only succeeds once the meter is full`() {
		val meter = RngMeter(needed)
		meter.gain(needed - 1)
		assertFalse(meter.claim())
		meter.gain(1.0)
		assertTrue(meter.claim())
		assertFalse(meter.claim(), "one fill pays out once")
	}

	@Test
	fun `a claim keeps the overshoot`() {
		val meter = RngMeter(needed)
		meter.gain(needed + 40_000)
		assertTrue(meter.claim())
		assertEquals(40_000.0, meter.progress, "progress past the payout carries over")
	}

	/**
	 * The case this exists for: a Pendant that drops while the meter is part-full came from the
	 * table, not the meter, so it must still count as loot.
	 */
	@Test
	fun `a part full meter does not claim a natural drop`() {
		val meter = RngMeter(needed)
		meter.gain(vanguard * 50)
		assertFalse(meter.claim())
		assertEquals(vanguard * 50, meter.progress, "a natural drop leaves progress alone")
	}

	@Test
	fun `progress can be corrected and never goes negative`() {
		val meter = RngMeter(needed)
		meter.set(1_000_000.0)
		assertEquals(1_000_000.0, meter.progress)
		meter.set(-5.0)
		assertEquals(0.0, meter.progress)
		meter.gain(-100.0)
		assertEquals(0.0, meter.progress)
	}

	@Test
	fun `fraction reports how full it is`() {
		val meter = RngMeter(needed)
		meter.gain(needed / 4)
		assertEquals(0.25, meter.fraction)
		meter.gain(needed)
		assertEquals(1.0, meter.fraction, "never reads past full")
	}
}
