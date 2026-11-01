package com.example.tudursguns.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** 1.20.5+ attribute modifiers are identified by a ResourceLocation; 1.20.1's by a UUID (and a name).
 * The UUID is derived from the id, so the same modifier always gets the same one. */
public final class Attrs {
	private Attrs() {
	}

	public static UUID uuid(ResourceLocation id) {
		return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
	}

	public static AttributeModifier modifier(ResourceLocation id, double amount, AttributeModifier.Operation operation) {
		return new AttributeModifier(uuid(id), id.toString(), amount, operation);
	}
}
