package io.github.tunnelvisionmod.tunnelvision.data.value

import io.github.tunnelvisionmod.tunnelvision.core.config.BazaarPriceType
import io.github.tunnelvisionmod.tunnelvision.data.bazaar.Bazaar
import io.github.tunnelvisionmod.tunnelvision.data.bazaar.BazaarPrices
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalValue
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType

/**
 * What one looted corpse is worth, net of its key.
 *
 * Three parts: the loot table priced off the Bazaar, the Frozen Corpse RNG meter (we only ever set
 * it to Shattered Locket, so the meter XP a corpse grants is worth that share of a Locket), and the
 * key the corpse costs to open. Keys are expensive enough to matter - a Vanguard corpse's loot is
 * worth less than its Skeleton Key, and only the meter makes it profitable.
 *
 * A crystal you already carry cannot drop again, and the roll is taken over the rest of the table
 * instead, so locked crystals do not simply lose their value - their weight moves to every other
 * entry. Only 5 of the 7 crystals drop from corpses, and Lapis corpses drop none at all.
 */
object CorpseValue {
	/** The only RNG meter drop worth selecting; anything else is a loss. */
	const val LOCKET_METER_XP = 2_500_000.0
	const val LOCKET_ID = "SHATTERED_PENDANT"
	const val LOCKET_FALLBACK = 500_000_000.0

	fun locketCoins(): Double = Bazaar.lowestBin(LOCKET_ID) ?: LOCKET_FALLBACK

	fun table(type: CorpseType): CorpseTable = when (type) {
		CorpseType.LAPIS -> CorpseLootTables.LAPIS
		CorpseType.UMBER, CorpseType.TUNGSTEN -> CorpseLootTables.UMBER_TUNGSTEN
		CorpseType.VANGUARD -> CorpseLootTables.VANGUARD
	}

	/**
	 * Coins a corpse's loot table is worth, or null while a price it needs is missing.
	 *
	 * [lockedCrystals] are the crystals already carried, which cannot drop. Their weight is left out
	 * of the divisor so the remaining entries absorb it, which is what rerolling the drop means.
	 */
	fun lootValue(type: CorpseType, priceType: BazaarPriceType, lockedCrystals: Set<CrystalType> = emptySet()): Double? {
		val table = table(type)
		val usable = table.drops.filterNot { it.item.isLocked(lockedCrystals) }
		val weight = usable.sumOf { it.weight }
		if (weight <= 0) return 0.0
		var total = 0.0
		for (drop in usable) {
			val value = itemValue(drop.item, priceType) ?: return null
			total += table.rolls * drop.weight / weight * drop.amount * value
		}
		return total
	}

	/** Coins the RNG meter XP from one corpse is worth, as a fraction of a Shattered Locket. */
	fun meterValue(type: CorpseType): Double = table(type).meterXp / LOCKET_METER_XP * locketCoins()

	/**
	 * What the key costs. Umber and Tungsten keys are stocked up on with buy orders before a mining
	 * session; Skeleton Keys do not fill as buy orders, so those are priced as an instant buy.
	 */
	fun keyCost(type: CorpseType): Double? {
		val product = type.keyProductId ?: return 0.0
		val prices = Bazaar.livePrice(product) ?: return null
		return if (type == CorpseType.VANGUARD) prices.sellOffer else prices.instantSell
	}

	/** Net coins per looted corpse, or null while any price it needs is missing. */
	fun net(type: CorpseType, priceType: BazaarPriceType, lockedCrystals: Set<CrystalType> = emptySet()): Double? {
		val loot = lootValue(type, priceType, lockedCrystals) ?: return null
		val key = keyCost(type) ?: return null
		return loot + meterValue(type) - key
	}

	private fun CorpseDropItem.isLocked(lockedCrystals: Set<CrystalType>): Boolean =
		this is CorpseDropItem.Crystal && crystal in lockedCrystals

	private fun itemValue(item: CorpseDropItem, priceType: BazaarPriceType): Double? = when (item) {
		is CorpseDropItem.Auction -> Bazaar.lowestBin(item.itemId) ?: item.fallback
		is CorpseDropItem.Bazaar -> Bazaar.price(item.productId)?.let { priceType.of(it) }
		is CorpseDropItem.Crystal -> CrystalValue.of(item.crystal)
	}
}

fun BazaarPriceType.of(prices: BazaarPrices): Double = when (this) {
	BazaarPriceType.SELL_OFFER -> prices.sellOffer
	BazaarPriceType.INSTANT_SELL -> prices.instantSell
}
