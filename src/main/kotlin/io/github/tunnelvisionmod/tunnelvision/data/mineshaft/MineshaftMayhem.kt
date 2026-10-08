package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import io.github.tunnelvisionmod.tunnelvision.core.events.ChatReceivedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.DisconnectEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.LocationTracker
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock

enum class MayhemBuff(private val key: String, val displayName: String) {
	SCRAP_CHANCE("Suspicious Scrap", "Scrap Chance"),
	MINING_FORTUNE("Mining Fortune buff", "☘ Fortune"),
	MINING_SPEED("Mining Speed buff", "⸕ Speed"),
	COLD_RESISTANCE("Cold Resistance buff", "❄ Cold Res"),
	ABILITY_COOLDOWN("Pickaxe Ability cooldown", "Cooldown");

	companion object {
		fun fromText(text: String): MayhemBuff? = entries.firstOrNull { it.key in text }
	}
}

/**
 * The Mineshaft Mayhem buff of the shaft you are in, announced in chat shortly after you enter.
 *
 * Without the perk no message ever comes, so [settled] gives up waiting after [SETTLE_MS].
 */
object MineshaftMayhem {
	private const val PREFIX = "MAYHEM! "
	private const val SETTLE_MS = 5_000L
	const val COLD_RESISTANCE_BONUS = 10.0

	var buff: MayhemBuff? = null
		private set

	private var enteredAt: Long? = null

	private val location = LocationTracker()

	/** True once the buff is known or long enough has passed that none is coming. */
	val settled: Boolean
		get() = buff != null || enteredAt?.let { System.currentTimeMillis() - it >= SETTLE_MS } == true

	val coldResistanceBonus: Double get() = if (buff == MayhemBuff.COLD_RESISTANCE) COLD_RESISTANCE_BONUS else 0.0

	fun init() {
		EventBus.on<ChatReceivedEvent> { onChat(it) }
		EventBus.on<ClientTickEvent> { if (SkyBlock.isInMineshaft && enteredAt == null) enteredAt = System.currentTimeMillis() }
		EventBus.on<LocationChangedEvent> { if (location.isNewLocation(it)) reset() }
		EventBus.on<DisconnectEvent> {
			location.forget()
			reset()
		}
	}

	fun parse(message: String): MayhemBuff? {
		if (!message.startsWith(PREFIX)) return null
		return MayhemBuff.fromText(message)
	}

	private fun onChat(event: ChatReceivedEvent) {
		if (!SkyBlock.isOnSkyBlock) return
		for (line in event.text.lines()) {
			parse(line.trim())?.let {
				Debug.log { "Mineshaft: Mayhem $it" }
				buff = it
			}
		}
	}

	private fun reset() {
		buff = null
		enteredAt = null
	}
}
