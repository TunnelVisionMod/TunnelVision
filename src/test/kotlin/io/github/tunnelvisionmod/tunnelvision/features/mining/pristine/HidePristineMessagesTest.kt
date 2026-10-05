package io.github.tunnelvisionmod.tunnelvision.features.mining.pristine

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HidePristineMessagesTest {
	@Test
	fun `a normal proc is hidden`() {
		assertTrue(HidePristineMessages.shouldHide("PRISTINE! You found Flawed Ruby Gemstone x2!"))
		assertTrue(HidePristineMessages.shouldHide("PRISTINE! You found \u2726 Flawed Amethyst Gemstone x5!"))
	}

	@Test
	fun `a high proc is hidden too`() {
		assertTrue(HidePristineMessages.shouldHide("PRISTINE! You found Flawed Jade Gemstone x12!"))
	}

	@Test
	fun `an unparsed pristine line stays visible`() {
		assertFalse(HidePristineMessages.shouldHide("PRISTINE! You found something new x1!"))
		assertFalse(HidePristineMessages.shouldHide("PRISTINE! You found nothing at all"))
	}

	@Test
	fun `unrelated chat stays visible`() {
		assertFalse(HidePristineMessages.shouldHide("You used your Mining Speed Boost Pickaxe Ability!"))
		assertFalse(HidePristineMessages.shouldHide("Party > ImNeppy: !ptme"))
		assertFalse(HidePristineMessages.shouldHide(""))
	}
}
