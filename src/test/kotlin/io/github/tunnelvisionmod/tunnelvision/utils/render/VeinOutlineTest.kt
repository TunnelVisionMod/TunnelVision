package io.github.tunnelvisionmod.tunnelvision.utils.render

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VeinOutlineTest {
	@Test
	fun `a single block has all twelve edges`() {
		assertEquals(12, VeinOutline.edges(setOf(Pos(0, 0, 0))).size)
	}

	@Test
	fun `two blocks side by side share no inner edges`() {
		val edges = VeinOutline.edges(setOf(Pos(0, 0, 0), Pos(1, 0, 0)))
		assertEquals(16, edges.size)
		assertTrue(edges.none { (a, b) -> a.x == 1 && b.x == 1 })
	}

	@Test
	fun `an L shape draws the inner corner once`() {
		val edges = VeinOutline.edges(setOf(Pos(0, 0, 0), Pos(1, 0, 0), Pos(0, 0, 1)))
		assertEquals(1, edges.count { (a, b) -> a == Pos(1, 0, 1) && b == Pos(1, 1, 1) })
		assertEquals(edges.size, edges.toSet().size)
	}
}
