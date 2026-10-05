package io.github.tunnelvisionmod.tunnelvision.features.party.partyshare

import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftParser
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MineshaftPartyShareTest {
	private fun tab(vararg corpseLines: String) =
		listOf("Area: Mineshaft", "Frozen Corpses:") + corpseLines + listOf("Powders:", "Mithril: 1,234")

	@Test
	fun `corpses counted per kind`() {
		val lines = tab("Lapis: NOT LOOTED", "Tungsten: LOOTED", "Lapis: NOT LOOTED", "Umber: NOT LOOTED")
		assertEquals(mapOf("Lapis" to 2, "Tungsten" to 1, "Umber" to 1), MineshaftParser.parseCorpses(lines))
	}

	@Test
	fun `looted and unlooted both count`() {
		assertEquals(mapOf("Vanguard" to 2), MineshaftParser.parseCorpses(tab("Vanguard: LOOTED", "Vanguard: NOT LOOTED")))
	}

	@Test
	fun `order follows the widget`() {
		val lines = tab("Umber: NOT LOOTED", "Lapis: NOT LOOTED", "Tungsten: NOT LOOTED")
		assertEquals(listOf("Umber", "Lapis", "Tungsten"), MineshaftParser.parseCorpses(lines)?.keys?.toList())
	}

	@Test
	fun `stops at the next widget`() {
		val lines = listOf("Frozen Corpses:", "Lapis: NOT LOOTED", "Powders:", "Umber: LOOTED")
		assertEquals(mapOf("Lapis" to 1), MineshaftParser.parseCorpses(lines))
	}

	@Test
	fun `widget present but empty`() {
		assertEquals(emptyMap<String, Int>(), MineshaftParser.parseCorpses(tab()))
	}

	@Test
	fun `no widget at all`() {
		assertNull(MineshaftParser.parseCorpses(listOf("Area: Mineshaft", "Powders:")))
	}

	@Test
	fun `total still matches the breakdown`() {
		val lines = tab("Lapis: NOT LOOTED", "Tungsten: LOOTED", "Lapis: NOT LOOTED")
		assertEquals(3, MineshaftParser.parseCorpseCount(lines))
		assertNull(MineshaftParser.parseCorpseCount(listOf("Powders:")))
	}

	@Test
	fun `message format`() {
		assertEquals(
			"!ptme Mineshafttype: JASP_1, Corpses: Lapis 2, Tungsten 1, Umber 1",
			MineshaftPartyShare.buildMessage(MineshaftType.JASPER, mapOf("Lapis" to 2, "Tungsten" to 1, "Umber" to 1)),
		)
	}

	@Test
	fun `message with a single corpse`() {
		assertEquals(
			"!ptme Mineshafttype: TOPA_1, Corpses: Lapis 1",
			MineshaftPartyShare.buildMessage(MineshaftType.TOPAZ_1, mapOf("Lapis" to 1)),
		)
	}
}
