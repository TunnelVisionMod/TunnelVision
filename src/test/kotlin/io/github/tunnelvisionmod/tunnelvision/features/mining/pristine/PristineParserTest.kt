package io.github.tunnelvisionmod.tunnelvision.features.mining.pristine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PristineParserTest {
	@Test
	fun `plain proc`() {
		assertEquals(
			PristineProc("Ruby", 2),
			PristineParser.parse("PRISTINE! You found Flawed Ruby Gemstone x2!"),
		)
	}

	@Test
	fun `proc with a leftover marker between found and tier`() {
		assertEquals(
			PristineProc("Amethyst", 5),
			PristineParser.parse("PRISTINE! You found \u2726 Flawed Amethyst Gemstone x5!"),
		)
	}

	@Test
	fun `every gemstone type`() {
		for (gem in listOf("Ruby", "Amethyst", "Jade", "Sapphire", "Amber", "Topaz", "Jasper", "Opal", "Aquamarine", "Citrine", "Peridot", "Onyx")) {
			assertEquals(PristineProc(gem, 3), PristineParser.parse("PRISTINE! You found Flawed $gem Gemstone x3!"), gem)
		}
	}

	@Test
	fun `multi digit amount`() {
		assertEquals(PristineProc("Jade", 12), PristineParser.parse("PRISTINE! You found Flawed Jade Gemstone x12!"))
	}

	@Test
	fun `surrounding whitespace is tolerated`() {
		assertEquals(PristineProc("Topaz", 4), PristineParser.parse("  PRISTINE! You found Flawed Topaz Gemstone x4!  "))
	}

	@Test
	fun `unrelated chat is ignored`() {
		assertNull(PristineParser.parse("You used your Mining Speed Boost Pickaxe Ability!"))
		assertNull(PristineParser.parse("PRISTINE! You found nothing at all"))
		assertNull(PristineParser.parse(""))
	}

	@Test
	fun `pristine lines are recognised even when unparsed`() {
		assertTrue(PristineParser.isPristineMessage("PRISTINE! You found something new x1!"))
		assertTrue(PristineParser.isPristineMessage("  PRISTINE! You found Flawed Ruby Gemstone x2!"))
		assertFalse(PristineParser.isPristineMessage("You found Flawed Ruby Gemstone x2!"))
	}
}
