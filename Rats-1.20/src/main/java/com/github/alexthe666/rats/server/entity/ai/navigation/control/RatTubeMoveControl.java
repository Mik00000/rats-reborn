package com.github.alexthe666.rats.server.entity.ai.navigation.control;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.block.RatTubeBlock;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.NodeEvaluator;

public class RatTubeMoveControl extends RatMoveControl {

	private static final float TUBE_SPEED_MULTIPLIER = RatTubeBlock.TUBE_SPEED_MULTIPLIER;

	private final TamedRat rat;

	public RatTubeMoveControl(TamedRat rat) {
		super(rat);
		this.speedModifier = TUBE_SPEED_MULTIPLIER;
		this.rat = rat;
	}

	@Override
	public void tick() {
		try {
			if (!this.rat.isInTube()) {
				this.rat.climbingTube = false;
				this.rat.setTubeClimbDirection(0);
				super.tick();
				return;
			}
			if (this.operation == MoveControl.Operation.STRAFE) {
				float speed = (float) this.rat.getAttribute(Attributes.MOVEMENT_SPEED).getValue() * TUBE_SPEED_MULTIPLIER;
				float strafeSpeed = (float) this.speedModifier * speed;
				float forward = this.strafeForwards;
				float right = this.strafeRight;
				float length = Mth.sqrt(forward * forward + right * right);
				if (length < 1.0F) {
					length = 1.0F;
				}
				length = strafeSpeed / length;
				forward *= length;
				right *= length;
				float sin = Mth.sin(this.rat.getYRot() * Mth.DEG_TO_RAD);
				float cos = Mth.cos(this.rat.getYRot() * Mth.DEG_TO_RAD);
				float moveX = forward * cos - right * sin;
				float moveZ = right * cos + forward * sin;
				PathNavigation navigation = this.rat.getNavigation();
				NodeEvaluator evaluator = navigation.getNodeEvaluator();
				if (evaluator != null && this.rat.level().getBlockState(BlockPos.containing(this.rat.getX() + moveX, this.rat.getY(), this.rat.getZ() + moveZ)).isSolid()) {
					this.strafeForwards = 1.0F;
					this.strafeRight = 0.0F;
					strafeSpeed = speed;
				}
				this.rat.setSpeed(strafeSpeed);
				this.rat.setZza(this.strafeForwards);
				this.rat.setXxa(this.strafeRight);
				this.operation = MoveControl.Operation.WAIT;
			} else if (this.operation == MoveControl.Operation.MOVE_TO) {
				this.operation = MoveControl.Operation.WAIT;
				double deltaX = this.getWantedX() - this.mob.getX();
				double deltaZ = this.getWantedZ() - this.mob.getZ();
				double deltaY = this.getWantedY() - this.mob.getY();
				double distanceSqr = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
				if (distanceSqr < 2.5000003E-7F) {
					this.mob.setZza(0.0F);
					this.rat.climbingTube = false;
					this.rat.setTubeClimbDirection(0);
					return;
				}
				float yaw = (float) (Mth.atan2(deltaZ, deltaX) * Mth.RAD_TO_DEG) - 90.0F;
				this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yaw, 90.0F));
				float moveSpeed = (float) (this.speedModifier * this.mob.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
				this.mob.setSpeed(moveSpeed);
				if (Math.abs(deltaY) > 0.05D) {
					this.rat.climbingTube = true;
					this.rat.setTubeClimbDirection(deltaY > 0.0D ? 1 : -1);
					this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0D, Mth.clamp(deltaY, -0.35D, 0.35D) * 0.15D, 0.0D));
				} else {
					this.rat.climbingTube = false;
					this.rat.setTubeClimbDirection(0);
				}
			} else if (this.operation == MoveControl.Operation.JUMPING) {
				this.rat.setSpeed((float) (this.speedModifier * this.rat.getAttribute(Attributes.MOVEMENT_SPEED).getValue()));
				this.rat.climbingTube = true;
				this.rat.setTubeClimbDirection(1);
				if (this.rat.onGround()) {
					this.operation = MoveControl.Operation.WAIT;
				}
			} else {
				this.rat.setZza(0.0F);
				this.rat.climbingTube = false;
				this.rat.setTubeClimbDirection(0);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error moving rat {} in a tube: {}", this.rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}
}
