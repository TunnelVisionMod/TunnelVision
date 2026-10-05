package io.github.tunnelvisionmod.tunnelvision.core.events

import io.github.tunnelvisionmod.tunnelvision.utils.removeFormatting
import net.minecraft.network.chat.Component

class ChatReceivedEvent(val message: Component) : CancellableEvent() {
	val text: String by lazy { message.string.removeFormatting() }
}
