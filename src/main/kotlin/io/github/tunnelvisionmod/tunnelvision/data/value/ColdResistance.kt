package io.github.tunnelvisionmod.tunnelvision.data.value

import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.DisconnectEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftMayhem
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.LocationTracker
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.Storage
import io.github.tunnelvisionmod.tunnelvision.utils.TabList

/**
 * The Cold Resistance you end up with in a shaft, learned from the shafts you stayed in.
 *
 * On entry the widget shows too little: the orb and Cut Loose stacks only come in while you are
 * inside, and how much they add depends on your orb and on how many mobs there are. So we keep the
 * peak of every shaft you stayed in for [MIN_STAY_MS], without the Mayhem buff since that changes
 * from shaft to shaft, and rate with the best of the last [REMEMBERED]. One shaft with few mobs or a
 * forgotten orb does not drag it down, and a real downgrade still shows up after a few shafts.
 */
object ColdResistance {
	private const val MIN_STAY_MS = 3 * 60_000L
	private const val CHECK_TICKS = 20
	private const val REMEMBERED = 5

	/** Used until anything is known, which only makes the shaft look a little shorter than it is. */
	const val FALLBACK = 100.0

	private val stat = Regex("""^Cold Resistance: \D*?([\d,.]+)""")

	private val location = LocationTracker()
	private var enteredAt: Long? = null
	private var peak: Double? = null
	private var ticks = 0

	fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<LocationChangedEvent> { if (location.isNewLocation(it)) finishShaft() }
		EventBus.on<DisconnectEvent> {
			location.forget()
			finishShaft()
		}
	}

	fun parse(tabLines: List<String>): Double? =
		tabLines.firstNotNullOfOrNull { stat.find(it.trim())?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() }

	/** Your Cold Resistance in a shaft without Mayhem, or null while there is nothing to go on. */
	val base: Double?
		get() = Storage.data.coldResistancePeaks.maxOrNull()
			?: parse(TabList.lines)?.let { it - MineshaftMayhem.coldResistanceBonus }

	/** True while neither a learned value nor the Stats widget gives us anything to rate with. */
	val missing: Boolean get() = base == null

	/** What the shaft you are in will reach, its Mayhem buff included. */
	val forCurrentShaft: Double get() = (base ?: FALLBACK) + MineshaftMayhem.coldResistanceBonus

	/** What an average future shaft reaches, for the long-run rate. */
	val forLoop: Double get() = base ?: FALLBACK

	private fun onTick() {
		if (!SkyBlock.isInMineshaft) return
		if (enteredAt == null) enteredAt = System.currentTimeMillis()
		if (++ticks % CHECK_TICKS != 0) return
		val now = parse(TabList.lines)?.let { it - MineshaftMayhem.coldResistanceBonus } ?: return
		if (now > (peak ?: Double.NEGATIVE_INFINITY)) peak = now
	}

	private fun finishShaft() {
		val reached = peak
		val stayed = enteredAt?.let { System.currentTimeMillis() - it }
		peak = null
		enteredAt = null
		if (reached == null || stayed == null || stayed < MIN_STAY_MS) return
		val peaks = Storage.data.coldResistancePeaks
		peaks += reached
		while (peaks.size > REMEMBERED) peaks.removeAt(0)
		Storage.save()
		Debug.log { "ColdResistance: learned $reached, base now ${peaks.max()}" }
	}
}
