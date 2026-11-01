package com.example.tudursguns.client.render;

import com.example.tudursguns.handheld.ArmorDefinition;
import com.example.tudursguns.handheld.ModDefinitions;
import com.example.tudursguns.item.ArmorItem;
import com.example.tudursguns.registry.ModComponents;
import com.example.tudursvehiclemod.client.render.ObjModel;
import com.example.tudursvehiclemod.client.render.ObjModelLoader;
import com.example.tudursvehiclemod.compat.mc.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Set;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Draws OBJ armor (definitions with a model) on anything with a humanoid model - players, armor
 * stands, zombies, soldiers... Forge 1.20.1 version of the Fabric class (same drawing; 1.20.1's
 * RenderLayer takes the entity instead of a render state).
 *
 * Each piece is drawn in the space of a body part: origin at the part's pivot, +Y up, +Z the way the
 * wearer faces, 1 = a block - then moved by the definition's "worn" transform.
 * - A model with groups named after body parts (head, body, right_arm, left_arm, right_leg,
 *   left_leg) has each of those groups drawn on its part; other groups aren't drawn.
 * - Any other model is drawn whole on its slot's part: the head, the body, or each leg (legs, feet).
 *
 * Vanilla draws nothing for these: they aren't vanilla ArmorItems (HumanoidArmorLayer skips them)
 * and CustomHeadLayerMixin keeps a helmet from being drawn as an item on the head. */
public class ObjArmorFeatureRenderer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

	private static final List<String> PART_GROUPS = List.of("head", "body", "right_arm", "left_arm", "right_leg", "left_leg");

	public ObjArmorFeatureRenderer(RenderLayerParent<T, M> context) {
		super(context);
	}

	@Override
	public void render(PoseStack matrices, MultiBufferSource buffers, int light, T entity, float limbSwing,
			float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		M model = this.getParentModel();
		SubmitNodeCollector queue = new SubmitNodeCollector(buffers);
		draw(matrices, queue, light, model, entity.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD);
		draw(matrices, queue, light, model, entity.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST);
		draw(matrices, queue, light, model, entity.getItemBySlot(EquipmentSlot.LEGS), EquipmentSlot.LEGS);
		draw(matrices, queue, light, model, entity.getItemBySlot(EquipmentSlot.FEET), EquipmentSlot.FEET);
	}

	private static void draw(PoseStack matrices, SubmitNodeCollector queue, int light, HumanoidModel<?> model, ItemStack stack,
			EquipmentSlot slot) {
		if (stack == null || !(stack.getItem() instanceof ArmorItem)) {
			return;
		}
		ArmorDefinition def = ModDefinitions.ARMOR.getAny(ModComponents.ARMOR.get(stack));
		if (def == null || def.slot() != slot || def.model().isEmpty() || def.texture().isEmpty()) {
			return;
		}
		ObjModel obj = ObjModelLoader.get(def.model().get()).orElse(null);
		Set<String> groups = obj != null ? obj.getGroupNames() : Set.of();
		boolean split = PART_GROUPS.stream().anyMatch(groups::contains);
		if (split) {
			for (String group : PART_GROUPS) {
				if (groups.contains(group)) {
					drawOn(matrices, queue, light, def, part(model, group), group);
				}
			}
			return;
		}
		switch (slot) {
			case HEAD -> drawOn(matrices, queue, light, def, model.head, null);
			case CHEST -> drawOn(matrices, queue, light, def, model.body, null);
			default -> {
				drawOn(matrices, queue, light, def, model.rightLeg, null);
				drawOn(matrices, queue, light, def, model.leftLeg, null);
			}
		}
	}

	private static ModelPart part(HumanoidModel<?> model, String group) {
		return switch (group) {
			case "head" -> model.head;
			case "body" -> model.body;
			case "right_arm" -> model.rightArm;
			case "left_arm" -> model.leftArm;
			case "right_leg" -> model.rightLeg;
			default -> model.leftLeg;
		};
	}

	/** The whole model (group null) or one group, in part's space. */
	private static void drawOn(PoseStack matrices, SubmitNodeCollector queue, int light, ArmorDefinition def, ModelPart part, String group) {
		if (!part.visible) {
			return;
		}
		matrices.pushPose();
		part.translateAndRotate(matrices);
		// Model space is upside down and facing -Z; turn it to +Y up, +Z forward.
		matrices.mulPose(Axis.XP.rotationDegrees(180f));
		WeaponModelDrawer.applyTransform(matrices, def.worn());
		if (group == null) {
			WeaponModelDrawer.drawObj(queue, matrices, def.model().get(), def.texture().get(), Set.of(), light, OverlayTexture.NO_OVERLAY);
		} else {
			WeaponModelDrawer.drawGroups(queue, matrices, def.model().get(), def.texture().get(), List.of(group), Set.of(), light,
					OverlayTexture.NO_OVERLAY);
		}
		matrices.popPose();
	}
}
