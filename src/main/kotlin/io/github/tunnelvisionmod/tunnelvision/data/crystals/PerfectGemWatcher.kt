package io.github.tunnelvisionmod.tunnelvision.data.crystals

import io.github.tunnelvisionmod.tunnelvision.data.forge.ForgeSlot

class PerfectGemWatcher {
	private val perfectGem = Regex("""\bPerfect (\w+) Gem""")
	private var known: Map<Int, String>? = null

	fun newPerfectGems(slots: List<ForgeSlot>): List<CrystalType> {
		val current = slots.associate { it.slot to it.item }
		val previous = known
		known = current
		if (previous == null) return emptyList()
		return current.filter { (slot, item) -> previous[slot] != item }
			.values
			.mapNotNull { item -> perfectGem.find(item)?.groupValues?.get(1)?.let { CrystalType.byDisplayName(it) } }
	}

	fun reset() {
		known = null
	}
}
