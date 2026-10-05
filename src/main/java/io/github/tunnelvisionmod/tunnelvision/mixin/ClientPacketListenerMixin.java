package io.github.tunnelvisionmod.tunnelvision.mixin;

import io.github.tunnelvisionmod.tunnelvision.core.events.EntityDeathEvent;
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Shadow
	private ClientLevel level;

	@Inject(method = "handleEntityEvent", at = @At("TAIL"))
	private void tunnelvision$onEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
		if (packet.getEventId() != EntityEvent.DEATH) return;
		Entity entity = packet.getEntity(level);
		if (entity != null) EventBus.INSTANCE.post(new EntityDeathEvent(entity));
	}
}
