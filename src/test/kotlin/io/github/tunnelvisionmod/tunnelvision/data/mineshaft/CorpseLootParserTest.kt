package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CorpseLootParserTest {
	@Test
	fun `reads the corpse off a loot header`() {
		assertEquals(CorpseType.LAPIS, CorpseLootParser.corpseType("LAPIS CORPSE LOOT!"))
		assertEquals(CorpseType.VANGUARD, CorpseLootParser.corpseType("    VANGUARD CORPSE LOOT!    "))
		assertEquals(CorpseType.TUNGSTEN, CorpseLootParser.corpseType("TUNGSTEN CORPSE LOOT! "))
		assertEquals(CorpseType.UMBER, CorpseLootParser.corpseType("UMBER CORPSE LOOT!"))
	}

	@Test
	fun `other lines are not headers`() {
		assertNull(CorpseLootParser.corpseType("LAPIS CORPSE"))
		assertNull(CorpseLootParser.corpseType("Party > Bob: LAPIS CORPSE LOOT!"))
		assertNull(CorpseLootParser.corpseType("PRISTINE! You found Flawed Ruby Gemstone x2!"))
	}

	@Test
	fun `count can lead or trail`() {
		assertEquals(LootedItem("Flawed Peridot Gemstone", 20), CorpseLootParser.item("20x Flawed Peridot Gemstone"))
		assertEquals(LootedItem("Flawed Peridot Gemstone", 20), CorpseLootParser.item("Flawed Peridot Gemstone x20"))
		assertEquals(LootedItem("Glacite Powder", 2048), CorpseLootParser.item("2,048x Glacite Powder"))
	}

	@Test
	fun `a bare name is one of it`() {
		assertEquals(LootedItem("Glacite Jewel", 1), CorpseLootParser.item("Glacite Jewel"))
		assertEquals(LootedItem("Shattered Locket", 1), CorpseLootParser.item("Shattered Locket"))
	}

	@Test
	fun `bullets and drop banners are decoration`() {
		assertEquals(LootedItem("Glacite Jewel", 1, symbol = "✦"), CorpseLootParser.item(" ✦ Glacite Jewel"))
		assertEquals(LootedItem("Caged Wisp", 1), CorpseLootParser.item("RARE DROP! Caged Wisp"))
		assertEquals(LootedItem("Shattered Locket", 1), CorpseLootParser.item("CRAZY RARE DROP! Shattered Locket"))
		assertEquals(LootedItem("Onyx Crystal", 1, symbol = "+"), CorpseLootParser.item("+ Onyx Crystal"))
	}

	@Test
	fun `a trailing magic find note is dropped`() {
		assertEquals(LootedItem("Caged Wisp", 1), CorpseLootParser.item("RARE DROP! Caged Wisp (+15% ✯ Magic Find)"))
		assertEquals(LootedItem("Glacite Jewel", 4), CorpseLootParser.item("4x Glacite Jewel (+5% Magic Find)"))
	}

	/** Both of these shipped wrong once: the gem symbol stuck to the name and killed every lookup. */
	@Test
	fun `a gem symbol in front of the name is decoration`() {
		assertEquals(LootedItem("Fine Peridot Gemstone", 12, symbol = "✦"), CorpseLootParser.item(" ✦ Fine Peridot Gemstone ×12"))
		assertEquals(LootedItem("Flawed Onyx Gemstone", 60, symbol = "✖"), CorpseLootParser.item("60x ✖ Flawed Onyx Gemstone"))
		assertEquals("Fine Onyx Gemstone", CorpseLootParser.item(" ✖ Fine Onyx Gemstone ×4")?.name)
	}

	@Test
	fun `the count may use a multiplication sign`() {
		assertEquals(LootedItem("Suspicious Scrap", 4), CorpseLootParser.item("Suspicious Scrap ×4"))
		assertEquals(LootedItem("Glacite Powder", 15759), CorpseLootParser.item("Glacite Powder ×15,759"))
	}

	@Test
	fun `REWARDS is a section label, not a drop`() {
		assertNull(CorpseLootParser.item("REWARDS"))
		assertTrue(CorpseLootParser.isFrame("REWARDS"))
		assertFalse(CorpseLootParser.isRule("REWARDS"))
	}

	@Test
	fun `the bonus drop note is not a drop`() {
		// The real line is "+1 bonus drop!" - the shout mark once ended the block early and left
		// the rest of the loot printed raw.
		for (line in listOf("+1 bonus drop!", "+1 Bonus Drop", "+1", "+ 1", "+2 Bonus Drops", "1 Bonus Drop")) {
			assertTrue(CorpseLootParser.isFrame(line), "$line should be frame")
			assertNull(CorpseLootParser.item(line))
		}
	}

	@Test
	fun `rules are told apart from the rest of the frame`() {
		assertTrue(CorpseLootParser.isRule("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"))
		assertFalse(CorpseLootParser.isRule(""))
		assertFalse(CorpseLootParser.isRule("Glacite Jewel"))
	}

	@Test
	fun `frames and blanks hold nothing`() {
		assertTrue(CorpseLootParser.isFrame(""))
		assertTrue(CorpseLootParser.isFrame("   "))
		assertTrue(CorpseLootParser.isFrame("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"))
		assertFalse(CorpseLootParser.isFrame("Glacite Jewel"))
	}

	@Test
	fun `a line with a colon is somebody talking, not a drop`() {
		assertNull(CorpseLootParser.item("Party > Bob: glacite jewel"))
		assertNull(CorpseLootParser.item("Lapis: NOT LOOTED"))
		assertNull(CorpseLootParser.item("[MVP+] ImNeppy: 20x Flawed Peridot Gemstone"))
	}

	@Test
	fun `a sentence is not a drop`() {
		assertNull(CorpseLootParser.item("You have reached the maximum amount of corpses you can loot today, come back tomorrow"))
		assertNull(CorpseLootParser.item("▬▬▬▬"))
		assertNull(CorpseLootParser.item("0x Glacite Jewel"))
	}
}
