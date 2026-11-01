package com.example.tudursguns.client.mixin;

import com.example.tudursguns.client.render.AimRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player (or soldier) aiming a handheld weapon holds it up with both hands, using vanilla's
 * loaded-crossbow pose on the main arm. A player sprinting with one carries it across the body
 * instead. Forge 1.20.1 version: setupAnim takes the entity itself. */
@Mixin(HumanoidModel.class)
public abstract class BipedEntityModelAimMixin {

	@Shadow
	public HumanoidModel.ArmPose leftArmPose;
	@Shadow
	public HumanoidModel.ArmPose rightArmPose;

	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
	private void tudursguns$raiseWeapon(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
			float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (!AimRenderState.aims(entity)) {
			return;
		}
		if (entity.getMainArm() == HumanoidArm.LEFT) {
			this.leftArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
		} else {
			this.rightArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
		}
	}

	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void tudursguns$sprintCarry(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
			float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (AimRenderState.aims(entity) || !AimRenderState.sprintCarries(entity)) {
			return;
		}
		HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
		// Main arm low and turned in, the other arm reaching across to hold the fore-end.
		boolean left = entity.getMainArm() == HumanoidArm.LEFT;
		ModelPart main = left ? model.leftArm : model.rightArm;
		ModelPart support = left ? model.rightArm : model.leftArm;
		float side = left ? -1f : 1f;
		main.xRot = -0.7f;
		main.yRot = -0.45f * side;
		main.zRot = 0f;
		support.xRot = -1.0f;
		support.yRot = 0.75f * side;
		support.zRot = 0f;
	}
}
