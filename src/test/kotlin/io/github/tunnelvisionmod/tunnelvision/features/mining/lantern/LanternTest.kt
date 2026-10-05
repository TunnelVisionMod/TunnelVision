package io.github.tunnelvisionmod.tunnelvision.features.mining.lantern

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LanternTest {
	@Test
	fun `picks the strongest lantern in the inventory`() {
		assertEquals(Lantern.WILL_O_WISP, Lantern.strongest(listOf("Mithril Lantern", "Will-o'-wisp", "Titanium Drill DR-X555")))
		assertEquals(Lantern.GLACITE, Lantern.strongest(listOf("Dwarven Lantern", "Glacite Lantern")))
		assertEquals(Lantern.DWARVEN, Lantern.strongest(listOf("Dwarven Lantern")))
	}

	@Test
	fun `no lantern in the inventory`() {
		assertNull(Lantern.strongest(listOf("Titanium Drill DR-X555", "Enchanted Mithril", "")))
	}

	@Test
	fun `despawn message`() {
		assertEquals(Lantern.WILL_O_WISP, Lantern.fromDespawnMessage("Your Will-o'-wisp despawned."))
		assertEquals(Lantern.GLACITE, Lantern.fromDespawnMessage("Your Glacite Lantern despawned."))
		assertNull(Lantern.fromDespawnMessage("Your Power Orb despawned."))
		assertNull(Lantern.fromDespawnMessage("Will-o'-wisp"))
	}
}
