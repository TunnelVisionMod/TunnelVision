package io.github.tunnelvisionmod.tunnelvision.features.mineshaft

/**
 * How long the outside leg of the loop takes and how much Jade it pays on the way.
 *
 * Every block you break has a `1 / (2000 - weight)` chance to spawn a mineshaft and then adds its
 * Block Quality to the weight, so a spawn is guaranteed once the weight reaches 2000 and the weight
 * resets afterwards. Blocks broken by Pickobulus or by Gemstone Spread only count half quality.
 *
 * The rotation is a Pickobulus throw on Glacite the moment it comes off cooldown, then Jade until
 * the next throw. Block counts come from the mining speed in [MiningProfile], not from measured
 * averages, so the result follows the stats.
 */
object MineshaftSpawn {
	const val PITY_WEIGHT = 2000
	const val GLACITE_QUALITY = 4.0
	const val JADE_QUALITY = 8.0
	const val PICKOBULUS_BLOCKS = 170
	const val CYCLE_SECONDS = 34.0
	const val THROW_SECONDS = 2.0
	const val ENTER_SECONDS = 3.0

	/** [seconds] includes entering the portal; [jadeBlocks] counts swings, Spread extras aside. */
	data class Leg(val seconds: Double, val jadeBlocks: Double)

	/** The leg for the stat profile we always rate against, which never changes. */
	val maxed: Leg by lazy { outsideLeg() }

	/**
	 * Walks the rotation block by block and accumulates the expected spawn time. The weight is
	 * deterministic, only the spawn roll is random, so this is an exact expectation rather than a
	 * simulation - every block contributes `P(still waiting) * P(spawn here)`.
	 */
	fun outsideLeg(stats: MiningStats = MiningProfile.outside): Leg {
		val swingsPerSecond = GemstoneIncome.blocksPerSecond(GemstoneGroup.SOFT, stats)
		val jadeQuality = JADE_QUALITY + stats.gemstoneSpread / 100.0 * (JADE_QUALITY / 2)
		val pickobulusQuality = GLACITE_QUALITY / 2

		var surviving = 1.0
		var expectedSeconds = 0.0
		var expectedJade = 0.0
		var weight = 0.0
		var jade = 0.0
		var cycleStart = 0.0

		while (surviving > PRECISION) {
			repeat(PICKOBULUS_BLOCKS) { index ->
				val at = cycleStart + THROW_SECONDS * index / PICKOBULUS_BLOCKS
				val spawn = spawnChance(weight)
				expectedSeconds += surviving * spawn * at
				expectedJade += surviving * spawn * jade
				surviving *= 1 - spawn
				weight += pickobulusQuality
			}
			val swings = ((CYCLE_SECONDS - THROW_SECONDS) * swingsPerSecond).toInt()
			repeat(swings) { index ->
				val at = cycleStart + THROW_SECONDS + index / swingsPerSecond
				val spawn = spawnChance(weight)
				expectedSeconds += surviving * spawn * at
				expectedJade += surviving * spawn * jade
				surviving *= 1 - spawn
				weight += jadeQuality
				jade += 1
			}
			cycleStart += CYCLE_SECONDS
		}
		return Leg(expectedSeconds + ENTER_SECONDS, expectedJade)
	}

	/**
	 * Block Quality does not divide the pity cap evenly, so the weight can stop just short of it -
	 * at 1999.6, say, where the raw `1 / (2000 - weight)` would read 2.5. Within one weight of the
	 * cap the next block is simply certain to spawn one.
	 */
	private fun spawnChance(weight: Double): Double =
		if (weight >= PITY_WEIGHT) 1.0 else (1.0 / (PITY_WEIGHT - weight)).coerceAtMost(1.0)

	private const val PRECISION = 1e-12
}
