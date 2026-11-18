package com.mik.rats_squirrel.canopy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Local passability model for canopy movement.
 *
 * <p>Delegates to {@link CanopyBlocks}; an explicit fruit matcher may be supplied for tests.</p>
 */
public final class CanopyGraph {
    private final Predicate<BlockState> fruitMatcher;

    public CanopyGraph(Predicate<BlockState> fruitMatcher) {
        this.fruitMatcher = fruitMatcher;
    }

    public CanopyGraph() {
        this(state -> CanopyBlocks.classifier().matches(state));
    }

    /** Returns {@code true} for blocks that belong to the connected canopy. */
    public boolean isCanopyPart(BlockState state) {
        return CanopyBlocks.isCanopyPart(state) || (!state.isAir() && !isLog(state) && fruitMatcher.test(state));
    }

    /** Returns {@code true} for blocks the rat may occupy or move through in the canopy. */
    public boolean isPassable(BlockState state) {
        return !isLog(state) && (CanopyBlocks.isPassable(state) || fruitMatcher.test(state));
    }

    /** Returns {@code true} for logs, which are never valid canopy nodes. */
    public boolean isLog(BlockState state) {
        return CanopyBlocks.isLog(state);
    }

    /** Returns {@code true} when a node must be rejected by the pathfinder. */
    public boolean isBlocked(BlockState state) {
        return !isPassable(state);
    }

    /** Passable and touching foliage or bark; see {@link CanopyBlocks#isClingNode}. */
    public boolean isClingNode(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isPassable(state)) {
            return false;
        }
        if (isCanopyPart(state)) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            BlockState neighbour = level.getBlockState(pos.relative(direction));
            if (isCanopyPart(neighbour) || isLog(neighbour)) {
                return true;
            }
        }
        return false;
    }

    /** Returns the six orthogonal neighbours of a position. */
    public List<BlockPos> neighbours(BlockPos pos) {
        List<BlockPos> result = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            result.add(pos.relative(direction));
        }
        return result;
    }
}
