package com.example.tudursguns.forge;

import com.example.tudursguns.TudursGuns;
import com.example.tudursvehiclemod.compat.Registration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Forge entry point, built like Tudur's Vehicle Mod's (TudursVehicleModForge). The shared (Fabric-shaped)
 * initializers run once, at the first RegisterEvent - Forge only lets items, blocks and entity types be
 * made while registration is open. Their Registry.register(...) calls go to the vehicle mod's
 * compat.Registration queue, handed to Forge in each registry's RegisterEvent; the Fabric-style events and
 * the network channel are the vehicle mod's compatibility layer, fed by its Forge event bridges.
 */
@Mod(TudursGuns.MOD_ID)
public final class TudursGunsForge {
	private static boolean initialized;

	public TudursGunsForge() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(EventPriority.HIGH, (RegisterEvent e) -> {
			if (!initialized) {
				initialized = true;
				new TudursGuns().onInitialize();
				if (FMLEnvironment.dist == Dist.CLIENT) {
					ClientInit.initClient();
				}
			}
			Registration.onRegister(e);
		});
		ForgeBridge.register(MinecraftForge.EVENT_BUS);
		if (FMLEnvironment.dist == Dist.CLIENT) {
			ClientInit.registerEvents(modBus);
		}
		modBus.addListener(com.example.tudursguns.compat.Spawns::onRegister);
		BIOME_MODIFIERS.register("enemy_soldier_spawns", () -> EnemySoldierSpawns.CODEC);
		BIOME_MODIFIERS.register(modBus);
	}

	private static final net.minecraftforge.registries.DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.world.BiomeModifier>> BIOME_MODIFIERS =
			net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, TudursGuns.MOD_ID);

	/** Separate class so the dedicated server never loads client classes. */
	private static final class ClientInit {
		static void initClient() {
			new com.example.tudursguns.client.TudursGunsClient().onInitializeClient();
		}

		static void registerEvents(IEventBus modBus) {
			com.example.tudursguns.forge.client.TudursGunsForgeClient.register(modBus, MinecraftForge.EVENT_BUS);
		}
	}
}
