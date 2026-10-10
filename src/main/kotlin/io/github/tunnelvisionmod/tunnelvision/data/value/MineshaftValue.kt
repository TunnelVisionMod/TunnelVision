package io.github.tunnelvisionmod.tunnelvision.data.value

import io.github.tunnelvisionmod.tunnelvision.core.config.BazaarPriceType
import io.github.tunnelvisionmod.tunnelvision.core.config.LootMode
import io.github.tunnelvisionmod.tunnelvision.data.bazaar.Bazaar
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseLoot
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.LootRule
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftType
import java.util.concurrent.ConcurrentHashMap

/**
 * The gemstone a mineshaft is made of, and the break-time class it belongs to.
 *
 * [blocksPerVein] is the average vein in the bundled gemstone routes. Small veins mean more walking
 * between them for the same gems, which costs a fast gem like Ruby the most.
 */
enum class GemstoneShaft(private val codePrefix: String, val gemName: String, val group: GemstoneGroup, val blocksPerVein: Double) {
	RUBY("RUBY", "Ruby", GemstoneGroup.RUBY, 29.1),
	OPAL("OPAL", "Opal", GemstoneGroup.SOFT, 24.9),
	AMETHYST("AMET", "Amethyst", GemstoneGroup.SOFT, 24.6),
	SAPPHIRE("SAPP", "Sapphire", GemstoneGroup.SOFT, 29.5),
	JADE("JADE", "Jade", GemstoneGroup.SOFT, 48.9),
	AMBER("AMBE", "Amber", GemstoneGroup.SOFT, 29.3),
	TOPAZ("TOPA", "Topaz", GemstoneGroup.TOPAZ, 40.2),
	JASPER("JASP", "Jasper", GemstoneGroup.JASPER, 27.8),
	PERIDOT("PERI", "Peridot", GemstoneGroup.HARD, 29.5),
	CITRINE("CITR", "Citrine", GemstoneGroup.HARD, 28.3),
	ONYX("ONYX", "Onyx", GemstoneGroup.HARD, 27.3),
	AQUAMARINE("AQUA", "Aquamarine", GemstoneGroup.HARD, 31.1);

	val fineGemId: String get() = "FINE_${gemName.uppercase()}_GEM"

	companion object {
		fun of(type: MineshaftType): GemstoneShaft? = entries.firstOrNull { type.code.startsWith(it.codePrefix) }
	}
}

/**
 * [coinsPerHour] is what staying in this shaft earns over leaving it, and [targetPerHour] is the
 * long-run rate of the leave-and-respawn loop that it has to beat.
 *
 * [neededPrice] is the same decision expressed as a Fine gem price, so it can be read against the
 * Bazaar: the price this gem would have to reach for the shaft to be worth mining. It is null when
 * the corpses alone clear the bar and no price is needed.
 */
data class MineshaftVerdict(
	val gemstone: GemstoneShaft,
	val price: Double,
	val neededPrice: Double?,
	val coinsPerHour: Double,
	val targetPerHour: Double,
	val corpses: Int,
	val shouldMine: Boolean,
)

/**
 * Decides whether a mineshaft is worth mining against the alternative: leave it, mine Jade outside
 * until the next one spawns, and take that one instead.
 *
 * That makes it an optimal-stopping problem rather than a price threshold. Let `R` be the coins per
 * second the whole loop earns in the long run. Staying in a shaft costs `shaft` seconds, the time
 * until the cold freezes you out ([ShaftTime]), and leaving costs [SKIP_SECONDS], so the shaft is
 * worth mining exactly when
 *
 * ```
 * mineValue - R * shaft >= skipValue - R * SKIP_SECONDS
 * ```
 *
 * `R` itself depends on which shafts we would mine, so it is the root of a decreasing function and
 * found by bisection over the pool of shafts we could respawn into. A shaft therefore gets rated
 * against your own best alternative instead of against a hardcoded number.
 */
object MineshaftValue {
	/**
	 * How long looting the corpses takes. Skipping a shaft is only that and leaving; mining one starts
	 * with it too, so it comes off the time the gems are mined in.
	 */
	const val SKIP_SECONDS = 30.0

	/** The part of a shaft spent mining gems, once the corpses are looted. */
	fun miningSeconds(shaftSeconds: Double): Double = maxOf(0.0, shaftSeconds - SKIP_SECONDS)

