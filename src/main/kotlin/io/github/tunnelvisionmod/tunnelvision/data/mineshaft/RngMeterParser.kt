package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

/**
 * How far the meter has come, and what it is working towards when that is written down.
 *
 * [needed] is null on the hub menu, which lists stored XP per category without naming a target.
 */
data class MeterReading(val progress: Double, val needed: Double? = null)

/**
 * Reads the Frozen Corpse RNG meter out of the RNG Meter menu.
 *
 * The menu is the only place the real number is written down. Counting corpses ourselves drifts -
 * loot taken with the feature off, or on another account, is never seen - and one missed payout
 * misprices a corpse by a whole Shattered Pendant, so the menu is treated as the authority whenever
 * it is open.
 *
 * The line reads `Frozen Corpse XP: 1,250,000/2,500,000`, and the numbers may be abbreviated
 * (`1.2M/2.5M`), so both forms are accepted.
 */
object RngMeterParser {
	/** Every RNG meter menu ends with this, whatever it is a meter for. */
	private const val MENU_TITLE = "RNG Meter"

	/** The drop we always set the meter to, and so the row whose target we want. */
	const val PENDANT_NAME = "Shattered Pendant"

	/** Inside the Frozen Corpse menu, every drop is listed as `Frozen Corpse XP: stored/target`. */
	private val corpseScore = Regex("""Frozen Corpse XP:\s*([\d,.]+[kMB]?)\s*/\s*([\d,.]+[kMB]?)""", RegexOption.IGNORE_CASE)

	/** On the hub that `/rngmeter` opens, each category only shows `Stored Frozen Corpse XP: n`. */
	private val corpseStored = Regex("""Stored\s+Frozen\s+Corpse\s+XP:\s*([\d,.]+[kMB]?)""", RegexOption.IGNORE_CASE)
	private val amount = Regex("""^([\d,.]+)([kMB]?)$""", RegexOption.IGNORE_CASE)

	fun isMeterMenu(title: String): Boolean = title.trim().endsWith(MENU_TITLE, ignoreCase = true)

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
	 * of the Pendant's 2,500,000. The stored XP on the left is the same on every row, so only the
	 * target is at stake, and the Pendant's row is the one that answers for it.
	 */
	fun parseMenu(items: List<Pair<String, List<String>>>): MeterReading? {
		val pendant = items.firstOrNull { it.first.contains(PENDANT_NAME, ignoreCase = true) }
		pendant?.let { row -> parseLore(row.second)?.let { return it } }
		// A row with a target beats the hub's bare total, which cannot confirm what we are aiming at.
		val readings = items.mapNotNull { parseLore(it.second) }
		return readings.firstOrNull { it.needed != null } ?: readings.firstOrNull()
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
