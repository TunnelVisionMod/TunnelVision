package io.github.tunnelvisionmod.tunnelvision.features.party.sharedwarp

import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftType
import io.github.tunnelvisionmod.tunnelvision.features.party.partyshare.MineshaftPartyShare
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SharedMineshaftTest {
	@Test
	fun `parses shared mineshaft`() {
		assertEquals(
			SharedMineshaft(MineshaftType.JASPER, "Lapis 2, Tungsten 1"),
			SharedMineshaftParser.parse("Mineshafttype: JASP_1, Corpses: Lapis 2, Tungsten 1"),
		)
	}

	@Test
	fun `parses shared mineshaft after ptme`() {
		assertEquals(
			SharedMineshaft(MineshaftType.UMBER, "Lapis 1"),
			SharedMineshaftParser.parse("!ptme Mineshafttype: UMBE_1, Corpses: Lapis 1"),
		)
	}

	@Test
	fun `understands the party share message`() {
		val message = MineshaftPartyShare.buildMessage(MineshaftType.JASPER, mapOf("Lapis" to 2, "Tungsten" to 1))
		assertEquals(SharedMineshaft(MineshaftType.JASPER, "Lapis 2, Tungsten 1"), SharedMineshaftParser.parse(message))
	}

	@Test
	fun `parses shared mineshaft without corpses`() {
		assertEquals(SharedMineshaft(MineshaftType.TOPAZ_1, null), SharedMineshaftParser.parse("Mineshafttype: TOPA_1"))
	}

	@Test
	fun `ignores unknown type and other messages`() {
		assertNull(SharedMineshaftParser.parse("Mineshafttype: XXXX_9, Corpses: Lapis 1"))
		assertNull(SharedMineshaftParser.parse("!warp"))
		assertNull(SharedMineshaftParser.parse("anyone got a jasper shaft?"))
	}

	@Test
	fun `warp window`() {
		val window = WarpWindow()
		assertFalse(window.isOpen(now = 0, windowSeconds = 10))
		window.open(now = 1_000)
		assertTrue(window.isOpen(now = 10_999, windowSeconds = 10))
		assertFalse(window.isOpen(now = 11_000, windowSeconds = 10))
	}

	@Test
	fun `zero seconds means no time limit`() {
		val window = WarpWindow()
		window.open(now = 0)
		assertTrue(window.isOpen(now = 3_600_000, windowSeconds = 0))
	}

	@Test
	fun `closed window stays closed`() {
		val window = WarpWindow()
		window.open(now = 0)
		window.close()
		assertFalse(window.isOpen(now = 1, windowSeconds = 0))
	}
}
