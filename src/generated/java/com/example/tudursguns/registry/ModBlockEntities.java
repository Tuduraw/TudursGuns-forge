package com.example.tudursguns.registry;

import com.example.tudursguns.TudursGuns;
import com.example.tudursguns.block.AmmoBoxBlockEntity;
import com.example.tudursguns.block.SoldierPostBlockEntity;
import com.example.tudursvehiclemod.compat.fabric.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {

	private ModBlockEntities() {
	}

	public static BlockEntityType<AmmoBoxBlockEntity> AMMO_BOX;
	public static BlockEntityType<SoldierPostBlockEntity> SOLDIER_POST;

	public static void register() {
		AMMO_BOX = com.example.tudursvehiclemod.compat.Registration.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new net.minecraft.resources.ResourceLocation(TudursGuns.MOD_ID, "ammo_box"),
				FabricBlockEntityTypeBuilder.create(AmmoBoxBlockEntity::new, ModBlocks.AMMO_BOX).build());
		SOLDIER_POST = com.example.tudursvehiclemod.compat.Registration.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new net.minecraft.resources.ResourceLocation(TudursGuns.MOD_ID, "soldier_post"),
				FabricBlockEntityTypeBuilder.create(SoldierPostBlockEntity::new, ModBlocks.SOLDIER_POST).build());
	}
}
