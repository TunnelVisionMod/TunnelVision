package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlueCheeseTest {
	@Test
	fun `blue cheese upgrade module`() {
		assertTrue(BlueCheese.isUpgradeModule("goblin_omelette_blue_cheese"))
		assertTrue(BlueCheese.isUpgradeModule("GOBLIN_OMELETTE_BLUE_CHEESE"))
	}

	@Test
	fun `other upgrade modules`() {
		assertFalse(BlueCheese.isUpgradeModule("goblin_omelette_pesto"))
		assertFalse(BlueCheese.isUpgradeModule(null))
	}
}
