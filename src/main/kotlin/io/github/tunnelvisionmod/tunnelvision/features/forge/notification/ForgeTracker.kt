package io.github.tunnelvisionmod.tunnelvision.features.forge.notification

import io.github.tunnelvisionmod.tunnelvision.data.forge.ForgeSlot

class ForgeTracker {
	private val notified = mutableSetOf<Pair<Int, String>>()

	fun newlyReady(slots: List<ForgeSlot>): List<ForgeSlot> {
		val ready = slots.filter { it.isReady }
		notified.retainAll(ready.map { it.slot to it.item }.toSet())
		return ready.filter { notified.add(it.slot to it.item) }
	}

	fun reset() {
		notified.clear()
	}
}
