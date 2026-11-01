package com.example.tudursguns.forge;

import com.example.tudursguns.compat.fabric.Disconnect;
import com.example.tudursguns.compat.fabric.EntityTrackingEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Fires the Fabric-style events Tudur's Guns needs beyond the vehicle mod's compatibility layer. */
public final class ForgeBridge {
	private ForgeBridge() {
	}

	public static void register(IEventBus forgeBus) {
		forgeBus.addListener((PlayerEvent.StartTracking e) -> {
			if (e.getEntity() instanceof ServerPlayer player) {
				EntityTrackingEvents.START_TRACKING.fire(e.getTarget(), player);
			}
		});
		forgeBus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
			if (e.getEntity() instanceof ServerPlayer player) {
				Disconnect.fire(player);
			}
		});
	}
}
