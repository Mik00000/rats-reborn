package com.github.alexthe666.rats.server.block;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.monster.PiedPiper;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;

public abstract class AbstractGarbageBlock extends FallingBlock implements BonemealableBlock {
   public final MobSpawnType spawnReason = MobSpawnType.SPAWNER;
   public final double spawnRateModifier;

   public AbstractGarbageBlock(Properties properties, double spawnRateModifier) {
      super(properties);
      this.spawnRateModifier = spawnRateModifier;
   }

   protected static <T extends AbstractGarbageBlock> MapCodec<T> garbageCodec(AbstractGarbageBlock.BiConstructor<Properties, Double, T> factory) {
      return RecordCodecBuilder.mapCodec(
         instance -> instance.group(propertiesCodec(), Codec.DOUBLE.fieldOf("spawn_rate_modifier").forGetter(block -> block.spawnRateModifier))
               .apply(instance, factory::create)
      );
   }

   protected abstract EntityType<? extends PathfinderMob> getEntityToSpawn();

   protected void postInitSpawn(PathfinderMob mob, RandomSource random) {
   }

   public boolean trySpawnMob(ServerLevel level, BlockPos pos, RandomSource random, boolean force) {
      if (level.getBlockState(pos.above()).isSuffocating(level, pos)) {
         return false;
      }
      if (!force && !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
         return false;
      }

      PathfinderMob mob = (PathfinderMob)this.getEntityToSpawn().create(level);
      if (mob == null) {
         return false;
      }

      mob.moveTo((double)pos.getX() + 0.5, (double)pos.getY() + 1.0, (double)pos.getZ() + 0.5, 0.0F, 0.0F);

      if (!mob.isInWall() && mob.checkSpawnObstruction(level)) {
         if (mob instanceof AbstractRat) {
            if (!level.getGameRules().getBoolean(RatsMod.SPAWN_RATS)) {
               return false;
            }

            List<AbstractRat> nearbyRats = level.getEntitiesOfClass(AbstractRat.class, new AABB(pos).inflate(10.0));
            if (!force && nearbyRats.size() >= 6) {
               return false;
            }

            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(pos), this.spawnReason, null);
            this.postInitSpawn(mob, random);
            level.tryAddFreshEntityWithPassengers(mob);
            return true;
         } else {
            if (mob instanceof PiedPiper && !level.getGameRules().getBoolean(RatsMod.SPAWN_PIPERS)) {
               return false;
            }

            List<PiedPiper> nearbyPipers = level.getEntitiesOfClass(PiedPiper.class, new AABB(pos).inflate(16.0));
            if (!force && nearbyPipers.size() >= 2) {
               return false;
            }

            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(pos), this.spawnReason, null);
            this.postInitSpawn(mob, random);
            level.tryAddFreshEntityWithPassengers(mob);
            return true;
         }
      }
      return false;
   }

   public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      int tickSpeed = 3;
      try {
         IntegerValue rule = level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING);
         if (rule != null) {
            tickSpeed = rule.get();
         }
      } catch (Exception ignored) {}

      if (tickSpeed > 0 && (double)random.nextFloat() <= RatConfig.garbageSpawnRate * this.spawnRateModifier) {
         this.trySpawnMob(level, pos, random, false);
      }
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
      return !level.getBlockState(pos.above()).isSuffocating(level, pos);
   }

   @Override
   public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
      this.trySpawnMob(level, pos, random, true);
   }

   @FunctionalInterface
   protected interface BiConstructor<A, B, R> {
      R create(A var1, B var2);
   }
}
