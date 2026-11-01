package com.example.tudursguns.client.render;

import com.example.tudursguns.TudursGuns;
import com.example.tudursguns.handheld.AmmoDefinition;
import com.example.tudursguns.handheld.ArmorDefinition;
import com.example.tudursguns.handheld.AttachmentDefinition;
import com.example.tudursguns.handheld.EquipmentDefinition;
import com.example.tudursguns.handheld.HandheldDefinition;
import com.example.tudursguns.handheld.MineDefinition;
import com.example.tudursguns.handheld.ModDefinitions;
import com.example.tudursguns.handheld.ThrowableDefinition;
import com.example.tudursguns.registry.ModComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import org.joml.Vector3fc;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import com.example.tudursvehiclemod.compat.mc.client.renderer.SubmitNodeCollector;
import com.example.tudursguns.compat.client.SpecialModelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws a data-defined item (one item model type per Kind: tudursguns:obj_attachment, obj_mine, ...)
 * from its definition's OBJ model, texture and display transforms, or its flat icon.
 *
 * OBJ armor is modelled facing +Z everywhere. In the "head" context (an OBJ-only helmet, drawn by
 * vanilla on the head like a carved pumpkin) vanilla's front is -Z, so the model is turned 180
 * degrees about the item's centre before display.head is applied. */
public class ObjDefinedItemRenderer implements SpecialModelRenderer<ResourceLocation> {

	/** What a definition says about its item model. */
	private record Look(Optional<ResourceLocation> model, Optional<ResourceLocation> texture,
			Map<ItemDisplayContext, HandheldDefinition.DisplayTransform> display, Optional<ResourceLocation> icon) {

		Look(Optional<ResourceLocation> model, Optional<ResourceLocation> texture, Map<ItemDisplayContext, HandheldDefinition.DisplayTransform> display) {
			this(model, texture, display, Optional.empty());
		}
	}

	public enum Kind {
		ATTACHMENT("obj_attachment"),
		THROWABLE("obj_throwable"),
		MINE("obj_mine"),
		ARMOR("obj_armor"),
		EQUIPMENT("obj_equipment"),
		AMMO("obj_ammo");

		public final ResourceLocation typeId;
		public final MapCodec<com.example.tudursguns.client.render.ObjDefinedItemRenderer.Unbaked> codec;

		Kind(String path) {
			this.typeId = new net.minecraft.resources.ResourceLocation(TudursGuns.MOD_ID, path);
			this.codec = MapCodec.unit(new com.example.tudursguns.client.render.ObjDefinedItemRenderer.Unbaked(this));
		}

		ResourceLocation idOf(ItemStack stack) {
			return switch (this) {
				case ATTACHMENT -> com.example.tudursguns.registry.ModComponents.ATTACHMENT.get(stack);
				case THROWABLE -> com.example.tudursguns.registry.ModComponents.THROWABLE.get(stack);
				case MINE -> com.example.tudursguns.registry.ModComponents.MINE.get(stack);
				case ARMOR -> com.example.tudursguns.registry.ModComponents.ARMOR.get(stack);
				case EQUIPMENT -> com.example.tudursguns.registry.ModComponents.EQUIPMENT.get(stack);
				case AMMO -> com.example.tudursguns.registry.ModComponents.AMMO_TYPE.get(stack);
			};
		}

		Look look(ResourceLocation id) {
			return switch (this) {
				case ATTACHMENT -> {
					AttachmentDefinition def = ModDefinitions.ATTACHMENTS.getAny(id);
					yield def == null ? null : new Look(def.model(), def.texture(), def.display());
				}
				case THROWABLE -> {
					ThrowableDefinition def = ModDefinitions.THROWABLES.getAny(id);
					yield def == null ? null : new Look(def.model(), def.texture(), def.display());
				}
				case MINE -> {
					MineDefinition def = ModDefinitions.MINES.getAny(id);
					yield def == null ? null : new Look(def.model(), def.texture(), def.display());
				}
				case ARMOR -> {
					ArmorDefinition def = ModDefinitions.ARMOR.getAny(id);
					yield def == null ? null : new Look(def.model(), def.texture(), def.display());
				}
				case EQUIPMENT -> {
					EquipmentDefinition def = ModDefinitions.EQUIPMENT.getAny(id);
					yield def == null ? null : new Look(def.model(), def.texture(), def.display());
				}
				case AMMO -> {
					AmmoDefinition def = ModDefinitions.AMMO.getAny(id);
					yield def == null ? null : new Look(def.model(), def.texture(), def.display(), def.icon());
				}
			};
		}
	}

	private final Kind kind;

	public ObjDefinedItemRenderer(Kind kind) {
		this.kind = kind;
	}

	@Override
	public ResourceLocation extractArgument(ItemStack stack) {
		return this.kind.idOf(stack);
	}

	@Override
	public void submit(ResourceLocation id, ItemDisplayContext displayContext, PoseStack matrices,
			SubmitNodeCollector queue, int light, int overlay, boolean glint, int outlineColor) {
		Look look = id == null ? null : this.kind.look(id);
		if (look == null) {
			return;
		}
		// A flat icon in inventories - or everywhere, when there's no model.
		boolean noModel = look.model().isEmpty() || look.texture().isEmpty();
		if (look.icon().isPresent() && (displayContext == ItemDisplayContext.GUI || noModel)) {
			matrices.pushPose();
			if (displayContext != ItemDisplayContext.GUI) {
				WeaponModelDrawer.applyTransform(matrices, look.display().getOrDefault(displayContext, HandheldDefinition.DisplayTransform.IDENTITY));
			}
			WeaponModelDrawer.drawIcon(queue, matrices, look.icon().get(), light, overlay);
			matrices.popPose();
			return;
		}
		if (noModel) {
			return;
		}
		matrices.pushPose();
		if (this.kind == Kind.ARMOR && displayContext == ItemDisplayContext.HEAD) {
			// Vanilla draws a head-slot item with its front towards -Z (like a carved pumpkin's face);
			// OBJ armor is modelled with +Z forward (as for "worn"), so turn it round the item's centre.
			matrices.translate(0.5, 0.0, 0.5);
			matrices.mulPose(Axis.YP.rotationDegrees(180f));
			matrices.translate(-0.5, 0.0, -0.5);
		}
		WeaponModelDrawer.applyTransform(matrices, look.display().getOrDefault(displayContext, HandheldDefinition.DisplayTransform.IDENTITY));
		WeaponModelDrawer.drawObj(queue, matrices, look.model().get(), look.texture().get(), Set.of(), light, overlay);
		matrices.popPose();
	}

	@Override
	public void getExtents(Consumer<Vector3fc> consumer) {
		WeaponModelDrawer.unitCube(consumer);
	}

	public record Unbaked(Kind kind) implements SpecialModelRenderer.Unbaked {

		@Override
		public MapCodec<com.example.tudursguns.client.render.ObjDefinedItemRenderer.Unbaked> type() {
			return this.kind.codec;
		}

		@Override
		public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
			return new ObjDefinedItemRenderer(this.kind);
		}
	}
}
