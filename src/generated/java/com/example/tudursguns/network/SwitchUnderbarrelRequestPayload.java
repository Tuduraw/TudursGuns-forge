package com.example.tudursguns.network;

import com.example.tudursguns.TudursGuns;
import com.example.tudursvehiclemod.compat.mc.network.RegistryFriendlyByteBuf;
import com.example.tudursvehiclemod.compat.mc.network.codec.StreamCodec;
import com.example.tudursvehiclemod.compat.mc.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: the underbarrel switch key was pressed - toggles the held weapon between its own fire and its underbarrel launcher. */
public record SwitchUnderbarrelRequestPayload() implements CustomPacketPayload {

	public static final SwitchUnderbarrelRequestPayload INSTANCE = new SwitchUnderbarrelRequestPayload();

	public static final CustomPacketPayload.Type<SwitchUnderbarrelRequestPayload> ID =
			new CustomPacketPayload.Type<>(new net.minecraft.resources.ResourceLocation(TudursGuns.MOD_ID, "switch_underbarrel_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SwitchUnderbarrelRequestPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
