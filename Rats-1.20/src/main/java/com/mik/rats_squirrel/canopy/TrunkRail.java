package com.mik.rats_squirrel.canopy;

import com.mik.rats_squirrel.tree.TrunkRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertical "rail" hugging one side of a trunk, used for climbing up and down.
 *
 * <p>The rat moves along this rail kinematically with collision disabled, so an overhanging
 * leaf layer can no longer stop it under the canopy. The rail ends at {@link #entryNode()},
 * the first block of the rail column that is inside or touching foliage; canopy travel starts
 * from there.</p>
 *
 * @param base      lowest log of the trunk
 * @param side      side of the trunk the rail runs on
 * @param x         rail x coordinate (rat feet)
 * @param z         rail z coordinate (rat feet)
 * @param floorY    feet height at the bottom of the rail
 * @param entryNode block of the rail column where the rat enters the canopy
 */
public record TrunkRail(BlockPos base, Direction side, double x, double z, int floorY, BlockPos entryNode) {
    /** Distance from the trunk centre to the rat centre; bark is at 0.5, rat half-width is ~0.25. */
    private static final double OFFSET = 0.78D;

    public static TrunkRail compute(BlockGetter level, TrunkRoute route) {
        BlockPos base = route.base();
        Direction side = chooseSide(level, base, route.standPos());
        BlockPos column = base.relative(side);

        int floorY = findFloor(level, column, route.standPos().getY());
        int topY = route.topLog().map(BlockPos::getY).orElse(base.getY() + 1) + 1;
        int entryY = topY;
        for (int y = Math.max(floorY + 2, base.getY() + 1); y <= topY; y++) {
            if (touchesFoliage(level, new BlockPos(column.getX(), y, column.getZ()))) {
                entryY = y;
                break;
            }
        }

        double x = base.getX() + 0.5D + side.getStepX() * OFFSET;
        double z = base.getZ() + 0.5D + side.getStepZ() * OFFSET;
        return new TrunkRail(base.immutable(), side, x, z, floorY, new BlockPos(column.getX(), entryY, column.getZ()));
    }

    /** Point on the rail at the given feet height. */
    public Vec3 at(double y) {
        return new Vec3(x, y, z);
    }

    /** Feet position where climbing ends and canopy travel begins. */
    public Vec3 top() {
        return at(entryNode.getY() + 0.05D);
    }

    /** Feet position at the foot of the trunk. */
    public Vec3 bottom() {
        return at(floorY);
    }

    /** Centre of the trunk at the given height, used to face the bark while climbing. */
    public Vec3 trunkCentre(double y) {
        return new Vec3(base.getX() + 0.5D, y, base.getZ() + 0.5D);
    }

    /** Horizontal distance from {@code pos} to the rail. */
    public double horizontalDistance(Vec3 pos) {
        double dx = pos.x - x;
        double dz = pos.z - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public int height() {
        return entryNode.getY() - floorY;
    }

    private static Direction chooseSide(BlockGetter level, BlockPos base, BlockPos stand) {
        int dx = Integer.signum(stand.getX() - base.getX());
        int dz = Integer.signum(stand.getZ() - base.getZ());
        List<Direction> candidates = new ArrayList<>(4);
        if (dx != 0) {
            candidates.add(dx > 0 ? Direction.EAST : Direction.WEST);
        }
        if (dz != 0) {
            candidates.add(dz > 0 ? Direction.SOUTH : Direction.NORTH);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!candidates.contains(direction)) {
                candidates.add(direction);
            }
        }
        for (Direction direction : candidates) {
            BlockPos column = base.relative(direction);
            if (!CanopyBlocks.isTerrain(level.getBlockState(column.above()))
                    && !CanopyBlocks.isLog(level.getBlockState(column.above()))) {
                return direction;
            }
        }
        return candidates.getFirst();
    }

    private static int findFloor(BlockGetter level, BlockPos column, int fallback) {
        for (int y = column.getY() + 1; y >= column.getY() - 3; y--) {
            BlockPos feet = new BlockPos(column.getX(), y, column.getZ());
            BlockState below = level.getBlockState(feet.below());
            if (CanopyBlocks.isPassable(level.getBlockState(feet))
                    && (CanopyBlocks.isTerrain(below) || CanopyBlocks.isLog(below))) {
                return y;
            }
        }
        return fallback;
    }

    private static boolean touchesFoliage(BlockGetter level, BlockPos pos) {
        if (CanopyBlocks.isCanopyPart(level.getBlockState(pos))) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (CanopyBlocks.isCanopyPart(level.getBlockState(pos.relative(direction)))) {
                return true;
            }
        }
        return CanopyBlocks.isCanopyPart(level.getBlockState(pos.above()));
    }
}