	/**
	 * All 17 mineshaft types are equally likely at 1 in 17. Mining a gemstone as the last block
	 * before a spawn would raise that type to 1 in 3, but only 17% of spawns land outside a
	 * Pickobulus throw on Glacite, and modelling the bias moves the result by under 2%.
	 */
	const val TYPE_COUNT = 17

	/**
	 * Titanium, Tungsten, Umber, Fairy and Littlefoot's Den. We treat these as corpses only and
	 * leave immediately; the ore in a Tungsten or Umber shaft is not modelled.
	 */
	const val NON_GEMSTONE_TYPES = 5

	/**
	 * A shaft holds 1-3 corpses by size, plus one more at 45% with maxed Dead Man's Chest and
	 * Frozen Corpse Milestones 2 and 5, capped at 4.
	 */
	const val EXTRA_CORPSE_CHANCE = 0.45

	private const val BISECTION_STEPS = 80

	/** How far Greedy's bar moves from leaving the missing crystals out towards counting them in full. */
	const val GREEDY_CRYSTAL_SHARE = 0.25
	private const val RATE_CEILING = 1e7

	/** Corpse kinds in a gemstone shaft: half Lapis, a quarter each Umber and Tungsten. */
	private val GEMSTONE_MIX = mapOf(CorpseType.LAPIS to 0.5, CorpseType.UMBER to 0.25, CorpseType.TUNGSTEN to 0.25)

	/** A Fairy shaft always holds Vanguard corpses, which no other shaft does. */
	private val FAIRY_MIX = mapOf(CorpseType.VANGUARD to 1.0)

	/** How likely a shaft is to hold 1, 2, 3 or 4 corpses. */
	val corpseCountChances: Map<Int, Double> = buildMap {
		val size = 1.0 / 3
		for (base in 1..3) {
			merge(base, size * (1 - EXTRA_CORPSE_CHANCE), Double::plus)
			merge(base + 1, size * EXTRA_CORPSE_CHANCE, Double::plus)
		}
	}

	private data class Inputs(
		val priceType: BazaarPriceType,
		val mode: LootMode,
		val crystalsFull: Boolean,
		val openVanguards: Boolean,
		val lockedCrystals: Set<CrystalType>,
		val coldResistance: Double,
		val meterBonus: Int = CorpseValue.meterBonusPercent(),
	)

	private data class CacheKey(val generation: Int, val inputs: Inputs)

	/** Greedy solves two bars per decision, so a single slot would throw the other one away each time. */
	private val cached = ConcurrentHashMap<CacheKey, Double>()

	/** Corpses we would loot if we decided against mining, which is what skipping is worth. */
	fun skipRule(mode: LootMode, crystalsFull: Boolean, openVanguards: Boolean): LootRule =
		CorpseLoot.rule(mode, crystalsFull, shouldMine = false, openVanguards = openVanguards)

	/**
	 * Corpses we loot in a shaft we do mine. This still goes through the mode: Lapis Only never opens
	 * anything but Lapis, not even in a shaft worth mining.
	 */
	fun mineRule(mode: LootMode, crystalsFull: Boolean, openVanguards: Boolean): LootRule =
		CorpseLoot.rule(mode, crystalsFull, shouldMine = true, openVanguards = openVanguards)

	/** The share of a corpse mix [rule] actually opens, which is what the Pristine bonus counts. */
	private fun lootedShare(mix: Map<CorpseType, Double>, rule: LootRule): Double =
		mix.entries.sumOf { (type, share) -> if (rule.includes(type)) share else 0.0 }

