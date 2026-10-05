package io.github.tunnelvisionmod.tunnelvision.utils

import java.util.Locale
import kotlin.math.abs

private val formattingCode = Regex("§.")

fun String.removeFormatting(): String = replace(formattingCode, "")

/**
 * Coins at a glance: `1.2B`, `11.9M`, `352k`, `940`. Keeps the sign, so deltas read correctly.
 *
 * Formatted against [Locale.ROOT] on purpose - the default locale would put a comma in the decimal
 * on a German client, so the same number would read differently for different players.
 */
fun formatCoins(coins: Double): String = when (val magnitude = abs(coins)) {
	in 1_000_000_000.0..Double.MAX_VALUE -> format("%.1fB", coins / 1_000_000_000)
	in 1_000_000.0..Double.MAX_VALUE -> format("%.1fM", coins / 1_000_000)
	in 1_000.0..Double.MAX_VALUE -> format("%.0fk", coins / 1_000)
	else -> format("%.0f", if (magnitude < 0.5) 0.0 else coins)
}

private fun format(pattern: String, value: Double): String = String.format(Locale.ROOT, pattern, value)

/** A price with thousands separators: `62,988`. Locale-pinned, like [formatCoins]. */
fun formatPrice(coins: Double): String = String.format(Locale.ROOT, "%,.0f", coins)

fun formatDuration(millis: Long): String {
	val totalSeconds = millis / 1000
	val hours = totalSeconds / 3600
	val minutes = totalSeconds % 3600 / 60
	val seconds = totalSeconds % 60
	return when {
		hours > 0 -> "${hours}h ${minutes}m"
		minutes > 0 -> "${minutes}m ${seconds}s"
		else -> "${seconds}s"
	}
}
