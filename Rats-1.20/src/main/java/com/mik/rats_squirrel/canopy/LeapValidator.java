package com.mik.rats_squirrel.canopy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Validates short leaps between adjacent tree canopies.
 *
 * <p>Phase 3 contract: horizontal distance must be 1.0-3.0 blocks, vertical delta -1.5..+1.5.
 * The line from launch to landing is sampled every 0.25 blocks; only air, leaves and fruit are
 * allowed. The landing must have a support block and two blocks of headroom, and must not be
 * lava, fire or cactus.</p>
 */
public final class LeapValidator {
    private final CanopyGraph graph;

    public LeapValidator(CanopyGraph graph) {
        this.graph = graph;
    }

    /**
     * Returns {@code true} only when the complete leap is safe.
     */
    public boolean canLeap(ServerLevel level, Vec3 from, Vec3 to, double maxDistance) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        if (horizDist < 1.0D || horizDist > maxDistance) {
            return false;
        }

        if (dy < -1.5D || dy > 1.5D) {
            return false;
        }

        BlockPos landingPos = BlockPos.containing(to);
        if (!isValidLanding(level, landingPos)) {
            return false;
        }

        return isPathClear(level, from, to);
    }

    /** Returns {@code true} when the landing position has support and headroom. */
    public boolean isValidLanding(ServerLevel level, BlockPos landing) {
        BlockState standState = level.getBlockState(landing);
        if (!graph.isPassable(standState) || graph.isLog(standState) || isHazard(standState)) {
            return false;
        }

        BlockState headState = level.getBlockState(landing.above());
        if (!graph.isPassable(headState) || graph.isLog(headState) || isHazard(headState)) {
            return false;
        }

        BlockState floorState = level.getBlockState(landing.below());
        boolean hasSupport = floorState.isSolid() || floorState.is(BlockTags.LEAVES) || graph.isCanopyPart(floorState);
        if (!hasSupport || isHazard(floorState)) {
            return false;
        }

        return true;
    }

    /** Returns {@code true} when the sampled segment contains no solid non-canopy block. */
    public boolean isPathClear(ServerLevel level, Vec3 from, Vec3 to) {
        double dist = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(dist / 0.25D));

        for (int i = 1; i <= steps; i++) {
            Vec3 sample = from.add(to.subtract(from).scale((double) i / steps));
            BlockPos pos = BlockPos.containing(sample);
            BlockState state = level.getBlockState(pos);
            if (graph.isBlocked(state)) {
                return false;
            }
        }
        return true;
    }

    private boolean isHazard(BlockState state) {
        return state.is(Blocks.LAVA)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.CACTUS);
    }

    public CanopyGraph graph() {
        return graph;
    }
}
