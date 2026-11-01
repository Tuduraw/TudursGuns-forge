package com.example.tudursguns.compat.fabric;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** Fabric API's EntityTrackingEvents.START_TRACKING, fired from forge.ForgeBridge (PlayerEvent.StartTracking). */
public final class EntityTrackingEvents {
	private EntityTrackingEvents() {
	}

	public interface StartTracking {
		void onStartTracking(Entity trackedEntity, ServerPlayer player);
	}

	public static final Event START_TRACKING = new Event();

	public static final class Event {
		private final List<StartTracking> listeners = new ArrayList<>();

		public void register(StartTracking listener) {
			this.listeners.add(listener);
		}

		public void fire(Entity entity, ServerPlayer player) {
			for (StartTracking listener : this.listeners) {
				listener.onStartTracking(entity, player);
			}
		}
	}
}
