package io.github.tunnelvisionmod.tunnelvision.data.value

import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalState
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftParser
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftState
import io.github.tunnelvisionmod.tunnelvision.utils.LocationTracker
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.TabList

/**
 * Whether the shaft you are in is worth mining, decided once and then left alone.
 *
 * [current] is read from the render path by the Mineshaft To-Do widget, so it must not do the work
 * every frame. It also must not let the answer drift: the title says MINE on entry, and the to-do
 * list would contradict it if a Bazaar refresh moved the verdict halfway through the shaft.
 */
object ShaftVerdict {
	private val loot get() = ConfigManager.config.mineshaft.corpses.loot

	private val location = LocationTracker()

	private var verdict: MineshaftVerdict? = null

	fun init() {
		EventBus.on<LocationChangedEvent> { onLocationChanged(it) }
	}

	// Only an actual move is a new mineshaft; Hypixel may repeat the location packet, and re-deciding
	// on a repeat would be exactly the mid-shaft change this is built to avoid.
	private fun onLocationChanged(event: LocationChangedEvent) {
		if (!location.isNewLocation(event)) return
		verdict = null
	}

	/**
	 * The verdict for the shaft you are in, worked out on the first call that has everything it
	 * needs and then fixed for the rest of the shaft. Null in a shaft with no gemstones to rate, and
	 * while the corpse widget or the Bazaar prices are still missing.
	 */
	fun current(): MineshaftVerdict? {
		if (!SkyBlock.isInMineshaft) return null
		verdict?.let { return it }
		return decide()?.also { verdict = it }
	}

	private fun decide(): MineshaftVerdict? {
		val type = MineshaftState.type ?: return null
		val corpses = corpses() ?: return null
		return MineshaftValue.evaluate(
			type = type,
			corpses = corpses,
			priceType = ConfigManager.config.general.bazaarPrice,
			mode = loot.lootMode,
			crystalsFull = CrystalState.crystalsAndForgeFull,
			openVanguards = loot.openVanguards,
			lockedCrystals = lockedCrystals(),
		)
	}

	/**
	 * The crystals we already carry, which therefore cannot drop from a corpse. Until the Heart of the
	 * Mountain menu has been read we do not know, and assume none are locked.
	 */
	private fun lockedCrystals(): Set<CrystalType> =
		if (CrystalState.known) CrystalState.carried else emptySet()

	/**
	 * Every corpse in the shaft, looted or not. A shaft is worth what it was worth when you walked in,
	 * so this reads the full list rather than the unlooted one.
	 */
	private fun corpses(): Map<CorpseType, Int>? =
		MineshaftParser.parseCorpses(TabList.lines)?.mapNotNull { (name, count) ->
			CorpseType.fromTabName(name)?.let { it to count }
		}?.toMap()
}
