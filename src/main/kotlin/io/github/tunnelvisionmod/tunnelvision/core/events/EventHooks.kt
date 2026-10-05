package io.github.tunnelvisionmod.tunnelvision.core.events

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.minecraft.world.InteractionResult

object EventHooks {
	fun register() {
		ClientTickEvents.END_CLIENT_TICK.register { ClientTickEvent.post() }
		ClientReceiveMessageEvents.ALLOW_GAME.register { message, overlay ->
			overlay || !ChatReceivedEvent(message).post().isCancelled
		}
		ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> mc.execute { DisconnectEvent.post() } }
		LevelRenderEvents.COLLECT_SUBMITS.register { WorldRenderEvent(it).post() }
		AttackEntityCallback.EVENT.register { _, level, _, entity, _ ->
			if (level.isClientSide) AttackEntityEvent(entity).post()
			InteractionResult.PASS
		}
	}
}
