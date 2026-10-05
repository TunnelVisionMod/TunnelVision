package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MineshaftEntryParserTest {
	@Test
	fun `reads the guest that was warped in`() {
		assertEquals("ISingularity_", MineshaftEntryParser.playerEntered(" ⛏ ISingularity_ entered the mineshaft!"))
		assertEquals("lrg89", MineshaftEntryParser.playerEntered("⛏ lrg89 entered the mineshaft!"))
	}

	@Test
	fun `reads your own arrival in someone elses mineshaft`() {
		assertEquals("ImNeppy", MineshaftEntryParser.playerEntered(" ⛏ ImNeppy entered the mineshaft!"))
	}

	@Test
	fun `ignores other messages`() {
		assertNull(MineshaftEntryParser.playerEntered("⚔ [MVP+] ISingularity_ warped to your instance"))
		assertNull(MineshaftEntryParser.playerEntered("Party > [MVP+] ISingularity_: !w"))
		assertNull(MineshaftEntryParser.playerEntered("The mineshaft entrance has caved in..."))
		assertNull(MineshaftEntryParser.playerEntered(""))
	}
}
