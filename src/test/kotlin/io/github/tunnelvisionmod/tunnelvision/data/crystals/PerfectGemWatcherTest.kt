package io.github.tunnelvisionmod.tunnelvision.data.crystals

import io.github.tunnelvisionmod.tunnelvision.data.forge.ForgeSlot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PerfectGemWatcherTest {
	private val watcher = PerfectGemWatcher()
	private val handle = ForgeSlot(1, "Bejeweled Handle", false)
	private val perfectCitrine = ForgeSlot(2, "Perfect Citrine Gemstone", false)

	@Test
	fun `first reading is only a baseline`() {
		assertEquals(emptyList<CrystalType>(), watcher.newPerfectGems(listOf(handle, perfectCitrine)))
	}

	@Test
	fun `new perfect gemstone uses up its crystal`() {
		watcher.newPerfectGems(listOf(handle))
		assertEquals(listOf(CrystalType.CITRINE), watcher.newPerfectGems(listOf(handle, perfectCitrine)))
	}

	@Test
	fun `same perfect gemstone is reported only once`() {
		watcher.newPerfectGems(listOf(handle))
		watcher.newPerfectGems(listOf(handle, perfectCitrine))
		assertEquals(emptyList<CrystalType>(), watcher.newPerfectGems(listOf(handle, perfectCitrine.copy(isReady = true))))
	}

	@Test
	fun `perfect gemstone with an icon or a cut off name still counts`() {
		watcher.newPerfectGems(listOf(handle))
		assertEquals(
			listOf(CrystalType.OPAL, CrystalType.RUBY),
			watcher.newPerfectGems(listOf(handle, ForgeSlot(2, " Perfect Opal Gemstone", false), ForgeSlot(3, "Perfect Ruby Gem...", false))),
		)
	}

	@Test
	fun `other forge items are ignored`() {
		watcher.newPerfectGems(emptyList())
		assertEquals(emptyList<CrystalType>(), watcher.newPerfectGems(listOf(handle, ForgeSlot(2, "Flawless Citrine Gemstone", false))))
	}

	@Test
	fun `reset starts a new baseline`() {
		watcher.newPerfectGems(emptyList())
		watcher.reset()
		assertEquals(emptyList<CrystalType>(), watcher.newPerfectGems(listOf(perfectCitrine)))
	}
}
