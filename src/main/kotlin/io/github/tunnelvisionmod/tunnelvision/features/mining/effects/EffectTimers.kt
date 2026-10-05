package io.github.tunnelvisionmod.tunnelvision.features.mining.effects

import kotlin.math.abs

class EffectTimers {
	private companion object {
		const val TAB_LAG_TOLERANCE_MS = 5_000L
		const val MIN_TAB_START_SECONDS = 10
	}

	private val expiries = mutableMapOf<MiningEffect, Long?>()

	fun start(effect: MiningEffect, now: Long, durationSeconds: Int?) {
		expiries[effect] = durationSeconds?.let { now + it * 1000L }
	}

	fun sync(tab: TabEffect, now: Long): Boolean {
		if (tab.effect !in expiries && tab.seconds < MIN_TAB_START_SECONDS) return false
		val tabExpiry = now + tab.seconds * 1000L
		val current = expiries[tab.effect]
		val tolerance = tab.precisionSeconds * 1000L + TAB_LAG_TOLERANCE_MS
		if (current != null && abs(tabExpiry - current) <= tolerance) return false
		expiries[tab.effect] = tabExpiry
		return true
	}

	fun expire(now: Long): List<MiningEffect> {
		val expired = expiries.filterValues { it != null && it <= now }.keys.toList()
		expired.forEach { expiries.remove(it) }
		return expired
	}

	fun remaining(now: Long): Map<MiningEffect, Long?> =
		expiries.filterValues { it == null || it > now }.mapValues { (_, expiry) -> expiry?.minus(now) }

	fun clear() {
		expiries.clear()
	}
}
