package io.github.tunnelvisionmod.tunnelvision.data.value

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ColdResistanceTest {
	@Test
	fun `cold resistance from tab stats`() {
		assertEquals(25.0, ColdResistance.parse(listOf("Stats:", "Mining Speed: 2,500⸕", "Cold Resistance: 25❄")))
		assertEquals(7.5, ColdResistance.parse(listOf("Cold Resistance: 7.5❄")))
		assertEquals(25.0, ColdResistance.parse(listOf("Cold Resistance: ❄25")))
		assertEquals(1234.5, ColdResistance.parse(listOf("Cold Resistance: ❄1,234.5")))
		assertNull(ColdResistance.parse(listOf("Stats:", "Mining Speed: 2,500⸕")))
	}
}
