package io.github.tunnelvisionmod.tunnelvision.utils

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object ChatUtils {
	private val prefix: Component = Component.literal("[TunnelVision] ").withStyle(ChatFormatting.DARK_AQUA)

	fun send(message: Component) {
		Compat.chat.addClientSystemMessage(Component.empty().append(prefix).append(message))
	}

	/**
	 * A line with no mod prefix, for replacing a block the server printed. A prefix on every line of
	 * a multi-line block reads as spam, and the block it stands in for had none.
	 */
	fun sendRaw(message: Component) {
		Compat.chat.addClientSystemMessage(message)
	}
}
