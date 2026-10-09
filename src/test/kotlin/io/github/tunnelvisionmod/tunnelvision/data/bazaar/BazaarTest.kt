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

	@Test
	fun `parses lowest bins`() {
		val json = """
			{"SHATTERED_PENDANT": 505000000, "CAGED_WISP": 42500000}
		""".trimIndent()
		val bins = Bazaar.parseLowestBins(json)
		assertEquals(505_000_000.0, bins["SHATTERED_PENDANT"])
		assertEquals(42_500_000.0, bins["CAGED_WISP"])
	}

	@Test
	fun `broken lowest bin feed has no prices`() {
		assertTrue(Bazaar.parseLowestBins("<html>525</html>").isEmpty())
	}

	@Test
	fun `live prices are capped at their median`() {
		val live = BazaarPrices(sellOffer = 41_569.0, instantSell = 22_192.0)
		val median = MedianPrices(sellOffer = 22_609.0, instantSell = 20_238.0, fetchedAt = 0L)
		assertEquals(BazaarPrices(sellOffer = 22_609.0, instantSell = 20_238.0), Bazaar.capped(live, median))
	}

	@Test
	fun `prices below their median stay live`() {
		val live = BazaarPrices(sellOffer = 18_000.0, instantSell = 16_000.0)
		val median = MedianPrices(sellOffer = 22_609.0, instantSell = 20_238.0, fetchedAt = 0L)
		assertEquals(live, Bazaar.capped(live, median))
	}

	@Test
	fun `no median means live prices`() {
		val live = BazaarPrices(sellOffer = 41_569.0, instantSell = 22_192.0)
		assertEquals(live, Bazaar.capped(live, null))
	}
}
