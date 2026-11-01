package com.example.tudursguns.registry;

import com.example.tudursguns.TudursGuns;
import com.example.tudursguns.TudursGunsConfig;
import com.example.tudursguns.entity.LaserSpotEntity;
import com.example.tudursguns.entity.MineEntity;
import com.example.tudursguns.entity.SmokeCloudEntity;
import com.example.tudursguns.entity.SmokeDecoyEntity;
import com.example.tudursguns.soldier.EnemySoldierEntity;
import com.example.tudursguns.soldier.FriendlySoldierEntity;
import com.example.tudursvehiclemod.compat.fabric.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntityTypes {

	private ModEntityTypes() {
	}

	public static EntityType<SmokeCloudEntity> SMOKE_CLOUD;
	public static EntityType<SmokeDecoyEntity> SMOKE_DECOY;
	public static EntityType<LaserSpotEntity> LASER_SPOT;
	public static EntityType<MineEntity> MINE;
	public static EntityType<FriendlySoldierEntity> SOLDIER;
	public static EntityType<EnemySoldierEntity> ENEMY_SOLDIER;
	public static Item ENEMY_SOLDIER_SPAWN_EGG;

	public static void register() {
		SMOKE_CLOUD = register("smoke_cloud", EntityType.Builder.<SmokeCloudEntity>of(SmokeCloudEntity::new, MobCategory.MISC)
				.sized(0.5f, 0.5f).clientTrackingRange(16).noSave().noSummon());
		SMOKE_DECOY = register("smoke_decoy", EntityType.Builder.<SmokeDecoyEntity>of(SmokeDecoyEntity::new, MobCategory.MISC)
				.sized(0.1f, 0.1f).clientTrackingRange(16).noSave().noSummon());
		FabricDefaultAttributeRegistry.register(SMOKE_DECOY, SmokeDecoyEntity::createAttributes);
		LASER_SPOT = register("laser_spot", EntityType.Builder.<LaserSpotEntity>of(LaserSpotEntity::new, MobCategory.MISC)
				.sized(0.1f, 0.1f).clientTrackingRange(16).noSave().noSummon());
		FabricDefaultAttributeRegistry.register(LASER_SPOT, SmokeDecoyEntity::createAttributes);
		// Saved with the world (a minefield stays laid); tracked every tick so one stuck to a moving
		// vehicle keeps up with it.
		MINE = register("mine", EntityType.Builder.<MineEntity>of(MineEntity::new, MobCategory.MISC)
				.sized(0.5f, 0.2f).clientTrackingRange(8).updateInterval(1).noSummon());

		// Only a soldier post sends these out (a summoned one has no post and would vanish at once).
		SOLDIER = register("soldier", EntityType.Builder.<FriendlySoldierEntity>of(FriendlySoldierEntity::new, MobCategory.MISC)
				.sized(0.6f, 1.8f).clientTrackingRange(10).noSummon());
		FabricDefaultAttributeRegistry.register(SOLDIER, FriendlySoldierEntity::createAttributes);
		ENEMY_SOLDIER = register("enemy_soldier", EntityType.Builder.<EnemySoldierEntity>of(EnemySoldierEntity::new, MobCategory.MONSTER)
				.sized(0.6f, 1.8f).clientTrackingRange(10));
		FabricDefaultAttributeRegistry.register(ENEMY_SOLDIER, EnemySoldierEntity::createAttributes);
		com.example.tudursguns.compat.Spawns.onGround(ENEMY_SOLDIER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EnemySoldierEntity::canSpawn);
		// Natural spawning: forge.EnemySoldierSpawns (a biome modifier that reads the server config).
		ResourceKey<Item> eggKey = ResourceKey.create(Registries.ITEM, new net.minecraft.resources.ResourceLocation(TudursGuns.MOD_ID, "enemy_soldier_spawn_egg"));
		ENEMY_SOLDIER_SPAWN_EGG = com.example.tudursvehiclemod.compat.Registration.register(BuiltInRegistries.ITEM, eggKey, new net.minecraftforge.common.ForgeSpawnEggItem(() -> ENEMY_SOLDIER, 0x585C64, 0x962020, new Item.Properties()));
	}

	private static <T extends Entity> EntityType<T> register(String path, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, new net.minecraft.resources.ResourceLocation(TudursGuns.MOD_ID, path));
		return com.example.tudursvehiclemod.compat.Registration.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key.location().toString()));
	}
}
