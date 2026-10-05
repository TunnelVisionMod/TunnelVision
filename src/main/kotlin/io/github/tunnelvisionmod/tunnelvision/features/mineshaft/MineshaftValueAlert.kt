package io.github.tunnelvisionmod.tunnelvision.features.mineshaft

import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.features.crystals.CrystalNotifications
import io.github.tunnelvisionmod.tunnelvision.features.crystals.CrystalType
import io.github.tunnelvisionmod.tunnelvision.features.mining.MineshaftDetection
import io.github.tunnelvisionmod.tunnelvision.features.mining.MineshaftParser
import io.github.tunnelvisionmod.tunnelvision.utils.Bazaar
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.LocationTracker
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.TabList
import io.github.tunnelvisionmod.tunnelvision.utils.formatPrice
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

/**
 * Decided once, when you enter the shaft, and then left alone.
 *
 * [currentVerdict] is read from the render path by the Mineshaft To-Do widget, so it must not do the
 * work every frame. It also must not let the answer drift: the title says MINE on entry, and the
 * to-do list would contradict it if a Bazaar refresh moved the verdict halfway through the shaft.
 */
object MineshaftValueAlert {
	private val config get() = ConfigManager.config.mineshaft.mineshaftValue

	private val location = LocationTracker()

	private var verdict: MineshaftVerdict? = null
	private var announced = false

	fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<LocationChangedEvent> { onLocationChanged(it) }
	}

	// Only an actual move is a new mineshaft; Hypixel may repeat the location packet, and re-deciding
	// on a repeat would be exactly the mid-shaft change this is built to avoid.
	private fun onLocationChanged(event: LocationChangedEvent) {
		if (!location.isNewLocation(event)) return
		verdict = null
		announced = false
	}

	private fun onTick() {
		if (!config.enabled || !SkyBlock.isOnMiningIsland) return
		Bazaar.refreshIfStale()
		if (announced) return
		val decided = currentVerdict() ?: return
		announced = true
		Debug.log {
			"MineshaftValue: " + MineshaftDetection.type?.code + ", loot mode: " + config.lootMode +
				", crystals full: " + CrystalNotifications.crystalsAndForgeFull + ", " + decided
		}
		announce(decided)
	}

	/**
	 * The verdict for the shaft you are in, worked out on the first tick that has everything it needs
	 * and then fixed for the rest of the shaft. Null in a shaft with no gemstones to rate, and while
	 * the corpse widget or the Bazaar prices are still missing.
	 */
	fun currentVerdict(): MineshaftVerdict? {
		if (!SkyBlock.isInMineshaft) return null
		verdict?.let { return it }
		return decide()?.also { verdict = it }
	}

	private fun decide(): MineshaftVerdict? {
		val type = MineshaftDetection.type ?: return null
		val corpses = corpses() ?: return null
		return MineshaftValue.evaluate(
			type = type,
			corpses = corpses,
			priceType = config.priceType,
			mode = config.lootMode,
			crystalsFull = CrystalNotifications.crystalsAndForgeFull,
			openVanguards = config.openVanguards,
			lockedCrystals = lockedCrystals(),
		)
	}

	/**
	 * The crystals we already carry, which therefore cannot drop from a corpse. Until the Heart of the
	 * Mountain menu has been read we do not know, and assume none are locked.
	 */
	private fun lockedCrystals(): Set<CrystalType> =
		if (CrystalNotifications.crystalsKnown) CrystalNotifications.carriedCrystals else emptySet()

	/**
	 * Every corpse in the shaft, looted or not. A shaft is worth what it was worth when you walked in,
	 * so this reads the full list rather than the unlooted one.
	 */
	private fun corpses(): Map<CorpseType, Int>? =
		MineshaftParser.parseCorpses(TabList.lines)?.mapNotNull { (name, count) ->
			CorpseType.fromTabName(name)?.let { it to count }
		}?.toMap()

	private fun announce(verdict: MineshaftVerdict) {
		val headline = if (verdict.shouldMine) {
			Component.literal("MINE").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
		} else {
			Component.literal("DON'T MINE").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
		}
		val details = Component.literal(reason(verdict)).withStyle(ChatFormatting.GRAY)
		Compat.setTitleTimes(0, 60, 10)
		Compat.setSubtitle(details)
		Compat.setTitle(headline)
		if (config.sendChat) ChatUtils.send(headline.copy().append(Component.literal(" ")).append(details))
	}

	private fun reason(verdict: MineshaftVerdict): String {
		val price = "Fine " + verdict.gemstone.gemName + " " + formatPrice(verdict.price)
		val needed = verdict.neededPrice
			?: return price + " · worth mining at any price"
		return price + " / " + formatPrice(needed) + " needed"
	}
}
