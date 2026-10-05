package io.github.tunnelvisionmod.tunnelvision.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormatCoinsTest {
	@Test
	fun `scales to billions millions and thousands`() {
		assertEquals("1.2B", formatCoins(1_234_567_890.0))
		assertEquals("11.9M", formatCoins(11_864_825.0))
		assertEquals("352k", formatCoins(352_372.0))
		assertEquals("940", formatCoins(940.0))
	}

	@Test
	fun `keeps the sign so deltas read correctly`() {
		assertEquals("-49k", formatCoins(-49_000.0))
		assertEquals("-1.8M", formatCoins(-1_804_000.0))
	}

	@Test
	fun `rounds to zero without a sign`() {
		assertEquals("0", formatCoins(0.0))
		assertEquals("0", formatCoins(-0.2))
	}

	@Test
	fun `boundaries land on the larger unit`() {
		assertEquals("1000k", formatCoins(999_999.0))
		assertEquals("1.0M", formatCoins(1_000_000.0))
		assertEquals("1k", formatCoins(1_000.0))
	}
}
