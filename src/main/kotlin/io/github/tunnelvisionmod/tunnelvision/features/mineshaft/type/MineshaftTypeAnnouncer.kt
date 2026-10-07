package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.type

import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.MineshaftEnteredEvent
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSound
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSounds
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftType
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import io.github.tunnelvisionmod.tunnelvision.utils.Titles
import net.minecraft.network.chat.Component

/** Announces the mineshaft type as soon as it is known, and again if a warp corrects it. */
object MineshaftTypeAnnouncer : Feature {
	private val config get() = ConfigManager.config.mineshaft.mineshaftType

	override fun init() {
		EventBus.on<MineshaftEnteredEvent> { announce(it.type) }
	}

	private fun announce(mineshaft: MineshaftType) {
		if (!config.enabled) return
		val name = Component.literal(mineshaft.displayName).withStyle(mineshaft.color)
		if (config.announceEntry) ChatUtils.send(name)
		if (config.showTitle) {
			Titles.show(name, TitleSound.MINESHAFT_TYPE, 0, 50, 10)
		} else if (config.announceEntry) {
			TitleSounds.play(TitleSound.MINESHAFT_TYPE)
		}
	}
}
