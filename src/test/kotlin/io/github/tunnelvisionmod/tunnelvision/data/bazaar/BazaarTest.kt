package io.github.tunnelvisionmod.tunnelvision.data.bazaar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BazaarTest {
	@Test
	fun `parses quick status prices`() {
		val json = """
			{"success": true, "lastUpdated": 1, "products": {
				"FINE_JASPER_GEM": {"product_id": "FINE_JASPER_GEM", "quick_status": {"buyPrice": 62996.4, "sellPrice": 59345.1}},
				"FINE_RUBY_GEM": {"product_id": "FINE_RUBY_GEM", "quick_status": {"buyPrice": 21649.0, "sellPrice": 19770.0}}
			}}
		""".trimIndent()
		val prices = Bazaar.parse(json)
		assertEquals(BazaarPrices(sellOffer = 62996.4, instantSell = 59345.1), prices["FINE_JASPER_GEM"])
		assertEquals(BazaarPrices(sellOffer = 21649.0, instantSell = 19770.0), prices["FINE_RUBY_GEM"])
	}

	@Test
	fun `failed response has no prices`() {
		assertTrue(Bazaar.parse("""{"success": false, "cause": "Too many requests"}""").isEmpty())
	}
}
