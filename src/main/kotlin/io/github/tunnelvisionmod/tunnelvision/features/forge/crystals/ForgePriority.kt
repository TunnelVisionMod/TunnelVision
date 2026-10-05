package io.github.tunnelvisionmod.tunnelvision.features.forge.crystals

import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType

/** One carried crystal, with what forging it is worth and whether it should go in now. */
data class ForgePick(val crystal: CrystalType, val value: Double?, val forgeNow: Boolean)

/**
 * Which crystals to put in the forge when you carry more than you have slots for.
 *
 * Forge the most valuable first, for two reasons that happen to agree. The obvious one is that a
 * forge slot is the scarce resource - 7 slots at 18 hours each, against far more crystals than that
 * from crystal mineshafts - so a slot should carry the biggest Perfect you can give it. The second
 * is that forging a crystal stops you carrying it, and a crystal you do not carry can drop from a
 * corpse again; you would rather that be an expensive one. Holding a cheap crystal keeps its slot
 * in the corpse loot table blocked, and that roll is taken again over everything else, including
 * the Shattered Locket.
 */
object ForgePriority {
	/**
	 * [carried] ranked by what forging each is worth, most valuable first, with the first
	 * [openSlots] marked to forge now. Crystals with no known price sort last and are never picked -
	 * guessing with a missing price would be worse than saying nothing.
	 */
	fun rank(carried: Collection<CrystalType>, openSlots: Int, value: (CrystalType) -> Double?): List<ForgePick> {
		val ranked = carried.distinct()
			.map { it to value(it) }
			.sortedWith(compareByDescending(nullsFirst()) { (_, worth) -> worth })
		var left = openSlots.coerceAtLeast(0)
		return ranked.map { (crystal, worth) ->
			val pick = worth != null && left > 0
			if (pick) left--
			ForgePick(crystal, worth, pick)
		}
	}

	/** Just the crystals to forge now, most valuable first. */
	fun toForge(carried: Collection<CrystalType>, openSlots: Int, value: (CrystalType) -> Double?): List<CrystalType> =
		rank(carried, openSlots, value).filter { it.forgeNow }.map { it.crystal }
}
