package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

/**
 * One line of a corpse loot message: `✦ Fine Peridot Gemstone ×12` reads as 12 of that gem.
 *
 * [color] is the packed colour the server printed the name in, which is its rarity. We take it from
 * the message rather than keeping a rarity table, so a new drop is coloured right without us
 * knowing anything about it.
 */
data class LootedItem(
	val name: String,
	val amount: Int,
	val color: Int? = null,
	val symbol: String? = null,
	val symbolColor: Int? = null,
)

/**
 * Reads the loot block Hypixel prints when you open a Frozen Corpse:
 *
 * ```
 * ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
 *  TUNGSTEN CORPSE LOOT!
 * REWARDS
 *   ✦ Fine Peridot Gemstone ×12
 *   Suspicious Scrap ×4
 *   Glacite Powder ×15,759
 * ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
 * ```
 *
 * The header is the only wording we rely on. Item lines are read loosely - every leading symbol is
 * decoration, a trailing `(+15% Magic Find)` is noise, and the count may lead (`12x Gem`) or trail
 * (`Gem ×12`) - because an item we fail to read is worse than one we show without a price, and the
 * decoration has changed before.
 *
 * Reading loosely is safe because the caller only asks about lines inside a block whose header it
 * has already seen, so unrelated chat is never offered here.
 */
object CorpseLootParser {
	/** Longer than any real item name, so a sentence that slips into a block is not read as a drop. */
	private const val MAX_NAME = 60

	private val header = Regex("""^(LAPIS|UMBER|TUNGSTEN|VANGUARD) CORPSE LOOT!""")
	private val rule = Regex("""^[\s▬─━—=_]+$""")

	/** `REWARDS` and friends: a shouted label, never an item, since item names are Title Case. */
	private val sectionLabel = Regex("""^[A-Z][A-Z!' ]*$""")

	/**
	 * The bonus roll note, not a thing that dropped. Written as a bare `+1`, with the wordier
	 * `+1 Bonus Drop` accepted too so the line is ignored either way.
	 *
	 * Catching this matters beyond tidiness: a line that reads as neither frame nor drop ends the
	 * block, so missing it would cut off every drop printed after it.
	 */
	private val bonusDrop = Regex("""^(?:\+\s*\d+|\+?\s*\d+\s+bonus\s+drops?)$""", RegexOption.IGNORE_CASE)

	private val dropPrefix = Regex("""^[A-Z! ]*DROP!\s*""")
	private val trailingNote = Regex("""\s*\([^)]*\)\s*$""")

	/** Gem symbols and bullets lead a name and are not part of it. */
	private val leadingSymbols = Regex("""^[^\p{L}\p{N}]+""")

	// Hypixel writes the count with a multiplication sign; we also accept a plain x.
	private val countFirst = Regex("""^(\d[\d,]*)\s*[x×]\s*(.+)$""", RegexOption.IGNORE_CASE)
	private val countLast = Regex("""^(.+?)\s*[x×]\s*(\d[\d,]*)$""", RegexOption.IGNORE_CASE)

	/** The corpse a loot block belongs to, or null when the line is not a loot header. */
	fun corpseType(line: String): CorpseType? {
		val code = header.find(line.trim())?.groupValues?.get(1) ?: return null
		return CorpseType.entries.firstOrNull { it.name == code }
	}

	/**
	 * True for a line that belongs to the block but holds no drop: the rules framing it, the blank
	 * lines inside it, the `REWARDS` label and the `+1 Bonus Drop` note. The caller keeps the block
	 * open on these.
	 */
	fun isFrame(line: String): Boolean {
		// The shout marks are decoration: the line reads "+1 bonus drop!", not "+1 bonus drop".
		val trimmed = line.trim().trimEnd('!', '.').trim()
		return trimmed.isEmpty() ||
			rule.matches(trimmed) ||
			sectionLabel.matches(trimmed) ||
			bonusDrop.matches(trimmed)
	}

	/** True for one of the rules drawn above and below the block, which we reuse as our own. */
	fun isRule(line: String): Boolean = rule.matches(line.trim())

	/** One drop off an item line, or null when the line does not read like one. */
	fun item(line: String): LootedItem? {
		val cleaned = line.replace(dropPrefix, "").replace(trailingNote, "").trim().trimEnd('!').trim()
		if (cleaned.isEmpty() || cleaned.length > MAX_NAME) return null
		countFirst.matchEntire(cleaned)?.let { return looted(it.groupValues[2], it.groupValues[1]) }
		countLast.matchEntire(cleaned)?.let { return looted(it.groupValues[1], it.groupValues[2]) }
		return looted(cleaned, "1")
	}

	private fun looted(name: String, amount: String): LootedItem? {
		val symbol = leadingSymbols.find(name)?.value?.trim()?.takeIf { it.isNotEmpty() }
		val cleaned = name.replace(leadingSymbols, "").trim()
		if (!isItemName(cleaned)) return null
		val count = amount.replace(",", "").toIntOrNull()?.takeIf { it > 0 } ?: return null
		return LootedItem(cleaned, count, symbol = symbol)
	}

	/**
	 * A name has to read like an item: letters, no colon, not a shouted label and not the bonus
	 * drop note. The colon is what rules out a player's own message and the tab-list lines, both of
	 * which can otherwise look like a bare item name.
	 *
	 * The frame lines are rejected here as well as by [isFrame], so a caller that reaches for a
	 * drop without checking the frame first still cannot invent one.
	 */
	private fun isItemName(name: String): Boolean =
		name.isNotEmpty() &&
			name.length <= MAX_NAME &&
			name.any { it.isLetter() } &&
			':' !in name &&
			!sectionLabel.matches(name) &&
			!bonusDrop.matches(name)
}
