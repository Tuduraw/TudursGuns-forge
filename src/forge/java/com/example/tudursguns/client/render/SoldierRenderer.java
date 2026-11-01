package com.example.tudursguns.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Soldiers (posted and enemy) - the player model, with a skin of their own, vanilla armor and the
 * held weapon (OBJ armor is added for every humanoid renderer, see forge.client.TudursGunsForgeClient).
 * Forge 1.20.1 version. */
public class SoldierRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {

	private final ResourceLocation texture;

	public SoldierRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.texture = texture;
		this.addLayer(new HumanoidArmorLayer<>(this,
				new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
				new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
				context.getModelManager()));
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return this.texture;
	}
}
