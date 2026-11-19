package com.github.alexthe666.rats.server.entity.ai.goal.harvest;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.data.tags.RatsBlockTags;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.server.block.entity.RatQuarryBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class RatQuarryGoal extends BaseRatHarvestGoal {
	private enum QuarryMode {
		MINE_LAYER,
		MOVE_TO_LAYER,
		RETURN_TO_DEPOSIT
	}

	private final TamedRat rat;
	private QuarryMode mode = QuarryMode.MOVE_TO_LAYER;
	private int breakingTime;
	private int previousBreakProgress;
	private int pathStuckTicks;
	private BlockState prevMiningState = null;

	public RatQuarryGoal(TamedRat rat) {
		super(rat);
		this.rat = rat;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (!super.canUse() || this.rat.getDepositPos().isEmpty() || !this.rat.getDepositPos().get().dimension().equals(this.rat.level().dimension()) || !this.rat.level().getBlockState(this.rat.getDepositPos().get().pos()).is(RatsBlockRegistry.RAT_QUARRY.get()) || !this.checkTheBasics(false, false)) {
			return false;
		}

		this.resetTarget();
		return this.getTargetBlock() != null;
	}

	@Override
	public boolean isReadyToFire() {
		if (this.nextStartTick > 0) {
			this.nextStartTick--;
			return false;
		}
		this.nextStartTick = this.adjustedTickDelay(RatConfig.ratHarvestDelay / 2);
		return true;
	}

	// Picks the target: return to the quarry, approach the pillar or mine the reserved block.
	private void resetTarget() {
		try {
			this.setTargetBlock(null);
			GlobalPos quarryPos = this.rat.getDepositPos().orElse(null);
			if (quarryPos == null || !(this.rat.level().getBlockEntity(quarryPos.pos()) instanceof RatQuarryBlockEntity quarry)) {
				return;
			}
			if (!this.rat.getMainHandItem().isEmpty()) {
				this.mode = QuarryMode.RETURN_TO_DEPOSIT;
				this.setTargetBlock(quarry.getQuarryDepositWaitPos(this.rat.level()));
				return;
			}
			BlockPos miningTarget = quarry.claimNextTarget(this.rat.level(), this.rat.getUUID(), this.rat.blockPosition());
			if (miningTarget != null) {
				if (this.canMineFromCurrentPosition(quarry, miningTarget)) {
					this.mode = QuarryMode.MINE_LAYER;
				} else if (!this.isAtStandPos(quarry.getStandPosForTarget(this.rat.level(), miningTarget, this.rat.blockPosition()))) {
					this.mode = QuarryMode.MOVE_TO_LAYER;
				} else {
					this.mode = QuarryMode.MINE_LAYER;
				}
				this.setTargetBlock(miningTarget);
			}
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Failed to pick a quarry target for rat {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Checks the exact work position; takes the foot position, returns true if it matches the rat's block.
	private boolean isAtStandPos(BlockPos standPos) {
		try {
			return this.rat.blockPosition().equals(standPos);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Failed to check the position of rat {} at the quarry", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Checks whether a block can drop an item for the rat; takes the level and position, returns true if the item fits.
	private boolean doesListContainBlock(Level level, BlockPos pos) {
		try {
			BlockState state = level.getBlockState(pos);
			ItemStack getStack = state.getBlock().getCloneItemStack(level, pos, state);
			return this.rat.canRatPickupItem(getStack);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Failed to check mining block {} for rat {}", pos, this.rat.getUUID(), exception);
			throw exception;
		}
	}

	private boolean canRatHoldTargetedBlock(Level level, BlockPos pos) {
		if (this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
			return true;
		}
		BlockState state = level.getBlockState(pos);
		LootParams.Builder loot = new LootParams.Builder((ServerLevel) level).withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withParameter(LootContextParams.ORIGIN, this.rat.position()).withLuck(1.0F);
		List<ItemStack> drops = state.getDrops(loot);
		return drops.stream().map(ItemStack::getItem).toList().contains(this.rat.getItemInHand(InteractionHand.MAIN_HAND).getItem());
	}

	@Override
	public boolean canContinueToUse() {
		if (this.getTargetBlock() == null) {
			return false;
		}
		if (this.mode == QuarryMode.RETURN_TO_DEPOSIT) {
			return this.checkTheBasics(false, false);
		}
		if (!this.checkTheBasics(false, true)) {
			return false;
		}
		if (this.mode == QuarryMode.MOVE_TO_LAYER || this.mode == QuarryMode.RETURN_TO_DEPOSIT) {
			return true;
		}
		BlockState state = this.rat.level().getBlockState(this.getTargetBlock());
		RatQuarryBlockEntity quarry = this.getQuarry();
		if (quarry != null && quarry.isSupportTarget(this.getTargetBlock())) {
			return true;
		}
		return quarry != null && this.canMineBlock(quarry, this.getTargetBlock()) && !state.isAir();
	}

	public void stop() {
		RatQuarryBlockEntity quarry = this.getQuarry();
		BlockPos target = this.getTargetBlock();
		if (quarry != null && target != null && this.mode != QuarryMode.RETURN_TO_DEPOSIT) {
			quarry.releaseTarget(this.rat.getUUID());
		}
		super.stop();
		this.rat.crafting = false;
		this.mode = QuarryMode.MOVE_TO_LAYER;
		this.pathStuckTicks = 0;
	}

	// Drops an unreachable target and picks a new one on the same layer; takes the quarry, returns nothing.
	private void retargetAfterBlockedWork(RatQuarryBlockEntity quarry) {
		try {
			BlockPos oldTarget = this.getTargetBlock();
			quarry.abandonTarget(this.rat.level(), this.rat.getUUID(), oldTarget);
			this.breakingTime = 0;
			this.previousBreakProgress = -1;
			this.prevMiningState = null;
			this.pathStuckTicks = 0;
			this.setTargetBlock(null);
			this.resetTarget();
			if (this.getTargetBlock() == null) {
				this.stop();
			}
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Rat {} failed to reassign the target of quarry {}", this.rat.getUUID(), quarry.getBlockPos(), exception);
			this.stop();
			throw exception;
		}
	}

	@Override
	public void tick() {
		if (this.getTargetBlock() == null) {
			return;
		}
		if (this.mode == QuarryMode.MOVE_TO_LAYER || this.mode == QuarryMode.RETURN_TO_DEPOSIT) {
			this.tickPathMovement();
			return;
		}
		this.tickMining();
	}

	// Moving up or down the support ladder.
	private void tickPathMovement() {
		try {
			BlockPos target = this.getTargetBlock();
			RatQuarryBlockEntity quarry = this.getQuarry();
			if (this.mode == QuarryMode.RETURN_TO_DEPOSIT) {
				if (quarry != null && this.moveQuarryGridTo(quarry, target)) {
					return;
				}
				if (quarry == null || !quarry.isValidQuarryDepositStand(this.rat.level(), this.rat.blockPosition())) {
					return;
				}
				this.stop();
				return;
			}
			if (quarry == null) {
				this.stop();
				return;
			}
			BlockPos standPos = quarry.getStandPosForTarget(this.rat.level(), target, this.rat.blockPosition());
			if (this.isStandingOnTarget(target) && this.moveOffTargetBlock(quarry, target, standPos)) {
				return;
			}
			if (this.canMineFromCurrentPosition(quarry, target)) {
				this.mode = QuarryMode.MINE_LAYER;
				this.rat.getNavigation().stop();
				return;
			}
			if (this.moveQuarryGridTo(quarry, standPos)) {
				this.pathStuckTicks = 0;
				return;
			}
			if (this.isAtStandPos(standPos) || this.canMineFromCurrentPosition(quarry, target)) {
				this.pathStuckTicks = 0;
				this.mode = QuarryMode.MINE_LAYER;
				return;
			}
			this.pathStuckTicks++;
			if (this.pathStuckTicks >= 40) {
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Rat {} got stuck at target {} of quarry {}, reassigning", this.rat.getUUID(), target, quarry.getBlockPos());
				this.retargetAfterBlockedWork(quarry);
			}
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Error moving along the ladder of quarry {}: {}", this.rat.getUUID(), exception.getMessage(), exception);
			this.stop();
		}
	}

	// Moves the rat along the quarry corner column; takes the quarry and target, returns true if the step is already done.
	private boolean moveAlongAccessColumn(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			if (!quarry.isPrimaryAccessColumnStandPos(target)) {
				return false;
			}
			int currentY = this.rat.blockPosition().getY();
			if (currentY == target.getY() || !quarry.shouldUseColumnVerticalAssist(currentY, target.getY())) {
				return false;
			}
			if (!quarry.isOnPrimaryAccessColumnSurface(this.rat.level(), this.rat.blockPosition())) {
				return false;
			}
			BlockPos currentColumn = quarry.getPrimaryAccessColumnStandPos(currentY);
			double targetX = currentColumn.getX() + 0.5D;
			double targetZ = currentColumn.getZ() + 0.5D;
			if (Math.abs(this.rat.getX() - targetX) > 0.85D || Math.abs(this.rat.getZ() - targetZ) > 0.85D) {
				return false;
			}
			int nextY = currentY + Integer.compare(target.getY(), currentY);
			BlockPos nextStep = quarry.getPrimaryAccessColumnStandPos(nextY);
			if (!quarry.isPrimaryAccessColumnStandPos(nextStep)) {
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Quarry {} could not move rat {} along the pillar through {}", quarry.getBlockPos(), this.rat.getUUID(), nextStep);
				return false;
			}
			if (!quarry.hasAccessColumnPlatformBelow(this.rat.level(), nextStep)) {
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Quarry {} did not move rat {} along the pillar to {} without a platform", quarry.getBlockPos(), this.rat.getUUID(), nextStep);
				return false;
			}
			this.rat.getNavigation().stop();
			this.animateQuarryStep(this.rat.blockPosition(), nextStep);
			this.rat.setDeltaMovement(Vec3.ZERO);
			this.rat.setPos(targetX, nextY, targetZ);
			return true;
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Error moving rat {} along the pillar of quarry {}", this.rat.getUUID(), quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Safely brings the rat to the work standing spot level via the corner column; takes the quarry and standing spot, returns true if the step is done.
	private boolean moveAlongAccessColumnToStandLevel(RatQuarryBlockEntity quarry, BlockPos standPos) {
		try {
			int currentY = this.rat.blockPosition().getY();
			if (!quarry.shouldUseColumnVerticalAssist(currentY, standPos.getY())) {
				return false;
			}
			BlockPos columnTarget = quarry.getPrimaryAccessColumnStandPos(standPos.getY());
			if (this.moveAlongAccessColumn(quarry, columnTarget)) {
				return true;
			}
			BlockPos approach = quarry.getPrimaryAccessColumnStandPos(currentY);
			if (!quarry.isOnPrimaryAccessColumnSurface(this.rat.level(), this.rat.blockPosition())) {
				return this.moveQuarryGridHorizontally(quarry, approach);
			}
			return false;
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Rat {} failed to safely traverse the shaft of quarry {}", this.rat.getUUID(), quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Leads the rat inside the quarry along the safe grid; takes the quarry and foot point, returns true if a movement step was done.
	private boolean moveQuarryGridTo(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			BlockPos current = this.rat.blockPosition();
			if (current.equals(target)) {
				return false;
			}
			this.rat.getNavigation().stop();
			this.rat.setDeltaMovement(Vec3.ZERO);
			if (current.getY() != target.getY()) {
				BlockPos currentColumn = quarry.getPrimaryAccessColumnStandPos(current.getY());
				if (!current.equals(currentColumn)) {
					return this.moveQuarryGridHorizontally(quarry, currentColumn);
				}
				int nextY = current.getY() + Integer.compare(target.getY(), current.getY());
				BlockPos nextStep = quarry.getPrimaryAccessColumnStandPos(nextY);
				if (!quarry.hasAccessColumnPlatformBelow(this.rat.level(), nextStep)) {
					com.github.alexthe666.rats.RatsMod.LOGGER.warn("Rat {} cannot climb the shaft of quarry {} to {} without a platform underfoot", this.rat.getUUID(), quarry.getBlockPos(), nextStep);
					return false;
				}
				this.animateQuarryStep(current, nextStep);
				this.rat.setPos(nextStep.getX() + 0.5D, nextStep.getY(), nextStep.getZ() + 0.5D);
				return true;
			}
			return this.moveQuarryGridHorizontally(quarry, target);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Rat {} failed to walk the grid of quarry {} to {}", this.rat.getUUID(), quarry.getBlockPos(), target, exception);
			return false;
		}
	}

	// Moves the rat horizontally strictly cell by cell; takes the quarry and foot target, returns true if the step is done.
	private boolean moveQuarryGridHorizontally(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			BlockPos current = this.rat.blockPosition();
			if (current.equals(target)) {
				return false;
			}
			BlockPos next = quarry.findSafeHorizontalGridStep(this.rat.level(), current, target);
			if (next == null) {
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Rat {} found no safe horizontal step in quarry {} from {} to {}", this.rat.getUUID(), quarry.getBlockPos(), current, target);
				return false;
			}
			this.rat.getNavigation().stop();
			this.animateQuarryStep(current, next);
			this.rat.setDeltaMovement(Vec3.ZERO);
			this.rat.setPos(next.getX() + 0.5D, next.getY(), next.getZ() + 0.5D);
			return true;
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Rat {} failed to walk the cells to {} in quarry {}", this.rat.getUUID(), target, quarry.getBlockPos(), exception);
			return false;
		}
	}

	// Turns the rat and starts the step animation; takes the current and next cell, returns nothing.
	private void animateQuarryStep(BlockPos current, BlockPos next) {
		try {
			double dx = next.getX() - current.getX();
			double dz = next.getZ() - current.getZ();
			if (dx == 0.0D && dz == 0.0D) {
				return;
			}
			float yaw = (float) (Math.atan2(dz, dx) * (180F / (float) Math.PI)) - 90.0F;
			this.rat.setYRot(yaw);
			this.rat.yBodyRot = yaw;
			this.rat.yHeadRot = yaw;
			this.rat.yRotO = yaw;
			this.rat.getLookControl().setLookAt(next.getX() + 0.5D, next.getY() + 0.5D, next.getZ() + 0.5D);
			this.rat.walkAnimation.update(1.0F, 0.4F);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Failed to update the step animation of rat {} from {} to {}", this.rat.getUUID(), current, next, exception);
			throw exception;
		}
	}

	// Mines the current block on the layer.
	private void tickMining() {
		try {
			BlockPos target = this.getTargetBlock();
			RatQuarryBlockEntity quarry = this.getQuarry();
			if (target == null || quarry == null) {
				this.stop();
				return;
			}
			BlockPos standPos = quarry.getStandPosForTarget(this.rat.level(), target, this.rat.blockPosition());
			if (this.isStandingOnTarget(target) && this.moveOffTargetBlock(quarry, target, standPos)) {
				this.mode = QuarryMode.MOVE_TO_LAYER;
				return;
			}
			if (!this.isAtStandPos(standPos) && !this.canMineFromCurrentPosition(quarry, target)) {
				this.mode = QuarryMode.MOVE_TO_LAYER;
				return;
			}
			if (this.rat.isInLava() && this.rat.level().getBlockState(this.rat.blockPosition().above()).isAir()) {
				this.rat.level().setBlockAndUpdate(this.rat.blockPosition().above(), Blocks.WATER.defaultBlockState());
				this.rat.heal(15.0F);
				this.rat.getJumpControl().jump();
			}
			BlockState block = this.rat.level().getBlockState(target);
			if (quarry.isSupportTarget(target) && (block.isAir() || block.canBeReplaced() || block.liquid())) {
				this.finishSupportTarget(quarry, target);
				return;
			}
			if (quarry.isFluidSealTarget(this.rat.level(), target)) {
				this.finishFluidSealTarget(quarry, target);
				return;
			}
			if (!this.canMineBlock(quarry, target) || !this.canReachTargetFromCurrentPosition(quarry, target)) {
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Rat {} cannot mine {} in quarry {}, reassigning", this.rat.getUUID(), target, quarry.getBlockPos());
				this.retargetAfterBlockedWork(quarry);
				return;
			}
			this.mineTargetBlock(quarry, target, block);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Quarry mining error for rat {}", this.rat.getUUID(), exception);
			this.stop();
			throw exception;
		}
	}

	// Checks whether the rat stands on the block being mined; takes the target, returns true if the target is at or below its feet.
	private boolean isStandingOnTarget(BlockPos target) {
		try {
			BlockPos current = this.rat.blockPosition();
			return target.equals(current) || target.equals(current.below());
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Failed to check the position of rat {} above target {}", this.rat.getUUID(), target, exception);
			throw exception;
		}
	}

	// Moves the rat off the target block; takes the quarry, target and safe standing spot, returns true if movement started or finished.
	private boolean moveOffTargetBlock(RatQuarryBlockEntity quarry, BlockPos target, BlockPos standPos) {
		try {
			if (quarry.isSupportTarget(target)) {
				BlockPos current = this.rat.blockPosition();
				if (target.equals(current) || target.equals(current.below())) {
					quarry.snapRatOntoSupportPlatform(this.rat.level(), this.rat, target);
					return true;
				}
				return false;
			}
			double targetX = standPos.getX() + 0.5D;
			double targetZ = standPos.getZ() + 0.5D;
			double distance = this.rat.distanceToSqr(targetX, standPos.getY(), targetZ);
			if (distance <= 5.0D && this.rat.level().getBlockState(standPos).isAir() && this.rat.level().getBlockState(standPos.above()).isAir()) {
				this.rat.getNavigation().stop();
				this.rat.setDeltaMovement(Vec3.ZERO);
				this.rat.setPos(targetX, standPos.getY(), targetZ);
			} else {
				return this.moveQuarryGridHorizontally(quarry, standPos);
			}
			return true;
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Rat {} failed to step off target {} of quarry {}", this.rat.getUUID(), target, quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Mines the current target block; takes the quarry, target and state, returns nothing until mining finishes.
	private void mineTargetBlock(RatQuarryBlockEntity quarry, BlockPos target, BlockState block) {
		try {
			SoundType soundType = block.getBlock().getSoundType(block, this.rat.level(), target, null);
			this.rat.getNavigation().stop();
			this.rat.level().broadcastEntityEvent(this.rat, (byte) 85);
			this.rat.crafting = true;
			if (block != this.prevMiningState) {
				this.rat.level().broadcastEntityEvent(this.rat, (byte) 86);
				this.rat.crafting = false;
			}
			this.breakingTime++;
			int hardness = Math.max(1, (int) (block.getDestroySpeed(this.rat.level(), target) * 10));
			int progress = (int) ((float) this.breakingTime / hardness * 10.0F);
			if (this.breakingTime % 5 == 0) {
				this.rat.playSound(soundType.getHitSound(), soundType.getVolume() + 1, soundType.getPitch());
			}
			if (progress != this.previousBreakProgress) {
				this.rat.level().destroyBlockProgress(this.rat.getId(), target, progress);
				this.previousBreakProgress = progress;
			}
			if (this.breakingTime >= hardness) {
				this.finishMinedTarget(quarry, target, block, soundType);
			}
			this.prevMiningState = block;
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Failed to mine target {} with quarry {}", target, quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finishes mining a block; takes the quarry, target, state and sound, moves the drops and updates the target.
	private void finishMinedTarget(RatQuarryBlockEntity quarry, BlockPos target, BlockState state, SoundType soundType) {
		try {
			this.rat.level().broadcastEntityEvent(this.rat, (byte) 86);
			this.rat.playSound(soundType.getBreakSound(), soundType.getVolume(), soundType.getPitch());
			this.rat.playSound(SoundEvents.ITEM_PICKUP, 1, 1F);
			this.breakingTime = 0;
			this.previousBreakProgress = -1;
			if (!state.is(Blocks.SPONGE) && !state.is(Blocks.WET_SPONGE)) {
				this.holdItemHarvestedIfPossible(this.rat, Block.getDrops(state, (ServerLevel) this.rat.level(), target, null, this.rat, this.rat.getMainHandItem()));
			}
			this.rat.level().destroyBlock(target, false);
			if (quarry.isSupportTarget(target)) {
				quarry.placeSupport(this.rat.level(), target);
				quarry.snapRatOntoSupportPlatform(this.rat.level(), this.rat, target);
			}
			quarry.sealExposedFluidsAfterMining(this.rat.level(), target);
			quarry.completeTarget(this.rat.getUUID(), target);
			this.stop();
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Failed to finish mining target {} with quarry {}", target, quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finishes fluid sealing; takes the quarry and target, replaces water or lava with cobblestone without mining an item.
	private void finishFluidSealTarget(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			if (quarry.sealFluidBlock(this.rat.level(), target)) {
				quarry.completeTarget(this.rat.getUUID(), target);
				this.stop();
			} else {
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Rat {} failed to plug fluid {} with quarry {}", this.rat.getUUID(), target, quarry.getBlockPos());
				this.retargetAfterBlockedWork(quarry);
			}
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Failed to finish sealing fluid {} with quarry {}", target, quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finishes an empty support target; takes the quarry and target, places a platform without mining.
	private void finishSupportTarget(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			if (quarry.placeSupport(this.rat.level(), target)) {
				quarry.snapRatOntoSupportPlatform(this.rat.level(), this.rat, target);
				quarry.completeTarget(this.rat.getUUID(), target);
				this.stop();
			} else {
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Rat {} failed to place platform {} with quarry {}", this.rat.getUUID(), target, quarry.getBlockPos());
				this.retargetAfterBlockedWork(quarry);
			}
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Failed to finish support {} with quarry {}", target, quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	private RatQuarryBlockEntity getQuarry() {
		GlobalPos quarryPos = this.rat.getDepositPos().orElse(null);
		if (quarryPos == null || !quarryPos.dimension().equals(this.rat.level().dimension())) {
			return null;
		}
		return this.rat.level().getBlockEntity(quarryPos.pos()) instanceof RatQuarryBlockEntity quarry ? quarry : null;
	}

	// Checks whether the target can be mined; takes the quarry and position, returns true for a regular or support block.
	private boolean canMineBlock(RatQuarryBlockEntity quarry, BlockPos rayPos) {
		BlockPos feet = this.rat.blockPosition();
		if (!quarry.isSupportTarget(rayPos) && !quarry.isInsideWorkVolume(rayPos)) {
			return false;
		}
		if (!quarry.isSupportTarget(rayPos) && (rayPos.equals(feet) || rayPos.equals(feet.below()))) {
			return false;
		}
		if (quarry.isQuarryLadderProtected(this.rat.level(), rayPos) && !quarry.isSupportTarget(rayPos)) {
			return false;
		}
		BlockState state = this.rat.level().getBlockState(rayPos);
		if (state.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) || state.liquid() || this.rat.level().getBlockEntity(rayPos) != null) {
			return false;
		}
		return !state.is(RatsBlockTags.QUARRY_IGNORABLES) && this.doesListContainBlock(this.rat.level(), rayPos) && RatUtils.canRatBreakBlock(this.rat.level(), rayPos, this.rat);
	}

	// Checks whether the rat can dig from its current spot; takes the quarry and target, returns true for a safe nearby position.
	private boolean canMineFromCurrentPosition(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			BlockPos current = this.rat.blockPosition();
			if (quarry.isSupportTarget(target)) {
				BlockPos placementStand = quarry.getSupportPlacementStandPos(this.rat.level(), target, current);
				if (placementStand != null && quarry.canPlaceSupportFromStand(current, target)) {
					return true;
				}
				return target.equals(current.below()) || this.canReachSupportTargetFromCurrentPosition(target, current);
			}
			if (quarry.isFluidSealTarget(this.rat.level(), target)) {
				return quarry.canReachFluidSealBlockFromStand(current, target);
			}
			if (quarry.canReachLayerBlockFromStand(current, target)) {
				return true;
			}
			BlockState feet = this.rat.level().getBlockState(current);
			BlockState head = this.rat.level().getBlockState(current.above());
			if ((!feet.isAir() && !feet.canBeReplaced() && !feet.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) || feet.liquid() || (!head.isAir() && !head.canBeReplaced()) || head.liquid()) {
				return false;
			}
			if (target.equals(current) || target.equals(current.below())) {
				return false;
			}
			return this.canReachTargetFromCurrentPosition(quarry, target);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Failed to check the current position of rat {} for target {}", this.rat.getUUID(), target, exception);
			throw exception;
		}
	}

	private boolean canReachTargetFromCurrentPosition(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			if (quarry.isSupportTarget(target)) {
				BlockPos current = this.rat.blockPosition();
				if (quarry.canPlaceSupportFromStand(current, target)) {
					return true;
				}
				return target.equals(current.below()) || this.canReachSupportTargetFromCurrentPosition(target, current);
			}
			BlockPos current = this.rat.blockPosition();
			if (quarry.isFluidSealTarget(this.rat.level(), target)) {
				return quarry.canReachFluidSealBlockFromStand(current, target);
			}
			if (quarry.canReachLayerBlockFromStand(current, target)) {
				return true;
			}
			int dx = Math.abs(target.getX() - current.getX());
			int dy = target.getY() - current.getY();
			int dz = Math.abs(target.getZ() - current.getZ());
			return dx <= 2 && dz <= 2 && dy >= -2 && dy <= 1 && this.rat.getRatDistanceCenterSq(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) < this.rat.getRatHarvestDistance(3.0D);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Failed to check reach of target {} with quarry {}", target, quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks reach of a support block; takes the target and current foot position, returns true for an adjacent block.
	private boolean canReachSupportTargetFromCurrentPosition(BlockPos target, BlockPos current) {
		try {
			int dx = Math.abs(target.getX() - current.getX());
			int dy = target.getY() - current.getY();
			int dz = Math.abs(target.getZ() - current.getZ());
			return dx <= 1 && dz <= 1 && dy >= -1 && dy <= 0 && !target.equals(current) && this.rat.getRatDistanceCenterSq(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) < this.rat.getRatHarvestDistance(2.0D);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Failed to check support reach {} for rat {}", target, this.rat.getUUID(), exception);
			throw exception;
		}
	}

}