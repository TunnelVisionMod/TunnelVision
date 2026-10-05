package io.github.tunnelvisionmod.tunnelvision.features.mining.effects

data class TabEffect(val effect: MiningEffect, val seconds: Int, val precisionSeconds: Int)

object EffectParser {
	private val effectLine = Regex("""^(.+?):? ((?:\d+[dhms] ?)+)$""")
	private val durationPart = Regex("""(\d+)([dhms])""")
	private val unitSeconds = mapOf('d' to 86_400, 'h' to 3_600, 'm' to 60, 's' to 1)
	private val effectsMenuTitle = Regex("""^(?:\(\d+/\d+\) )?Active Effects$""")
	private val remainingLore = Regex("""^Remaining: ((?:\d+:)*\d+)$""")
	private val potionLore = Regex("""^(.+) \(((?:\d+:)*\d+)\)$""")

	fun parseTab(lines: List<String>): List<TabEffect> = lines.mapNotNull { line ->
		val (name, time) = effectLine.matchEntire(line.trim())?.destructured ?: return@mapNotNull null
		val effect = MiningEffect.entries.firstOrNull { it.tabName == name } ?: return@mapNotNull null
		val parts = durationPart.findAll(time).map { it.groupValues[1].toInt() to unitSeconds.getValue(it.groupValues[2][0]) }.toList()
		TabEffect(effect, parts.sumOf { (amount, unit) -> amount * unit }, parts.minOf { it.second })
	}

	fun isEffectsMenu(title: String): Boolean = effectsMenuTitle.matches(title)

	fun parseMenuItem(name: String, lore: List<String>): TabEffect? {
		val effect = MiningEffect.entries.firstOrNull { it.tabName == name } ?: return null
		val time = lore.firstNotNullOfOrNull { remainingLore.matchEntire(it)?.groupValues?.get(1) } ?: return null
		return TabEffect(effect, parseClock(time), 1)
	}

	fun parsePotionLore(lore: List<String>): Pair<MiningEffect, Int>? = lore.firstNotNullOfOrNull { line ->
		val (name, time) = potionLore.matchEntire(line)?.destructured ?: return@firstNotNullOfOrNull null
		val effect = MiningEffect.entries.firstOrNull { it.isPotion && it.tabName == name } ?: return@firstNotNullOfOrNull null
		effect to parseClock(time)
	}

	private fun parseClock(time: String): Int = time.split(":").fold(0) { total, part -> total * 60 + part.toInt() }

	fun parseGainedMessage(message: String): MiningEffect? =
		MiningEffect.entries.firstOrNull { effect -> effect.gainedMessage?.let { message.startsWith(it) } == true }
}