	fun evaluate(
		type: MineshaftType,
		corpses: Map<CorpseType, Int>,
		priceType: BazaarPriceType,
		mode: LootMode,
		crystalsFull: Boolean,
		openVanguards: Boolean,
		lockedCrystals: Set<CrystalType>,
		coldResistance: Double,
		loopColdResistance: Double,
	): MineshaftVerdict? {
		val gemstone = GemstoneShaft.of(type) ?: return null
		val finePrice = Bazaar.price(gemstone.fineGemId)?.let { priceType.of(it) } ?: return null
		val rate = longRunRate(priceType, mode, crystalsFull, openVanguards, lockedCrystals, loopColdResistance) ?: return null
		val shaft = ShaftTime.secondsToFreeze(coldResistance)
		val mineRule = mineRule(mode, crystalsFull, openVanguards)
		val total = corpses.values.sum()
		// Only a corpse we open gives its Pristine, so Lapis Only gets it from the Lapis ones alone.
		val looted = corpses.entries.sumOf { (type, count) -> if (mineRule.includes(type)) count else 0 }
		val mined = miningSeconds(shaft) *
			GemstoneIncome.coinsPerSecond(gemstone.group, MiningProfile.inside, finePrice, looted.toDouble(), gemstone.blocksPerVein)
		val mineCorpses = corpseValue(corpses, mineRule, priceType, lockedCrystals) ?: return null
		val skip = corpseValue(corpses, skipRule(mode, crystalsFull, openVanguards), priceType, lockedCrystals) ?: return null
		val mine = mined + mineCorpses
		val marginal = (mine - skip) / (shaft - SKIP_SECONDS)
		return MineshaftVerdict(
			gemstone = gemstone,
			price = finePrice,
			neededPrice = breakEvenPrice(gemstone.group, looted, rate, mineCorpses - skip, shaft, gemstone.blocksPerVein),
			coinsPerHour = marginal * 3600,
			targetPerHour = rate * 3600,
			corpses = total,
			shouldMine = marginal >= rate,
		)
	}

	/**
	 * The Fine gem price at which this shaft would exactly meet the bar, or null when the corpses
	 * already clear it on their own.
	 *
	 * Mining income is linear in the Fine price and the corpses do not depend on it at all, so the
	 * margin is `((shaft - SKIP) * perPrice * price + corpseGain) / (shaft - SKIP)` and setting that equal to
	 * [rate] inverts exactly - no search needed.
	 */
	fun breakEvenPrice(
		group: GemstoneGroup,
		looted: Int,
		rate: Double,
		corpseGain: Double,
		shaftSeconds: Double,
		blocksPerVein: Double? = null,
	): Double? {
		val needed = (rate * (shaftSeconds - SKIP_SECONDS) - corpseGain) /
			(miningSeconds(shaftSeconds) * finePerSecond(group, looted, blocksPerVein))
		return needed.takeIf { it > 0 }
	}

	/** Fine gems mined per second, which is what makes the margin linear in the Fine price. */
	fun finePerSecond(group: GemstoneGroup, looted: Int, blocksPerVein: Double? = null): Double =
		GemstoneIncome.blocksPerSecond(group, MiningProfile.inside, blocksPerVein) *
			GemstoneIncome.finePerBlock(MiningProfile.inside, looted.toDouble())

	/** Coins the corpses in a shaft are worth under [rule], or null while a price is missing. */
	fun corpseValue(
		corpses: Map<CorpseType, Int>,
		rule: LootRule,
		priceType: BazaarPriceType,
		lockedCrystals: Set<CrystalType> = emptySet(),
	): Double? {
		var total = 0.0
		for ((type, count) in corpses) {
			if (!rule.includes(type)) continue
			total += count * (CorpseValue.net(type, priceType, lockedCrystals) ?: return null)
		}
		return total
	}

	/**
	 * The coins per second the leave-and-respawn loop earns, cached per Bazaar refresh.
	 *
	 * Greedy sits between two bars that are both wrong, a quarter of the way from one to the other.
	 * Counting the missing crystals as income in every future shaft assumes they keep dropping
	 * forever, when they stop once all five are carried - that bar ran so high not even a Jasper
	 * shaft cleared it. Leaving them out entirely ignores that a skip opens corpses sooner, so it mined
	 * almost everything and collected slowly. Simulated, every share from 0 to 0.5 earned the same
	 * coins per hour, about 3% over Normal; a quarter still mines Jasper with no crystals carried and
	 * collects all five corpse crystals in about four fifths of the time.
	 *
	 * Normal keeps the crystals you really carry. That bar runs high while crystals are missing, which
	 * makes Normal skip more and reach the crystal mineshafts sooner.
	 */
	fun longRunRate(
		priceType: BazaarPriceType,
		mode: LootMode,
		crystalsFull: Boolean,
		openVanguards: Boolean,
		lockedCrystals: Set<CrystalType>,
		coldResistance: Double,
	): Double? {
		val carried = Inputs(priceType, mode, crystalsFull, openVanguards, lockedCrystals, coldResistance)
		if (mode != LootMode.GREEDY) return longRunRate(carried)
		val steady = longRunRate(Inputs(priceType, mode, crystalsFull = true, openVanguards, CrystalType.entries.toSet(), coldResistance))
			?: return null
		if (crystalsFull) return steady
		val withCrystals = longRunRate(carried) ?: return null
		return steady + GREEDY_CRYSTAL_SHARE * (withCrystals - steady)
	}

