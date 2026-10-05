package io.github.tunnelvisionmod.tunnelvision.features.mining.effects

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EffectTimersTest {
	private val timers = EffectTimers()
	private val cold = MiningEffect.COLD_RESISTANCE
	private val filet = MiningEffect.FILET_O_FORTUNE

	@Test
	fun `started effect counts down`() {
		timers.start(filet, now = 0, durationSeconds = 3600)
		assertEquals(mapOf(filet to 3_590_000L), timers.remaining(now = 10_000))
	}

	@Test
	fun `effect without duration is active with unknown time`() {
		timers.start(cold, now = 0, durationSeconds = null)
		assertEquals(mapOf(cold to null), timers.remaining(now = 5_000))
	}

	@Test
	fun `tab fills in unknown duration`() {
		timers.start(cold, now = 0, durationSeconds = null)
		timers.sync(TabEffect(cold, 600, 60), now = 1_000)
		assertEquals(mapOf(cold to 600_000L), timers.remaining(now = 1_000))
	}

	@Test
	fun `tab detects effect after joining`() {
		timers.sync(TabEffect(filet, 1800, 60), now = 0)
		assertEquals(mapOf(filet to 1_800_000L), timers.remaining(now = 0))
	}

	@Test
	fun `ignores tab rounding within precision`() {
		timers.start(filet, now = 0, durationSeconds = 3600)
		timers.sync(TabEffect(filet, 59 * 60, 60), now = 20_000)
		assertEquals(mapOf(filet to 3_580_000L), timers.remaining(now = 20_000))
	}

	@Test
	fun `corrects when tab differs more than precision`() {
		timers.start(filet, now = 0, durationSeconds = 3600)
		timers.sync(TabEffect(filet, 30 * 60, 60), now = 0)
		assertEquals(mapOf(filet to 1_800_000L), timers.remaining(now = 0))
	}

	@Test
	fun `follows tab when effect was extended`() {
		timers.sync(TabEffect(cold, 60, 1), now = 0)
		timers.sync(TabEffect(cold, 900, 60), now = 1_000)
		assertEquals(mapOf(cold to 900_000L), timers.remaining(now = 1_000))
	}

	@Test
	fun `expired effect disappears`() {
		timers.start(filet, now = 0, durationSeconds = 60)
		timers.expire(now = 60_000)
		assertEquals(emptyMap<MiningEffect, Long?>(), timers.remaining(now = 60_000))
	}

	@Test
	fun `reports expired effects once`() {
		timers.start(filet, now = 0, durationSeconds = 60)
		timers.start(cold, now = 0, durationSeconds = 120)
		assertEquals(emptyList<MiningEffect>(), timers.expire(now = 59_999))
		assertEquals(listOf(filet), timers.expire(now = 60_000))
		assertEquals(emptyList<MiningEffect>(), timers.expire(now = 61_000))
	}

	@Test
	fun `effect with unknown duration never expires on its own`() {
		timers.start(cold, now = 0, durationSeconds = null)
		assertEquals(emptyList<MiningEffect>(), timers.expire(now = 10_000_000))
	}

	@Test
	fun `lagging tab does not bring back expired effect`() {
		timers.start(cold, now = 0, durationSeconds = 60)
		timers.expire(now = 60_000)
		timers.sync(TabEffect(cold, 2, 1), now = 60_050)
		assertEquals(emptyMap<MiningEffect, Long?>(), timers.remaining(now = 60_050))
	}

	@Test
	fun `clear removes everything`() {
		timers.start(filet, now = 0, durationSeconds = 60)
		timers.start(cold, now = 0, durationSeconds = null)
		timers.clear()
		assertEquals(emptyMap<MiningEffect, Long?>(), timers.remaining(now = 0))
	}
}
