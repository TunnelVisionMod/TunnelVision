package io.github.tunnelvisionmod.tunnelvision.features.mining.pristine

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ChatReceivedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import net.minecraft.ChatFormatting
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents

/**
 * Warns when a Pristine proc drops fewer gemstones than it should, which means the wrong gear is
 * equipped. Every low proc warns - the nagging is the point, so there is deliberately no cooldown.
 */
object WrongGearWarning : Feature {
	private val config get() = ConfigManager.config.mining.pristine.wrongGear

	override fun init() {
		EventBus.on<ChatReceivedEvent> { onChat(it) }
	}

	private fun onChat(event: ChatReceivedEvent) {
		if (!config.enabled || !SkyBlock.isOnSkyBlock) return
		val proc = PristineParser.parse(event.text)
		if (proc == null) {
			// A Pristine line we cannot read is worth knowing about; the warning silently
			// never fires otherwise.
			if (PristineParser.isPristineMessage(event.text)) {
				Debug.log { "WrongGear: could not parse pristine line: ${event.text}" }
			}
			return
		}
		if (proc.amount > config.lowProc) return
		warn(proc)
	}

	private fun warn(proc: PristineProc) {
		Debug.log { "WrongGear: ${proc.gemstone} x${proc.amount} at or below ${config.lowProc}" }
		ChatUtils.send(
			Component.literal("Wrong gear! ").withStyle(ChatFormatting.RED)
				.append(
					Component.literal("Pristine dropped ${proc.amount}x ${proc.gemstone}.")
						.withStyle(ChatFormatting.GRAY),
				),
		)
		if (config.showTitle) {
			Compat.setTitleTimes(0, 20, 5)
			Compat.setTitle(Component.literal("Wrong Gear!").withStyle(ChatFormatting.RED))
		}
		if (config.playSound) {
			mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1f))
		}
	}
}
