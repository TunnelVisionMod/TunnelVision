package io.github.tunnelvisionmod.tunnelvision.data.crystals

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CrystalParserTest {
	/** The lore of the "Crystal Hollows Crystals" item, as it reads in the menu. */
	private val hotmLore = listOf(
		"Crystal Hollows Crystals",
		"Crystals are used to forge Gems into Perfect Gems. They can be",
		"found hidden within the Crystal Hollows.",
		"",
		"Find and place the full set of 5 Crystals in the Crystal Nucleus to",
		"unlock rare loot chests!",
		"",
		"Your Crystal Nucleus",
		"Jade \u2716 Not Found",
		"Amber \u2716 Not Found",
		"Amethyst \u2716 Not Found",
		"Sapphire \u2716 Not Found",
		"Topaz \u2714 Placed",
		"",
		"Your Other Crystals",
		"Jasper \u2716 Not Found",
		"Ruby \u2714 Found",
		"Opal \u2716 Not Found",
		"Aquamarine \u2716 Not Found",
		"Peridot \u2714 Found",
		"Onyx \u2714 Found",
		"Citrine \u2716 Not Found",
	)

	@Test
	fun `reads the real menu lore`() {
		assertEquals(
			mapOf(
				CrystalType.JASPER to false,
				CrystalType.RUBY to true,
				CrystalType.OPAL to false,
				CrystalType.AQUAMARINE to false,
				CrystalType.PERIDOT to true,
				CrystalType.ONYX to true,
				CrystalType.CITRINE to false,
			),
			CrystalParser.parseHotmLore(hotmLore),
		)
	}

	@Test
	fun `nucleus crystals are not ours`() {
		val states = CrystalParser.parseHotmLore(hotmLore)
		assertEquals(CrystalType.entries.size, states.size, "only the seven we track")
	}

	@Test
	fun `not found is not mistaken for found`() {
		assertEquals(CrystalReading(CrystalType.JASPER, false), CrystalParser.parseCrystalLine("Jasper \u2716 Not Found"))
		assertEquals(CrystalReading(CrystalType.RUBY, true), CrystalParser.parseCrystalLine("Ruby \u2714 Found"))
	}

	@Test
	fun `works without the tick and cross`() {
		assertEquals(CrystalReading(CrystalType.ONYX, true), CrystalParser.parseCrystalLine("Onyx Found"))
		assertEquals(CrystalReading(CrystalType.OPAL, false), CrystalParser.parseCrystalLine("Opal Not Found"))
	}

	@Test
	fun `prose and headers are not crystals`() {
		assertNull(CrystalParser.parseCrystalLine("Your Other Crystals"))
		assertNull(CrystalParser.parseCrystalLine("Crystals are used to forge Gems into Perfect Gems. They can be"))
		assertNull(CrystalParser.parseCrystalLine(""))
		assertNull(CrystalParser.parseCrystalLine("Ruby"), "a bare name says nothing about state")
	}

	@Test
	fun `the drop line is the crystal name, indented`() {
		assertEquals(CrystalType.JASPER, CrystalParser.parseChatGained("    Jasper Crystal"))
		assertEquals(CrystalType.RUBY, CrystalParser.parseChatGained("  Ruby Crystal"))
		assertEquals(CrystalType.ONYX, CrystalParser.parseChatGained("Onyx Crystal"))
		assertEquals(CrystalType.OPAL, CrystalParser.parseChatGained("    OPAL CRYSTAL"))
		assertEquals(CrystalType.PERIDOT, CrystalParser.parseChatGained("    Peridot"), "name on its own")
	}

	@Test
	fun `a sentence mentioning a crystal is not a drop`() {
		// The whole line must be the name, or chatter about crystals would hand you one.
		assertNull(CrystalParser.parseChatGained("You found a Jasper Crystal!"))
		assertNull(CrystalParser.parseChatGained("I need one more Ruby Crystal"))
		assertNull(CrystalParser.parseChatGained("RARE DROP! ONYX CRYSTAL"))
	}

	@Test
	fun `spending is checked before gaining`() {
		// A spend line names a crystal too, so it matches both - the feature checks consumed first.
		assertEquals(CrystalType.RUBY, CrystalParser.parseChatConsumed("You used a Ruby Crystal!"))
		assertEquals(CrystalType.ONYX, CrystalParser.parseChatConsumed("You placed an Onyx Crystal!"))
		assertNull(CrystalParser.parseChatConsumed("You found a Jasper Crystal!"))
	}

	@Test
	fun `player chat is ignored whatever tags it carries`() {
		// Level and rank are both optional, so every combination has to be caught.
		assertTrue(CrystalParser.isPlayerChat("[534] [MVP+] ImNeppy: Hello"))
		assertTrue(CrystalParser.isPlayerChat("[MVP+] ImNeppy: jasper crystal"))
		assertTrue(CrystalParser.isPlayerChat("[534] ImNeppy: ruby crystal"))
		assertTrue(CrystalParser.isPlayerChat("ImNeppy: onyx crystal"))
		assertTrue(CrystalParser.isPlayerChat("Party > [MVP++] Bob: opal crystal"))
		assertTrue(CrystalParser.isPlayerChat("Guild > Bob [Officer]: citrine crystal"))
	}

	@Test
	fun `server messages are not player chat`() {
		assertFalse(CrystalParser.isPlayerChat("    Jasper Crystal"))
		assertFalse(CrystalParser.isPlayerChat("RARE DROP! ONYX CRYSTAL"))
		assertFalse(CrystalParser.isPlayerChat("PRISTINE! You found Flawed Ruby Gemstone x2!"))
	}

	@Test
	fun `unrelated chat is ignored`() {
		assertNull(CrystalParser.parseChatGained("PRISTINE! You found Flawed Ruby Gemstone x2!"))
		assertNull(CrystalParser.parseChatGained(""))
	}

	@Test
	fun `every crystal we track is readable`() {
		for (crystal in CrystalType.entries) {
			assertEquals(
				CrystalReading(crystal, true),
				CrystalParser.parseCrystalLine(crystal.displayName + " \u2714 Found"),
				crystal.displayName,
			)
		}
	}
}
