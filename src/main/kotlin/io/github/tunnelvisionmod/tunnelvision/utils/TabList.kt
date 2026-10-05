package io.github.tunnelvisionmod.tunnelvision.utils

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.mixin.PlayerTabOverlayAccessor

object TabList {
	var lines: List<String> = emptyList()
		private set
	var footerLines: List<String> = emptyList()
		private set

	fun register() {
		EventBus.on<ClientTickEvent> { update() }
	}

	private fun update() {
		if (!SkyBlock.isOnSkyBlock || mc.player == null) {
			lines = emptyList()
			footerLines = emptyList()
			return
		}
		val overlay = Compat.tabList as PlayerTabOverlayAccessor
		lines = overlay.`tunnelvision$getPlayerInfos`().mapNotNull { info ->
			info.tabListDisplayName?.string?.removeFormatting()?.trim()?.takeIf { it.isNotEmpty() }
		}
		footerLines = overlay.`tunnelvision$getFooter`()?.string?.removeFormatting()
			?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }
			?: emptyList()
	}
}
