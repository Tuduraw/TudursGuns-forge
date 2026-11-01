package com.example.tudursguns.client.mixin;

import com.example.tudursguns.item.ArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge 1.20.1 only: vanilla draws any head-slot item that isn't a vanilla helmet as an item on the
 * head. This mod's helmets are drawn by ObjArmorFeatureRenderer instead (on 1.21.11 the empty
 * equipment asset does this). */
@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin {

	@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
			at = @At("HEAD"), cancellable = true)
	private void tudursguns$skipObjHelmet(PoseStack matrices, MultiBufferSource buffers, int light, LivingEntity entity,
			float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch,
			CallbackInfo ci) {
		if (entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ArmorItem) {
			ci.cancel();
		}
	}
}
