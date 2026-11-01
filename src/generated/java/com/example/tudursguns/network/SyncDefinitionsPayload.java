package com.example.tudursguns.network;

import com.example.tudursguns.TudursGuns;
import com.example.tudursguns.handheld.WeaponSummary;
import java.util.List;
import java.util.Optional;
import com.example.tudursvehiclemod.compat.mc.network.RegistryFriendlyByteBuf;
import com.example.tudursvehiclemod.compat.mc.network.codec.ByteBufCodecs;
import com.example.tudursvehiclemod.compat.mc.network.codec.StreamCodec;
import com.example.tudursvehiclemod.compat.mc.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C: every definition of one kind (see ModDefinitions) as JSON, with the display name the server
 * worked out for it and, for kinds that name a weapon file, that file's summary - both may come from
 * a weapon file the client can't read. */
public record SyncDefinitionsPayload(String kind, List<Entry> entries) implements CustomPacketPayload {

	public record Entry(ResourceLocation id, String definitionJson, String displayName, Optional<WeaponSummary> weapon) {

		static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
				com.example.tudursvehiclemod.compat.mc.network.codec.ByteBufCodecs.RESOURCE_LOCATION, Entry::id,
				ByteBufCodecs.STRING_UTF8, Entry::definitionJson,
				ByteBufCodecs.STRING_UTF8, Entry::displayName,
				com.example.tudursguns.compat.Codecs.optional(WeaponSummary.PACKET_CODEC), Entry::weapon,
				Entry::new);
	}

	public static final CustomPacketPayload.Type<SyncDefinitionsPayload> ID =
			new CustomPacketPayload.Type<>(new net.minecraft.resources.ResourceLocation(TudursGuns.MOD_ID, "sync_definitions"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SyncDefinitionsPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, SyncDefinitionsPayload::kind,
			Entry.CODEC.apply(ByteBufCodecs.list()), SyncDefinitionsPayload::entries,
			SyncDefinitionsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
