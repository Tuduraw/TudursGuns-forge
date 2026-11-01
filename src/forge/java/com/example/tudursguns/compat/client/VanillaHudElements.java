package com.example.tudursguns.compat.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/** Forge 1.20.1 stand-in for Fabric's VanillaHudElements: the vanilla overlays' ids. */
public final class VanillaHudElements {

	private VanillaHudElements() {
	}

	public static final ResourceLocation CROSSHAIR = VanillaGuiOverlay.CROSSHAIR.id();
}
