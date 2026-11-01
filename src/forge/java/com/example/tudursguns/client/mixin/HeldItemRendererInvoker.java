package com.example.tudursguns.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla's first-person arm drawing (the one used for holding a map with both hands) - it finds the
 * local player's skin and model itself. Forge 1.20.1 version (takes a buffer source). */
@Mixin(ItemInHandRenderer.class)
public interface HeldItemRendererInvoker {

	@Invoker("renderMapHand")
	void tudursguns$renderArm(PoseStack matrices, MultiBufferSource buffers, int light, HumanoidArm arm);
}
