package io.github.tunnelvisionmod.tunnelvision.data.bazaar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PriceHistoryTest {
	private fun history(points: List<Pair<Double, Double>>): String =
		points.joinToString(prefix = """{"productId": "FINE_RUBY_GEM", "history": [""", postfix = "]}") { (buy, sell) ->
			"""{"timestamp": "2026-10-09T10:00:00+00:00", "instaBuyPrice": $buy, "instaSellPrice": $sell}"""
		}

	@Test
	fun `median of odd and even lists`() {
		assertEquals(2.0, PriceHistory.median(listOf(3.0, 1.0, 2.0)))
		assertEquals(2.5, PriceHistory.median(listOf(4.0, 1.0, 3.0, 2.0)))
	}

	@Test
	fun `a buyout covering a few hours does not move the median`() {
		val normal = List(380) { 22_600.0 to 20_200.0 }
		val buyout = List(20) { 41_569.0 to 22_192.0 }
		val median = PriceHistory.parse(history(normal + buyout), now = 5L)
		assertEquals(MedianPrices(sellOffer = 22_600.0, instantSell = 20_200.0, fetchedAt = 5L), median)
	}

	@Test
	fun `too little history has no median`() {
		assertNull(PriceHistory.parse(history(List(10) { 22_600.0 to 20_200.0 }), now = 0L))
		assertNull(PriceHistory.parse("""{"history": [], "resolution": "hour"}""", now = 0L))
	}

	@Test
	fun `missing and zero prices are skipped`() {
		val points = List(30) { 22_600.0 to 20_200.0 } + List(5) { 0.0 to 0.0 }
		val json = history(points).replace("]}", """, {"timestamp": "x"}]}""")
		assertEquals(22_600.0, PriceHistory.parse(json, now = 0L)?.sellOffer)
	}

	@Test
	fun `broken response is a failure`() {
		assertThrows<Exception> { PriceHistory.parse("<html>525</html>", now = 0L) }
	}
}
