package com.github.alexthe666.rats.server.entity.ai.goal.harvest;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RatPlaceGoal extends BaseRatHarvestGoal {
	private final TamedRat rat;

	public RatPlaceGoal(TamedRat rat) {
		super(rat);
		this.rat = rat;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (!super.canUse() || !this.checkTheBasics(false, false) || !this.holdingBlock()) {
			return false;
		}
		this.resetTarget();
		return this.getTargetBlock() != null;
	}

	private boolean holdingBlock() {
		ItemStack stack = this.rat.getItemInHand(InteractionHand.MAIN_HAND);
		return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
	}

	@Override
	public boolean canContinueToUse() {
		return this.getTargetBlock() != null && this.holdingBlock() && this.rat.level().getBlockState(this.getTargetBlock()).canBeReplaced();
	}

	@Override
	public void tick() {
		try {
			if (this.getTargetBlock() != null && this.holdingBlock()) {
				this.placeHeldBlock();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to place a block at {}", this.rat.getUUID(), this.getTargetBlock(), exception);
			throw exception;
		}
	}

	// Places a block from the rat's paw; takes the current target state from the goal field, returns nothing.
	private void placeHeldBlock() {
		try {
			ItemStack stack = this.rat.getItemInHand(InteractionHand.MAIN_HAND);
			BlockItem blockItem = (BlockItem) stack.getItem();
			BlockPos moveToPos = this.getTargetBlock();
			this.rat.moveSmoothlyTo(moveToPos.getX() + 0.5D, moveToPos.getY(), moveToPos.getZ() + 0.5D, this.rat.getHungerAdjustedWorkSpeed(1.25D));
			if (!this.rat.level().getBlockState(moveToPos).canBeReplaced() || !RatUtils.canRatPlaceBlock(this.rat.level(), moveToPos, this.rat)) {
				RatsMod.LOGGER.warn("Rat {} cannot place {} at {}: the spot is occupied or protected", this.rat.getUUID(), stack.getHoverName().getString(), moveToPos);
				this.setTargetBlock(null);
				this.stop();
				return;
			}
			double distance = this.rat.getRatDistanceCenterSq(moveToPos.getX(), moveToPos.getY(), moveToPos.getZ());
			if (distance < this.rat.getRatHarvestDistance(0.0D)) {
				ItemStack placeStack = stack.copy();
				placeStack.setCount(1);
				BlockHitResult raytrace = this.rat.level().clip(new ClipContext(new Vec3(moveToPos.getX(), moveToPos.getY(), moveToPos.getZ()), new Vec3(moveToPos.getX(), moveToPos.getY(), moveToPos.getZ()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.rat));
				BlockPlaceContext itemusecontext = new BlockPlaceContext(this.rat.level(), null, InteractionHand.MAIN_HAND, placeStack, raytrace);
				BlockState placementState = blockItem.getBlock().getStateForPlacement(itemusecontext);
				if (placementState == null || !placementState.canSurvive(this.rat.level(), moveToPos)) {
					RatsMod.LOGGER.warn("Rat {} cannot place {} at {}: the block state does not survive", this.rat.getUUID(), stack.getHoverName().getString(), moveToPos);
					this.setTargetBlock(null);
					this.stop();
					return;
				}
				stack.shrink(1);
				this.rat.level().setBlockAndUpdate(moveToPos, placementState);
				if (this.rat.isInWall()) {
					this.rat.setPos(this.rat.getX(), this.rat.getY() + 1, this.rat.getZ());
				}
				SoundType placeSound = placementState.getBlock().getSoundType(placementState, this.rat.level(), moveToPos, this.rat);
				this.rat.playSound(placeSound.getPlaceSound(), (placeSound.getVolume() + 1.0F) / 2.0F, placeSound.getPitch() * 0.8F);
				this.setTargetBlock(null);
				this.stop();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to finish placing block {}", this.rat.getUUID(), this.getTargetBlock(), exception);
			throw exception;
		}
	}

	private void resetTarget() {
		BlockPos newTarget = null;
		if (this.rat.getWorkAnchorPoint().isPresent()) {
			newTarget = this.rat.getWorkAnchorPoint().get().pos();
			if (!this.rat.level().getBlockState(newTarget).canBeReplaced()) {
				newTarget = newTarget.above();
			}
		}
		if (newTarget != null && RatUtils.canRatPlaceBlock(this.rat.level(), newTarget, this.rat)) {
			this.setTargetBlock(newTarget);
		}
	}
}
