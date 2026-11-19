package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.compat.farmersdelight.FarmersDelightCompat;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;

public class FarmersDelightChefGoal extends Goal {
	private static final double WORK_REACH_EXPANSION = 1.0D;
	private static final double SEAT_WORK_REACH = 8.0D;
	private final TamedRat rat;
	private BlockPos workPos;
	private BlockPos pickupPos;
	private BlockPos depositPos;
	private int nextStartTick;

	// Creates the Farmer's Delight chef goal; takes the tamed rat, stores it and sets the movement flags.
	public FarmersDelightChefGoal(TamedRat rat) {
		try {
			this.rat = rat;
			this.nextStartTick = rat.getRandom().nextInt(30);
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to create the Farmer's Delight chef goal for rat {}", rat.getUUID(), exception);
			throw exception;
		}
	}

	// Checks whether the goal can start; no parameters, returns true if the rat is ready to work at an FD block.
	@Override
	public boolean canUse() {
		try {
			if (this.nextStartTick > 0) {
				this.nextStartTick--;
				return false;
			}
			this.nextStartTick = this.adjustedTickDelay(20 + this.rat.getRandom().nextInt(20));
			if (!RatsMod.FARMERS_DELIGHT_LOADED || !this.canRatWorkAsChef()) {
				return false;
			}
			Optional<GlobalPos> work = this.rat.getCreateWorkPos();
			Optional<GlobalPos> pickup = this.rat.getPickupPos();
			Optional<GlobalPos> deposit = this.rat.getDepositPos();
			if (work.isEmpty() || pickup.isEmpty() || deposit.isEmpty()) {
				return false;
			}
			if (!work.get().dimension().equals(this.rat.level().dimension())
					|| !pickup.get().dimension().equals(this.rat.level().dimension())
					|| !deposit.get().dimension().equals(this.rat.level().dimension())) {
				return false;
			}
			this.workPos = work.get().pos();
			this.pickupPos = pickup.get().pos();
			this.depositPos = deposit.get().pos();
			if (RatUtils.isBlockProtected(this.rat.level(), this.workPos, this.rat)
					|| RatUtils.isBlockProtected(this.rat.level(), this.pickupPos, this.rat)
					|| RatUtils.isBlockProtected(this.rat.level(), this.depositPos, this.rat)) {
				return false;
			}
			BlockEntity blockEntity = this.rat.level().getBlockEntity(this.workPos);
			return blockEntity != null && FarmersDelightCompat.isSupportedWorkBlock(blockEntity);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to check starting the Farmer's Delight chef goal", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Checks whether the goal continues; no parameters, returns true while the work block is kept and the rat can work.
	@Override
	public boolean canContinueToUse() {
		try {
			if (this.workPos == null || !this.canRatWorkAsChef()) {
				return false;
			}
			BlockEntity blockEntity = this.rat.level().getBlockEntity(this.workPos);
			return blockEntity != null && FarmersDelightCompat.isSupportedWorkBlock(blockEntity);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to check continuing the Farmer's Delight chef goal", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Starts the goal; no parameters, marks the rat as working.
	@Override
	public void start() {
		try {
			this.rat.isCurrentlyWorking = true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to start the Farmer's Delight chef goal", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Stops the goal; no parameters, clears the state and sets a short pause before the next cycle.
	@Override
	public void stop() {
		try {
			this.rat.isCurrentlyWorking = false;
			this.workPos = null;
			this.pickupPos = null;
			this.depositPos = null;
			this.nextStartTick = 10;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to stop the Farmer's Delight chef goal", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Runs a goal step; no parameters, leads the rat to the use point or works from the seat.
	@Override
	public void tick() {
		try {
			if (this.workPos == null || this.pickupPos == null || this.depositPos == null) {
				this.stop();
				return;
			}
			if (this.canWorkFromCurrentSeat()) {
				this.performChefCycle();
				return;
			}
			if (this.rat.isPassenger()) {
				this.rat.stopRiding();
			}
			Vec3 movePos = Vec3.atBottomCenterOf(this.workPos.relative(this.rat.createWorkFacing));
			this.rat.moveSmoothlyTo(movePos.x(), movePos.y(), movePos.z(), this.rat.getHungerAdjustedWorkSpeed(1.25D));
			double distance = this.rat.distanceToSqr(movePos.x(), movePos.y(), movePos.z());
			double reach = this.rat.getRatHarvestDistance(WORK_REACH_EXPANSION);
			if (distance <= reach * reach) {
				this.performChefCycle();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to run a Farmer's Delight chef goal step", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Checks the basic chef work conditions; no parameters, returns true if the command, upgrade and state are suitable.
	private boolean canRatWorkAsChef() {
		try {
			return this.rat.canMove()
					&& this.rat.canWorkBySchedule()
					&& this.rat.canWorkFromHunger()
					&& !this.rat.isInCage()
					&& this.rat.getTarget() == null
					&& this.rat.getCommand() == RatCommand.HARVEST
					&& RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_CHEF.get());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to check the Farmer's Delight chef work conditions", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Runs the work cycle at the block; no parameters, calls the compat layer and finishes the goal after the attempt.
	private void performChefCycle() {
		try {
			boolean worked = FarmersDelightCompat.tryRunChefCycle(this.rat, this.workPos, this.pickupPos, this.depositPos);
			if (!worked) {
				RatsMod.LOGGER.debug("Rat {} did not perform a Farmer's Delight action at {}", this.rat.getUUID(), this.workPos);
			}
			this.stop();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to run the Farmer's Delight work cycle at {}", this.rat.getUUID(), this.workPos, exception);
			throw exception;
		}
	}

	// Checks working from a seat; no parameters, returns true if the rat sits on a seat and the FD block is in range.
	private boolean canWorkFromCurrentSeat() {
		try {
			return this.isMountedOnSeat()
					&& this.workPos != null
					&& this.rat.distanceToSqr(Vec3.atCenterOf(this.workPos)) <= SEAT_WORK_REACH * SEAT_WORK_REACH;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to check working from a Farmer's Delight seat", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Checks whether the rat sits on a seat entity; no parameters, returns true based on the seat entity class name.
	private boolean isMountedOnSeat() {
		try {
			Entity vehicle = this.rat.getVehicle();
			if (vehicle == null) {
				return false;
			}
			String name = vehicle.getClass().getName().toLowerCase();
			return name.contains("seat");
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to check the Farmer's Delight seat", this.rat.getUUID(), exception);
			throw exception;
		}
	}
}
