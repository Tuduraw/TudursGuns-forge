package com.example.tudursguns.client.mixin;

import com.example.tudursguns.forge.client.SpecialItemModels;
import com.example.tudursvehiclemod.compat.mc.client.renderer.SubmitNodeCollector;
import com.example.tudursguns.compat.client.SpecialModelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge 1.20.1 only: draws stacks whose item definition is a special model (SpecialItemModels) the
 * way 1.21.4 does - the base model's display transform, then the special renderer, every frame.
 * getModel hands out the base model so callers use its GUI lighting and transforms. */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererSpecialMixin {

	@Inject(method = "getModel(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Lnet/minecraft/client/resources/model/BakedModel;",
			at = @At("HEAD"), cancellable = true)
	private void tudursguns$baseModel(ItemStack stack, Level level, LivingEntity entity, int seed, CallbackInfoReturnable<BakedModel> cir) {
		SpecialItemModels.Entry entry = SpecialItemModels.get(stack);
		if (entry != null) {
			cir.setReturnValue(Minecraft.getInstance().getModelManager().getModel(entry.base()));
		}
	}

	@Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
			at = @At("HEAD"), cancellable = true)
	private void tudursguns$renderSpecial(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack matrices,
			MultiBufferSource buffers, int light, int overlay, BakedModel model, CallbackInfo ci) {
		SpecialItemModels.Entry entry = SpecialItemModels.get(stack);
		if (entry == null) {
			return;
		}
		ci.cancel();
		matrices.pushPose();
		BakedModel base = Minecraft.getInstance().getModelManager().getModel(entry.base());
		base.applyTransform(displayContext, matrices, leftHand);
		matrices.translate(-0.5f, -0.5f, -0.5f);
		tudursguns$submit(entry.renderer(), stack, displayContext, matrices, buffers, light, overlay);
		matrices.popPose();
	}

	private static <T> void tudursguns$submit(SpecialModelRenderer<T> renderer, ItemStack stack, ItemDisplayContext displayContext,
			PoseStack matrices, MultiBufferSource buffers, int light, int overlay) {
		renderer.submit(renderer.extractArgument(stack), displayContext, matrices, new SubmitNodeCollector(buffers), light, overlay,
				stack.hasFoil(), 0);
	}
}
