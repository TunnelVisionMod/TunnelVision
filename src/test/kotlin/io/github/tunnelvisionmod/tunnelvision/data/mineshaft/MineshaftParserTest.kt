package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MineshaftParserTest {
	private fun sidebar(serverId: String) = listOf(
		"SKYBLOCK",
		"Mineshaft",
		"Cold: -13",
		"09/29/25 $serverId",
	)

	private fun tab(vararg corpseLines: String) =
		listOf("Area: Mineshaft", "Frozen Corpses:") + corpseLines + listOf("Powders:", "Mithril: 1,234")

	@Test
	fun `type from server id`() {
		assertEquals(MineshaftType.TOPAZ_1, MineshaftParser.parseType(sidebar("mTOPA_1x")))
	}

	@Test
	fun `type for every variant`() {
		for (type in MineshaftType.entries) {
			assertEquals(type, MineshaftParser.parseType(sidebar("m${type.code}a")), type.name)
		}
	}

	@Test
	fun `crystal variant is not confused with numbered one`() {
		assertEquals(MineshaftType.RUBY_CRYSTAL, MineshaftParser.parseType(sidebar("mRUBY_Cq")))
	}

	@Test
	fun `no type outside a mineshaft`() {
		assertNull(MineshaftParser.parseType(listOf("SKYBLOCK", "Area: Dwarven Mines", "09/29/25 m12AB")))
	}

	@Test
	fun `type is ignored when not in the last word`() {
		assertNull(MineshaftParser.parseType(listOf("TOPA_1 is a mineshaft", "09/29/25 m12AB")))
	}

	@Test
	fun `no type from an empty sidebar`() {
		assertNull(MineshaftParser.parseType(emptyList()))
	}

	@Test
	fun `corpse count`() {
		val lines = tab("Lapis: NOT LOOTED", "Umber: LOOTED", "Tungsten: NOT LOOTED")
		assertEquals(3, MineshaftParser.parseCorpseCount(lines))
	}

	@Test
	fun `single corpse`() {
		assertEquals(1, MineshaftParser.parseCorpseCount(tab("Vanguard: NOT LOOTED")))
	}

	@Test
	fun `widget present but empty`() {
		assertEquals(0, MineshaftParser.parseCorpseCount(tab()))
	}

	@Test
	fun `widget as last line`() {
		assertEquals(0, MineshaftParser.parseCorpseCount(listOf("Area: Mineshaft", "Frozen Corpses:")))
	}

	@Test
	fun `no widget`() {
		assertNull(MineshaftParser.parseCorpseCount(listOf("Area: Mineshaft", "Powders:")))
	}

	@Test
	fun `count stops at the next widget`() {
		val lines = listOf("Frozen Corpses:", "Lapis: NOT LOOTED", "Powders:", "Umber: LOOTED")
		assertEquals(1, MineshaftParser.parseCorpseCount(lines))
	}

	@Test
	fun `mineshaft codes are unique and do not overlap`() {
		val codes = MineshaftType.entries.map { it.code }
		assertEquals(codes.size, codes.toSet().size)
		for (code in codes) {
			assertEquals(listOf(code), codes.filter { it in code }, code)
		}
	}
}
