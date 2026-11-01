package com.example.tudursguns.forge.client;

import com.example.tudursguns.client.render.ObjArmorFeatureRenderer;
import com.example.tudursguns.compat.client.HudElementRegistry;
import com.example.tudursguns.compat.client.Outlines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Client-only Forge events for what Tudur's Vehicle Mod's compatibility layer doesn't cover:
 * this mod's HUD elements, special item models, OBJ armor layers and mine-detector outlines. */
public final class TudursGunsForgeClient {

	private TudursGunsForgeClient() {
	}

	public static void register(IEventBus modBus, IEventBus forgeBus) {
		HudElementRegistry.register(modBus, forgeBus);
		modBus.addListener(SpecialItemModels::onRegisterAdditional);
		modBus.addListener(TudursGunsForgeClient::addArmorLayers);
		forgeBus.addListener((TickEvent.ClientTickEvent e) -> {
			if (e.phase == TickEvent.Phase.START) {
				Outlines.beginTick();
			}
		});
		forgeBus.addListener((RenderLevelStageEvent e) -> {
			if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
				Outlines.render(e.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), e.getCamera().getPosition());
			}
		});
	}

	/** OBJ armor on anything drawn with a humanoid model (players, armor stands, zombies, soldiers...). */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void addArmorLayers(EntityRenderersEvent.AddLayers event) {
		for (String skin : event.getSkins()) {
			addArmorLayer(event.getSkin(skin));
		}
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			Object renderer;
			try {
				renderer = event.getRenderer((EntityType) type);
			} catch (ClassCastException notLiving) {
				continue;
			}
			if (renderer instanceof LivingEntityRenderer<?, ?> living) {
				addArmorLayer(living);
			}
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void addArmorLayer(LivingEntityRenderer renderer) {
		if (renderer != null && renderer.getModel() instanceof HumanoidModel<?>) {
			renderer.addLayer(new ObjArmorFeatureRenderer(renderer));
		}
	}
}
