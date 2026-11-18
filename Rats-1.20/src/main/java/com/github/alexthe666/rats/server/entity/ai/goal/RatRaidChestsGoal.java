package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.data.tags.RatsBlockTags;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.DiggingRat;
import com.github.alexthe666.rats.server.misc.RatPathingHelper;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.neoforged.neoforge.event.EventHooks;

public class RatRaidChestsGoal extends RatMoveToBlockGoal {
   private final DiggingRat rat;

   public RatRaidChestsGoal(DiggingRat entity) {
      super(entity, 1.0, 16);
      this.rat = entity;
   }

   public boolean canUse() {
      if (!this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
         return false;
      } else {
         return this.rat.canMove() && this.rat.getOwner() == null && RatConfig.ratsStealItems && super.canUse();
      }
   }

   public boolean canContinueToUse() {
      return super.canContinueToUse() && this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty();
   }

   public void tick() {
      super.tick();
      BlockEntity be = this.rat.level().getBlockEntity(this.blockPos);
      if (this.isReachedTarget() && be != null) {
         double distance = this.rat.distanceToSqr((double)this.blockPos.getX(), (double)this.blockPos.getY(), (double)this.blockPos.getZ());
         if (distance < 6.25 && distance > 2.72F) {
            if (be instanceof Container feeder) {
               this.toggleChest(feeder, true);
            }
            this.rat.level().playSound(null, this.blockPos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS);
         }

         if (distance <= 2.89F) {
            if (be instanceof Container feeder) {
               this.toggleChest(feeder, false);
            }
            this.rat.level().playSound(null, this.blockPos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS);
            ItemStack stack = RatUtils.extractFoodFromInventory(this.rat.level(), this.blockPos, be, this.rat.level().getRandom());
            if (!stack.isEmpty()) {
               if (!this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                  this.rat.spawnAtLocation(this.rat.getItemInHand(InteractionHand.MAIN_HAND), 0.0F);
               }

               this.rat.setItemInHand(InteractionHand.MAIN_HAND, stack);
               this.rat.setFleePos(this.blockPos);
            }
         }
      }
   }

   protected boolean isValidTarget(LevelReader reader, BlockPos pos) {
      if (!reader.getBlockState(pos).is(RatsBlockTags.UNRAIDABLE_CONTAINERS) && reader.getBlockState(pos).getBlock() instanceof EntityBlock) {
         if (!RatPathingHelper.canSeeOrDigToBlock(this.rat, pos)) {
            return false;
         }

         BlockEntity entity = reader.getBlockEntity(pos);
         if (entity != null) {
            try {
               if (entity instanceof RandomizableContainerBlockEntity container
                  && container.saveWithFullMetadata(this.rat.level().registryAccess()).contains("LootTable")) {
                  return false;
               }

               if (RatUtils.doesContainFood(this.rat.level(), pos, entity)) {
                  return true;
               }
            } catch (Exception var6) {
               RatsMod.LOGGER.warn("Rats stopped a {} from causing a crash during access", entity.getClass().getSimpleName(), var6);
               return false;
            }
         }
      }

      return false;
   }

   public void toggleChest(Container te, boolean open) {
      if (te instanceof ChestBlockEntity chest) {
         if (open) {
            this.rat.level().blockEvent(this.blockPos, chest.getBlockState().getBlock(), 1, 1);
         } else {
            this.rat.level().blockEvent(this.blockPos, chest.getBlockState().getBlock(), 1, 0);
         }
      }
   }
}
