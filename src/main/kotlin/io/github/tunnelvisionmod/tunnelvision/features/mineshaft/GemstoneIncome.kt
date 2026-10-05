package io.github.tunnelvisionmod.tunnelvision.features.mineshaft

import kotlin.math.ceil
import kotlin.math.max

/**
 * Gemstones fall into five break-time classes. [hardness] turns a mining speed into the ticks one
 * block takes: `ticks = max(4, ceil(hardness / speed - 0.5))`, which reproduces Hypixel's published
 * speed-per-tick table exactly for every class and, unlike the table, also holds below its lowest
 * row. Four ticks is the floor no amount of mining speed gets past.
 */
enum class GemstoneGroup(val hardness: Int) {
	RUBY(69_000),
	SOFT(90_000),
	TOPAZ(114_000),
	JASPER(144_000),
	HARD(156_000);

	fun ticksPerBlock(miningSpeed: Double): Int = max(MIN_TICKS, ceil(hardness / miningSpeed - 0.5).toInt())

	companion object {
		const val MIN_TICKS = 4
	}
}

/**
 * Coins per second of gemstone mining.
 *
 * One block drops `4 * (1 + Pristine * 0.79) * (1 + (Mining Fortune + Gemstone Fortune) / 100)`
 * rough gemstones on average, Gemstone Spread adds a free second block, and 6400 rough make one
 * Fine gem, which is the tier we price off the Bazaar.
 */
object GemstoneIncome {
	const val BASE_DROP = 4.0
	const val ROUGH_PER_PRISTINE = 0.79
	const val ROUGH_PER_FINE = 80 * 80
	private const val TICKS_PER_SECOND = 20.0

	fun roughPerBlock(stats: MiningStats, extraPristine: Double = 0.0): Double =
		BASE_DROP *
			(1 + (stats.pristine + extraPristine) * ROUGH_PER_PRISTINE) *
			(1 + (stats.miningFortune + stats.gemstoneFortune) / 100.0)

	fun blocksPerSecond(group: GemstoneGroup, stats: MiningStats): Double =
		TICKS_PER_SECOND / group.ticksPerBlock(stats.miningSpeed)

	/** Fine gems one broken block is worth, Gemstone Spread included. */
	fun finePerBlock(stats: MiningStats, extraPristine: Double = 0.0): Double =
		roughPerBlock(stats, extraPristine) * (1 + stats.gemstoneSpread / 100.0) / ROUGH_PER_FINE

	fun coinsPerSecond(group: GemstoneGroup, stats: MiningStats, finePrice: Double, extraPristine: Double = 0.0): Double =
		blocksPerSecond(group, stats) * finePerBlock(stats, extraPristine) * finePrice
}
