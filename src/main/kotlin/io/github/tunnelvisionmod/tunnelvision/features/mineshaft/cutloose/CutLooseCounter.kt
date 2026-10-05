package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.cutloose

class CutLooseCounter {
	companion object {
		const val MAX_KILLS = 10
	}

	var kills = 0
		private set

	private val hitMobs = mutableSetOf<Int>()

	fun onHit(entityId: Int) {
		hitMobs += entityId
	}

	fun onDeath(entityId: Int): Boolean {
		if (!hitMobs.remove(entityId) || kills >= MAX_KILLS) return false
		kills++
		return true
	}

	fun reset() {
		kills = 0
		hitMobs.clear()
	}
}
