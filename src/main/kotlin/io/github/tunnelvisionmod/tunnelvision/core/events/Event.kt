package io.github.tunnelvisionmod.tunnelvision.core.events

abstract class Event

abstract class CancellableEvent : Event() {
	var isCancelled = false

	fun cancel() {
		isCancelled = true
	}
}

fun <T : Event> T.post(): T = EventBus.post(this)
