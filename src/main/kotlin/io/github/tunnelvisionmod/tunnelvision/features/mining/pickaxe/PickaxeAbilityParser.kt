package io.github.tunnelvisionmod.tunnelvision.features.mining.pickaxe

data class TabAbility(val name: String, val secondsLeft: Int?)

object PickaxeAbilityParser {
	private const val TAB_HEADER = "Pickaxe Ability:"
	private val tabLine = Regex("""^(.+): (?:(?:(\d+)m )?(\d+)s|Available)$""")
	private val usedMessage = Regex("""^You used your (.+) Pickaxe Ability!$""")
	private val availableMessage = Regex("""^(.+) is now available!$""")
	private val loreCooldown = Regex("""^Cooldown: (\d+)s$""")
	private val miningToolTypes = listOf(" DRILL", " PICKAXE", " GAUNTLET")

	fun parseTab(lines: List<String>): TabAbility? {
		val header = lines.indexOf(TAB_HEADER)
		if (header < 0) return null
		val match = tabLine.matchEntire(lines.getOrNull(header + 1) ?: return null) ?: return null
		val (name, minutes, seconds) = match.destructured
		if (seconds.isEmpty()) return TabAbility(name, null)
		return TabAbility(name, (minutes.toIntOrNull() ?: 0) * 60 + seconds.toInt())
	}

	fun parseUsedMessage(message: String): String? = usedMessage.matchEntire(message)?.groupValues?.get(1)

	fun parseAvailableMessage(message: String): String? = availableMessage.matchEntire(message)?.groupValues?.get(1)

	fun parseLoreCooldown(lore: List<String>): Int? =
		lore.asReversed().firstNotNullOfOrNull { loreCooldown.matchEntire(it)?.groupValues?.get(1)?.toInt() }

	fun isMiningTool(lore: List<String>): Boolean {
		val type = lore.lastOrNull() ?: return false
		return miningToolTypes.any { type.contains(it) } && lore.any { it.startsWith("Mining Speed:") }
	}
}
