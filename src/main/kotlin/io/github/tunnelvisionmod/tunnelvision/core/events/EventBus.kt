package io.github.tunnelvisionmod.tunnelvision.core.events

import io.github.tunnelvisionmod.tunnelvision.TunnelVision

object EventBus {
	private val handlers = mutableMapOf<Class<out Event>, MutableList<(Event) -> Unit>>()

	inline fun <reified T : Event> on(noinline handler: (T) -> Unit) = on(T::class.java, handler)

	fun <T : Event> on(type: Class<T>, handler: (T) -> Unit) {
		@Suppress("UNCHECKED_CAST")
		handlers.getOrPut(type) { mutableListOf() }.add(handler as (Event) -> Unit)
	}

	fun <T : Event> post(event: T): T {
		handlers[event.javaClass]?.forEach { handler ->
			try {
				handler(event)
			} catch (e: Exception) {
				TunnelVision.logger.error("Error while handling ${event.javaClass.simpleName}", e)
			}
		}
		return event
	}
}
