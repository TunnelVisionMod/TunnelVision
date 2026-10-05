package io.github.tunnelvisionmod.tunnelvision.features.forge.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ForgeSummaryTest {
	@Test
	fun `groups duplicate items`() {
		assertEquals(
			listOf("Refined Diamond ×3", "Perfect Ruby Gemstone"),
			ForgeSummary.grouped(listOf("Refined Diamond", "Perfect Ruby Gemstone", "Refined Diamond", "Refined Diamond")),
		)
	}

	@Test
	fun `subtitle names a single kind of item`() {
		assertEquals("Perfect Ruby Gemstone", ForgeSummary.subtitle(listOf("Perfect Ruby Gemstone")))
		assertEquals("Refined Diamond ×2", ForgeSummary.subtitle(listOf("Refined Diamond", "Refined Diamond")))
	}

	@Test
	fun `subtitle counts several kinds of items`() {
		assertEquals("5 items ready", ForgeSummary.subtitle(listOf("A", "B", "B", "C", "C")))
	}
}
