package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.compat.create.CreateCompat;
import com.github.alexthe666.rats.data.tags.RatsBlockTags;
import com.github.alexthe666.rats.server.block.entity.RatCheeseFeederBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import com.github.alexthe666.rats.server.misc.RatUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public class RatSeekCheeseGoal extends Goal {
   private static final int MAX_PATH_CHECKS_PER_SEARCH = 16;
   private final TamedRat rat;
   private BlockPos targetBlock;
   private int nextStartTick;

   public RatSeekCheeseGoal(TamedRat rat) {
      try {
         this.rat = rat;
         this.nextStartTick = 40 + rat.getRandom().nextInt(120);
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to create the cheese search goal for rat {}", rat.getUUID(), var3);
         throw var3;
      }
   }

   public boolean canUse() {
      try {
         if (this.nextStartTick > 0) {
            this.nextStartTick--;
            return false;
         } else {
            this.nextStartTick = reducedTickDelay(120 + this.rat.getRandom().nextInt(80));
            if ((this.rat.canMove() || CreateCompat.isMountedOnCreateSeat(this.rat)) && !this.rat.isInCage() && this.rat.getTarget() == null) {
               if (RatDaySchedule.shouldSleep(this.rat.level())) {
                  return false;
               } else if (this.rat.consumeCheeseFromReserve("personal stock") && !this.rat.needsCheeseNow() && !this.rat.shouldRefillCheeseReserve()) {
                  return false;
               } else if (!this.rat.needsCheeseNow() && !this.rat.shouldRefillCheeseReserve()) {
                  return false;
               } else {
                  this.targetBlock = this.findTargetBlock();
                  if (this.targetBlock == null) {
                     this.nextStartTick = reducedTickDelay(240 + this.rat.getRandom().nextInt(160));
                     this.rat.setLastPathFailure("no path to a cheese source found within radius " + RatConfig.pathfindFeederDistance);
                     return false;
                  } else {
                     this.rat.clearLastPathFailure();
                     return true;
                  }
               }
            } else {
               return false;
            }
         }
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to start the cheese search of rat {}", this.rat.getUUID(), var2);
         throw var2;
      }
   }

   public boolean canContinueToUse() {
      try {
         return this.targetBlock != null
            && (this.rat.canMove() || CreateCompat.isMountedOnCreateSeat(this.rat))
            && (this.rat.needsCheeseNow() || this.rat.shouldRefillCheeseReserve());
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to continue the cheese search of rat {}", this.rat.getUUID(), var2);
         throw var2;
      }
   }

   public void stop() {
      try {
         this.targetBlock = null;
         this.rat.getNavigation().stop();
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to stop the cheese search of rat {}", this.rat.getUUID(), var2);
         throw var2;
      }
   }

   public void tick() {
      try {
         if (this.targetBlock != null) {
            if (CreateCompat.isMountedOnCreateSeat(this.rat)) {
               this.rat.stopRiding();
               this.rat.setOrderedToSit(false);
               RatsMod.LOGGER.debug("????? {} ?????? ? Create-???????, ????? ????? ?? ???? {}", this.rat.getUUID(), this.targetBlock);
            }

            Vec3 target = Vec3.atCenterOf(this.targetBlock);
            this.rat.moveSmoothlyTo(target.x(), target.y(), target.z(), 1.25);
            if (this.rat.distanceToSqr(target) <= this.rat.getRatHarvestDistance(0.0)) {
               if (!this.collectCheeseFromTarget()) {
                  RatsMod.LOGGER.warn("Rat {} reached cheese source {} but could not get cheese", this.rat.getUUID(), this.targetBlock);
               }

               this.stop();
            }
         }
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to run the cheese search of rat {}", this.rat.getUUID(), var2);
         throw var2;
      }
   }

   private BlockPos findTargetBlock() {
      try {
         BlockPos feeder = this.findFeederTarget();
         if (feeder != null) {
            return feeder;
         } else {
            return this.rat.needsCheeseNow() && this.rat.getCheeseEatenToday() <= 0
               ? this.findRaidTarget()
               : null;
         }
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to find a cheese source for rat {}", this.rat.getUUID(), var2);
         throw var2;
      }
   }

   private BlockPos findFeederTarget() {
      try {
         BlockPos center = this.rat.getSearchCenter();
         int radius = RatConfig.pathfindFeederDistance;
         List<RatSeekCheeseGoal.CheeseSearchCandidate> candidates = new ArrayList<>();

         for (BlockEntity entity : this.collectNearbyBlockEntities(center, radius)) {
            BlockPos pos = entity.getBlockPos();
            if (pos.closerThan(center, (double)radius + 1.0) && entity instanceof RatCheeseFeederBlockEntity) {
               RatCheeseFeederBlockEntity feeder = (RatCheeseFeederBlockEntity)entity;
               if (this.isValidFeeder(feeder)) {
                  candidates.add(new RatSeekCheeseGoal.CheeseSearchCandidate(pos, this.rat.distanceToSqr(Vec3.atCenterOf(pos))));
               }
            }
         }

         return this.firstReachableCandidate(candidates);
      } catch (RuntimeException var8) {
         RatsMod.LOGGER.error("Failed to find a feeder with cheese for rat {}", this.rat.getUUID(), var8);
         throw var8;
      }
   }

   private BlockPos findRaidTarget() {
      try {
         BlockPos center = this.rat.getSearchCenter();
         int radius = RatConfig.pathfindFeederDistance;
         List<RatSeekCheeseGoal.CheeseSearchCandidate> candidates = new ArrayList<>();

         for (BlockEntity entity : this.collectNearbyBlockEntities(center, radius)) {
            BlockPos pos = entity.getBlockPos();
            if (pos.closerThan(center, (double)radius + 1.0) && this.isValidRaidContainer(entity)) {
               candidates.add(new RatSeekCheeseGoal.CheeseSearchCandidate(pos, this.rat.distanceToSqr(Vec3.atCenterOf(pos))));
            }
         }

         return this.firstReachableCandidate(candidates);
      } catch (RuntimeException var7) {
         RatsMod.LOGGER.error("Failed to find a container with cheese for rat {}", this.rat.getUUID(), var7);
         throw var7;
      }
   }

   private BlockPos firstReachableCandidate(List<RatSeekCheeseGoal.CheeseSearchCandidate> candidates) {
      try {
         candidates.sort(Comparator.comparingDouble(RatSeekCheeseGoal.CheeseSearchCandidate::distance));
         int checkedPaths = 0;

         for (RatSeekCheeseGoal.CheeseSearchCandidate candidate : candidates) {
            if (checkedPaths++ >= 16) {
               return null;
            }

            if (this.canReach(candidate.pos())) {
               return candidate.pos();
            }
         }

         return null;
      } catch (RuntimeException var5) {
         RatsMod.LOGGER.error("Failed to pick a reachable cheese source for rat {}", this.rat.getUUID(), var5);
         throw var5;
      }
   }

   private Iterable<BlockEntity> collectNearbyBlockEntities(BlockPos center, int radius) {
      try {
         List<BlockEntity> entities = new ArrayList<>();
         int minChunkX = center.getX() - radius >> 4;
         int maxChunkX = center.getX() + radius >> 4;
         int minChunkZ = center.getZ() - radius >> 4;
         int maxChunkZ = center.getZ() + radius >> 4;

         for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
               if (this.rat.level().hasChunk(chunkX, chunkZ)) {
                  LevelChunk chunk = this.rat.level().getChunk(chunkX, chunkZ);
                  entities.addAll(chunk.getBlockEntities().values());
               }
            }
         }

         return entities;
      } catch (RuntimeException var11) {
         RatsMod.LOGGER.error("Failed to collect block entities around {} for rat {}", center, this.rat.getUUID(), var11);
         throw var11;
      }
   }

   private boolean canReach(BlockPos pos) {
      try {
         Path path = this.rat.getNavigation().createPath(pos, 1);
         boolean reachable = path != null && path.getNodeCount() > 0;
         if (!reachable) {
            this.rat.setLastPathFailure("no path to cheese " + pos);
         }

         return reachable;
      } catch (RuntimeException var4) {
         RatsMod.LOGGER.error("Failed to check the path of rat {} to cheese {}", this.rat.getUUID(), pos, var4);
         throw var4;
      }
   }

   private boolean isValidFeeder(LevelReader reader, BlockPos pos) {
      try {
         if (RatUtils.isBlockProtected(this.rat.level(), pos, this.rat)) {
            return false;
         } else {
            if (reader.getBlockEntity(pos) instanceof RatCheeseFeederBlockEntity feeder && this.isValidFeeder(feeder)) {
               return true;
            }

            return false;
         }
      } catch (RuntimeException var5) {
         RatsMod.LOGGER.error("Failed to check feeder {} for rat {}", pos, this.rat.getUUID(), var5);
         throw var5;
      }
   }

   private boolean isValidFeeder(RatCheeseFeederBlockEntity feeder) {
      try {
         return feeder.hasCheese();
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to check found feeder {} for rat {}", feeder.getBlockPos(), this.rat.getUUID(), var3);
         throw var3;
      }
   }

   private boolean isValidRaidContainer(LevelReader reader, BlockPos pos) {
      try {
         if (reader.getBlockState(pos).is(RatsBlockTags.UNRAIDABLE_CONTAINERS)) {
            return false;
         } else if (!(reader.getBlockState(pos).getBlock() instanceof EntityBlock)) {
            return false;
         } else {
            BlockEntity entity = reader.getBlockEntity(pos);
            if (entity instanceof RandomizableContainerBlockEntity container
               && container.saveWithFullMetadata(this.rat.level().registryAccess()).contains("LootTable")) {
               return false;
            }

            if (entity != null && this.containerHasCheese(this.rat.level(), pos, entity)) {
               return true;
            }

            return false;
         }
      } catch (RuntimeException var5) {
         RatsMod.LOGGER.error("Failed to check container {} for hungry rat {}", pos, this.rat.getUUID(), var5);
         throw var5;
      }
   }

   private boolean isValidRaidContainer(BlockEntity entity) {
      try {
         BlockPos pos = entity.getBlockPos();
         if (this.rat.level().getBlockState(pos).is(RatsBlockTags.UNRAIDABLE_CONTAINERS)) {
            return false;
         } else {
            if (entity instanceof RandomizableContainerBlockEntity container
               && container.saveWithFullMetadata(this.rat.level().registryAccess()).contains("LootTable")) {
               return false;
            }

            if (this.containerHasCheese(this.rat.level(), pos, entity)) {
               return true;
            }

            return false;
         }
      } catch (RuntimeException var4) {
         RatsMod.LOGGER.error("Failed to check found container {} for hungry rat {}", entity.getBlockPos(), this.rat.getUUID(), var4);
         throw var4;
      }
   }

   private boolean containerHasCheese(net.minecraft.world.level.Level level, BlockPos pos, BlockEntity entity) {
      try {
         if (entity instanceof Container inventory) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
               if (TamedRat.isCheeseReserveItem(inventory.getItem(slot))) {
                  return true;
               }
            }
         }
         if (level != null && pos != null) {
            net.neoforged.neoforge.items.IItemHandler handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, null);
            if (handler != null) {
               for (int slot = 0; slot < handler.getSlots(); slot++) {
                  if (TamedRat.isCheeseReserveItem(handler.getStackInSlot(slot))) {
                     return true;
                  }
               }
            }
         }
         return false;
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to check cheese in the container for rat {}", this.rat.getUUID(), var3);
         throw var3;
      }
   }

   private boolean collectCheeseFromTarget() {
      try {
         if (this.targetBlock == null) {
            return false;
         } else {
            BlockEntity entity = this.rat.level().getBlockEntity(this.targetBlock);
            if (entity instanceof RatCheeseFeederBlockEntity feeder) {
               return this.collectFromFeeder(feeder);
            } else if (entity != null) {
               return this.eatFromContainer(this.rat.level(), this.targetBlock, entity);
            }
            return false;
         }
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to get cheese for rat {} from target {}", this.rat.getUUID(), this.targetBlock, var3);
         throw var3;
      }
   }

   private boolean collectFromFeeder(RatCheeseFeederBlockEntity feeder) {
      try {
         boolean changed = false;
         if (this.rat.needsCheeseNow()) {
            ItemStack cheese = feeder.takeCheeseForRat(1);
            if (!cheese.isEmpty()) {
               boolean eaten = this.rat.eatCheeseFromStack(cheese, "feeder");
               if (!eaten) {
                  feeder.addCheeseFrom(cheese);
                  throw new IllegalStateException("The rat got cheese from the feeder but did not eat it");
               }

               changed = true;
            }
         }

         while (this.rat.hasCheeseReserveSpace() && feeder.hasCheese()) {
            ItemStack cheese = feeder.takeCheeseForRat(this.rat.getMaxCheeseReserve());
            if (cheese.isEmpty()) {
               break;
            }

            int inserted = this.rat.addCheeseToReserve(cheese);
            if (!cheese.isEmpty()) {
               feeder.addCheeseFrom(cheese);
            }

            if (inserted <= 0) {
               break;
            }

            changed = true;
         }

         return changed;
      } catch (RuntimeException var5) {
         RatsMod.LOGGER.error("Failed to get cheese for rat {} from feeder {}", this.rat.getUUID(), feeder.getBlockPos(), var5);
         throw var5;
      }
   }

   private boolean eatFromContainer(net.minecraft.world.level.Level level, BlockPos pos, BlockEntity entity) {
      try {
         if (entity instanceof Container inventory) {
            this.toggleChest(inventory, false);
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
               ItemStack stack = inventory.getItem(slot);
               if (TamedRat.isCheeseReserveItem(stack) && this.rat.eatCheeseFromStack(stack, "hungry raid")) {
                  if (stack.isEmpty()) {
                     inventory.setItem(slot, ItemStack.EMPTY);
                  } else {
                     inventory.setChanged();
                  }

                  return true;
               }
            }
         }

         if (level != null && pos != null) {
            net.neoforged.neoforge.items.IItemHandler handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, null);
            if (handler != null) {
               for (int slot = 0; slot < handler.getSlots(); slot++) {
                  ItemStack simulated = handler.extractItem(slot, 64, true);
                  if (TamedRat.isCheeseReserveItem(simulated)) {
                     ItemStack extracted = handler.extractItem(slot, 1, false);
                     if (this.rat.eatCheeseFromStack(extracted, "hungry raid")) {
                        return true;
                     } else {
                        handler.insertItem(slot, extracted, false);
                     }
                  }
               }
            }
         }

         return false;
      } catch (RuntimeException var4) {
         RatsMod.LOGGER.error("Failed to steal cheese for rat {}", this.rat.getUUID(), var4);
         throw var4;
      }
   }

   private void toggleChest(Container inventory, boolean open) {
      try {
         if (this.targetBlock != null && inventory instanceof ChestBlockEntity chest) {
            this.rat.level().blockEvent(this.targetBlock, chest.getBlockState().getBlock(), 1, open ? 1 : 0);
            this.rat.level().playSound(null, this.targetBlock, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS);
         }
      } catch (RuntimeException var4) {
         RatsMod.LOGGER.error("Failed to switch the chest during the cheese search of rat {}", this.rat.getUUID(), var4);
         throw var4;
      }
   }

   private static record CheeseSearchCandidate(BlockPos pos, double distance) {
   }
}
