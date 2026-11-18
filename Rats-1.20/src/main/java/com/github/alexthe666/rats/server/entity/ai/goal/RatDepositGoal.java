/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Vec3i
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.Container
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.ai.goal.Goal
 *  net.minecraft.world.entity.ai.goal.Goal$Flag
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.LayeredCauldronBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.ChestBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.gameevent.GameEvent
 *  net.minecraft.world.level.material.Fluid
 *  net.minecraft.world.level.material.Fluids
 *  net.minecraft.world.level.pathfinder.Path
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.capabilities.Capabilities$EnergyStorage
 *  net.neoforged.neoforge.capabilities.Capabilities$FluidHandler
 *  net.neoforged.neoforge.common.NeoForgeMod
 *  net.neoforged.neoforge.energy.IEnergyStorage
 *  net.neoforged.neoforge.fluids.FluidStack
 *  net.neoforged.neoforge.fluids.capability.IFluidHandler
 *  net.neoforged.neoforge.fluids.capability.IFluidHandler$FluidAction
 */
package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.block.entity.AutoCurdlerBlockEntity;
import com.github.alexthe666.rats.server.block.entity.RatQuarryBlockEntity;
import com.github.alexthe666.rats.server.entity.ai.goal.RatWorkGoal;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.message.UpdateCurdlerFluidPacket;
import com.github.alexthe666.rats.server.message.UpdateRatFluidPacket;
import com.github.alexthe666.rats.server.misc.RatInventoryAccess;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import com.github.alexthe666.rats.server.misc.RatUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class RatDepositGoal
extends Goal
implements RatWorkGoal {
    private static Field createItemDrainInternalTankField;
    private static Class<?> createInternalTankClass;
    private static Method createTankAllowInsertionMethod;
    private static Method createTankForbidInsertionMethod;
    private static Method createTankGetPrimaryHandlerMethod;
    private static Class<?> createNotifyUpdateClass;
    private static Method createNotifyUpdateMethod;
    private final TamedRat rat;
    private final DepositType type;
    private BlockPos targetBlock = null;
    private int nextStartTick;
    private int nextPlatterNearbyCheckTick;
    private boolean cachedPlatterNearbyItems;
    private ItemStack cachedPlatterStack = ItemStack.EMPTY;

    public RatDepositGoal(TamedRat entity, DepositType type) {
        this.rat = entity;
        this.type = type;
        this.nextStartTick = entity.getRandom().nextInt(10);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        }
        this.nextStartTick = this.adjustedTickDelay(5 + this.rat.getRandom().nextInt(6));
        if (!this.rat.canMove()) {
            return false;
        }
        if (!this.rat.canWorkBySchedule()) {
            return false;
        }
        if (!this.rat.canWorkFromHunger()) {
            return false;
        }
        if (!this.rat.getCommand().workCommand) {
            return false;
        }
        if (this.rat.getTarget() != null) {
            return false;
        }
        if (this.rat.getDepositPos().isEmpty() || !this.rat.getDepositPos().get().dimension().equals(this.rat.level().dimension()) || RatUtils.isBlockProtected(this.rat.level(), this.rat.getDepositPos().get().pos(), this.rat)) {
            return false;
        }
        BlockPos depositPos = this.rat.getDepositPos().get().pos();
        BlockEntity te = this.rat.level().getBlockEntity(depositPos);
        if (!(te != null || this.type == DepositType.FLUID && this.canDepositFluidInEmptyCauldron(depositPos))) {
            return false;
        }
        if (this.type == DepositType.INVENTORY) {
            if (!this.rat.shouldDepositItem(this.rat.getMainHandItem())) {
                return false;
            }
            if (this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                return false;
            }
            if (RatUpgradeUtils.hasUpgrade(this.rat, (Item)RatsItemRegistry.RAT_UPGRADE_PLATTER.get()) && this.rat.getMainHandItem().getCount() < 64 && this.hasItemsOfTypeAroundCached(this.rat.getMainHandItem(), 10)) {
                return false;
            }
            if (!RatInventoryAccess.canInsert(this.rat.level(), te.getBlockPos(), this.rat.depositFacing, this.rat.getMainHandItem(), this.rat, "\u0441\u0434\u0430\u0447\u0430 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432")) {
                return false;
            }
        } else if (this.type == DepositType.ENERGY) {
            if (this.rat.getRFTransferRate() <= 0 || this.rat.getHeldRF() <= 0) {
                return false;
            }
            if (this.rat.level().getCapability(Capabilities.EnergyStorage.BLOCK, te.getBlockPos(), this.rat.depositFacing) == null) {
                return false;
            }
        } else if (this.type == DepositType.FLUID) {
            if (this.rat.transportingFluid.isEmpty() || this.rat.transportingFluid.getAmount() == 0) {
                return false;
            }
            if (te != null && this.resolveFluidHandler(te.getBlockPos(), this.rat.depositFacing) == null && !this.canDepositFluidInEmptyCauldron(depositPos)) {
                return false;
            }
        }
        this.resetTarget();
        return this.targetBlock != null;
    }

    private List<ItemEntity> getItemsOfTypeAround(ItemStack stack) {
        try {
            List<ItemEntity> items = this.rat.level().getEntitiesOfClass(ItemEntity.class, this.rat.getBoundingBox().inflate((double)this.rat.getRadius()), item -> ItemStack.isSameItemSameComponents(stack, item.getItem()));
            if (items.isEmpty()) {
                return List.of();
            }
            items.sort(Comparator.comparingDouble(arg_0 -> ((TamedRat)this.rat).distanceToSqr((Entity)arg_0)));
            ArrayList<ItemEntity> reachable = new ArrayList<ItemEntity>();
            int checkedPaths = 0;
            for (ItemEntity item2 : items) {
                if (checkedPaths++ >= 8) break;
                Path path = this.rat.getNavigation().createPath((Entity)item2, 1);
                if (path == null || path.getNodeCount() <= 0) continue;
                reachable.add(item2);
            }
            return reachable;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Крыса {} не смогла проверить ближайшие предметы перед сдачей", (Object)this.rat.getUUID(), (Object)exception);
            throw exception;
        }
    }

    private void resetTarget() {
        this.targetBlock = this.rat.getDepositPos().get().pos();
    }

    public boolean canContinueToUse() {
        if (this.type == DepositType.FLUID) {
            return this.targetBlock != null && !this.rat.transportingFluid.isEmpty();
        }
        if (this.type == DepositType.ENERGY) {
            return this.targetBlock != null && this.rat.getHeldRF() > 0;
        }
        return this.targetBlock != null && !this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() && this.rat.shouldDepositItem(this.rat.getMainHandItem());
    }

    private boolean hasItemsOfTypeAroundCached(ItemStack stack, int delayTicks) {
        try {
            if (ItemStack.isSameItemSameComponents((ItemStack)stack, (ItemStack)this.cachedPlatterStack) && this.rat.tickCount < this.nextPlatterNearbyCheckTick) {
                return this.cachedPlatterNearbyItems;
            }
            this.cachedPlatterStack = stack.copyWithCount(1);
            this.cachedPlatterNearbyItems = !this.getItemsOfTypeAround(stack).isEmpty();
            this.nextPlatterNearbyCheckTick = this.rat.tickCount + Math.max(1, delayTicks);
            return this.cachedPlatterNearbyItems;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043a\u0435\u0448 \u0431\u043b\u0438\u0436\u0430\u0439\u0448\u0438\u0445 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 \u043f\u0435\u0440\u0435\u0434 \u0441\u0434\u0430\u0447\u0435\u0439", (Object)this.rat.getUUID(), (Object)exception);
            throw exception;
        }
    }

    public void start() {
        this.rat.isCurrentlyWorking = true;
    }

    public void stop() {
        this.rat.isCurrentlyWorking = false;
    }

    private Vec3 getMovePos() {
        BlockEntity entity = this.rat.level().getBlockEntity(this.targetBlock);
        if (entity instanceof RatQuarryBlockEntity) {
            RatQuarryBlockEntity quarry = (RatQuarryBlockEntity)entity;
            return Vec3.atBottomCenterOf((Vec3i)quarry.getQuarryDepositWaitPos(this.rat.level()));
        }
        return Vec3.atBottomCenterOf((Vec3i)this.targetBlock.relative(this.rat.depositFacing));
    }

    public void tick() {
        BlockEntity te = this.rat.level().getBlockEntity(this.targetBlock);
        if (this.targetBlock != null && (te != null || this.canDepositFluidInEmptyCauldron(this.targetBlock))) {
            Container container;
            if (te instanceof RatQuarryBlockEntity) {
                RatQuarryBlockEntity quarry = (RatQuarryBlockEntity)te;
                if (!this.isAtQuarryDepositStand(quarry)) {
                    if (!this.moveQuarryGridTo(quarry, quarry.getQuarryDepositWaitPos(this.rat.level()))) {
                        RatsMod.LOGGER.warn("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043d\u0430\u0448\u043b\u0430 \u0431\u0435\u0437\u043e\u043f\u0430\u0441\u043d\u044b\u0439 \u043f\u0443\u0442\u044c \u043a \u0441\u0434\u0430\u0447\u0435 \u043a\u0430\u0440\u044c\u0435\u0440\u0430 {} \u0441 \u043f\u043e\u0437\u0438\u0446\u0438\u0438 {}", (Object)this.rat.getUUID(), (Object)quarry.getBlockPos(), (Object)this.rat.blockPosition());
                        this.rat.getNavigation().stop();
                    }
                    return;
                }
                this.finishQuarryDeposit(te);
                return;
            }
            Vec3 movePos = this.getMovePos();
            this.rat.moveSmoothlyTo(movePos.x(), movePos.y(), movePos.z(), this.rat.getHungerAdjustedWorkSpeed(1.25));
            double distance = Math.sqrt(this.rat.distanceToSqr(movePos.x(), movePos.y(), movePos.z()));
            double distanceToChest = Math.sqrt(this.rat.distanceToSqr(Vec3.atCenterOf(this.targetBlock)));
            if (this.rat.tickCount % 20 == 0) {
                RatsMod.LOGGER.info("[RAT_DEPOSIT_TICK] Rat {} tick: distToChest={}, distToMovePos={}, movePos={}, targetBlock={}",
                    this.rat.getUUID(), distanceToChest, distance, movePos, this.targetBlock);
            }
            if (distance < 4.5 * this.rat.getRatDistanceModifier() && distance > 2.5 * this.rat.getRatDistanceModifier() && te instanceof Container) {
                container = (Container)te;
                this.toggleChest(container, true);
            }
            if (distance <= 2.0 * this.rat.getRatDistanceModifier() || distanceToChest <= 2.5 * this.rat.getRatDistanceModifier()) {
                RatsMod.LOGGER.info("[RAT_DEPOSIT_REACHED] Rat {} reached chest! Calling executeTask...", this.rat.getUUID());
                if (te instanceof Container) {
                    container = (Container)te;
                    this.toggleChest(container, false);
                }
                this.executeTask(te);
                this.targetBlock = null;
                this.rat.getNavigation().stop();
                this.stop();
            }
        }
    }

    private void finishQuarryDeposit(BlockEntity te) {
        try {
            this.rat.getNavigation().stop();
            this.rat.setDeltaMovement(Vec3.ZERO);
            this.executeTask(te);
            this.targetBlock = null;
            this.stop();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0437\u0430\u0432\u0435\u0440\u0448\u0438\u0442\u044c \u0441\u0434\u0430\u0447\u0443 \u0432 \u043a\u0430\u0440\u044c\u0435\u0440 {}", (Object)this.rat.getUUID(), (Object)te.getBlockPos(), (Object)exception);
            throw exception;
        }
    }

    private boolean moveAlongQuarryColumn(RatQuarryBlockEntity quarry, BlockPos target) {
        try {
            BlockPos climbTarget = quarry.getPrimaryAccessColumnStandPos(target.getY());
            if (!quarry.isPrimaryAccessColumnStandPos(climbTarget)) {
                return false;
            }
            int currentY = this.rat.blockPosition().getY();
            if (currentY == climbTarget.getY() || !quarry.shouldUseColumnVerticalAssist(currentY, climbTarget.getY())) {
                return false;
            }
            if (!quarry.isOnPrimaryAccessColumnSurface(this.rat.level(), this.rat.blockPosition())) {
                BlockPos approach = quarry.getPrimaryAccessColumnStandPos(currentY);
                return this.moveQuarryGridHorizontally(quarry, approach);
            }
            BlockPos currentColumn = quarry.getPrimaryAccessColumnStandPos(currentY);
            double targetX = (double)currentColumn.getX() + 0.5;
            double targetZ = (double)currentColumn.getZ() + 0.5;
            if (Math.abs(this.rat.getX() - targetX) > 0.85 || Math.abs(this.rat.getZ() - targetZ) > 0.85) {
                return false;
            }
            int nextY = currentY + Integer.compare(climbTarget.getY(), currentY);
            BlockPos nextStep = quarry.getPrimaryAccessColumnStandPos(nextY);
            if (!quarry.isPrimaryAccessColumnStandPos(nextStep)) {
                return false;
            }
            this.rat.getNavigation().stop();
            this.animateQuarryStep(this.rat.blockPosition(), nextStep);
            this.rat.setDeltaMovement(Vec3.ZERO);
            this.rat.setPos(targetX, nextY, targetZ);
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043e\u0434\u043d\u044f\u0442\u044c\u0441\u044f \u043f\u043e \u0441\u0442\u043e\u043b\u0431\u0443 \u043a\u0430\u0440\u044c\u0435\u0440\u0430 {}", (Object)this.rat.getUUID(), (Object)quarry.getBlockPos(), (Object)exception);
            throw exception;
        }
    }

    private boolean moveQuarryGridTo(RatQuarryBlockEntity quarry, BlockPos target) {
        try {
            BlockPos current = this.rat.blockPosition();
            if (current.equals((Object)target)) {
                return false;
            }
            this.rat.getNavigation().stop();
            this.rat.setDeltaMovement(Vec3.ZERO);
            if (current.getY() != target.getY()) {
                BlockPos currentColumn = quarry.getPrimaryAccessColumnStandPos(current.getY());
                if (!current.equals((Object)currentColumn)) {
                    return this.moveQuarryGridHorizontally(quarry, currentColumn);
                }
                int nextY = current.getY() + Integer.compare(target.getY(), current.getY());
                BlockPos nextStep = quarry.getPrimaryAccessColumnStandPos(nextY);
                this.animateQuarryStep(current, nextStep);
                this.rat.setPos((double)nextStep.getX() + 0.5, nextStep.getY(), (double)nextStep.getZ() + 0.5);
                return true;
            }
            return this.moveQuarryGridHorizontally(quarry, target);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0439\u0442\u0438 \u043a \u0441\u0434\u0430\u0447\u0435 \u043a\u0430\u0440\u044c\u0435\u0440\u0430 {}", (Object)this.rat.getUUID(), (Object)quarry.getBlockPos(), (Object)exception);
            return false;
        }
    }

    private boolean moveQuarryGridHorizontally(RatQuarryBlockEntity quarry, BlockPos target) {
        try {
            BlockPos current = this.rat.blockPosition();
            if (current.equals((Object)target)) {
                return false;
            }
            BlockPos next = quarry.findSafeHorizontalGridStep(this.rat.level(), current, target);
            if (next == null) {
                RatsMod.LOGGER.warn("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043d\u0430\u0448\u043b\u0430 \u0431\u0435\u0437\u043e\u043f\u0430\u0441\u043d\u044b\u0439 \u0433\u043e\u0440\u0438\u0437\u043e\u043d\u0442\u0430\u043b\u044c\u043d\u044b\u0439 \u0448\u0430\u0433 \u043a \u0441\u0434\u0430\u0447\u0435 \u0438\u0437 {} \u043a {}", (Object)this.rat.getUUID(), (Object)current, (Object)target);
                return false;
            }
            this.rat.getNavigation().stop();
            this.animateQuarryStep(current, next);
            this.rat.setDeltaMovement(Vec3.ZERO);
            this.rat.setPos((double)next.getX() + 0.5, next.getY(), (double)next.getZ() + 0.5);
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0439\u0442\u0438 \u043f\u043e \u043a\u043b\u0435\u0442\u043a\u0430\u043c \u043a \u0441\u0434\u0430\u0447\u0435 {}", (Object)this.rat.getUUID(), (Object)target, (Object)exception);
            return false;
        }
    }

    private void animateQuarryStep(BlockPos current, BlockPos next) {
        try {
            double dx = next.getX() - current.getX();
            double dz = next.getZ() - current.getZ();
            if (dx == 0.0 && dz == 0.0) {
                return;
            }
            float yaw = (float)(Math.atan2(dz, dx) * 57.2957763671875) - 90.0f;
            this.rat.setYRot(yaw);
            this.rat.yBodyRot = yaw;
            this.rat.yHeadRot = yaw;
            this.rat.yRotO = yaw;
            this.rat.getLookControl().setLookAt((double)next.getX() + 0.5, (double)next.getY() + 0.5, (double)next.getZ() + 0.5);
            this.rat.walkAnimation.update(1.0f, 0.4f);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0431\u043d\u043e\u0432\u0438\u0442\u044c \u0430\u043d\u0438\u043c\u0430\u0446\u0438\u044e \u0448\u0430\u0433\u0430 \u043a\u0440\u044b\u0441\u044b {} \u0438\u0437 {} \u0432 {}", (Object)this.rat.getUUID(), (Object)current, (Object)next, (Object)exception);
            throw exception;
        }
    }

    private boolean isAtQuarryDepositStand(RatQuarryBlockEntity quarry) {
        try {
            return quarry.isValidQuarryDepositStand(this.rat.level(), this.rat.blockPosition());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0442\u043e\u0447\u043a\u0443 \u0441\u0434\u0430\u0447\u0438 \u043a\u0430\u0440\u044c\u0435\u0440\u0430 \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", (Object)this.rat.getUUID(), (Object)exception);
            throw exception;
        }
    }

    public void toggleChest(Container te, boolean open) {
        if (te instanceof ChestBlockEntity) {
            ChestBlockEntity chest = (ChestBlockEntity)te;
            if (open) {
                this.rat.level().blockEvent(this.targetBlock, chest.getBlockState().getBlock(), 1, 1);
            } else {
                this.rat.level().blockEvent(this.targetBlock, chest.getBlockState().getBlock(), 1, 0);
            }
            this.rat.level().gameEvent((Entity)this.rat, (Holder)(open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE), chest.getBlockPos());
        }
    }

    private void executeTask(BlockEntity entity) {
        if (this.type == DepositType.INVENTORY) {
            ItemStack duplicate = this.rat.getItemInHand(InteractionHand.MAIN_HAND).copy();
            RatsMod.LOGGER.info("[RAT_DEPOSIT_EXEC] Rat {} trying insert into {} facing={}: item={}",
                this.rat.getUUID(), entity.getBlockPos(), this.rat.depositFacing, duplicate);
            ItemStack shrunkenStack = RatInventoryAccess.insert(this.rat.level(), entity.getBlockPos(), this.rat.depositFacing, duplicate, this.rat, "\u0441\u0434\u0430\u0447\u0430 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432");
            RatsMod.LOGGER.info("[RAT_DEPOSIT_EXEC] Primary insert result: remaining={}", shrunkenStack);
            if (shrunkenStack.getCount() == duplicate.getCount()) {
                net.neoforged.neoforge.items.IItemHandler unsided = (net.neoforged.neoforge.items.IItemHandler)this.rat.level().getCapability(Capabilities.ItemHandler.BLOCK, entity.getBlockPos(), null);
                if (unsided != null) {
                    shrunkenStack = net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(unsided, duplicate, false);
                    RatsMod.LOGGER.info("[RAT_DEPOSIT_EXEC] Unsided insert result: remaining={}", shrunkenStack);
                }
                if (shrunkenStack.getCount() == duplicate.getCount() && entity instanceof Container container) {
                    shrunkenStack = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, container, duplicate, null);
                    RatsMod.LOGGER.info("[RAT_DEPOSIT_EXEC] Hopper addItem result: remaining={}", shrunkenStack);
                }
            }
            if (shrunkenStack.getCount() != duplicate.getCount()) {
                if (shrunkenStack.isEmpty()) {
                    this.rat.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    RatsMod.LOGGER.info("[RAT_DEPOSIT_EXEC] Rat {} hand cleared! Deposit successful.", this.rat.getUUID());
                } else {
                    this.rat.setItemInHand(InteractionHand.MAIN_HAND, shrunkenStack);
                    RatsMod.LOGGER.info("[RAT_DEPOSIT_EXEC] Rat {} partial deposit, hand left with {}", this.rat.getUUID(), shrunkenStack);
                }
            } else {
                RatsMod.LOGGER.warn("[RAT_DEPOSIT_EXEC] Rat {} FAILED to insert any items into chest at {}!", this.rat.getUUID(), entity.getBlockPos());
            }
        } else if (this.type == DepositType.ENERGY) {
            IEnergyStorage storage = (IEnergyStorage)this.rat.level().getCapability(Capabilities.EnergyStorage.BLOCK, entity.getBlockPos(), this.rat.depositFacing);
            if (storage != null) {
                int howMuchWeHave = this.rat.getHeldRF();
                int inputtedEnergy = 0;
                try {
                    if (storage.canReceive() && storage.receiveEnergy(howMuchWeHave, true) > 0) {
                        inputtedEnergy = storage.receiveEnergy(howMuchWeHave, false);
                    }
                }
                catch (RuntimeException exception) {
                    RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u044c \u044d\u043d\u0435\u0440\u0433\u0438\u044e \u0432 \u0431\u043b\u043e\u043a {}", (Object)this.rat.getUUID(), (Object)entity.getBlockPos(), (Object)exception);
                    throw exception;
                }
                if (inputtedEnergy > 0) {
                    this.rat.setHeldRF(Math.max(0, this.rat.getHeldRF() - inputtedEnergy));
                }
            }
        } else if (this.type == DepositType.FLUID) {
            IFluidHandler fluidHandler;
            FluidStack copiedFluid = this.rat.transportingFluid.copy();
            IFluidHandler iFluidHandler = fluidHandler = entity == null ? null : this.resolveFluidHandler(entity.getBlockPos(), this.rat.depositFacing);
            if (fluidHandler == null && entity == null) {
                this.fillEmptyCauldronWithFluid(this.targetBlock);
                return;
            }
            if (fluidHandler != null && !this.rat.transportingFluid.isEmpty()) {
                int minusAmount = 0;
                try {
                    if (fluidHandler.getTanks() > 0) {
                        FluidStack firstTank = fluidHandler.getFluidInTank(0);
                        if (fluidHandler.getTanks() > 1) {
                            for (int i = 0; i < fluidHandler.getTanks(); ++i) {
                                FluidStack otherTank = fluidHandler.getFluidInTank(i);
                                if (copiedFluid == null || !copiedFluid.isFluidEqual(otherTank)) continue;
                                firstTank = otherTank;
                            }
                        }
                        if ((firstTank.isEmpty() || copiedFluid == null || copiedFluid.isFluidEqual(firstTank)) && fluidHandler.fill(copiedFluid, IFluidHandler.FluidAction.SIMULATE) != 0) {
                            minusAmount = fluidHandler.fill(copiedFluid, IFluidHandler.FluidAction.EXECUTE);
                        }
                        if (minusAmount == 0 && entity != null) {
                            minusAmount = this.fillCreateItemDrain(entity, copiedFluid);
                        }
                        if (minusAmount == 0) {
                            this.nextStartTick = this.adjustedTickDelay(40 + this.rat.getRandom().nextInt(40));
                        }
                    }
                }
                catch (RuntimeException exception) {
                    RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u044c \u0436\u0438\u0434\u043a\u043e\u0441\u0442\u044c \u0432 \u0431\u043b\u043e\u043a {}", (Object)this.rat.getUUID(), (Object)entity.getBlockPos(), (Object)exception);
                    throw exception;
                }
                if (minusAmount > 0) {
                    int total = copiedFluid.getAmount() - minusAmount;
                    if (total <= 0) {
                        this.rat.transportingFluid = FluidStack.EMPTY;
                    } else {
                        this.rat.transportingFluid.setAmount(total);
                    }
                    if (!this.rat.level().isClientSide()) {
                        RatsNetworkHandler.CHANNEL.sendToTrackingEntityAndSelf((Entity)this.rat, new UpdateRatFluidPacket(this.rat.getId(), this.rat.transportingFluid));
                        BlockEntity blockEntity = this.rat.level().getBlockEntity(this.targetBlock);
                        if (blockEntity instanceof AutoCurdlerBlockEntity) {
                            AutoCurdlerBlockEntity curdler = (AutoCurdlerBlockEntity)blockEntity;
                            curdler.setChanged();
                            RatsNetworkHandler.CHANNEL.send(RatsNetworkHandler.allTarget(), new UpdateCurdlerFluidPacket(this.targetBlock.asLong(), curdler.getTank().getFluid()));
                        }
                    }
                    SoundEvent sound = this.rat.transportingFluid.isEmpty() ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL;
                    this.rat.playSound(sound, 1.0f, 1.0f);
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private int fillCreateItemDrain(BlockEntity entity, FluidStack fluid) {
        try {
            if (entity == null) return 0;
            if (fluid == null) return 0;
            if (fluid.isEmpty()) return 0;
            if (!"com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity".equals(entity.getClass().getName())) {
                return 0;
            }
            Field internalTankField = this.getCreateItemDrainInternalTankField(entity.getClass());
            Object internalTank = internalTankField.get(entity);
            if (internalTank == null) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0437\u0430\u043b\u0438\u0442\u044c \u0436\u0438\u0434\u043a\u043e\u0441\u0442\u044c \u0432 Create Item Drain {}: \u0432\u043d\u0443\u0442\u0440\u0435\u043d\u043d\u0438\u0439 \u0431\u0430\u043a \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d", (Object)this.rat.getUUID(), (Object)entity.getBlockPos());
                return 0;
            }
            this.ensureCreateTankMethods(internalTank.getClass());
            createTankAllowInsertionMethod.invoke(internalTank, new Object[0]);
            try {
                Object primaryHandler = createTankGetPrimaryHandlerMethod.invoke(internalTank, new Object[0]);
                if (!(primaryHandler instanceof IFluidHandler)) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0437\u0430\u043b\u0438\u0442\u044c \u0436\u0438\u0434\u043a\u043e\u0441\u0442\u044c \u0432 Create Item Drain {}: \u043e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 \u0431\u0430\u043a \u043d\u0435 \u044f\u0432\u043b\u044f\u0435\u0442\u0441\u044f IFluidHandler", (Object)this.rat.getUUID(), (Object)entity.getBlockPos());
                    int n = 0;
                    return n;
                }
                IFluidHandler handler = (IFluidHandler)primaryHandler;
                int simulated = handler.fill(fluid.copy(), IFluidHandler.FluidAction.SIMULATE);
                if (simulated <= 0) {
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0437\u0430\u043b\u0438\u0442\u044c {} \u0432 Create Item Drain {}: \u0431\u0430\u043a \u043e\u0442\u043a\u0430\u0437\u0430\u043b\u0441\u044f \u043f\u0440\u0438\u043d\u044f\u0442\u044c \u0436\u0438\u0434\u043a\u043e\u0441\u0442\u044c", (Object)this.rat.getUUID(), (Object)fluid.getHoverName().getString(), (Object)entity.getBlockPos());
                    int n = 0;
                    return n;
                }
                FluidStack toInsert = fluid.copy();
                toInsert.setAmount(Math.min(toInsert.getAmount(), simulated));
                int inserted = handler.fill(toInsert, IFluidHandler.FluidAction.EXECUTE);
                if (inserted > 0) {
                    entity.setChanged();
                    this.notifyCreateBlockEntity(entity);
                    RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0437\u0430\u043b\u0438\u043b\u0430 {} \u043c\u0411 {} \u0432 Create Item Drain {}", (Object)this.rat.getUUID(), (Object)inserted, (Object)fluid.getHoverName().getString(), (Object)entity.getBlockPos());
                }
                int n = inserted;
                return n;
            }
            finally {
                createTankForbidInsertionMethod.invoke(internalTank, new Object[0]);
            }
        }
        catch (ReflectiveOperationException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043e\u0431\u0440\u0430\u0442\u0438\u0442\u044c\u0441\u044f \u043a API Create Item Drain \u0432 {}", (Object)this.rat.getUUID(), (Object)(entity == null ? null : entity.getBlockPos()), (Object)exception);
            throw new IllegalStateException("\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0432\u043c\u0435\u0441\u0442\u0438\u043c\u043e\u0441\u0442\u0438 \u0441 Create Item Drain: " + exception.getMessage(), exception);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0437\u0430\u043b\u0438\u0442\u044c \u0436\u0438\u0434\u043a\u043e\u0441\u0442\u044c \u0432 Create Item Drain {}", (Object)this.rat.getUUID(), (Object)(entity == null ? null : entity.getBlockPos()), (Object)exception);
            throw exception;
        }
    }

    private Field getCreateItemDrainInternalTankField(Class<?> entityClass) throws NoSuchFieldException {
        try {
            if (createItemDrainInternalTankField != null && createItemDrainInternalTankField.getDeclaringClass().isAssignableFrom(entityClass)) {
                return createItemDrainInternalTankField;
            }
            Field field = entityClass.getDeclaredField("internalTank");
            field.setAccessible(true);
            createItemDrainInternalTankField = field;
            return field;
        }
        catch (NoSuchFieldException exception) {
            throw exception;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u043f\u043e\u043b\u0435 \u0431\u0430\u043a\u0430 Create Item Drain", (Object)this.rat.getUUID(), (Object)exception);
            throw exception;
        }
    }

    private void ensureCreateTankMethods(Class<?> tankClass) throws NoSuchMethodException {
        try {
            if (tankClass.equals(createInternalTankClass) && createTankAllowInsertionMethod != null && createTankForbidInsertionMethod != null && createTankGetPrimaryHandlerMethod != null) {
                return;
            }
            createInternalTankClass = tankClass;
            createTankAllowInsertionMethod = tankClass.getMethod("allowInsertion", new Class[0]);
            createTankForbidInsertionMethod = tankClass.getMethod("forbidInsertion", new Class[0]);
            createTankGetPrimaryHandlerMethod = tankClass.getMethod("getPrimaryHandler", new Class[0]);
            createTankAllowInsertionMethod.setAccessible(true);
            createTankForbidInsertionMethod.setAccessible(true);
            createTankGetPrimaryHandlerMethod.setAccessible(true);
        }
        catch (NoSuchMethodException exception) {
            throw exception;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043e\u0434\u0433\u043e\u0442\u043e\u0432\u0438\u0442\u044c \u043c\u0435\u0442\u043e\u0434\u044b \u0431\u0430\u043a\u0430 Create Item Drain", (Object)this.rat.getUUID(), (Object)exception);
            throw exception;
        }
    }

    private void notifyCreateBlockEntity(BlockEntity entity) {
        try {
            Method notifyUpdate = this.getCreateNotifyUpdateMethod(entity.getClass());
            notifyUpdate.invoke((Object)entity, new Object[0]);
        }
        catch (NoSuchMethodException ignored) {
            entity.setChanged();
        }
        catch (ReflectiveOperationException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043e\u0431\u043d\u043e\u0432\u0438\u0442\u044c Create Item Drain {}", (Object)this.rat.getUUID(), (Object)entity.getBlockPos(), (Object)exception);
            throw new IllegalStateException("\u041e\u0448\u0438\u0431\u043a\u0430 \u043e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u044f Create Item Drain: " + exception.getMessage(), exception);
        }
    }

    private Method getCreateNotifyUpdateMethod(Class<?> entityClass) throws NoSuchMethodException {
        try {
            if (entityClass.equals(createNotifyUpdateClass) && createNotifyUpdateMethod != null) {
                return createNotifyUpdateMethod;
            }
            Method method = entityClass.getMethod("notifyUpdate", new Class[0]);
            method.setAccessible(true);
            createNotifyUpdateClass = entityClass;
            createNotifyUpdateMethod = method;
            return method;
        }
        catch (NoSuchMethodException exception) {
            throw exception;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u043c\u0435\u0442\u043e\u0434 \u043e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u044f Create block entity", (Object)this.rat.getUUID(), (Object)exception);
            throw exception;
        }
    }

    private IFluidHandler resolveFluidHandler(BlockPos pos, Direction preferredSide) {
        try {
            IFluidHandler handler = (IFluidHandler)this.rat.level().getCapability(Capabilities.FluidHandler.BLOCK, pos, preferredSide);
            if (handler != null) {
                return handler;
            }
            for (Direction direction : Direction.values()) {
                if (direction == preferredSide || (handler = (IFluidHandler)this.rat.level().getCapability(Capabilities.FluidHandler.BLOCK, pos, direction)) == null) continue;
                RatsMod.LOGGER.debug("Крыса {} использует сторону {} для сдачи жидкости в {}, потому что сторона {} не дала обработчик", (Object)this.rat.getUUID(), (Object)direction, (Object)pos, (Object)preferredSide);
                return handler;
            }
            return null;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043d\u0430\u0439\u0442\u0438 \u0436\u0438\u0434\u043a\u043e\u0441\u0442\u043d\u044b\u0439 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a \u0434\u043b\u044f \u0441\u0434\u0430\u0447\u0438 \u0432 {}", (Object)this.rat.getUUID(), (Object)pos, (Object)exception);
            throw exception;
        }
    }

    private boolean canDepositFluidInEmptyCauldron(BlockPos pos) {
        try {
            return this.type == DepositType.FLUID && pos != null && this.rat.level().getBlockState(pos).is(Blocks.CAULDRON) && !this.rat.transportingFluid.isEmpty() && this.rat.transportingFluid.getAmount() >= 1000 && (this.rat.transportingFluid.getFluid().isSame((Fluid)NeoForgeMod.MILK.get()) || this.rat.transportingFluid.getFluid().isSame((Fluid)Fluids.WATER));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043a\u043e\u0442\u0451\u043b \u0434\u043b\u044f \u0436\u0438\u0434\u043a\u043e\u0441\u0442\u0438 {}", (Object)this.rat.getUUID(), (Object)pos, (Object)exception);
            throw exception;
        }
    }

    private void fillEmptyCauldronWithFluid(BlockPos pos) {
        try {
            if (!this.canDepositFluidInEmptyCauldron(pos)) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043c\u043e\u0436\u0435\u0442 \u0432\u044b\u043b\u0438\u0442\u044c \u0436\u0438\u0434\u043a\u043e\u0441\u0442\u044c {} \u0432 \u043a\u043e\u0442\u0451\u043b {}", (Object)this.rat.getUUID(), (Object)this.rat.transportingFluid.getHoverName().getString(), (Object)pos);
                return;
            }
            if (this.rat.transportingFluid.getFluid().isSame((Fluid)NeoForgeMod.MILK.get())) {
                this.rat.level().setBlockAndUpdate(pos, ((Block)RatsBlockRegistry.MILK_CAULDRON.get()).defaultBlockState());
            } else if (this.rat.transportingFluid.getFluid().isSame((Fluid)Fluids.WATER)) {
                this.rat.level().setBlockAndUpdate(pos, (BlockState)Blocks.WATER_CAULDRON.defaultBlockState().setValue((Property)LayeredCauldronBlock.LEVEL, (Comparable)Integer.valueOf(3)));
            } else {
                throw new IllegalStateException("\u041f\u0443\u0441\u0442\u043e\u0439 \u043a\u043e\u0442\u0451\u043b \u043d\u0435 \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0435\u0442 \u0436\u0438\u0434\u043a\u043e\u0441\u0442\u044c " + this.rat.transportingFluid.getHoverName().getString());
            }
            int remaining = this.rat.transportingFluid.getAmount() - 1000;
            if (remaining <= 0) {
                this.rat.transportingFluid = FluidStack.EMPTY;
            } else {
                this.rat.transportingFluid.setAmount(remaining);
            }
            if (!this.rat.level().isClientSide()) {
                RatsNetworkHandler.CHANNEL.sendToTrackingEntityAndSelf((Entity)this.rat, new UpdateRatFluidPacket(this.rat.getId(), this.rat.transportingFluid));
            }
            this.rat.playSound(SoundEvents.BUCKET_EMPTY, 1.0f, 1.0f);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0432\u044b\u043b\u0438\u0442\u044c \u0436\u0438\u0434\u043a\u043e\u0441\u0442\u044c \u0432 \u043a\u043e\u0442\u0451\u043b {}", (Object)this.rat.getUUID(), (Object)pos, (Object)exception);
            throw exception;
        }
    }

    @Override
    public RatWorkGoal.TaskType getRatTaskType() {
        return RatWorkGoal.TaskType.DEPOSIT;
    }

    public static enum DepositType {
        INVENTORY,
        FLUID,
        ENERGY;

    }
}

