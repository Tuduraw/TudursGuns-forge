package com.example.tudursguns.compat;

import com.example.tudursvehiclemod.compat.mc.network.codec.ByteBufCodecs;
import com.example.tudursvehiclemod.compat.mc.network.codec.StreamCodec;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Codecs Tudur's Guns uses that 1.20.1 and the vehicle mod's compat layer don't have. */
public final class Codecs {
	private Codecs() {
	}

	public static final StreamCodec<FriendlyByteBuf, Long> VAR_LONG = StreamCodec.of(FriendlyByteBuf::writeVarLong, FriendlyByteBuf::readVarLong);

	/** 1.20.5's EquipmentSlot.CODEC ("mainhand", "head", ...). */
	public static final Codec<EquipmentSlot> EQUIPMENT_SLOT = Codec.STRING.xmap(
			name -> EquipmentSlot.byName(name.toLowerCase(Locale.ROOT)), EquipmentSlot::getName);

	/** ByteBufCodecs.list(max): the size limit isn't enforced (the server checks what it receives anyway). */
	public static <B extends FriendlyByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int max) {
		return ByteBufCodecs.list();
	}

	public static <B extends FriendlyByteBuf, V> StreamCodec<B, Optional<V>> optional(StreamCodec<? super B, V> codec) {
		return StreamCodec.of((buf, value) -> {
			buf.writeBoolean(value.isPresent());
			value.ifPresent(v -> codec.encode(buf, v));
		}, buf -> buf.readBoolean() ? Optional.of(codec.decode(buf)) : Optional.empty());
	}

	/** Sends a value as NBT, encoded with its codec. */
	public static <T> StreamCodec<FriendlyByteBuf, T> fromCodec(Codec<T> codec) {
		return StreamCodec.of((buf, value) -> {
			CompoundTag holder = new CompoundTag();
			holder.put("v", codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, error -> {
			}));
			buf.writeNbt(holder);
		}, buf -> {
			CompoundTag holder = buf.readNbt();
			Tag tag = holder == null ? null : holder.get("v");
			return codec.parse(NbtOps.INSTANCE, tag).getOrThrow(false, error -> {
			});
		});
	}
}
