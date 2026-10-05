package io.github.tunnelvisionmod.tunnelvision.data.value

/**
 * The mining stats a value calculation runs on. Gemstone Spread is the chance, in percent, that
 * breaking a gemstone breaks a second one for free.
 */
data class MiningStats(
	val miningFortune: Double,
	val gemstoneFortune: Double,
	val pristine: Double,
	val miningSpeed: Double,
	val gemstoneSpread: Double,
)

/**
 * We rate a mineshaft for a maxed player instead of reading the real stats: Hypixel does not expose
 * Pristine or Gemstone Spread anywhere the client can see them, and the stats that are readable
 * differ inside and outside a mineshaft. Rating everyone as maxed keeps the gemstone ranking right -
 * the throughput of one gem relative to another moves by under 10% across the whole stat range -
 * and only shifts how much a corpse is worth relative to mining, which is a deliberate trade-off.
 */
object MiningProfile {
	val outside = MiningStats(
		miningFortune = 2090.5,
		gemstoneFortune = 319.0,
		pristine = 14.8,
		miningSpeed = 8711.0,
		gemstoneSpread = 5.0,
	)

	val inside = MiningStats(
		miningFortune = 2257.0,
		gemstoneFortune = 194.0,
		pristine = 15.05,
		miningSpeed = 9140.0,
		gemstoneSpread = 20.6,
	)
}
