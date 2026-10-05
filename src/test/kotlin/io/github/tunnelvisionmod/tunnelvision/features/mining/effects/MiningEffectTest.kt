package io.github.tunnelvisionmod.tunnelvision.features.mining.effects

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MiningEffectTest {
	@Test
	fun `potion uses lore duration plus affinity bonus`() {
		assertEquals(5400, MiningEffect.COLD_RESISTANCE.durationSeconds(potionDurationSeconds = 3600, affinityBonusPercent = 50))
		assertEquals(198, MiningEffect.COLD_RESISTANCE.durationSeconds(potionDurationSeconds = 180, affinityBonusPercent = 10))
	}

	@Test
	fun `potion falls back to one hour without lore`() {
		assertEquals(3600, MiningEffect.COLD_RESISTANCE.durationSeconds(potionDurationSeconds = null, affinityBonusPercent = 0))
	}

	@Test
	fun `filet o fortune ignores potion bonuses`() {
		assertEquals(3600, MiningEffect.FILET_O_FORTUNE.durationSeconds(potionDurationSeconds = 180, affinityBonusPercent = 50))
	}
}
