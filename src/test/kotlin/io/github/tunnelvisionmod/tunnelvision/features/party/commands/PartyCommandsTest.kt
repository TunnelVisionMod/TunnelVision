package io.github.tunnelvisionmod.tunnelvision.features.party.commands

import io.github.tunnelvisionmod.tunnelvision.data.party.PartyChatParser
import io.github.tunnelvisionmod.tunnelvision.data.party.PartyMessage
import io.github.tunnelvisionmod.tunnelvision.utils.Cooldown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PartyCommandsTest {
	@Test
	fun `party message with rank`() {
		assertEquals(PartyMessage("lrg89", "!warp"), PartyChatParser.parse("Party > [MVP+] lrg89: !warp"))
	}

	@Test
	fun `party message without rank`() {
		assertEquals(PartyMessage("nea89o", "!ptme"), PartyChatParser.parse("Party > nea89o: !ptme"))
	}

	@Test
	fun `party message keeps colons in the text`() {
		assertEquals(PartyMessage("Bob_1", "time: 12:30"), PartyChatParser.parse("Party > [VIP] Bob_1: time: 12:30"))
	}

	@Test
	fun `other chats are ignored`() {
		assertNull(PartyChatParser.parse("[250] [MVP+] lrg89: !warp"))
		assertNull(PartyChatParser.parse("Guild > [MVP+] lrg89 [Member]: !warp"))
		assertNull(PartyChatParser.parse("From [MVP+] lrg89: !warp"))
	}

	@Test
	fun `recognizes command aliases`() {
		assertEquals(PartyCommand.TRANSFER, PartyCommand.parse("!pt"))
		assertEquals(PartyCommand.TRANSFER, PartyCommand.parse("!ptme"))
		assertEquals(PartyCommand.WARP, PartyCommand.parse("!w"))
		assertEquals(PartyCommand.WARP, PartyCommand.parse("!WARP"))
		assertEquals(PartyCommand.WARP, PartyCommand.parse("!warp pls"))
	}

	@Test
	fun `ignores non commands`() {
		assertNull(PartyCommand.parse("warp"))
		assertNull(PartyCommand.parse("!wa"))
		assertNull(PartyCommand.parse("hello !warp"))
		assertNull(PartyCommand.parse(""))
	}

	@Test
	fun `builds hypixel commands`() {
		assertEquals("party transfer lrg89", PartyCommand.TRANSFER.hypixelCommand("lrg89"))
		assertEquals("p warp", PartyCommand.WARP.hypixelCommand("lrg89"))
	}

	@Test
	fun `warp works from yourself but transfer does not`() {
		assertTrue(PartyCommand.WARP.canBeUsedBy("Me", ownName = "Me"))
		assertFalse(PartyCommand.TRANSFER.canBeUsedBy("me", ownName = "Me"))
		assertTrue(PartyCommand.TRANSFER.canBeUsedBy("lrg89", ownName = "Me"))
	}

	@Test
	fun `warp cooldown`() {
		val cooldown = Cooldown(5_000)
		assertTrue(cooldown.tryUse(now = 10_000))
		assertFalse(cooldown.tryUse(now = 14_999))
		assertTrue(cooldown.tryUse(now = 15_000))
	}
}
