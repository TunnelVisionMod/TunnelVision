package io.github.tunnelvisionmod.tunnelvision.data.forge

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ForgeParserTest {
	private fun tab(vararg forgeLines: String) = listOf("Area: Dwarven Mines", "Forges:") + forgeLines + listOf("Commissions:", "Mithril Miner: 50%")

	@Test
	fun `parses running and ready slots`() {
		val slots = ForgeParser.parseTab(tab("1) Perfect Ruby Gemstone: 3h 2m", "2) Refined Diamond: Ready!"))
		assertEquals(
			listOf(ForgeSlot(1, "Perfect Ruby Gemstone", false), ForgeSlot(2, "Refined Diamond", true)),
			slots,
		)
	}

	@Test
	fun `strips the gemstone icon from item names`() {
		assertEquals(listOf(ForgeSlot(1, "Perfect Opal Gemstone", false)), ForgeParser.parseTab(tab("1)  Perfect Opal Gemstone: 19h 59m")))
	}

	@Test
	fun `accepts uppercase ready`() {
		assertEquals(listOf(ForgeSlot(1, "Refined Mithril", true)), ForgeParser.parseTab(tab("1) Refined Mithril: READY")))
	}

	@Test
	fun `skips empty and locked slots`() {
		val slots = ForgeParser.parseTab(tab("1) Refined Diamond: 12m 5s", "2) EMPTY", "3) LOCKED"))
		assertEquals(listOf(ForgeSlot(1, "Refined Diamond", false)), slots)
	}

	@Test
	fun `stops at next widget`() {
		val slots = ForgeParser.parseTab(tab("1) EMPTY"))
		assertEquals(emptyList<ForgeSlot>(), slots)
	}

	@Test
	fun `missing widget`() {
		assertNull(ForgeParser.parseTab(listOf("Area: Dwarven Mines", "Commissions:")))
	}

	@Test
	fun `forge is full when no slot is empty`() {
		val status = ForgeParser.parseStatus(tab("1) Refined Diamond: 12m", "2) Refined Mithril: Ready!", "3) LOCKED"))
		assertEquals(0, status?.openSlots)
		assertEquals(1, status?.lockedSlots)
		assertTrue(status!!.isFull)
	}

	@Test
	fun `forge is not full while a slot is empty`() {
		val status = ForgeParser.parseStatus(tab("1) Refined Diamond: 12m", "2) EMPTY", "3) LOCKED"))
		assertEquals(1, status?.openSlots)
		assertFalse(status!!.isFull)
	}

	@Test
	fun `locked slots do not count as open`() {
		val status = ForgeParser.parseStatus(tab("1) LOCKED", "2) LOCKED"))
		assertEquals(0, status?.openSlots)
		assertTrue(status!!.isFull, "locked slots need Quick Forge, not a free hand")
	}

	@Test
	fun `status is unknown without the widget`() {
		assertNull(ForgeParser.parseStatus(listOf("Area: Dwarven Mines", "Commissions:")))
	}

	@Test
	fun `a header with no slot lines is unknown rather than full`() {
		assertNull(ForgeParser.parseStatus(listOf("Forges:", "Commissions:", "Mithril Miner: 50%")))
		assertEquals(emptyList<ForgeSlot>(), ForgeParser.parseTab(listOf("Forges:", "Commissions:")))
	}
}
