package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

/**
 * How far the meter has come, and what it is working towards when that is written down.
 *
 * [needed] is null on the hub menu, which lists stored XP per category without naming a target.
 * [locketSelected] is null when the menu does not say which drop the meter is set to.
 */
data class MeterReading(val progress: Double, val needed: Double? = null, val locketSelected: Boolean? = null)

/**
 * Reads the Frozen Corpse RNG meter out of the RNG Meter menu.
 *
 * The menu is the only place the real number is written down. Counting corpses ourselves drifts -
 * loot taken with the feature off, or on another account, is never seen - and one missed payout
 * misprices a corpse by a whole Shattered Locket, so the menu is treated as the authority whenever
 * it is open.
 *
 * The line reads `Frozen Corpse XP: 1,250,000/2,500,000`, and the numbers may be abbreviated
 * (`1.2M/2.5M`), so both forms are accepted.
 */
object RngMeterParser {
	/** Every RNG meter menu ends with this, whatever it is a meter for. */
	private const val MENU_TITLE = "RNG Meter"

	/**
	 * The drop the meter is set to, and so the row whose target we want. Its id is SHATTERED_PENDANT
	 * but it is called a Locket in game, so either name finds the row.
	 */
	const val LOCKET_NAME = "Shattered Locket"
	private val locketNames = listOf(LOCKET_NAME, "Shattered Pendant")

	/** Inside the Frozen Corpse menu, every drop is listed as `Frozen Corpse XP: stored/target`. */
	private val corpseScore = Regex("""Frozen Corpse XP:\s*([\d,.]+[kMB]?)\s*/\s*([\d,.]+[kMB]?)""", RegexOption.IGNORE_CASE)

	/** On the hub that `/rngmeter` opens, each category only shows `Stored Frozen Corpse XP: n`. */
	private val corpseStored = Regex("""Stored\s+Frozen\s+Corpse\s+XP:\s*([\d,.]+[kMB]?)""", RegexOption.IGNORE_CASE)
	private val amount = Regex("""^([\d,.]+)([kMB]?)$""", RegexOption.IGNORE_CASE)

	/** The hub writes `Selected Drop` on one line and the drop's name on the next. */
	private val selectedDrop = Regex("""^Selected Drop:?\s*(.*)$""", RegexOption.IGNORE_CASE)
	private val noDrop = Regex("""don.t have an RNG drop selected""", RegexOption.IGNORE_CASE)

	/** With a drop selected the hub shows a progress bar ending in `1,760,450/2.5M`. */
	private val progressBar = Regex("""([\d,.]+[kMB]?)\s*/\s*([\d,.]+[kMB]?)\s*$""", RegexOption.IGNORE_CASE)

	/** Sent when a drop is selected: `You set your Frozen Corpses RNG Meter to drop Shattered Locket!` */
	private val selectedInChat = Regex("""^You set your Frozen Corpses? RNG Meter to drop (.+?)!?$""", RegexOption.IGNORE_CASE)

	/** Sent when it is cleared: `You reset your selected drop for your Frozen Corpses RNG Meter!` */
	private val resetInChat = Regex("""^You reset your selected drop for your Frozen Corpses? RNG Meter!?$""", RegexOption.IGNORE_CASE)

	fun isMeterMenu(title: String): Boolean = title.trim().endsWith(MENU_TITLE, ignoreCase = true)

	fun isLocket(name: String): Boolean = locketNames.any { name.contains(it, ignoreCase = true) }

	/**
	 * The corpse meter off one lore line, or null when the line is about something else.
	 *
	 * Both wordings are accepted: the hub's stored total and a drop row's stored/target.
	 */
	fun parse(line: String): MeterReading? {
		corpseScore.find(line)?.let { match ->
			val progress = parseAmount(match.groupValues[1]) ?: return null
			val needed = parseAmount(match.groupValues[2])?.takeIf { it > 0 } ?: return null
			return MeterReading(progress, needed)
		}
		corpseStored.find(line)?.let { match ->
			return MeterReading(parseAmount(match.groupValues[1]) ?: return null)
		}
		return null
	}

