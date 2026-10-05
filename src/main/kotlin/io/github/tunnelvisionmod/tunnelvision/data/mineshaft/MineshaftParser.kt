package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

object MineshaftParser {
	private const val CORPSE_HEADER = "Frozen Corpses:"
	private val corpseLine = Regex("""^(\w+): (?:NOT )?LOOTED$""")

	/**
	 * Reads the mineshaft variant off the scoreboard. The server id is the last word of the date
	 * line at the bottom of the sidebar and carries the variant as a tag, e.g. `09/29/25 mTOPA_1x`.
	 */
	fun parseType(sidebarLines: List<String>): MineshaftType? = sidebarLines.firstNotNullOfOrNull { line ->
		val serverId = line.substringAfterLast(' ')
		MineshaftType.entries.firstOrNull { it.code in serverId }
	}

	/**
	 * The corpses listed in the `Frozen Corpses` tab list widget, counted per kind and kept in the
	 * order Hypixel lists them, or null when the widget is missing — the player can turn it off in
	 * Hypixel's settings, and it takes a moment to populate after entering a mineshaft.
	 *
	 * One line is one corpse, so two Lapis corpses show up as two `Lapis: NOT LOOTED` lines.
	 */
	fun parseCorpses(tabLines: List<String>): Map<String, Int>? {
		val header = tabLines.indexOf(CORPSE_HEADER)
		if (header < 0) return null
		return tabLines.asSequence()
			.drop(header + 1)
			.map { corpseLine.matchEntire(it) }
			.takeWhile { it != null }
			.map { it!!.groupValues[1] }
			.groupingBy { it }
			.eachCount()
	}

	/** Total corpses in the widget, or null while it is missing. */
	fun parseCorpseCount(tabLines: List<String>): Int? = parseCorpses(tabLines)?.values?.sum()
}
