package com.example.tudursguns.network;

import com.example.tudursguns.TudursGuns;
import com.example.tudursvehiclemod.compat.mc.network.RegistryFriendlyByteBuf;
import com.example.tudursvehiclemod.compat.mc.network.codec.StreamCodec;
import com.example.tudursvehiclemod.compat.mc.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: the mode switch key was pressed - cycles the held weapon's ModeNum mode. */
public record SwitchModeRequestPayload() implements CustomPacketPayload {

	public static final SwitchModeRequestPayload INSTANCE = new SwitchModeRequestPayload();

	public static final CustomPacketPayload.Type<SwitchModeRequestPayload> ID =
			new CustomPacketPayload.Type<>(new net.minecraft.resources.ResourceLocation(TudursGuns.MOD_ID, "switch_mode_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SwitchModeRequestPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
