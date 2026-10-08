package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import io.github.tunnelvisionmod.tunnelvision.core.events.ChatReceivedEvent
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

/** The Mineshaft Mayhem buff of the shaft you are in, announced in chat shortly after you enter. */
object MineshaftMayhem {
	private const val PREFIX = "MAYHEM! "
	const val COLD_RESISTANCE_BONUS = 10.0

	var buff: MayhemBuff? = null
		private set

	private val location = LocationTracker()

	val coldResistanceBonus: Double get() = if (buff == MayhemBuff.COLD_RESISTANCE) COLD_RESISTANCE_BONUS else 0.0

	fun init() {
		EventBus.on<ChatReceivedEvent> { onChat(it) }
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
	}
}
