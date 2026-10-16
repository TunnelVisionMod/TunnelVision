package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType

/** A running result: how many corpses were opened and what they paid in total. */
data class CorpseTotal(val corpses: Int, val coins: Double) {
	companion object {
		val NONE = CorpseTotal(corpses = 0, coins = 0.0)
	}
}

/**
 * Profit and loss per corpse type, and over everything.
 *
 * Kept apart from storage so the arithmetic can be tested without a config directory: the feature
 * loads it from [io.github.tunnelvisionmod.tunnelvision.utils.Storage] on first use and writes it
 * back after every corpse.
 *
 * Totals are stored against [CorpseType.name] rather than its ordinal, so reordering the enum - or
 * Hypixel adding a fifth corpse - leaves existing numbers on the type that earned them.
 */
class CorpseTotals {
	private val coins = mutableMapOf<CorpseType, Double>()
	private val corpses = mutableMapOf<CorpseType, Int>()

	/** Every type opened at least once, in enum order. A type never opened is left out entirely. */
	val perType: List<Pair<CorpseType, CorpseTotal>>
		get() = CorpseType.entries.map { it to total(it) }.filter { (_, total) -> total.corpses > 0 }

	val overall: CorpseTotal get() = CorpseTotal(corpses.values.sum(), coins.values.sum())

	fun total(type: CorpseType): CorpseTotal =
		CorpseTotal(corpses.getOrDefault(type, 0), coins.getOrDefault(type, 0.0))

	fun add(type: CorpseType, net: Double) {
		coins[type] = coins.getOrDefault(type, 0.0) + net
		corpses[type] = corpses.getOrDefault(type, 0) + 1
	}

	fun reset() {
		coins.clear()
		corpses.clear()
	}

	/** Reads stored totals, ignoring names that no longer match a corpse type. */
	fun load(storedCoins: Map<String, Double>, storedCorpses: Map<String, Int>) {
		reset()
		for ((name, value) in storedCoins) byName(name)?.let { coins[it] = value }
		for ((name, value) in storedCorpses) byName(name)?.let { corpses[it] = value }
	}

	fun saveInto(storedCoins: MutableMap<String, Double>, storedCorpses: MutableMap<String, Int>) {
		storedCoins.clear()
		storedCorpses.clear()
		for ((type, value) in coins) storedCoins[type.name] = value
		for ((type, value) in corpses) storedCorpses[type.name] = value
	}

	private fun byName(name: String): CorpseType? = CorpseType.entries.firstOrNull { it.name == name }
}
