package io.github.tunnelvisionmod.tunnelvision.features.mining.pristine

import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ChatReceivedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock

/**
 * Hides the `PRISTINE! You found Flawed Ruby Gemstone x2!` spam from chat.
 *
 * [WrongGearWarning] reads the same lines off the same event, and [EventBus] runs every handler
 * regardless of cancellation, so a low proc still warns - what is hidden is only the noise you do
 * not need while your gear is right.
 *
 * A Pristine line that cannot be parsed stays visible on purpose: if Hypixel reworks the wording,
 * swallowing the message outright is worse than the spam, and [WrongGearWarning] cannot read it
 * either.
 */
object HidePristineMessages : Feature {
	private val config get() = ConfigManager.config.mining.pristine.hidePristineMessages

	override fun init() {
		EventBus.on<ChatReceivedEvent> { onChat(it) }
	}

	private fun onChat(event: ChatReceivedEvent) {
		if (!config.enabled || !SkyBlock.isOnSkyBlock) return
		if (shouldHide(event.text)) event.cancel()
	}

	/** True only for a Pristine line we fully understand. */
	fun shouldHide(message: String): Boolean = PristineParser.parse(message) != null
}
