package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType

sealed interface Todo {
	data class Corpse(val type: CorpseType, val count: Int, val hasKey: Boolean) : Todo
	data object Fossil : Todo
	data class GrabCrystal(val crystal: CrystalType) : Todo
}

object MineshaftTodos {
	/**
	 * Everything still worth doing in this mineshaft.
	 *
	 * A player who was warped in can neither mine the fossil nor grab the crystal - both are
	 * one per mineshaft and go to whoever opened it - so [warpedIn] leaves those two out and the
	 * guest is done once the corpses are looted.
	 */
	fun list(
		toLoot: Map<CorpseType, Int>,
		carriedItems: Set<String>,
		fossilPending: Boolean,
		crystalToGrab: CrystalType?,
		warpedIn: Boolean,
	): List<Todo> {
		val todos = mutableListOf<Todo>()
		todos += toLoot.entries.sortedBy { it.key.ordinal }.map { (type, count) ->
			Todo.Corpse(type, count, hasKey = type.keyName == null || type.keyName in carriedItems)
		}
		if (warpedIn) return todos
		if (fossilPending) todos += Todo.Fossil
		crystalToGrab?.let { todos += Todo.GrabCrystal(it) }
		return todos
	}

	fun isDone(todos: List<Todo>): Boolean = todos.all { it is Todo.Corpse && !it.hasKey }

	fun crystalToGrab(crystalShaft: CrystalType?, crystalsKnown: Boolean, carried: Set<CrystalType>): CrystalType? =
		crystalShaft?.takeIf { crystalsKnown && it !in carried }
}
