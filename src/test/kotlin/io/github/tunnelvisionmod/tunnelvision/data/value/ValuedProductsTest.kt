package io.github.tunnelvisionmod.tunnelvision.data.value

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValuedProductsTest {
	private val products = ValuedProducts.inPriority

	@Test
	fun `fine gems come first`() {
		assertEquals(GemstoneShaft.entries.map { it.fineGemId }, products.take(GemstoneShaft.entries.size))
	}

	@Test
	fun `every product is fetched once`() {
		assertEquals(products.distinct(), products)
	}

	@Test
	fun `perfects come before corpse loot`() {
		val lastPerfect = products.indexOf("PERFECT_RUBY_GEM")
		assertTrue(lastPerfect in 0..<products.indexOf("GLACITE_JEWEL"))
		assertTrue(lastPerfect < products.indexOf("ENCHANTED_GLACITE"))
	}

	@Test
	fun `forge inputs that never drop are not fetched`() {
		assertFalse("FLAWLESS_JASPER_GEM" in products)
		assertFalse("FLAWLESS_RUBY_GEM" in products)
	}
}
