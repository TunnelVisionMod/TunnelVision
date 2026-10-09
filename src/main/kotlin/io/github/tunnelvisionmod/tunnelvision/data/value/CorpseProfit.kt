package io.github.tunnelvisionmod.tunnelvision.data.value

import io.github.tunnelvisionmod.tunnelvision.core.config.BazaarPriceType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.LootedItem

/** What one drop off a corpse is worth. */
sealed interface LootPrice {
	/** Priced: [unit] coins each, [total] for the stack. */
	data class Coins(val unit: Double, val total: Double) : LootPrice

	/** We could not price it - an unknown item, or a market price we have not loaded yet. */
	data object Unknown : LootPrice
}

/**
 * One line of the loot message, with what it was worth. [drop] is null for an item we do not know.
 *
 * [fromMeter] marks the item the RNG meter paid out. It is priced like any other drop but left out
 * of the total, because the meter progress that bought it was already credited corpse by corpse.
 */
data class ProfitLine(val item: LootedItem, val drop: CorpseDropItem?, val price: LootPrice, val fromMeter: Boolean = false)

/**
 * What a corpse you just opened actually paid.
 *
 * [net] is null while the key price is missing, because a Vanguard corpse's loot is worth far less
 * than its Skeleton Key - calling that a profit would be wrong by tens of millions, so the corpse
 * goes unrecorded until the Bazaar answers. [unpriced] only ever understates the loot, so it is
 * reported alongside the total instead of withholding it.
 */
data class CorpseProfitBreakdown(
	val type: CorpseType,
	val lines: List<ProfitLine>,
	val lootCoins: Double,
	val meterCoins: Double,
	val keyCoins: Double?,
) {
	val net: Double? get() = keyCoins?.let { lootCoins + meterCoins - it }

	/** Drops we had no price for, which the message flags so a silent gap cannot look like a total. */
	val unpriced: Int get() = lines.count { it.price is LootPrice.Unknown }
}

/**
 * Prices the drops a corpse message listed, adds what its RNG meter was worth and takes off the key.
 *
 * This is the counterpart to [CorpseValue.lootValue]: that one averages the loot table to rate a
 * mineshaft before you open anything, while this one prices the drops you actually got. Both value
 * an item the same way, through [CorpseValue.itemValue], so the estimate and the result stay
 * comparable.
 *
 * The meter counts at full value because we only ever set it to a Shattered Locket: every corpse
 * moves it towards a guaranteed one, so the share it granted is coins earned whether or not this
 * corpse was the one that paid out.
 */
object CorpseProfit {
	/**
	 * [includeMeter] off leaves the meter out of [CorpseProfitBreakdown.net] as well as out of the
	 * message, so what is shown always adds up to what is claimed.
	 */
	/**
	 * [includeMeter] off leaves the meter out of [CorpseProfitBreakdown.net] as well as out of the
	 * message, so what is shown always adds up to what is claimed.
	 *
	 * [meterPayout] says this corpse is the one the meter paid out on. The payout item is then
	 * priced but not counted: every corpse along the way was already credited its slice of it, so
	 * counting the item as well would pay for it twice. The caller decides this, because only it
	 * knows how far along the meter was - a Shattered Pendant can also drop on its own, about as
	 * often as the meter fills, so the item alone says nothing about where it came from.
	 */
	fun of(
		type: CorpseType,
		items: List<LootedItem>,
		priceType: BazaarPriceType,
		includeMeter: Boolean = true,
		meterPayout: Boolean = false,
	): CorpseProfitBreakdown {
		// Powder is left out entirely rather than listed at zero: it only buys HotM upgrades, so a
		// line for it is noise in a message about coins.
		val lines = items.filterNot { CorpseDropNames.isWorthless(it.name) }.map { line(it, priceType, meterPayout) }
		return CorpseProfitBreakdown(
			type = type,
			lines = lines,
			lootCoins = lines.filterNot { it.fromMeter }.sumOf { (it.price as? LootPrice.Coins)?.total ?: 0.0 },
			meterCoins = if (includeMeter) CorpseValue.meterValue(type) else 0.0,
			keyCoins = CorpseValue.keyCost(type),
		)
	}

	/** The item the RNG meter is always set to, and so the one a payout hands over. */
	fun isMeterReward(drop: CorpseDropItem?): Boolean = drop == CorpseLootTables.SHATTERED_LOCKET

	private fun line(item: LootedItem, priceType: BazaarPriceType, meterPayout: Boolean): ProfitLine {
		val drop = CorpseDropNames.itemFor(item.name)
		val unit = drop?.let { CorpseValue.itemValue(it, priceType) }
		val price = if (unit == null) LootPrice.Unknown else LootPrice.Coins(unit, unit * item.amount)
		return ProfitLine(item, drop, price, fromMeter = meterPayout && isMeterReward(drop))
	}
}
