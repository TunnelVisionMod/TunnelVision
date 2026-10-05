package io.github.tunnelvisionmod.tunnelvision.core.config

enum class LootMode(private val label: String) {
	LAPIS_ONLY("Lapis Only"),
	NORMAL("Normal"),
	GREEDY("Greedy");

	override fun toString() = label
}
