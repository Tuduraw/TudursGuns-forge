package com.example.tudursguns.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Fabric API's PlayerLookup.around. */
public final class Players {
	private Players() {
	}

	public static List<ServerPlayer> around(ServerLevel level, Vec3 pos, double radius) {
		double r2 = radius * radius;
		return level.players().stream().filter(p -> p.distanceToSqr(pos) <= r2).toList();
	}
}
