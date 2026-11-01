package com.example.tudursguns.compat.client;

import com.example.tudursvehiclemod.compat.mc.client.DeltaTracker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.eventbus.api.IEventBus;

/** Forge 1.20.1 stand-in for Fabric's HudElementRegistry (the parts this mod uses). Elements become
 * GUI overlays (RegisterGuiOverlaysEvent); a replaced vanilla element is cancelled in
 * RenderGuiOverlayEvent.Pre and its replacement drawn instead, with the original still callable. */
public final class HudElementRegistry {

	private HudElementRegistry() {
	}

	@FunctionalInterface
	public interface HudElement {
		void render(GuiGraphics context, DeltaTracker tickCounter);
	}

	private record Entry(ResourceLocation id, ResourceLocation before, HudElement element) {
	}

	private static final List<Entry> ELEMENTS = new ArrayList<>();
	private static final Map<ResourceLocation, Function<HudElement, HudElement>> REPLACEMENTS = new HashMap<>();

	public static void addLast(ResourceLocation id, HudElement element) {
		ELEMENTS.add(new Entry(id, null, element));
	}

	public static void attachElementBefore(ResourceLocation before, ResourceLocation id, HudElement element) {
		ELEMENTS.add(new Entry(id, before, element));
	}

	public static void replaceElement(ResourceLocation id, Function<HudElement, HudElement> replacer) {
		REPLACEMENTS.merge(id, replacer, (a, b) -> original -> b.apply(a.apply(original)));
	}

	public static void register(IEventBus modBus, IEventBus forgeBus) {
		modBus.addListener((RegisterGuiOverlaysEvent e) -> {
			for (Entry entry : ELEMENTS) {
				net.minecraftforge.client.gui.overlay.IGuiOverlay overlay =
						(gui, graphics, partialTick, width, height) -> entry.element().render(graphics, new DeltaTracker(partialTick));
				if (entry.before() == null) {
					e.registerAboveAll(entry.id().getPath(), overlay);
				} else {
					e.registerBelow(entry.before(), entry.id().getPath(), overlay);
				}
			}
		});
		forgeBus.addListener((RenderGuiOverlayEvent.Pre e) -> {
			NamedGuiOverlay overlay = e.getOverlay();
			Function<HudElement, HudElement> replacer = REPLACEMENTS.get(overlay.id());
			if (replacer == null) {
				return;
			}
			e.setCanceled(true);
			Minecraft client = Minecraft.getInstance();
			int width = e.getWindow().getGuiScaledWidth();
			int height = e.getWindow().getGuiScaledHeight();
			HudElement original = (graphics, tick) -> overlay.overlay().render((ForgeGui) client.gui, graphics,
					tick.getGameTimeDeltaPartialTick(true), width, height);
			replacer.apply(original).render(e.getGuiGraphics(), new DeltaTracker(e.getPartialTick()));
		});
	}
}
