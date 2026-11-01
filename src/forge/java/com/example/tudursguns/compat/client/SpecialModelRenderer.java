package com.example.tudursguns.compat.client;

import com.example.tudursvehiclemod.compat.mc.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;

/** Forge 1.20.1 stand-in for 1.21.4's special item model renderer. 1.20.1 has no item definitions
 * (assets/<ns>/items/*.json); forge.client.SpecialItemModels reads them and ItemRendererSpecialMixin
 * calls these renderers in place of the item's baked model. */
public interface SpecialModelRenderer<T> {

	T extractArgument(ItemStack stack);

	void submit(T argument, ItemDisplayContext displayContext, PoseStack matrices, SubmitNodeCollector queue,
			int light, int overlay, boolean glint, int outlineColor);

	default void getExtents(Consumer<Vector3fc> consumer) {
	}

	interface Unbaked {
		MapCodec<? extends Unbaked> type();

		SpecialModelRenderer<?> bake(BakingContext context);
	}

	final class BakingContext {
		public static final BakingContext INSTANCE = new BakingContext();

		private BakingContext() {
		}
	}
}
