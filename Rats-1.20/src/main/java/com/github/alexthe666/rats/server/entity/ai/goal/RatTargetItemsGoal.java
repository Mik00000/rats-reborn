package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RatTargetItemsGoal extends Goal {
    private final TamedRat rat;
    @Nullable
    private ItemEntity targetItem = null;
    private int nextStartTick;

    public RatTargetItemsGoal(TamedRat rat) {
        this.rat = rat;
        this.nextStartTick = rat.getRandom().nextInt(20);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        }
        this.nextStartTick = this.adjustedTickDelay(10 + this.rat.getRandom().nextInt(10));
        if (!this.rat.canMove() || this.rat.isInCage() || this.rat.isCurrentlyWorking) {
            return false;
        }
        if (!this.rat.canWorkBySchedule()) {
            return false;
        }
        if (!this.rat.canWorkFromHunger()) {
            return false;
        }
        if (!this.rat.isTargetCommand() || this.rat.getDepositPos().isEmpty()) {
            return false;
        }
        if (this.checkIfRatCanHold()) {
            List<ItemEntity> items = this.rat.level().getEntitiesOfClass(
                ItemEntity.class,
                this.getTargetableArea(),
                item -> (item.onGround() || item.isInWater()) && this.rat.shouldCollectItem(item.getItem()) && this.rat.canRatPickupItem(item.getItem())
            );
            if (items.isEmpty()) {
                return false;
            }
            items.sort(Comparator.comparingDouble(arg_0 -> ((TamedRat)this.rat).distanceToSqr((Entity)arg_0)));
            int checkedPaths = 0;
            for (ItemEntity item2 : items) {
                if (checkedPaths++ >= 8) {
                    return false;
                }
                Path toPath = this.rat.getNavigation().createPath((Entity)item2, 1);
                if (toPath == null || toPath.getNodeCount() <= 0 || !this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() && RatUpgradeUtils.hasUpgrade(this.rat, (Item)RatsItemRegistry.RAT_UPGRADE_PLATTER.get()) && !ItemStack.isSameItemSameComponents((ItemStack)item2.getItem(), (ItemStack)this.rat.getItemInHand(InteractionHand.MAIN_HAND))) continue;
                this.targetItem = item2;
                return true;
            }
        }
        return false;
    }

    private boolean checkIfRatCanHold() {
        ItemStack stack = this.rat.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.isEmpty()) {
            return true;
        }
        return RatUpgradeUtils.hasUpgrade(this.rat, (Item)RatsItemRegistry.RAT_UPGRADE_PLATTER.get()) && stack.getCount() < 64;
    }

    protected AABB getTargetableArea() {
        if (this.rat.isTame()) {
            BlockPos pos = this.rat.getSearchCenter();
            double radius = Math.max(16.0, this.rat.getRadius());
            return new AABB(pos).inflate(radius, radius, radius);
        }
        return this.rat.getBoundingBox().inflate(8.0);
    }

    public boolean canContinueToUse() {
        return this.rat.isAlive() && !this.rat.isCurrentlyWorking && !this.rat.getNavigation().isDone() && !this.rat.getNavigation().isStuck() && this.targetItem != null && !this.targetItem.isRemoved() && this.checkIfRatCanHold();
    }

    public void start() {
        if (this.targetItem != null) {
            this.rat.moveSmoothlyTo((Entity)this.targetItem, this.rat.getHungerAdjustedWorkSpeed(1.25));
        }
    }

    public void stop() {
        this.targetItem = null;
    }

    public void tick() {
        if (this.targetItem != null && !this.targetItem.isRemoved()) {
            if (this.rat.distanceToSqr((Entity)this.targetItem) < this.rat.getRatHarvestDistance(1.0)) {
                ItemStack duplicate = this.targetItem.getItem().copy();
                if (RatUpgradeUtils.hasUpgrade(this.rat, (Item)RatsItemRegistry.RAT_UPGRADE_PLATTER.get())) {
                    ItemStack alreadyHolding = this.rat.getItemInHand(InteractionHand.MAIN_HAND);
                    int extractSize = alreadyHolding.getCount() + duplicate.getCount() < 64 ? duplicate.getCount() : Math.min(64 - alreadyHolding.getCount(), duplicate.getCount());
                    duplicate.setCount(extractSize);
                    this.targetItem.getItem().shrink(extractSize);
                    this.rat.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)duplicate.getItem(), alreadyHolding.getCount() + extractSize));
                } else {
                    duplicate.setCount(1);
                    this.targetItem.getItem().shrink(1);
                    if (!this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                        this.rat.spawnAtLocation(this.rat.getItemInHand(InteractionHand.MAIN_HAND), 0.0f);
                    }
                    this.rat.setItemInHand(InteractionHand.MAIN_HAND, duplicate);
                }
                this.stop();
            } else {
                this.rat.moveSmoothlyTo((Entity)this.targetItem, this.rat.getHungerAdjustedWorkSpeed(1.225));
            }
        }
    }
}
