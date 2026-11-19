package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.block.RatHouseBlock;
import com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;

public class RatReturnToHouseGoal extends Goal {
	private static final double ENTRY_ACCEPT_DISTANCE_SQR = 9.0D;
	private static final double HOUSE_ACCEPT_DISTANCE_SQR = 6.25D;
	private final TamedRat rat;
	private final double speedModifier;
	private BlockPos housePos;
	private Vec3 entryCenter;
	private int nextStartTick;

	// Creates the go-to-house goal; takes the rat and movement speed.
	public RatReturnToHouseGoal(TamedRat rat, double speedModifier) {
		this.rat = rat;
		this.speedModifier = speedModifier;
		this.nextStartTick = rat.getRandom().nextInt(20);
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	// Checks whether the rat needs to go to its bound house; no parameters, returns true if the house is valid.
	@Override
	public boolean canUse() {
		try {
			if (this.nextStartTick > 0) {
				this.nextStartTick--;
				return false;
			}
			this.nextStartTick = this.adjustedTickDelay(10 + this.rat.getRandom().nextInt(11));
			if (!this.rat.canMove() || this.rat.isInCage() || this.rat.isPassenger()) {
				return false;
			}
			if (!RatDaySchedule.shouldSleep(this.rat.level())) {
				return false;
			}
			Optional<GlobalPos> house = this.rat.getBoundHousePos();
			if (house.isEmpty() || !house.get().dimension().equals(this.rat.level().dimension())) {
				return false;
			}
			BlockPos pos = house.get().pos();
			if (this.rat.distanceToSqr(Vec3.atCenterOf(pos)) > (double) RatConfig.pathfindHomeWorkDistance * RatConfig.pathfindHomeWorkDistance) {
				this.rat.setLastPathFailure("house " + pos + " beyond the limit " + RatConfig.pathfindHomeWorkDistance);
				return false;
			}
			BlockState state = this.rat.level().getBlockState(pos);
			if (!(state.getBlock() instanceof RatHouseBlock)) {
				RatsMod.LOGGER.warn("Rat {} lost its bound house {}: the block is not a rat_house", this.rat.getUUID(), pos);
				this.rat.setBoundHousePos(null);
				return false;
			}
			BlockEntity blockEntity = this.rat.level().getBlockEntity(pos);
			if (!(blockEntity instanceof RatHouseBlockEntity houseEntity) || houseEntity.hasStoredRat() || !houseEntity.canAcceptRat(this.rat)) {
				return false;
			}
			this.housePos = pos;
			this.entryCenter = RatHouseBlock.getEntryCenter(state, pos);
			this.rat.clearLastPathFailure();
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to start the path of rat {} to the house: {}", this.rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Continues the path while the rat is alive and has not entered the house yet; no parameters.
	@Override
	public boolean canContinueToUse() {
		return this.rat.isAlive()
				&& this.housePos != null
				&& this.entryCenter != null
				&& RatDaySchedule.shouldSleep(this.rat.level())
				&& (!this.rat.getNavigation().isDone() || this.isCloseEnoughToEnter());
	}

	// Starts moving to the house entrance; no parameters.
	@Override
	public void start() {
		try {
			if (this.entryCenter == null) {
				throw new IllegalStateException("House entrance is not set");
			}
			boolean pathStarted = this.rat.moveSmoothlyTo(this.entryCenter.x, this.entryCenter.y, this.entryCenter.z, this.speedModifier);
			if (!pathStarted) {
				this.rat.setLastPathFailure("no path to the house " + this.housePos);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to start moving rat {} to house {}: {}", this.rat.getUUID(), this.housePos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Checks whether the entrance was reached and puts the rat inside the house; no parameters.
	@Override
	public void tick() {
		try {
			if (this.housePos == null || this.entryCenter == null) {
				return;
			}
			if (!this.isCloseEnoughToEnter()) {
				return;
			}
			BlockEntity blockEntity = this.rat.level().getBlockEntity(this.housePos);
			if (blockEntity instanceof RatHouseBlockEntity houseEntity && houseEntity.storeRat(this.rat)) {
				this.rat.clearLastPathFailure();
				this.rat.getNavigation().stop();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to put rat {} into house {}: {}", this.rat.getUUID(), this.housePos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Checks closeness to the house entrance; no parameters, returns true if the path brought the rat close enough to enter.
	private boolean isCloseEnoughToEnter() {
		try {
			if (this.housePos == null || this.entryCenter == null) {
				return false;
			}
			if (this.rat.distanceToSqr(this.entryCenter) <= ENTRY_ACCEPT_DISTANCE_SQR) {
				return true;
			}
			if (this.rat.distanceToSqr(Vec3.atCenterOf(this.housePos)) <= HOUSE_ACCEPT_DISTANCE_SQR) {
				return true;
			}
			BlockPos entryBlock = BlockPos.containing(this.entryCenter);
			BlockPos ratPos = this.rat.blockPosition();
			return ratPos.distManhattan(entryBlock) <= 2 && Math.abs(this.rat.getY() - this.entryCenter.y) <= 2.0D;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check how close rat {} is to the entrance of house {}", this.rat.getUUID(), this.housePos, exception);
			throw exception;
		}
	}

	// Resets temporary path points; no parameters.
	@Override
	public void stop() {
		this.housePos = null;
		this.entryCenter = null;
	}
}
