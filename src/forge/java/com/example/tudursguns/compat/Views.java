package com.example.tudursguns.compat;

import com.example.tudursvehiclemod.compat.mc.world.level.storage.ValueInput;
import com.example.tudursvehiclemod.compat.mc.world.level.storage.ValueOutput;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.Optional;

/** 1.21.6's ValueInput.read / ValueOutput.store(key, codec, value) on the vehicle mod's tag-backed views
 * (rules.json rewrites view.read(...) and view.store(...) to these). */
public final class Views {
	private Views() {
	}

	public static <T> Optional<T> read(ValueInput view, String key, Codec<T> codec) {
		Tag tag = view.tag().get(key);
		return tag == null ? Optional.empty() : codec.parse(NbtOps.INSTANCE, tag).result();
	}

	public static <T> void store(ValueOutput view, String key, Codec<T> codec, T value) {
		codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(tag -> view.tag().put(key, tag));
	}

	public static <T> void storeNullable(ValueOutput view, String key, Codec<T> codec, T value) {
		if (value != null) {
			store(view, key, codec, value);
		}
	}
}
