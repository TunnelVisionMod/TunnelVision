package io.github.tunnelvisionmod.tunnelvision.core.config

enum class CrystalHuntingSpeed(private val label: String, val share: Double) {
	SLOW("Slow", 0.0),
	NORMAL("Normal", 0.25),
	FAST("Fast", 0.5);

	override fun toString() = label
}
