package com.example.tudursguns.compat.fabric;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Fabric API's ServerPlayConnectionEvents.DISCONNECT, fired from forge.ForgeBridge (PlayerLoggedOutEvent).
 * The handler argument is the player itself here (listeners only use handler.player). */
public final class Disconnect {
	private Disconnect() {
	}

	public interface Listener {
		void onPlayDisconnect(Handler handler, MinecraftServer server);
	}

	public static final class Handler {
		public final ServerPlayer player;

		Handler(ServerPlayer player) {
			this.player = player;
		}
	}

	private static final List<Listener> LISTENERS = new ArrayList<>();

	public static void register(Listener listener) {
		LISTENERS.add(listener);
	}

	public static void fire(ServerPlayer player) {
		for (Listener listener : LISTENERS) {
			listener.onPlayDisconnect(new Handler(player), player.server);
		}
	}
}
