package com.example.tudursguns.client.mixin;

import com.example.tudursguns.client.render.AimRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records whether the entity being drawn aims / sprint-carries a handheld weapon, for its held
 * item's renderer (ObjHandheldModelRenderer reads it while the entity's layers are drawn). Forge
 * 1.20.1 version: set around render, cleared afterwards. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererAimMixin {

	@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
	private void tudursguns$beginEntity(LivingEntity entity, float yaw, float partialTick, PoseStack matrices,
			MultiBufferSource buffers, int light, CallbackInfo ci) {
		AimRenderState.setEntityBeingUpdatedAims(AimRenderState.aims(entity));
		AimRenderState.setEntityBeingUpdatedSprintCarries(AimRenderState.sprintCarries(entity));
	}

	@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
	private void tudursguns$endEntity(LivingEntity entity, float yaw, float partialTick, PoseStack matrices,
			MultiBufferSource buffers, int light, CallbackInfo ci) {
		AimRenderState.setEntityBeingUpdatedAims(false);
		AimRenderState.setEntityBeingUpdatedSprintCarries(false);
	}
}
