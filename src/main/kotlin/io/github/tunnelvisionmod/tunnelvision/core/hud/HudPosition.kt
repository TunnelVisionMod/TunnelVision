package io.github.tunnelvisionmod.tunnelvision.core.hud

import com.google.gson.annotations.Expose

data class HudPosition(
	@field:Expose var x: Float = 0f,
	@field:Expose var y: Float = 0f,
	@field:Expose var scale: Float = 1f,
)

class HudData {
	@field:Expose
	var positions: MutableMap<String, HudPosition> = mutableMapOf()
}
