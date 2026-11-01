package com.example.tudursguns.network;

import com.example.tudursguns.TudursGuns;
import com.example.tudursvehiclemod.compat.mc.network.RegistryFriendlyByteBuf;
import com.example.tudursvehiclemod.compat.mc.network.codec.StreamCodec;
import com.example.tudursvehiclemod.compat.mc.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C: the shooter's own weapon kicked - raise the view by pitch degrees and turn it by yaw. */
public record RecoilPayload(float pitch, float yaw) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<RecoilPayload> ID = new CustomPacketPayload.Type<>(new net.minecraft.resources.ResourceLocation(TudursGuns.MOD_ID, "recoil"));

	public static final StreamCodec<RegistryFriendlyByteBuf, RecoilPayload> CODEC = StreamCodec.ofMember(
			(value, buf) -> {
				buf.writeFloat(value.pitch());
				buf.writeFloat(value.yaw());
			},
			buf -> new RecoilPayload(buf.readFloat(), buf.readFloat())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
