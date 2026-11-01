package com.example.tudursguns.compat.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Forge 1.20.1 stand-in for 1.21.11's Gizmos.cuboid(box, stroke).setAlwaysOnTop(): box outlines
 * collected during a client tick, drawn (through walls) from the next tick until the one after. */
public final class Outlines {

	private Outlines() {
	}

	private record Box(AABB box, int argb) {
	}

	private static List<Box> pending = new ArrayList<>();
	private static List<Box> visible = List.of();

	public static void box(AABB box, int argb) {
		pending.add(new Box(box, argb));
	}

	/** Client tick start. */
	public static void beginTick() {
		visible = pending;
		pending = new ArrayList<>();
	}

	public static void render(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Vec3 camera) {
		if (visible.isEmpty()) {
			return;
		}
		VertexConsumer consumer = buffers.getBuffer(OnTopLines.TYPE);
		for (Box b : visible) {
			int a = (b.argb >>> 24) & 0xFF;
			LevelRenderer.renderLineBox(poseStack, consumer, b.box.move(-camera.x, -camera.y, -camera.z),
					((b.argb >> 16) & 0xFF) / 255f, ((b.argb >> 8) & 0xFF) / 255f, (b.argb & 0xFF) / 255f, a == 0 ? 1f : a / 255f);
		}
		buffers.endBatch(OnTopLines.TYPE);
	}

	private static final class OnTopLines extends RenderType {
		static final RenderType TYPE = RenderType.create("tudursguns_lines_on_top", DefaultVertexFormat.POSITION_COLOR_NORMAL,
				VertexFormat.Mode.LINES, 256, false, false, RenderType.CompositeState.builder()
						.setShaderState(RENDERTYPE_LINES_SHADER)
						.setLineState(new LineStateShard(OptionalDouble.of(2.0)))
						.setLayeringState(VIEW_OFFSET_Z_LAYERING)
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setWriteMaskState(COLOR_WRITE)
						.setCullState(NO_CULL)
						.setDepthTestState(NO_DEPTH_TEST)
						.createCompositeState(false));

		private OnTopLines(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling,
				boolean sort, Runnable setup, Runnable clear) {
			super(name, format, mode, size, crumbling, sort, setup, clear);
		}
	}
}
