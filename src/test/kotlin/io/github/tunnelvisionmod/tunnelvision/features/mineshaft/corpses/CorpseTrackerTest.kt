package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The price column is only as straight as this: a space is 4px and a bold space 5px, so hitting an
 * exact pixel width means mixing the two. Rounding to whole plain spaces is what left the `k`s
 * sitting at slightly different places.
 */
class CorpseTrackerTest {
	private val narrow = 4
	private val wide = 5

	private fun width(spacing: Pair<Int, Int>) = spacing.first * narrow + spacing.second * wide

	@Test
	fun `hits the exact pixel width`() {
		for (pixels in 12..200) {
			val spacing = CorpseTracker.spacing(pixels, narrow, wide)
			assertEquals(pixels, width(spacing), "could not make $pixels px")
		}
	}

	@Test
	fun `uses as few characters as it can`() {
		// 20px is five plain spaces or four bold ones; the shorter run wins.
		assertEquals(0 to 4, CorpseTracker.spacing(20, narrow, wide))
		assertEquals(2 to 0, CorpseTracker.spacing(8, narrow, wide))
		assertEquals(0 to 1, CorpseTracker.spacing(5, narrow, wide))
	}

	@Test
	fun `mixes both widths when neither alone fits`() {
		val spacing = CorpseTracker.spacing(13, narrow, wide)
		assertEquals(13, width(spacing))
		assertTrue(spacing.first > 0 && spacing.second > 0, "13px needs one of each")
	}

	@Test
	fun `never asks for negative space`() {
		assertEquals(0 to 0, CorpseTracker.spacing(0, narrow, wide))
		assertEquals(0 to 0, CorpseTracker.spacing(-20, narrow, wide))
	}

	@Test
	fun `falls back to plain spaces on a width it cannot hit`() {
		// 7px is neither 4a nor 5b nor a sum of them.
		assertEquals(1 to 0, CorpseTracker.spacing(7, narrow, wide))
	}
}
