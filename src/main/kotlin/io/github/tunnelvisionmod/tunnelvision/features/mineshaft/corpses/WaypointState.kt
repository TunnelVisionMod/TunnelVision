package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType
import io.github.tunnelvisionmod.tunnelvision.utils.render.Pos

data class CorpseWaypoint(val type: CorpseType, val pos: Pos)

class WaypointState {
	private companion object {
		const val CORPSE_AT_SPOT = 3.0
		const val SAME_CORPSE = 2.0
		const val LOOT_RANGE = 6.0
	}

	private class LootedCorpse(val type: CorpseType, val pos: Pos, val radius: Double)

	private val spots = mutableListOf<Pos>()
	private val corpseList = mutableListOf<CorpseWaypoint>()
	private val looted = mutableListOf<LootedCorpse>()

	val possibleSpots: List<Pos> get() = spots
	val corpses: List<CorpseWaypoint> get() = corpseList

	fun start(corpseSpots: List<Pos>) {
		reset()
		spots += corpseSpots
	}

	fun onSpotSeen(spot: Pos, corpse: CorpseWaypoint?) {
		spots.remove(spot)
		corpse?.let { onCorpseSeen(it) }
	}

	fun onCorpseSeen(corpse: CorpseWaypoint) {
		spots.removeIf { it.distanceTo(corpse.pos) <= CORPSE_AT_SPOT }
		if (looted.any { it.type == corpse.type && it.pos.distanceTo(corpse.pos) <= it.radius }) return
		if (corpseList.none { it.type == corpse.type && it.pos.distanceTo(corpse.pos) <= SAME_CORPSE }) corpseList += corpse
	}

	fun onLooted(type: CorpseType, player: Pos) {
		val corpse = corpseList.filter { it.type == type && it.pos.distanceTo(player) <= LOOT_RANGE }.minByOrNull { it.pos.distanceTo(player) }
		if (corpse == null) {
			looted += LootedCorpse(type, player, LOOT_RANGE)
			return
		}
		corpseList.remove(corpse)
		looted += LootedCorpse(type, corpse.pos, SAME_CORPSE)
	}

	fun onCorpseTotal(total: Int?) {
		if (total != null && spots.isNotEmpty() && corpseList.size + looted.size >= total) spots.clear()
	}

	fun reset() {
		spots.clear()
		corpseList.clear()
		looted.clear()
	}
}
