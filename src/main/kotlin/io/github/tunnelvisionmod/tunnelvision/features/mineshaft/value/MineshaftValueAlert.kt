package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.value

import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSound
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSounds
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalState
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftState
import io.github.tunnelvisionmod.tunnelvision.data.value.MineshaftVerdict
import io.github.tunnelvisionmod.tunnelvision.data.value.ShaftVerdict
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.LocationTracker
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.Titles
import io.github.tunnelvisionmod.tunnelvision.utils.formatPrice
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

/** Shows the [ShaftVerdict] as a title once per shaft, as soon as it is known. */
object MineshaftValueAlert : Feature {
	private val config get() = ConfigManager.config.mineshaft.gemstones.mineshaftValue

	private val location = LocationTracker()

	private var announced = false

	override fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<LocationChangedEvent> { onLocationChanged(it) }
	}

	private fun onLocationChanged(event: LocationChangedEvent) {
		if (!location.isNewLocation(event)) return
		announced = false
	}

	private fun onTick() {
		if (!config.enabled || !SkyBlock.isOnMiningIsland || announced) return
		val decided = ShaftVerdict.current() ?: return
		announced = true
		Debug.log {
			"MineshaftValue: " + MineshaftState.type?.code + ", loot mode: " + ConfigManager.config.mineshaft.corpses.loot.lootMode +
				", crystals full: " + CrystalState.crystalsAndForgeFull + ", " + decided
		}
		announce(decided)
	}

	private fun announce(verdict: MineshaftVerdict) {
		val headline = if (verdict.shouldMine) {
			Component.literal("MINE").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
		} else {
			Component.literal("DON'T MINE").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
		}
		val details = Component.literal(reason(verdict)).withStyle(ChatFormatting.GRAY)
		val sound = if (verdict.shouldMine) TitleSound.MINESHAFT_VERDICT else TitleSound.MINESHAFT_VERDICT_SKIP
		if (config.verdictTitle) {
			Titles.show(headline, sound, 0, 60, 10, details)
		} else if (config.sendChat) {
			TitleSounds.play(sound)
		}
		if (config.sendChat) ChatUtils.send(headline.copy().append(Component.literal(" ")).append(details))
	}

	private fun reason(verdict: MineshaftVerdict): String {
		val price = "Fine " + verdict.gemstone.gemName + " " + formatPrice(verdict.price)
		val needed = verdict.neededPrice
			?: return price + " · worth mining at any price"
		return price + " / " + formatPrice(needed) + " needed"
	}
}
