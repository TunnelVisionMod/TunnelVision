package io.github.tunnelvisionmod.tunnelvision.utils

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import net.minecraft.world.scores.DisplaySlot

/**
 * The scoreboard sidebar, as plain text. Hypixel puts the whole line into the team prefix and
 * suffix of an otherwise meaningless score holder, so the holder name itself is ignored.
 *
 * Lines are in no particular order; nothing that reads them should rely on it.
 */
object Sidebar {
	var lines: List<String> = emptyList()
		private set

	fun register() {
		EventBus.on<ClientTickEvent> { update() }
	}

	private fun update() {
		val scoreboard = mc.level?.scoreboard
		val objective = scoreboard?.getDisplayObjective(DisplaySlot.SIDEBAR)
		if (!SkyBlock.isOnSkyBlock || scoreboard == null || objective == null) {
			lines = emptyList()
			return
		}
		lines = scoreboard.trackedPlayers.mapNotNull { holder ->
			if (!scoreboard.listPlayerScores(holder).containsKey(objective)) return@mapNotNull null
			val team = scoreboard.getPlayersTeam(holder.scoreboardName) ?: return@mapNotNull null
			(team.playerPrefix.string + team.playerSuffix.string).removeFormatting().trim().takeIf { it.isNotEmpty() }
		}
	}
}
