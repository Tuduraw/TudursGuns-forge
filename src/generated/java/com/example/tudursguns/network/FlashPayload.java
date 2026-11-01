package com.example.tudursguns.network;

import com.example.tudursguns.TudursGuns;
import com.example.tudursvehiclemod.compat.mc.network.RegistryFriendlyByteBuf;
import com.example.tudursvehiclemod.compat.mc.network.codec.StreamCodec;
import com.example.tudursvehiclemod.compat.mc.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C: a flash went off in view - white out the screen at intensity (0-1), fading over durationTicks. */
public record FlashPayload(float intensity, int durationTicks) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<FlashPayload> ID = new CustomPacketPayload.Type<>(new net.minecraft.resources.ResourceLocation(TudursGuns.MOD_ID, "flash"));

	public static final StreamCodec<RegistryFriendlyByteBuf, FlashPayload> CODEC = StreamCodec.ofMember(
			(value, buf) -> {
				buf.writeFloat(value.intensity());
				buf.writeVarInt(value.durationTicks());
			},
			buf -> new FlashPayload(buf.readFloat(), buf.readVarInt())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
