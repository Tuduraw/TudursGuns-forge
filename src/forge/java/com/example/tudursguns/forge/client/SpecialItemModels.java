package com.example.tudursguns.forge.client;

import com.example.tudursguns.TudursGuns;
import com.example.tudursguns.compat.client.SpecialModelRenderer;
import com.example.tudursguns.compat.client.SpecialModelRenderers;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ModelEvent;

/** 1.21.4 item definitions (assets/<ns>/items/<item>.json) for 1.20.1, as far as this mod needs them:
 * a "minecraft:special" model, at the top or as the on_true branch of a "minecraft:has_component"
 * condition. Everything else falls back to models/item/<item>.json as usual (an on_false branch is
 * expected to name that model).
 *
 * Read in ModelEvent.RegisterAdditional, which also loads each special model's "base" model (its
 * display transforms and GUI lighting). ItemRendererSpecialMixin then draws such stacks. */
public final class SpecialItemModels {

	private SpecialItemModels() {
	}

	/** component: the item-data key that must be present (null: always). */
	public record Entry(String component, ResourceLocation base, SpecialModelRenderer<?> renderer) {
		public boolean appliesTo(ItemStack stack) {
			return component == null || (stack.getTag() != null && stack.getTag().contains(component));
		}
	}

	private static volatile Map<Item, Entry> entries = Map.of();

	public static Entry get(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		Entry entry = entries.get(stack.getItem());
		return entry != null && entry.appliesTo(stack) ? entry : null;
	}

	public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
		ResourceManager manager = Minecraft.getInstance().getResourceManager();
		Map<Item, Entry> found = new HashMap<>();
		for (Map.Entry<ResourceLocation, Resource> file : manager.listResources("items", id -> id.getPath().endsWith(".json")).entrySet()) {
			ResourceLocation fileId = file.getKey();
			String path = fileId.getPath().substring("items/".length(), fileId.getPath().length() - ".json".length());
			ResourceLocation itemId = new ResourceLocation(fileId.getNamespace(), path);
			Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
			if (item == null) {
				continue;
			}
			try (Reader reader = file.getValue().openAsReader()) {
				JsonElement root = JsonParser.parseReader(reader);
				Entry entry = root.isJsonObject() && root.getAsJsonObject().has("model")
						? parse(root.getAsJsonObject().getAsJsonObject("model"), null) : null;
				if (entry != null) {
					found.put(item, entry);
					event.register(entry.base());
				}
			} catch (Exception e) {
				TudursGuns.LOGGER.warn("Couldn't read item definition {}: {}", fileId, e.toString());
			}
		}
		entries = Map.copyOf(found);
	}

	private static Entry parse(JsonObject model, String component) {
		String type = model.has("type") ? model.get("type").getAsString() : "";
		if (type.equals("minecraft:condition") && component == null
				&& "minecraft:has_component".equals(model.has("property") ? model.get("property").getAsString() : null)
				&& model.has("on_true") && model.get("on_true").isJsonObject()) {
			return parse(model.getAsJsonObject("on_true"), model.get("component").getAsString());
		}
		if (!type.equals("minecraft:special") || !model.has("model") || !model.has("base")) {
			return null;
		}
		JsonObject special = model.getAsJsonObject("model");
		ResourceLocation specialType = new ResourceLocation(special.get("type").getAsString());
		MapCodec<? extends SpecialModelRenderer.Unbaked> codec = SpecialModelRenderers.ID_MAPPER.get(specialType);
		if (codec == null) {
			return null;
		}
		SpecialModelRenderer.Unbaked unbaked = codec.codec().parse(JsonOps.INSTANCE, special).result().orElse(null);
		if (unbaked == null) {
			return null;
		}
		return new Entry(component, new ResourceLocation(model.get("base").getAsString()),
				unbaked.bake(SpecialModelRenderer.BakingContext.INSTANCE));
	}
}
