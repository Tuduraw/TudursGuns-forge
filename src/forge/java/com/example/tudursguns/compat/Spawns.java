package com.example.tudursguns.compat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** 1.20.1 registers spawn rules in Forge's SpawnPlacementRegisterEvent (SpawnPlacements.register is private);
 * the shared code's SpawnPlacements.register(...) calls are queued here and handed over from that event. */
public final class Spawns {
	private Spawns() {
	}

	private static final List<Consumer<SpawnPlacementRegisterEvent>> PENDING = new ArrayList<>();

	public static <T extends Mob> void onGround(EntityType<T> type, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
		PENDING.add(event -> event.register(type, SpawnPlacements.Type.ON_GROUND, heightmap, predicate,
				SpawnPlacementRegisterEvent.Operation.REPLACE));
	}

	public static void onRegister(SpawnPlacementRegisterEvent event) {
		PENDING.forEach(registration -> registration.accept(event));
	}
}
