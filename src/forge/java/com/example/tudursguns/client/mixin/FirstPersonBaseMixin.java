package com.example.tudursguns.client.mixin;

import com.example.tudursguns.client.render.FirstPersonBase;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the pose first-person hand rendering starts from - see FirstPersonBase. Forge 1.20.1 version. */
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonBaseMixin {

	@Inject(method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V", at = @At("HEAD"))
	private void tudursguns$captureBase(float tickProgress, PoseStack matrices, MultiBufferSource.BufferSource buffers,
			LocalPlayer player, int light, CallbackInfo ci) {
		FirstPersonBase.capture(matrices);
	}
}
