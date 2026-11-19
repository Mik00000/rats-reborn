package com.github.alexthe666.rats.server.entity.ai.goal.harvest;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.ai.goal.RatWorkGoal;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public abstract class BaseRatHarvestGoal extends Goal implements RatWorkGoal {

	private final BlockSorter targetSorter;
	@Nullable
	private BlockPos targetBlock = null;
	@Nullable
	private Entity targetEntity = null;
	protected final TamedRat rat;

	protected int nextStartTick;
	private int nextHoldableItemsCheckTick;
	private boolean cachedHoldableItemsAround;
	private ItemStack cachedHoldableHandStack = ItemStack.EMPTY;

	protected BaseRatHarvestGoal(TamedRat rat) {
		this.rat = rat;
		this.targetSorter = new BlockSorter(rat);
		this.nextStartTick = rat.getRandom().nextInt(Math.max(1, RatConfig.ratHarvestDelay));
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Flag.LOOK));
	}

	public boolean checkTheBasics(boolean checkForItems, boolean checkForItemHolding) {
		if (!this.rat.canMove() || this.rat.getCommand() != RatCommand.HARVEST || this.rat.isInCage()) return false;
		if (!this.rat.canWorkBySchedule()) return false;
		if (!this.rat.canWorkFromHunger()) return false;
		if (checkForItemHolding && !this.checkIfItemCanBeHeld()) return false;
		return !checkForItems || !this.anyHoldableItemsAround();
	}

	@Override
	public boolean canUse() {
		return this.isReadyToFire();
	}

	public boolean isReadyToFire() {
		if (this.nextStartTick > 0) {
			this.nextStartTick--;
			return false;
		}
		this.nextStartTick = this.adjustedTickDelay(RatConfig.ratHarvestDelay);
		return true;
	}

	protected boolean checkIfItemCanBeHeld() {
		ItemStack stack = this.rat.getItemInHand(InteractionHand.MAIN_HAND);
		if (stack.isEmpty()) {
			return true;
		}
		return RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_PLATTER.get()) && stack.getCount() < 64;
	}

	private boolean anyHoldableItemsAround() {
		ItemStack handStack = this.rat.getItemInHand(InteractionHand.MAIN_HAND);
		if (ItemStack.isSameItemSameComponents(handStack, this.cachedHoldableHandStack) && this.rat.tickCount < this.nextHoldableItemsCheckTick) {
			return this.cachedHoldableItemsAround;
		}
		List<ItemEntity> items = this.rat.level().getEntitiesOfClass(ItemEntity.class, this.getTargetableArea(), item ->
				(item.onGround() || item.isInWater()) &&
						this.rat.canRatPickupItem(item.getItem()));
		if (items.isEmpty()) {
			this.cacheHoldableItemsResult(handStack, false);
			return false;
		}

		items.sort(Comparator.comparingDouble(this.rat::distanceToSqr));
		int checkedPaths = 0;
		for (ItemEntity item : items) {
			if (checkedPaths++ >= 8) {
				return false;
			}
			//please, only go after items you can actually reach
			Path toPath = this.rat.getNavigation().createPath(item, 1);
			if (toPath != null && toPath.getNodeCount() > 0) {
				if (!handStack.isEmpty()) {
					if (RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_PLATTER.get()) && !ItemStack.isSameItemSameComponents(item.getItem(), handStack))
						continue;
				}
				this.cacheHoldableItemsResult(handStack, true);
				return true;
			}
		}
		this.cacheHoldableItemsResult(handStack, false);
		return false;
	}

	// Caches the pickup item search result; takes the stack in the paws and the result, returns nothing.
	private void cacheHoldableItemsResult(ItemStack handStack, boolean hasItems) {
		this.cachedHoldableHandStack = handStack.copyWithCount(Math.min(1, handStack.getCount()));
		this.cachedHoldableItemsAround = hasItems;
		this.nextHoldableItemsCheckTick = this.rat.tickCount + 10 + this.rat.getRandom().nextInt(10);
	}

	protected AABB getTargetableArea() {
		if (this.rat.isTame() && this.rat.getRadiusCenter().isPresent()) {
			Vec3 center = new Vec3(this.rat.getSearchCenter().getX() + 0.5D, this.rat.getSearchCenter().getY() + 0.5D, this.rat.getSearchCenter().getZ() + 0.5D);
			double radius = this.rat.getRadius();
			AABB aabb = new AABB(-radius, -radius, -radius, radius, radius, radius);
			return aabb.move(center);
		}
		return this.rat.getBoundingBox().inflate(RatConfig.defaultRatRadius);
	}

	// Finds the nearest suitable block without an intermediate list; takes a position check, returns a position or null.
	@Nullable
	protected BlockPos findNearestBlockInRadius(Predicate<BlockPos> predicate) {
		try {
			int radius = this.rat.getRadius();
			BlockPos center = this.rat.getSearchCenter();
			BlockPos best = null;
			double bestDistance = Double.MAX_VALUE;
			for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
				if (!predicate.test(pos)) {
					continue;
				}
				double distance = this.distanceToSqr(pos);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = pos.immutable();
				}
			}
			return best;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to find the nearest work block for rat {}", this.rat.getUUID(), exception);
			throw new IllegalStateException("Failed to find the nearest work block for the rat " + this.rat.getUUID(), exception);
		}
	}

	// Computes the squared distance to a block; takes the position, returns the distance without taking the root.
	private double distanceToSqr(BlockPos pos) {
		double deltaX = this.rat.getX() - (pos.getX() + 0.5D);
		double deltaY = this.rat.getY() + this.rat.getEyeHeight() - (pos.getY() + 0.5D);
		double deltaZ = this.rat.getZ() - (pos.getZ() + 0.5D);
		return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
	}

	@Override
	public void start() {
		this.rat.isCurrentlyWorking = true;
	}

	@Override
	public void stop() {
		this.rat.isCurrentlyWorking = false;
		this.rat.getNavigation().stop();
		this.setTargetBlock(null);
		this.setTargetEntity(null);
	}

	public BlockSorter getTargetSorter() {
		return this.targetSorter;
	}

	@Nullable
	public BlockPos getTargetBlock() {
		return this.targetBlock;
	}

	public void setTargetBlock(@Nullable BlockPos pos) {
		this.targetBlock = pos;
	}

	@Nullable
	public Entity getTargetEntity() {
		return this.targetEntity;
	}

	public void setTargetEntity(@Nullable Entity entity) {
		this.targetEntity = entity;
	}

	public void holdItemHarvestedIfPossible(TamedRat rat, List<ItemStack> items) {
		for (ItemStack stack : items) {
			if (rat.getMainHandItem().isEmpty() && rat.canRatPickupItem(stack)) {
				rat.setItemInHand(InteractionHand.MAIN_HAND, stack);
			} else if (ItemStack.isSameItem(rat.getMainHandItem(), stack) && stack.getCount() + rat.getMainHandItem().getCount() <= 64) {
				rat.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(rat.getMainHandItem().getItem(), rat.getMainHandItem().getCount() + stack.getCount()));
			} else {
				ItemEntity item = new ItemEntity(this.rat.level(), this.rat.getX(), this.rat.getY(), this.rat.getZ(), stack);
				item.setExtendedLifetime();
				item.setNoPickUpDelay();
				this.rat.level().addFreshEntity(item);
			}
		}
	}

	@Override
	public TaskType getRatTaskType() {
		return TaskType.HARVEST;
	}

	public record BlockSorter(Entity entity) implements Comparator<BlockPos> {

		@Override
		public int compare(BlockPos pos1, BlockPos pos2) {
			double distance1 = this.distanceTo(pos1);
			double distance2 = this.distanceTo(pos2);
			return Double.compare(distance1, distance2);
		}

		private double distanceTo(BlockPos pos) {
			double deltaX = this.entity.getX() - (pos.getX() + 0.5);
			double deltaY = this.entity.getY() + this.entity.getEyeHeight() - (pos.getY() + 0.5);
			double deltaZ = this.entity.getZ() - (pos.getZ() + 0.5);
			return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
		}
	}
}
