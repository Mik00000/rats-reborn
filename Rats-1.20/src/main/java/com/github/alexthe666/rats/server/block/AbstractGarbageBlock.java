package com.github.alexthe666.rats.server.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.monster.PiedPiper;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.event.EventHooks;
import java.util.Objects;

@SuppressWarnings("deprecation")
public abstract class AbstractGarbageBlock extends FallingBlock {

	public final MobSpawnType spawnReason;
	public final double spawnRateModifier;

	public AbstractGarbageBlock(BlockBehaviour.Properties properties, double spawnRateModifier) {
		super(properties);
		this.spawnReason = MobSpawnType.SPAWNER;
		this.spawnRateModifier = spawnRateModifier;
	}

	protected static <T extends AbstractGarbageBlock> MapCodec<T> garbageCodec(BiConstructor<BlockBehaviour.Properties, Double, T> factory) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				propertiesCodec(),
				Codec.DOUBLE.fieldOf("spawn_rate_modifier").forGetter(block -> block.spawnRateModifier)
		).apply(instance, factory::create));
	}

	@FunctionalInterface
	protected interface BiConstructor<A, B, R> {
		R create(A first, B second);
	}

	protected abstract EntityType<? extends PathfinderMob> getEntityToSpawn();

	protected void postInitSpawn(PathfinderMob mob, RandomSource random) {

	}

	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (random.nextInt(level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).get()) <= 3) {
			if (!level.getBlockState(pos.above()).isSuffocating(level, pos) && level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
				if (random.nextFloat() <= RatConfig.garbageSpawnRate * spawnRateModifier) {
					PathfinderMob mob = this.getEntityToSpawn().create(level);
					if (mob == null) return;
					mob.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 0, 0);
					if (!mob.checkSpawnRules(level, this.spawnReason)) return;
					if (!mob.isInWall() && mob.checkSpawnObstruction(level)) {
						if (mob instanceof AbstractRat) {
							if (!level.getGameRules().getBoolean(RatsMod.SPAWN_RATS)) return;
							if (Objects.requireNonNull(level.getChunkSource().getLastSpawnState()).getMobCategoryCounts().getInt(RatsMod.RATS) >= RatsMod.RATS.getMaxInstancesPerChunk() * 2)
								return;
							if (RatConfig.ratsSpawnLikeMonsters && !this.isDarkEnoughForMonsterSpawns(level, mob.blockPosition(), random))
								return;
							EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(pos), this.spawnReason, null);
							this.postInitSpawn(mob, random);
							level.tryAddFreshEntityWithPassengers(mob);
						} else {
							if (mob instanceof PiedPiper && !level.getGameRules().getBoolean(RatsMod.SPAWN_PIPERS))
								return;
							if (this.isDarkEnoughForMonsterSpawns(level, mob.blockPosition(), random)) {
								EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(pos), this.spawnReason, null);
								this.postInitSpawn(mob, random);
								level.tryAddFreshEntityWithPassengers(mob);
							}
						}
					}
				}
			}
		}
	}

	//copy of Monster.isDarkEnoughToSpawn but without that strange sky check thing
	private boolean isDarkEnoughForMonsterSpawns(ServerLevelAccessor accessor, BlockPos pos, RandomSource random) {
		DimensionType dimensiontype = accessor.dimensionType();
		int i = dimensiontype.monsterSpawnBlockLightLimit();
		if (i < 15 && accessor.getBrightness(LightLayer.BLOCK, pos) > i) {
			return false;
		} else {
			int j = accessor.getLevel().isThundering() ? accessor.getMaxLocalRawBrightness(pos, 10) : accessor.getMaxLocalRawBrightness(pos);
			return j <= dimensiontype.monsterSpawnLightTest().sample(random);
		}
	}

}