package io.github.tunnelvisionmod.tunnelvision.features.mining.effects

enum class PotionAffinity(private val label: String, val bonusPercent: Int) {
	NONE("None", 0),
	TALISMAN("Talisman (+10%)", 10),
	RING("Ring (+25%)", 25),
	ARTIFACT("Artifact (+50%)", 50);

	override fun toString() = label
}