	private fun longRunRate(inputs: Inputs): Double? {
		val key = CacheKey(Bazaar.generation, inputs)
		cached[key]?.let { return it }
		val solved = solve(inputs) ?: return null
		cached.keys.removeIf { it.generation != key.generation }
		cached[key] = solved
		return solved
	}

	private fun solve(inputs: Inputs): Double? {
		val jadePrice = Bazaar.price(GemstoneShaft.JADE.fineGemId)?.let { inputs.priceType.of(it) } ?: return null
		val leg = MineshaftSpawn.maxed
		val outsideValue = leg.jadeBlocks * GemstoneIncome.finePerBlock(MiningProfile.outside) * jadePrice
		val finePrices = GemstoneShaft.entries.associateWith { shaft ->
			Bazaar.price(shaft.fineGemId)?.let { inputs.priceType.of(it) } ?: return null
		}
		val skipRule = skipRule(inputs.mode, inputs.crystalsFull, inputs.openVanguards)
		val mineRule = mineRule(inputs.mode, inputs.crystalsFull, inputs.openVanguards)
		val minedShare = lootedShare(GEMSTONE_MIX, mineRule)
		val mixAll = mixValue(GEMSTONE_MIX, mineRule, inputs.priceType, inputs.lockedCrystals) ?: return null
		val mixSkip = mixValue(GEMSTONE_MIX, skipRule, inputs.priceType, inputs.lockedCrystals) ?: return null
		// We never stay in a non-gemstone shaft, so its corpses are worth what leaving loots - which
		// means a Fairy shaft is worth nothing at all on Lapis Only, since it only holds Vanguards.
		val fairySkip = mixValue(FAIRY_MIX, skipRule, inputs.priceType, inputs.lockedCrystals) ?: return null
		val share = 1.0 / TYPE_COUNT
		val shaftSeconds = ShaftTime.secondsToFreeze(inputs.coldResistance)

		// How much a full loop earns over and above making [rate] for the time the loop takes. It
		// falls as the rate rises, so the rate the loop actually sustains is where it reaches zero.
		fun surplus(rate: Double): Double {
			var total = outsideValue - rate * leg.seconds
			for ((count, chance) in corpseCountChances) {
				val weight = share * chance
				for (shaft in GemstoneShaft.entries) {
					val mine = miningSeconds(shaftSeconds) *
						GemstoneIncome.coinsPerSecond(
							shaft.group,
							MiningProfile.inside,
							finePrices.getValue(shaft),
							count * minedShare,
							shaft.blocksPerVein,
						) + count * mixAll
					val skip = count * mixSkip
					total += weight * maxOf(mine - rate * shaftSeconds, skip - rate * SKIP_SECONDS)
				}
				// Fairy holds Vanguard corpses; the other four non-gemstone types hold the usual mix.
				total += weight * (count * fairySkip - rate * SKIP_SECONDS)
				total += weight * (NON_GEMSTONE_TYPES - 1) * (count * mixSkip - rate * SKIP_SECONDS)
			}
			return total
		}

		var low = 0.0
		var high = RATE_CEILING
		repeat(BISECTION_STEPS) {
			val mid = (low + high) / 2
			if (surplus(mid) > 0) low = mid else high = mid
		}
		return low
	}

	private fun mixValue(
		mix: Map<CorpseType, Double>,
		rule: LootRule,
		priceType: BazaarPriceType,
		lockedCrystals: Set<CrystalType>,
	): Double? {
		var total = 0.0
		for ((type, share) in mix) {
			if (!rule.includes(type)) continue
			total += share * (CorpseValue.net(type, priceType, lockedCrystals) ?: return null)
		}
		return total
	}
}
