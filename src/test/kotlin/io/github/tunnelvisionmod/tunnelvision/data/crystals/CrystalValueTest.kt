package io.github.tunnelvisionmod.tunnelvision.data.crystals

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CrystalValueTest {
	@Test
	fun `buys Fine when 80 of them cost less than a Flawless`() {
		val worth = CrystalValue.worth(perfect = 20_000_000.0, fineBuyOrder = 30_000.0, flawlessInstaBuy = 2_500_000.0)
		assertEquals(ForgeInput.FINE, worth?.input)
		assertEquals(20_000_000.0 - 400 * 30_000.0, worth?.value)
	}

	@Test
	fun `buys Flawless when insta-buying them is cheaper than 80 Fine`() {
		val worth = CrystalValue.worth(perfect = 20_000_000.0, fineBuyOrder = 30_000.0, flawlessInstaBuy = 2_200_000.0)
		assertEquals(ForgeInput.FLAWLESS, worth?.input)
		assertEquals(20_000_000.0 - 5 * 2_200_000.0, worth?.value)
	}

	@Test
	fun `falls back to whichever gem has a price`() {
		assertEquals(ForgeInput.FINE, CrystalValue.worth(20_000_000.0, 30_000.0, null)?.input)
		assertEquals(ForgeInput.FLAWLESS, CrystalValue.worth(20_000_000.0, null, 2_500_000.0)?.input)
	}

	@Test
	fun `is unknown without a Perfect price or any gem price`() {
		assertNull(CrystalValue.worth(null, 30_000.0, 2_500_000.0))
		assertNull(CrystalValue.worth(20_000_000.0, null, null))
	}
}
