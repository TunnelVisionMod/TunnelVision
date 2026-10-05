package io.github.tunnelvisionmod.tunnelvision.features.mineshaft

import io.github.tunnelvisionmod.tunnelvision.config.LootMode

enum class LootRule(private val types: Set<CorpseType>) {
	ALL(CorpseType.entries.toSet()),
	ALL_BUT_VANGUARD(CorpseType.entries.toSet() - CorpseType.VANGUARD),
	LAPIS(setOf(CorpseType.LAPIS)),
	LAPIS_AND_VANGUARD(setOf(CorpseType.LAPIS, CorpseType.VANGUARD));

	fun includes(type: CorpseType) = type in types
}

object CorpseLoot {
	private const val TAB_HEADER = "Frozen Corpses:"
	private val corpseLine = Regex("""^(\w+): (NOT )?LOOTED$""")

	/**
	 * [openVanguards] is false for a player who does not buy Skeleton Keys. A Vanguard corpse is then
	 * never worth looting, whatever the mode says, so it drops out of every rule - which also makes a
	 * Fairy mineshaft worthless, since Vanguards are the only corpses it holds.
	 */
	fun rule(mode: LootMode, crystalsFull: Boolean, shouldMine: Boolean?, openVanguards: Boolean): LootRule = when {
		mode == LootMode.LAPIS_ONLY -> LootRule.LAPIS
		mode == LootMode.GREEDY && !crystalsFull -> everything(openVanguards)
		shouldMine == true -> everything(openVanguards)
		openVanguards -> LootRule.LAPIS_AND_VANGUARD
		else -> LootRule.LAPIS
	}

	private fun everything(openVanguards: Boolean) = if (openVanguards) LootRule.ALL else LootRule.ALL_BUT_VANGUARD

	fun parseUnlooted(tabLines: List<String>): Map<CorpseType, Int>? {
		val header = tabLines.indexOf(TAB_HEADER)
		if (header < 0) return null
		return tabLines.asSequence()
			.drop(header + 1)
			.map { corpseLine.matchEntire(it) }
			.takeWhile { it != null }
			.filter { it!!.groupValues[2].isNotEmpty() }
			.mapNotNull { CorpseType.fromTabName(it!!.groupValues[1]) }
			.groupingBy { it }
			.eachCount()
	}

	fun toLoot(unlooted: Map<CorpseType, Int>, rule: LootRule): Map<CorpseType, Int> = unlooted.filterKeys { rule.includes(it) }
}
