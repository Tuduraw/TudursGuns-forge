package com.example.tudursguns.network;

import com.example.tudursguns.TudursGuns;
import com.example.tudursvehiclemod.compat.mc.network.RegistryFriendlyByteBuf;
import com.example.tudursvehiclemod.compat.mc.network.codec.StreamCodec;
import com.example.tudursvehiclemod.compat.mc.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C: another player (by entity id) started or stopped holding the aim key. */
public record PlayerAimPayload(int entityId, boolean held) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PlayerAimPayload> ID =
			new CustomPacketPayload.Type<>(new net.minecraft.resources.ResourceLocation(TudursGuns.MOD_ID, "player_aim"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PlayerAimPayload> CODEC = StreamCodec.ofMember(
			(value, buf) -> {
				buf.writeVarInt(value.entityId());
				buf.writeBoolean(value.held());
			},
			buf -> new PlayerAimPayload(buf.readVarInt(), buf.readBoolean())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
