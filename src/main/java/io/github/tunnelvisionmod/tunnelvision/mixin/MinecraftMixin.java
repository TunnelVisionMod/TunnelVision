package io.github.tunnelvisionmod.tunnelvision.mixin;

import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus;
import io.github.tunnelvisionmod.tunnelvision.core.events.RightClickEvent;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void tunnelvision$onStartUseItem(CallbackInfo ci) {
		if (EventBus.INSTANCE.post(new RightClickEvent()).isCancelled()) ci.cancel();
	}
}
