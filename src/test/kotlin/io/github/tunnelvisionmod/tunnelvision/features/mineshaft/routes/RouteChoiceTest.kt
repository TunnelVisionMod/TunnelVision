package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.routes

import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.WarpRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RouteChoiceTest {
	private val routes = setOf("RUBY_1", "JASP_1", "JASP_1_SOLO", "CRYSTAL", "CRYSTAL_SOLO")
	private fun choose(code: String, role: WarpRole) = RouteChoice.of(code, role, routes::contains)

	@Test
	fun `normal shafts always use their only route`() {
		assertEquals(RouteChoice("RUBY_1", GemstoneRoute.Part.ALL), choose("RUBY_1", WarpRole.SOLO))
		assertEquals(RouteChoice("RUBY_1", GemstoneRoute.Part.SECOND), choose("RUBY_1", WarpRole.WARPED_IN))
	}

	@Test
	fun `jasper picks the solo route or a half`() {
		assertEquals(RouteChoice("JASP_1_SOLO", GemstoneRoute.Part.ALL), choose("JASP_1", WarpRole.SOLO))
		assertEquals(RouteChoice("JASP_1", GemstoneRoute.Part.FIRST), choose("JASP_1", WarpRole.WARPED_SOMEONE))
		assertEquals(RouteChoice("JASP_1", GemstoneRoute.Part.SECOND), choose("JASP_1", WarpRole.WARPED_IN))
	}

	@Test
	fun `every crystal shaft shares the crystal route`() {
		assertEquals(RouteChoice("CRYSTAL_SOLO", GemstoneRoute.Part.ALL), choose("RUBY_C", WarpRole.SOLO))
		assertEquals(RouteChoice("CRYSTAL", GemstoneRoute.Part.FIRST), choose("JASP_C", WarpRole.WARPED_SOMEONE))
	}

	@Test
	fun `shafts without a route get nothing`() {
		assertNull(choose("TUNG_1", WarpRole.SOLO))
	}
}
