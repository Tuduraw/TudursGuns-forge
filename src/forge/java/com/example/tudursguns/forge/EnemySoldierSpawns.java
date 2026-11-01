package com.example.tudursguns.forge;

import com.example.tudursguns.TudursGunsConfig;
import com.example.tudursguns.registry.ModEntityTypes;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/** Natural spawning of enemy soldiers in the Overworld - the Forge form of the Fabric version's
 * BiomeModifications.addSpawn. Applied through data/tudursguns/forge/biome_modifier/enemy_soldier_spawns.json,
 * and reads the server config each time biomes are built (enemy_soldier_natural_spawn, weight, group size). */
public final class EnemySoldierSpawns implements BiomeModifier {
	public static final EnemySoldierSpawns INSTANCE = new EnemySoldierSpawns();
	public static final Codec<EnemySoldierSpawns> CODEC = Codec.unit(INSTANCE);

	private EnemySoldierSpawns() {
	}

	@Override
	public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
		TudursGunsConfig.Data config = TudursGunsConfig.get();
		if (phase != Phase.ADD || !biome.is(BiomeTags.IS_OVERWORLD) || !config.enemy_soldier_natural_spawn
				|| config.enemy_soldier_spawn_weight <= 0 || ModEntityTypes.ENEMY_SOLDIER == null) {
			return;
		}
		builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntityTypes.ENEMY_SOLDIER,
				config.enemy_soldier_spawn_weight, config.enemy_soldier_group_min, config.enemy_soldier_group_max));
	}

	@Override
	public Codec<? extends BiomeModifier> codec() {
		return CODEC;
	}
}
