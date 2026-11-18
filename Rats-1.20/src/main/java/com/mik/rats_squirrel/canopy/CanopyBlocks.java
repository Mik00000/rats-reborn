package com.mik.rats_squirrel.canopy;

import com.mik.rats_squirrel.fruit.FruitClassifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Single source of truth for "what is a tree block" questions.
 *
 * <p>Previously four classes carried their own slightly different copies of these predicates,
 * which made the movement code disagree with the pathfinder about what the rat may enter.</p>
 */
public final class CanopyBlocks {
    private static volatile FruitClassifier classifier;

    private CanopyBlocks() {
    }

    /** Shared default fruit classifier; adapters are stateless, so one instance is enough. */
    public static FruitClassifier classifier() {
        FruitClassifier local = classifier;
        if (local == null) {
            synchronized (CanopyBlocks.class) {
                local = classifier;
                if (local == null) {
                    local = FruitClassifier.createDefault();
                    classifier = local;
                }
            }
        }
        return local;
    }

    public static boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS)
                || (state.getBlock() instanceof RotatedPillarBlock && state.is(BlockTags.MINEABLE_WITH_AXE));
    }

    public static boolean isLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock;
    }

    /** Leaves, vines, cave vines and fruit blocks: the "foliage" a squirrel can move through. */
    public static boolean isCanopyPart(BlockState state) {
        if (state.isAir() || isLog(state)) {
            return false;
        }
        return isLeaves(state)
                || state.getBlock() instanceof VineBlock
                || state.is(BlockTags.CAVE_VINES)
                || classifier().matches(state);
    }

    /** Blocks the rat body may occupy while moving through a tree. Logs are never passable. */
    public static boolean isPassable(BlockState state) {
        if (isLog(state)) {
            return false;
        }
        return state.isAir() || state.canBeReplaced() || isCanopyPart(state);
    }

    /** Solid terrain, i.e. something that is neither passable foliage nor a trunk. */
    public static boolean isTerrain(BlockState state) {
        return !isPassable(state) && !isLog(state);
    }

    /**
     * A node the squirrel may stand on inside a tree: passable and touching foliage or bark.
     *
     * <p>The "touching" rule keeps routes glued to the tree instead of letting the rat fly
     * through open air around it.</p>
     */
    public static boolean isClingNode(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isPassable(state)) {
            return false;
        }
        if (isCanopyPart(state)) {
            return true;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            cursor.setWithOffset(pos, direction);
            BlockState neighbour = level.getBlockState(cursor);
            if (isCanopyPart(neighbour) || isLog(neighbour)) {
                return true;
            }
        }
        return false;
    }
}
