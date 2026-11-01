package com.example.tudursguns.compat.client;

import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Forge 1.20.1 stand-in for 1.21.4's SpecialModelRenderers: the special model types by id. */
public final class SpecialModelRenderers {

	private SpecialModelRenderers() {
	}

	public static final IdMapper ID_MAPPER = new IdMapper();

	public static final class IdMapper {
		private final Map<ResourceLocation, MapCodec<? extends SpecialModelRenderer.Unbaked>> codecs = new HashMap<>();

		public void put(ResourceLocation id, MapCodec<? extends SpecialModelRenderer.Unbaked> codec) {
			codecs.put(id, codec);
		}

		public MapCodec<? extends SpecialModelRenderer.Unbaked> get(ResourceLocation id) {
			return codecs.get(id);
		}
	}
}
