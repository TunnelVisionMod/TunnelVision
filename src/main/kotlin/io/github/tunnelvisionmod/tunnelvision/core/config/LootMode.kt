package io.github.tunnelvisionmod.tunnelvision.core.config

enum class LootMode(private val label: String) {
	LAPIS_ONLY("Lapis Only"),
	NORMAL("Adaptive"),
	GREEDY("Crystal Hunter");

	override fun toString() = label
}
