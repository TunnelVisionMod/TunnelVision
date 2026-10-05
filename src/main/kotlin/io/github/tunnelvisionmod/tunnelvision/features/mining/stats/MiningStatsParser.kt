package io.github.tunnelvisionmod.tunnelvision.features.mining.stats

enum class SkyMallBuff(private val key: String, val displayName: String) {
	MINING_SPEED("Mining Speed", "+100 ⸕ Speed"),
	MINING_FORTUNE("Mining Fortune", "+50 ☘ Fortune"),
	EXTRA_POWDER("more Powder", "+15% Powder"),
	ABILITY_COOLDOWN("Pickaxe Ability cooldowns", "-20% Cooldown"),
	GOBLIN_CHANCE("Golden and", "10x Goblins"),
	TITANIUM("Titanium drops", "5x Titanium");

	companion object {
		fun fromText(text: String): SkyMallBuff? = entries.firstOrNull { it.key in text }
	}
}

enum class MayhemBuff(private val key: String, val displayName: String) {
	SCRAP_CHANCE("Suspicious Scrap", "Scrap Chance"),
	MINING_FORTUNE("Mining Fortune buff", "☘ Fortune"),
	MINING_SPEED("Mining Speed buff", "⸕ Speed"),
	COLD_RESISTANCE("Cold Resistance buff", "❄ Cold Res"),
	ABILITY_COOLDOWN("Pickaxe Ability cooldown", "Cooldown");

	companion object {
		fun fromText(text: String): MayhemBuff? = entries.firstOrNull { it.key in text }
	}
}

sealed interface ActiveMiningEvent {
	data class FortunateFreezing(val fortuneBonus: Int?) : ActiveMiningEvent
	data class BetterTogether(val nearbyPlayers: Int?) : ActiveMiningEvent {
		val speedBonus: Int? get() = nearbyPlayers?.times(SPEED_PER_PLAYER)
		val fortuneBonus: Int? get() = nearbyPlayers?.times(FORTUNE_PER_PLAYER)

		private companion object {
			const val SPEED_PER_PLAYER = 250
			const val FORTUNE_PER_PLAYER = 20
		}
	}
}

object SkyBlockTime {
	const val EPOCH_MS = 1_560_275_700_000L
	const val DAY_MS = 20 * 60 * 1000L

	fun day(nowMs: Long): Long = Math.floorDiv(nowMs - EPOCH_MS, DAY_MS)
}

object MiningStatsParser {
	private const val SKY_MALL_CHAT_PREFIX = "New buff: "
	private const val SKY_MALL_ITEM_HEADER = "Your Current Effect"
	private const val MAYHEM_PREFIX = "MAYHEM! "
	private const val FORTUNATE_FREEZING = "Event: FORTUNATE FREEZING"
	private const val BETTER_TOGETHER = "Event: BETTER TOGETHER"
	private val eventBonus = Regex("""^Event Bonus: \+(\d+)""")
	private val nearbyPlayers = Regex("""^Nearby Players: (\d+|N/A)""")
	private val coldResistance = Regex("""^Cold Resistance: \D*?([\d,.]+)""")

	fun parseSkyMallChat(message: String): SkyMallBuff? {
		if (!message.startsWith(SKY_MALL_CHAT_PREFIX)) return null
		return SkyMallBuff.fromText(message.removePrefix(SKY_MALL_CHAT_PREFIX))
	}

	fun parseSkyMallItem(lore: List<String>): SkyMallBuff? {
		val header = lore.indexOf(SKY_MALL_ITEM_HEADER)
		if (header < 0) return null
		val effect = lore.drop(header + 1).firstOrNull { it.isNotBlank() } ?: return null
		return SkyMallBuff.fromText(effect.removePrefix("■").trim())
	}

	fun parseMayhem(message: String): MayhemBuff? {
		if (!message.startsWith(MAYHEM_PREFIX)) return null
		return MayhemBuff.fromText(message)
	}

	fun parseMiningEvent(sidebar: List<String>): ActiveMiningEvent? {
		fun hasEvent(name: String) = sidebar.any { it.equals(name, ignoreCase = true) }
		return when {
			hasEvent(FORTUNATE_FREEZING) ->
				ActiveMiningEvent.FortunateFreezing(sidebar.firstNotNullOfOrNull { eventBonus.find(it)?.groupValues?.get(1)?.toInt() })
			hasEvent(BETTER_TOGETHER) ->
				ActiveMiningEvent.BetterTogether(sidebar.firstNotNullOfOrNull { nearbyPlayers.find(it)?.groupValues?.get(1) }?.toIntOrNull())
			else -> null
		}
	}

	fun parseColdResistance(tabLines: List<String>): Double? =
		tabLines.firstNotNullOfOrNull { coldResistance.find(it.trim())?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() }
}
