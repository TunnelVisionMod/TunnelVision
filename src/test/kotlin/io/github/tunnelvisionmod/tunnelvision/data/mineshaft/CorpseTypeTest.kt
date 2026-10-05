package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CorpseTypeTest {
	@Test
	fun `detects corpse by skyblock id`() {
		assertEquals(CorpseType.LAPIS, CorpseType.fromHelmet("LAPIS_ARMOR_HELMET", ""))
		assertEquals(CorpseType.TUNGSTEN, CorpseType.fromHelmet("MINERAL_HELMET", ""))
		assertEquals(CorpseType.UMBER, CorpseType.fromHelmet("ARMOR_OF_YOG_HELMET", ""))
		assertEquals(CorpseType.VANGUARD, CorpseType.fromHelmet("VANGUARD_HELMET", ""))
	}

	@Test
	fun `detects corpse by helmet name when id is missing`() {
		assertEquals(CorpseType.LAPIS, CorpseType.fromHelmet(null, "Lapis Armor Helmet"))
		assertEquals(CorpseType.UMBER, CorpseType.fromHelmet(null, "Yog Helmet"))
	}

	@Test
	fun `ignores other helmets`() {
		assertNull(CorpseType.fromHelmet("DIVAN_HELMET", "Helmet of Divan"))
		assertNull(CorpseType.fromHelmet(null, ""))
	}
}
