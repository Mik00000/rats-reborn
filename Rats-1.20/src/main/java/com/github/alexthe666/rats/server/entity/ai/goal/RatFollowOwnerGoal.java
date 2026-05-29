package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.server.entity.RatMount;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RatFollowOwnerGoal extends Goal {
	private final TamedRat rat;
	private final double speedModifier;
	private final float startDistance;
	private final float stopDistance;
	private LivingEntity owner;
	private int timeToRecalcPath;

	public RatFollowOwnerGoal(TamedRat rat, double speedModifier, float startDist, float stopDist) {
		this.rat = rat;
		this.speedModifier = speedModifier;
		this.startDistance = startDist;
		this.stopDistance = stopDist;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (this.isUnableToMove()) {
			return false;
		}
		if (!this.rat.canMove() || !this.rat.isFollowing()) {
			return false;
		}
		LivingEntity livingentity = this.rat.getOwner();
		if (livingentity == null) {
			return false;
		}
		if (livingentity.isSpectator()) {
			return false;
		}
		if (this.rat.distanceToSqr(livingentity) < (double) (this.startDistance * this.startDistance)) {
			return false;
		}
		this.owner = livingentity;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.isUnableToMove()) {
			return false;
		}
		if (!this.rat.isFollowing()) {
			return false;
		}
		if (this.rat.getOwner() == null) {
			return false;
		}
		if (this.rat.getOwner().isSpectator()) {
			return false;
		}
		return this.rat.distanceToSqr(this.owner) > (double) (this.stopDistance * this.stopDistance);
	}

	private boolean isUnableToMove() {
		return this.rat.isOrderedToSit() || this.rat.getVehicle() instanceof Player || this.rat.isLeashed();
	}

	@Override
	public void start() {
		this.timeToRecalcPath = 0;
		this.rat.getNavigation().stop();
	}

	@Override
	public void stop() {
		this.rat.getNavigation().stop();
	}

	@Override
	public void tick() {
		this.rat.getLookControl().setLookAt(this.owner, 10.0F, (float) this.rat.getMaxHeadXRot());
		if (--this.timeToRecalcPath <= 0) {
			this.timeToRecalcPath = this.adjustedTickDelay(10);
			if (this.rat.distanceToSqr(this.owner) >= (this.rat.hasFlightUpgrade() ? 800.0D : 400.0D)) {
				if (!this.maybeTeleportMount()) {
					this.teleportToOwner();
				}
			} else {
				if (this.rat.hasFlightUpgrade()) {
					this.rat.getNavigation().moveTo(this.owner.getX(), this.owner.getY() + 2.5D, this.owner.getZ(), this.speedModifier);
					this.rat.setFlying(true);
				} else {
					this.rat.getNavigation().moveTo(this.owner, this.speedModifier);
				}
			}
		}
	}

	private void teleportToOwner() {
		BlockPos blockpos = this.owner.blockPosition();

		for (int i = 0; i < 10; ++i) {
			int j = this.rat.getRandom().nextIntBetweenInclusive(-3, 3);
			int k = this.rat.getRandom().nextIntBetweenInclusive(-1, 1);
			int l = this.rat.getRandom().nextIntBetweenInclusive(-3, 3);
			if (this.canTeleportTo(blockpos.offset(j, k, l))) {
				this.rat.moveTo((double) blockpos.getX() + 0.5D + j, blockpos.getY() + k, (double) blockpos.getZ() + 0.5D + l, this.rat.getYRot(), this.rat.getXRot());
				this.rat.getNavigation().stop();
				return;
			}
		}
	}

	private boolean canTeleportTo(BlockPos pos) {
		Level level = this.rat.level();
		BlockPos feet = BlockPos.containing(pos.getX(), this.rat.getBoundingBox().minY, pos.getZ());
		return level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir();
	}

	private boolean maybeTeleportMount() {
		if (this.rat.getVehicle() instanceof RatMount mount && mount.shouldTeleportWhenFarAway()) {
			BlockPos blockpos = this.owner.blockPosition();

			for (int i = 0; i < 10; ++i) {
				int j = this.rat.getRandom().nextIntBetweenInclusive(-3, 3);
				int k = this.rat.getRandom().nextIntBetweenInclusive(-1, 1);
				int l = this.rat.getRandom().nextIntBetweenInclusive(-3, 3);
				if (this.attemptTeleportEntity(this.rat.getVehicle(), blockpos.getX() + j, blockpos.getY() + k, blockpos.getZ() + l)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean attemptTeleportEntity(Entity mount, int x, int y, int z) {
		if (Math.abs((double) x - this.owner.getX()) < 2.0D && Math.abs((double) z - this.owner.getZ()) < 2.0D) {
			return false;
		}
		if (!this.canTeleportTo(new BlockPos(x, y, z))) {
			return false;
		}
		mount.moveTo((double) x + 0.5D, y, (double) z + 0.5D, this.rat.getYRot(), this.rat.getXRot());
		if (mount instanceof Mob mob) {
			mob.getNavigation().stop();
		}
		this.rat.getNavigation().stop();
		return true;
	}
}
