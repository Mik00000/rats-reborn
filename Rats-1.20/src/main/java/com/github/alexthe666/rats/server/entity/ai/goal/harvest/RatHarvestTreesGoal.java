package com.github.alexthe666.rats.server.entity.ai.goal.harvest;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatTreeUtils;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class RatHarvestTreesGoal extends BaseRatHarvestGoal {
	private final TamedRat rat;
	private int breakingTime;
	private int previousBreakProgress;
	private int treeSize;
	@Nullable
	private RatTreeUtils.TreeScanResult treeScan;
	@Nullable
	private Block sapling;

	public RatHarvestTreesGoal(TamedRat rat) {
		super(rat);
		this.rat = rat;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (!super.canUse() || !this.checkTheBasics(this.rat.getDepositPos().isPresent(), this.rat.getDepositPos().isPresent())) {
			return false;
		}
		this.resetTarget();
		return this.getTargetBlock() != null;
	}

	// Finds the nearest tree within the rat's radius; takes nothing, returns nothing, sets the target via setTargetBlock.
	private void resetTarget() {
		try {
			Level level = this.rat.level();
			int radius = this.rat.getRadius();
			BlockPos center = this.rat.getSearchCenter();
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			BlockPos bestPos = null;
			Path bestPath = null;
			double bestDistance = Double.MAX_VALUE;

			for (cursor.setWithOffset(center, -radius, -radius, -radius); cursor.getX() <= center.getX() + radius; cursor.setX(cursor.getX() + 1)) {
				for (int y = -radius; y <= radius; y++) {
					cursor.setY(center.getY() + y);
					for (int z = -radius; z <= radius; z++) {
						cursor.setZ(center.getZ() + z);
						if (!RatTreeUtils.isTreeLog(level.getBlockState(cursor))) {
							continue;
						}
						if (!RatTreeUtils.isTreeBase(level, cursor)) {
							continue;
						}

						int logAmount = RatTreeUtils.calculateLogAmount(level, cursor);
						if (logAmount <= 0) {
							continue;
						}

						BlockPos approachPos = this.getOffsetToAirPos(cursor);
						if (approachPos == null || !RatUtils.canRatBreakBlock(level, cursor, this.rat)) {
							continue;
						}

						Path path = this.rat.getNavigation().createPath(approachPos, 1);
						if (path == null) {
							continue;
						}

						double distance = this.rat.distanceToSqr(cursor.getX() + 0.5D, cursor.getY() + 0.5D, cursor.getZ() + 0.5D);
						if (distance < bestDistance) {
							bestDistance = distance;
							bestPos = cursor.immutable();
							bestPath = path;
							this.treeSize = logAmount;
						}
					}
				}
			}

			if (bestPos != null) {
				this.setTargetBlock(bestPos);
				this.rat.moveSmoothlyTo(bestPath, this.rat.getHungerAdjustedWorkSpeed(1.25D));
			} else {
				this.setTargetBlock(null);
				this.treeScan = null;
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to pick a tree for lumberjack {}: {}", this.rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	@Override
	public void start() {
		super.start();
		try {
			BlockPos target = this.getTargetBlock();
			if (target == null) {
				this.treeScan = null;
				return;
			}
			this.treeScan = RatTreeUtils.scanTree(this.rat.level(), target);
			if (!this.treeScan.valid()) {
				RatsMod.LOGGER.warn("Lumberjack {} failed to scan the tree at {}", this.rat.getUUID(), target);
				this.treeScan = null;
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to start chopping a tree with rat {}: {}", this.rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	@Override
	public boolean canContinueToUse() {
		return this.checkTheBasics(false, true) && this.getTargetBlock() != null && this.treeScan != null && this.treeScan.valid();
	}

	@Override
	public void stop() {
		super.stop();
		this.rat.crafting = false;
		this.sapling = null;
		this.treeScan = null;
		BlockPos target = this.getTargetBlock();
		if (target != null) {
			RatTreeUtils.highestleaf.remove(target);
		}
	}

	@Nullable
	public BlockPos getOffsetToAirPos(@Nullable BlockPos pos) {
		if (pos == null) {
			return null;
		}
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos offsetPos = pos.relative(direction);
			if (this.rat.level().isEmptyBlock(offsetPos) || !this.rat.level().getBlockState(offsetPos).canOcclude()) {
				return offsetPos;
			}
		}
		return pos;
	}

	@Override
	public void tick() {
		try {
			BlockPos target = this.getTargetBlock();
			if (target == null || this.treeScan == null || !this.treeScan.valid()) {
				this.stop();
				return;
			}

			BlockPos approachPos = this.getOffsetToAirPos(target);
			if (approachPos == null) {
				this.stop();
				return;
			}

			this.rat.getLookControl().setLookAt(target.getCenter());
			this.rat.moveSmoothlyTo(approachPos.getX() + 0.5D, approachPos.getY(), approachPos.getZ() + 0.5D, this.rat.getHungerAdjustedWorkSpeed(1.25D));

			if (!RatTreeUtils.isTreeLog(this.rat.level().getBlockState(target))) {
				this.stop();
				return;
			}

			double distance = this.rat.getRatDistanceCenterSq(target.getX(), target.getY(), target.getZ());
			if (distance >= this.rat.getRatHarvestDistance(0.0D)) {
				return;
			}

			this.rat.level().broadcastEntityEvent(this.rat, (byte) 85);
			this.rat.crafting = true;
			if (distance < this.rat.getRatHarvestDistance(-1.0D)) {
				this.rat.setDeltaMovement(Vec3.ZERO);
				this.rat.getNavigation().stop();
			}

			this.breakingTime++;
			int progress = (int) ((float) this.breakingTime / 160.0F * 10.0F);
			if (this.breakingTime % 10 == 0) {
				this.rat.playSound(SoundEvents.WOOD_HIT, 1, 1);
				this.rat.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1, 0.5F);
			}
			if (progress != this.previousBreakProgress) {
				this.rat.level().destroyBlockProgress(this.rat.getId(), target, progress);
				this.previousBreakProgress = progress;
			}
			if (this.breakingTime < 160) {
				return;
			}

			this.rat.level().broadcastEntityEvent(this.rat, (byte) 86);
			this.rat.playSound(SoundEvents.WOOD_BREAK, 1, 1);
			this.breakingTime = 0;
			this.previousBreakProgress = -1;
			this.fellTree();
			this.stop();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Lumberjack cycle error for rat {}: {}", this.rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Chops the whole tree, leaves and stumps; takes nothing, returns nothing, modifies the world around the target.
	private void fellTree() {
		try {
			if (this.treeScan == null || !this.treeScan.valid()) {
				return;
			}

			Level level = this.rat.level();
			boolean hasReplanter = RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_REPLANTER.get());

			for (BlockPos logPos : this.treeScan.logs()) {
				if (RatUtils.canRatBreakBlock(level, logPos, this.rat)) {
					level.destroyBlock(logPos, true);
				}
			}

			for (BlockPos leafPos : this.treeScan.leaves()) {
				BlockState leafState = level.getBlockState(leafPos);
				if (this.sapling == null && leafState.is(net.minecraft.tags.BlockTags.LEAVES) && level instanceof ServerLevel serverLevel) {
					this.sapling = RatTreeUtils.getSaplingFromLeaves(serverLevel, leafState.getBlock());
				}
				if (RatUtils.canRatBreakBlock(level, leafPos, this.rat)) {
					level.destroyBlock(leafPos, true);
				}
			}

			if (hasReplanter && this.sapling != null) {
				for (BlockPos stumpPos : this.treeScan.stumps()) {
					if (RatUtils.canRatPlaceBlock(level, stumpPos, this.rat)) {
						level.setBlockAndUpdate(stumpPos, this.sapling.defaultBlockState());
					}
				}
			} else {
				for (BlockPos stumpPos : this.treeScan.stumps()) {
					if (RatUtils.canRatBreakBlock(level, stumpPos, this.rat)) {
						level.destroyBlock(stumpPos, true);
					}
				}
			}

			BlockPos target = this.getTargetBlock();
			if (target != null) {
				RatTreeUtils.highestleaf.remove(target);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to chop a tree with rat {}: {}", this.rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}
}