	/** The first corpse meter line anywhere in one item's lore. */
	fun parseLore(lore: List<String>): MeterReading? = lore.firstNotNullOfOrNull { parse(it) }

	/**
	 * The meter read off a whole menu, as (item name, lore) per slot.
	 *
	 * The menu lists every drop you could select, each with its own requirement, so picking the
	 * first row that parses reads back some other drop's target - 12,000 XP for a cheap one instead
	 * of the Locket's 2,500,000. The stored XP on the left is the same on every row, so only the
	 * target is at stake, and the Locket's row is the one that answers for it.
	 */
	fun parseMenu(items: List<Pair<String, List<String>>>): MeterReading? {
		val locket = items.firstOrNull { (name, _) -> isLocket(name) }
		locket?.let { row -> parseLore(row.second)?.let { return it.copy(locketSelected = rowSelected(row.second)) } }
		// A row with a target beats the hub's bare total, which cannot confirm what we are aiming at.
		val readings = items.mapNotNull { (name, lore) ->
			val frozenCorpse = isFrozenCorpse(name, lore)
			val reading = parseLore(lore) ?: lore.takeIf { frozenCorpse }?.let { progressBarOf(it) } ?: return@mapNotNull null
			if (!frozenCorpse) return@mapNotNull reading
			val bar = progressBarOf(lore)
			reading.copy(needed = reading.needed ?: bar?.needed, locketSelected = hubSelected(lore))
		}
		return readings.firstOrNull { it.needed != null } ?: readings.firstOrNull()
	}

	/** The hub's Frozen Corpse category, the only one whose progress bar is ours. */
	fun isFrozenCorpse(name: String, lore: List<String>): Boolean =
		name.contains("Frozen Corpse", ignoreCase = true) || lore.any { it.contains("Stored Frozen Corpse XP", ignoreCase = true) }

	private fun progressBarOf(lore: List<String>): MeterReading? = lore.firstNotNullOfOrNull { line ->
		val match = progressBar.find(line) ?: return@firstNotNullOfOrNull null
		val progress = parseAmount(match.groupValues[1]) ?: return@firstNotNullOfOrNull null
		val needed = parseAmount(match.groupValues[2])?.takeIf { it > 0 } ?: return@firstNotNullOfOrNull null
		MeterReading(progress, needed)
	}

	/** Whether the Locket was selected, off the message the server sends when a drop is chosen. */
	fun selectedFromChat(message: String): Boolean? {
		val text = message.trim()
		if (resetInChat.matches(text)) return false
		return selectedInChat.find(text)?.groupValues?.get(1)?.let { isLocket(it) }
	}

	/**
	 * Whether the Locket's own row says it is the selected drop. Read loosely, since the wording has
	 * not been seen first hand: a selected row offers to deselect, any other offers to select.
	 */
	fun rowSelected(lore: List<String>): Boolean? = when {
		lore.any { it.contains("deselect", ignoreCase = true) || it.trim().equals("SELECTED", ignoreCase = true) } -> true
		lore.any { it.contains("click to select", ignoreCase = true) } -> false
		else -> null
	}

	/** Whether a hub category names the Locket as its drop, false when it says none is selected. */
	fun hubSelected(lore: List<String>): Boolean? {
		// The tooltip wraps that sentence over two lines, so it is looked for across them.
		if (noDrop.containsMatchIn(lore.joinToString(" "))) return false
		val index = lore.indexOfFirst { selectedDrop.matches(it.trim()) }
		if (index < 0) return null
		val inline = selectedDrop.matchEntire(lore[index].trim())?.groupValues?.get(1)?.trim().orEmpty()
		val drop = inline.ifEmpty { lore.drop(index + 1).firstOrNull { it.isNotBlank() }?.trim() } ?: return null
		return isLocket(drop)
	}

	/** `1,250,000`, `1.2M` and `250k` all read as coins-style numbers. */
	fun parseAmount(text: String): Double? {
		val match = amount.matchEntire(text.trim()) ?: return null
		val digits = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
		val scale = when (match.groupValues[2].lowercase()) {
			"k" -> 1_000.0
			"m" -> 1_000_000.0
			"b" -> 1_000_000_000.0
			else -> 1.0
		}
		return digits * scale
	}
}
