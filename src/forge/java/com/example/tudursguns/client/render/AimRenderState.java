package com.example.tudursguns.client.render;

import com.example.tudursguns.client.AimController;
import com.example.tudursguns.soldier.Soldier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Forge 1.20.1 version (1.20.1 has no entity render states): whether the living entity being drawn
 * aims / sprint-carries a handheld weapon. LivingEntityRendererAimMixin sets it around each
 * LivingEntityRenderer.render, during which the held items are drawn (so ObjHandheldModelRenderer
 * can read it); BipedEntityModelAimMixin asks the entity directly. Render thread only. */
public final class AimRenderState {

	private AimRenderState() {
	}

	private static boolean entityBeingUpdatedAims;
	private static boolean entityBeingUpdatedSprintCarries;

	public static boolean aims(LivingEntity entity) {
		return entity instanceof Player player ? AimController.isAiming(player)
				: entity instanceof Soldier soldier && soldier.isAimingWeapon();
	}

	public static boolean sprintCarries(LivingEntity entity) {
		return entity instanceof Player player && AimController.isSprintCarrying(player);
	}

	public static boolean entityBeingUpdatedSprintCarries() {
		return entityBeingUpdatedSprintCarries;
	}

	public static void setEntityBeingUpdatedSprintCarries(boolean carries) {
		entityBeingUpdatedSprintCarries = carries;
	}

	public static boolean entityBeingUpdatedAims() {
		return entityBeingUpdatedAims;
	}

	public static void setEntityBeingUpdatedAims(boolean aims) {
		entityBeingUpdatedAims = aims;
	}
}
