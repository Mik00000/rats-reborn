package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RatStayCloseToHomeGoal extends Goal {
	private final TamedRat rat;
	private double wantedX;
	private double wantedY;
	private double wantedZ;
	private final double speedModifier;
	private int nextStartTick;

	public RatStayCloseToHomeGoal(TamedRat rat, double speedModifier) {
		this.rat = rat;
		this.speedModifier = speedModifier;
		this.nextStartTick = rat.getRandom().nextInt(40);
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	public boolean canUse() {
		if (this.nextStartTick > 0) {
			this.nextStartTick--;
			return false;
		}
		this.nextStartTick = this.adjustedTickDelay(20 + this.rat.getRandom().nextInt(21));
		if (this.rat.canMove() && this.rat.shouldWander() && !this.rat.isInCage() && RatDaySchedule.canWork(this.rat.level())) {
			if (this.rat.getOwner() != null && this.rat.distanceToSqr(this.rat.getOwner()) < 14.0D * 14.0D) {
				return false;
			}
			if (this.rat.getWorkAnchorPoint().isPresent() && this.rat.getWorkAnchorPoint().get().dimension().equals(this.rat.level().dimension())) {
				BlockPos homePos = this.rat.getWorkAnchorPoint().get().pos();
				if (homePos.distSqr(this.rat.blockPosition()) >= (16 * 16)) {
					Vec3 vec3 = DefaultRandomPos.getPosTowards(this.rat, 16, 7, Vec3.atBottomCenterOf(homePos), Mth.HALF_PI);
					if (vec3 == null) {
						return false;
					} else {
						this.wantedX = vec3.x;
						this.wantedY = vec3.y;
						this.wantedZ = vec3.z;
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean canContinueToUse() {
		return !this.rat.getNavigation().isDone();
	}

	public void start() {
		this.rat.moveSmoothlyTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
	}
}
