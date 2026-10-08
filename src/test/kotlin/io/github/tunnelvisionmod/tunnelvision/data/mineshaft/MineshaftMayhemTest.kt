package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MineshaftMayhemTest {
	@Test
	fun `mineshaft mayhem`() {
		assertEquals(MayhemBuff.MINING_FORTUNE, MineshaftMayhem.parse("MAYHEM! You received a ☘ Mining Fortune buff from your Mineshaft Mayhem perk!"))
		assertEquals(MayhemBuff.MINING_SPEED, MineshaftMayhem.parse("MAYHEM! You received a ⸕ Mining Speed buff from your Mineshaft Mayhem perk!"))
		assertEquals(MayhemBuff.COLD_RESISTANCE, MineshaftMayhem.parse("MAYHEM! You received a ❄ Cold Resistance buff from your Mineshaft Mayhem perk!"))
		assertEquals(MayhemBuff.SCRAP_CHANCE, MineshaftMayhem.parse("MAYHEM! Your Suspicious Scrap chance was buffed by your Mineshaft Mayhem perk!"))
		assertEquals(MayhemBuff.ABILITY_COOLDOWN, MineshaftMayhem.parse("MAYHEM! Your Pickaxe Ability cooldown was reduced from your Mineshaft Mayhem perk!"))
		assertNull(MineshaftMayhem.parse("You received a Mining Fortune buff"))
	}
}
