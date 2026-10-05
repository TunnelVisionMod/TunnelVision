package io.github.tunnelvisionmod.tunnelvision.features.mining.effects

enum class MiningEffect(
	val tabName: String,
	val displayName: String,
	private val defaultDurationSeconds: Int,
	val isPotion: Boolean,
	val gainedMessage: String? = null,
	val itemName: String? = null,
) {
	COLD_RESISTANCE(
		tabName = "Cold Resistance IV",
		displayName = "Cold Resistance IV",
		defaultDurationSeconds = 60 * 60,
		isPotion = true,
		gainedMessage = "BUFF! You have gained Cold Resistance IV!",
	),
	FILET_O_FORTUNE(
		tabName = "Filet O' Fortune I",
		displayName = "Filet O' Fortune",
		defaultDurationSeconds = 60 * 60,
		isPotion = false,
		itemName = "Filet O' Fortune",
	);

	fun durationSeconds(potionDurationSeconds: Int?, affinityBonusPercent: Int): Int {
		if (!isPotion) return defaultDurationSeconds
		return (potionDurationSeconds ?: defaultDurationSeconds) * (100 + affinityBonusPercent) / 100
	}
}
