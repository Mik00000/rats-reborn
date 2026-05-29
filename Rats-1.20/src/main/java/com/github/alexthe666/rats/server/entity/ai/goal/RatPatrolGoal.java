package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.block.RatTubeBlock;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class RatPatrolGoal extends Goal {

	private final TamedRat rat;
	private GlobalPos nextNode;
	private int nodeIndex = 0;

	public RatPatrolGoal(TamedRat rat) {
		this.rat = rat;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		return this.rat.canMove()
				&& this.rat.isPatrolCommand()
				&& !this.rat.getPatrolNodes().isEmpty()
				&& (this.rat.getTarget() == null || !this.rat.getTarget().isAlive());
	}

	@Override
	public boolean canContinueToUse() {
		return this.canUse();
	}

	@Override
	public void start() {
		this.nodeIndex = 0;
		this.nextNode = null;
	}

	@Override
	public void tick() {
		List<GlobalPos> nodes = this.rat.getPatrolNodes();
		if (nodes.isEmpty()) {
			return;
		}
		if (this.nodeIndex < 0 || this.nodeIndex >= nodes.size()) {
			this.nodeIndex = 0;
		}
		if (this.nextNode == null || !this.nextNode.dimension().equals(this.rat.level().dimension())) {
			this.nextNode = nodes.get(this.nodeIndex);
		}
		if (!this.nextNode.dimension().equals(this.rat.level().dimension())) {
			RatsMod.LOGGER.warn("Крыса {}: точка патруля {} в другом измерении, пропуск", this.rat.getUUID(), this.nextNode);
			this.advanceNode(nodes);
			return;
		}
		if (this.rat.distanceToSqr(Vec3.atCenterOf(this.nextNode.pos())) <= this.rat.getRatHarvestDistance(0.0D)) {
			this.advanceNode(nodes);
			return;
		}
		Vec3 target = Vec3.atBottomCenterOf(this.nextNode.pos());
		double speed = this.rat.isInTube() ? RatTubeBlock.TUBE_SPEED_MULTIPLIER : 1.0D;
		this.rat.getNavigation().moveTo(target.x(), target.y(), target.z(), speed);
	}

	private void advanceNode(List<GlobalPos> nodes) {
		++this.nodeIndex;
		if (this.nodeIndex >= nodes.size()) {
			this.nodeIndex = 0;
		}
		this.nextNode = nodes.get(this.nodeIndex);
	}
}
