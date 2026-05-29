package com.github.alexthe666.rats.server.entity.ai.navigation.navigation;

import com.github.alexthe666.rats.server.entity.ai.navigation.evaluator.RatNodeEvaluator;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatTubeTransit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

public class RatNavigation extends GroundPathNavigation {
	private boolean stoppingPathForTubeTransit;
	private boolean movingWithoutTubePlanning;

	public RatNavigation(Mob mob, Level level) {
		super(mob, level);
	}

	@Override
	protected PathFinder createPathFinder(int nodes) {
		this.nodeEvaluator = new RatNodeEvaluator();
		this.nodeEvaluator.setCanPassDoors(true);
		this.nodeEvaluator.setCanOpenDoors(true);
		this.nodeEvaluator.setCanFloat(true);
		return new PathFinder(this.nodeEvaluator, nodes);
	}

	@Override
	public boolean moveTo(double x, double y, double z, double speed) {
		if (this.movingWithoutTubePlanning) {
			return super.moveTo(x, y, z, speed);
		}
		if (this.mob instanceof TamedRat rat) {
			Vec3 target = new Vec3(x, y, z);
			if (rat.isTubeTransitActive() && rat.getTubeTransit() != null && rat.getTubeTransit().targetsSame(target, 1.5D)) {
				if (rat.getTubeTransit().needsVanillaApproach()) {
					BlockPos entry = rat.getTubeTransit().entryStandPos();
					return this.moveToWithoutTubePlanning(entry.getX() + 0.5D, entry.getY(), entry.getZ() + 0.5D, rat.getTubeTransit().speed());
				}
				return true;
			}
			rat.clearTubeTransit();
			if (RatTubeTransit.tryStart(rat, target, speed) && rat.getTubeTransit() != null) {
				BlockPos entry = rat.getTubeTransit().entryStandPos();
				return this.moveToWithoutTubePlanning(entry.getX() + 0.5D, entry.getY(), entry.getZ() + 0.5D, speed);
			}
		}
		return super.moveTo(x, y, z, speed);
	}

	@Override
	public boolean moveTo(Entity entity, double speed) {
		if (this.movingWithoutTubePlanning) {
			return super.moveTo(entity, speed);
		}
		return this.moveTo(entity.getX(), entity.getY(), entity.getZ(), speed);
	}

	@Override
	public boolean moveTo(Path path, double speed) {
		if (this.movingWithoutTubePlanning) {
			return super.moveTo(path, speed);
		}
		if (path != null && this.mob instanceof TamedRat rat) {
			Vec3 target = path.getEndNode().asBlockPos().getCenter();
			if (rat.isTubeTransitActive() && rat.getTubeTransit() != null && rat.getTubeTransit().targetsSame(target, 1.5D)) {
				return true;
			}
			rat.clearTubeTransit();
			if (RatTubeTransit.tryStart(rat, target, speed) && rat.getTubeTransit() != null) {
				BlockPos entry = rat.getTubeTransit().entryStandPos();
				return this.moveToWithoutTubePlanning(entry.getX() + 0.5D, entry.getY(), entry.getZ() + 0.5D, speed);
			}
		}
		return super.moveTo(path, speed);
	}

	public boolean moveToWithoutTubePlanning(double x, double y, double z, double speed) {
		this.movingWithoutTubePlanning = true;
		try {
			return super.moveTo(x, y, z, speed);
		} finally {
			this.movingWithoutTubePlanning = false;
		}
	}

	public void stopPathOnly() {
		this.stoppingPathForTubeTransit = true;
		try {
			super.stop();
		} finally {
			this.stoppingPathForTubeTransit = false;
		}
	}

	@Override
	public void stop() {
		if (!this.stoppingPathForTubeTransit && this.mob instanceof TamedRat rat) {
			rat.clearTubeTransit();
		}
		super.stop();
	}
}
