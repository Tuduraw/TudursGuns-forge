package com.example.tudursguns.forge.client;

import com.example.tudursguns.TudursGuns;
import com.example.tudursguns.compat.client.SpecialModelRenderer;
import com.example.tudursguns.compat.client.SpecialModelRenderers;
import com.example.tudursguns.handheld.ArmorDefinition;
import com.example.tudursguns.handheld.ModDefinitions;
import com.example.tudursguns.item.ArmorItem;
import com.example.tudursguns.registry.ModComponents;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ModelEvent;

/** 1.21.4 item definitions (assets/<ns>/items/<id>.json) for 1.20.1, as far as this mod needs them.
 *
 * - A "minecraft:special" model, at the top or as the on_true branch of a "minecraft:has_component"
 *   condition: ItemRendererSpecialMixin draws the stack with the special renderer, using the special
 *   model's "base" model for display transforms and GUI lighting.
 * - The stack's item model override (1.21.4's minecraft:item_model component; this mod sets it from an
 *   armor definition's "item_model"): the named definition replaces the item's own - its plain model
 *   ("minecraft:model", or a condition's on_false branch) is used.
 * - Anything else falls back to models/item/<item>.json as usual.
 *
 * Read in ModelEvent.RegisterAdditional, which also loads every model named here. */
public final class SpecialItemModels {

	private SpecialItemModels() {
	}

	/** component: the item-data key that must be present (null: always). */
	public record Special(String component, ResourceLocation base, SpecialModelRenderer<?> renderer) {
		boolean appliesTo(ItemStack stack) {
			return component == null || (stack.getTag() != null && stack.getTag().contains(component));
		}
	}

	/** One item definition: its special model (or null) and the model shown otherwise (or null). */
	private record Definition(Special special, ResourceLocation plain) {
	}

	/** What to draw a stack with: a special renderer, or just another baked model. */
	public record Resolved(Special special, ResourceLocation model) {
		public BakedModel bakedModel() {
			return Minecraft.getInstance().getModelManager().getModel(special != null ? special.base() : model);
		}
	}

	private static volatile Map<ResourceLocation, Definition> definitions = Map.of();

	/** How stack is drawn, or null for vanilla's usual model. */
	public static Resolved resolve(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		ResourceLocation override = itemModelOverride(stack);
		Definition def = definitions.get(override != null ? override : BuiltInRegistries.ITEM.getKey(stack.getItem()));
		if (def == null) {
			return null;
		}
		if (def.special() != null && def.special().appliesTo(stack)) {
			return new Resolved(def.special(), null);
		}
		return override != null && def.plain() != null ? new Resolved(null, def.plain()) : null;
	}

	/** 1.21.4's minecraft:item_model component as this mod sets it: an armor definition's item_model. */
	private static ResourceLocation itemModelOverride(ItemStack stack) {
		if (!(stack.getItem() instanceof ArmorItem)) {
			return null;
		}
		ResourceLocation id = ModComponents.ARMOR.get(stack);
		ArmorDefinition def = id == null ? null : ModDefinitions.ARMOR.getAny(id);
		return def == null ? null : def.itemModel().orElse(null);
	}

	public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
		ResourceManager manager = Minecraft.getInstance().getResourceManager();
		Map<ResourceLocation, Definition> found = new HashMap<>();
		for (Map.Entry<ResourceLocation, Resource> file : manager.listResources("items", id -> id.getPath().endsWith(".json")).entrySet()) {
			ResourceLocation fileId = file.getKey();
			String path = fileId.getPath().substring("items/".length(), fileId.getPath().length() - ".json".length());
			try (Reader reader = file.getValue().openAsReader()) {
				JsonElement root = JsonParser.parseReader(reader);
				if (!root.isJsonObject() || !root.getAsJsonObject().has("model")) {
					continue;
				}
				JsonObject model = root.getAsJsonObject().getAsJsonObject("model");
				Definition def = new Definition(special(model, null), plain(model));
				if (def.special() == null && def.plain() == null) {
					continue;
				}
				found.put(new ResourceLocation(fileId.getNamespace(), path), def);
				if (def.special() != null) {
					event.register(def.special().base());
				}
				if (def.plain() != null) {
					event.register(def.plain());
				}
			} catch (Exception e) {
				TudursGuns.LOGGER.warn("Couldn't read item definition {}: {}", fileId, e.toString());
			}
		}
		definitions = Map.copyOf(found);
	}

	private static String type(JsonObject model) {
		return model.has("type") ? model.get("type").getAsString() : "";
	}

	private static ResourceLocation plain(JsonObject model) {
		return switch (type(model)) {
			case "minecraft:model" -> model.has("model") ? new ResourceLocation(model.get("model").getAsString()) : null;
			case "minecraft:condition" -> model.has("on_false") && model.get("on_false").isJsonObject()
					? plain(model.getAsJsonObject("on_false")) : null;
			default -> null;
		};
	}

	private static Special special(JsonObject model, String component) {
		String type = type(model);
		if (type.equals("minecraft:condition") && component == null
				&& "minecraft:has_component".equals(model.has("property") ? model.get("property").getAsString() : null)
				&& model.has("on_true") && model.get("on_true").isJsonObject()) {
			return special(model.getAsJsonObject("on_true"), model.get("component").getAsString());
		}
		if (!type.equals("minecraft:special") || !model.has("model") || !model.has("base")) {
			return null;
		}
		JsonObject special = model.getAsJsonObject("model");
		MapCodec<? extends SpecialModelRenderer.Unbaked> codec =
				SpecialModelRenderers.ID_MAPPER.get(new ResourceLocation(special.get("type").getAsString()));
		if (codec == null) {
			return null;
		}
		SpecialModelRenderer.Unbaked unbaked = codec.codec().parse(JsonOps.INSTANCE, special).result().orElse(null);
		if (unbaked == null) {
			return null;
		}
		return new Special(component, new ResourceLocation(model.get("base").getAsString()),
				unbaked.bake(SpecialModelRenderer.BakingContext.INSTANCE));
	}
}
