package io.github.tunnelvisionmod.tunnelvision.features.crystals

import io.github.tunnelvisionmod.tunnelvision.utils.Bazaar

/**
 * What a Gemstone Crystal is worth.
 *
 * A crystal has no sale price of its own - it is untradeable and exists only to forge a Perfect. So
 * it is worth the Perfect it unlocks less the Fine gems that Perfect would otherwise cost: 80 Fine
 * make a Flawless and 5 Flawless a Perfect, so 400 Fine.
 */
object CrystalValue {
	const val FINE_PER_PERFECT = 400

	/** Coins forging [crystal] is worth, or null while a price it needs is missing. */
	fun of(crystal: CrystalType): Double? {
		val gem = crystal.displayName.uppercase()
		val perfect = Bazaar.price("PERFECT_${gem}_GEM")?.sellOffer ?: return null
		val fine = Bazaar.price("FINE_${gem}_GEM")?.instantSell ?: return null
		return perfect - FINE_PER_PERFECT * fine
	}
}
