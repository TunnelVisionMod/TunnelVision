package io.github.tunnelvisionmod.tunnelvision.data.crystals

/**
 * Which crystals you are carrying, merged from the Heart of the Mountain menu, chat and the forge.
 *
 * The menu is the only complete source, but it is only readable while it is open, so chat and the
 * forge keep the picture current in between. Crucially every source can take a crystal away as well as
 * add one - a crystal spent in the forge is gone, and the menu will not say so until you next open
 * it.
 */
class CrystalTracker {
	private val held = linkedSetOf<CrystalType>()

	/** Crystals in hand, in enum order. */
	val carried: Set<CrystalType> get() = held

	/** True once every crystal is carried - the condition other features ask about. */
	val hasAll: Boolean get() = held.size == CrystalType.entries.size

	/** Applies what a source says about the crystals it mentions, in either direction. */
	fun apply(states: Map<CrystalType, Boolean>) {
		for ((crystal, carried) in states) {
			if (carried) held += crystal else held -= crystal
		}
	}

	/** Records a crystal picked up. Returns true if this is news. */
	fun gained(crystal: CrystalType): Boolean = held.add(crystal)

	/** Records a crystal spent - forged, or placed in the nucleus. Returns true if it was held. */
	fun consumed(crystal: CrystalType): Boolean = held.remove(crystal)

	fun reset() = held.clear()
}
