package com.example.tudursguns.client.mixin;

import com.example.tudursguns.forge.client.SpecialItemModels;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge 1.20.1 only: a special-model item (an OBJ) drawn in a GUI is clipped to its 16x16 slot.
 * 1.21.6+ draws every GUI item into a 16x16 cell of an atlas, so anything beyond the slot is cut off
 * there; without this, a model larger than its slot would spill over its neighbours here. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsItemClipMixin {

	@Unique
	private int tudursguns$clipDepth;

	@Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
			at = @At("HEAD"))
	private void tudursguns$clipStart(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, int guiOffset, CallbackInfo ci) {
		if (tudursguns$clips(stack)) {
			GuiGraphics self = (GuiGraphics) (Object) this;
			Matrix4f pose = self.pose().last().pose();
			int left = Math.round(x + pose.m30());
			int top = Math.round(y + pose.m31());
			self.flush();
			self.enableScissor(left, top, left + 16, top + 16);
		}
	}

	@Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
			at = @At("RETURN"))
	private void tudursguns$clipEnd(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, int guiOffset, CallbackInfo ci) {
		if (tudursguns$clipDepth > 0) {
			GuiGraphics self = (GuiGraphics) (Object) this;
			self.flush();
			self.disableScissor();
			tudursguns$clipDepth--;
		}
	}

	@Unique
	private boolean tudursguns$clips(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		SpecialItemModels.Resolved resolved = SpecialItemModels.resolve(stack);
		if (resolved == null || resolved.special() == null) {
			return false;
		}
		tudursguns$clipDepth++;
		return true;
	}
}
