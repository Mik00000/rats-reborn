package com.mik.rats_squirrel.canopy;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.config.SquirrelConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Sole owner of the rat's movement flags ({@code noPhysics}, {@code noGravity},
 * {@code isClimbingTrees}) and of every velocity written while the rat is in a tree.
 *
 * <p>Three modes:</p>
 * <ul>
 *   <li>{@link Mode#GROUND}: vanilla physics and navigation; all flags released.</li>
 *   <li>{@link Mode#RAIL}: kinematic climb along a {@link TrunkRail}.</li>
 *   <li>{@link Mode#CANOPY}: kinematic movement along a canopy route.</li>
 * </ul>
 *
 * <p>In the two tree modes collision and gravity stay off for the whole mode. Safety comes from
 * the routes themselves (they only contain passable blocks touching the tree) rather than from
 * toggling collision every tick, which used to trap the rat against leaf faces.</p>
 *
 * <p>Motion is steered: the velocity turns toward the desired direction with a bounded
 * acceleration instead of being overwritten each tick, and only the last waypoint of a route
 * slows the rat down. Periodic re-planning never pulls the rat back to the centre of the block
 * it already stands in.</p>
 */
public final class CanopyMovementController {
    public enum Mode { GROUND, RAIL, CANOPY }

    /** Maximum change of velocity per tick, in blocks/tick. */
    private static final double ACCELERATION = 0.045D;
    /** Maximum body turn per tick, in degrees. */
    private static final float TURN_RATE = 20.0F;
    /** Waypoints closer than this are considered passed. */
    private static final double PASS_DISTANCE = 0.45D;
    /** Feet height above the bottom of a waypoint block. */
    private static final double FEET_OFFSET = 0.1D;

    private final TamedRat rat;
    private Mode mode = Mode.GROUND;
    private Vec3 velocity = Vec3.ZERO;
    private List<Vec3> path = List.of();
    private List<BlockPos> pathNodes = List.of();
    private int index;

    public CanopyMovementController(TamedRat rat) {
        this.rat = rat;
    }

    // ---------------------------------------------------------------- modes

    /** Releases all flags and returns the rat to vanilla physics. */
    public void enterGround() {
        this.mode = Mode.GROUND;
        this.velocity = Vec3.ZERO;
        clearPath();
        this.rat.noPhysics = false;
        this.rat.setNoGravity(false);
        this.rat.isClimbingTrees = false;
        this.rat.fallDistance = 0.0F;
    }

    /** Kinematic trunk movement. */
    public void enterRail() {
        enterTreeMode(Mode.RAIL);
    }

    /** Kinematic canopy movement. */
    public void enterCanopy() {
        enterTreeMode(Mode.CANOPY);
    }

    private void enterTreeMode(Mode next) {
        if (this.mode == Mode.GROUND) {
            this.velocity = Vec3.ZERO;
        }
        this.mode = next;
        clearPath();
        this.rat.getNavigation().stop();
        applyTreeFlags();
    }

    private void applyTreeFlags() {
        this.rat.noPhysics = true;
        this.rat.setNoGravity(true);
        this.rat.isClimbingTrees = false;
        this.rat.fallDistance = 0.0F;
    }

    public Mode mode() {
        return mode;
    }

    public boolean isInTree() {
        return mode != Mode.GROUND;
    }

    /** Alias kept for callers that only need to release everything. */
    public void stopAndRelease() {
        enterGround();
    }

    // ---------------------------------------------------------------- steering

    /**
     * Steers toward a feet position.
     *
     * @param arrive slow down when close instead of passing through at full speed
     * @return the remaining distance before this tick's movement
     */
    public double steerTowards(Vec3 target, double maxSpeed, boolean arrive) {
        Vec3 diff = target.subtract(rat.position());
        double dist = diff.length();

        Vec3 desired;
        if (dist < 1.0E-4D) {
            desired = Vec3.ZERO;
        } else {
            double speed = maxSpeed;
            if (arrive) {
                // Linear braking inside ~0.6 blocks so the rat settles instead of overshooting.
                speed = Math.min(maxSpeed, Math.max(0.02D, dist * 0.35D));
            }
            desired = diff.scale(Math.min(speed, dist) / dist);
        }

        Vec3 change = desired.subtract(velocity);
        double changeLength = change.length();
        if (changeLength > ACCELERATION) {
            change = change.scale(ACCELERATION / changeLength);
        }
        velocity = velocity.add(change);
        if (arrive && velocity.length() > dist) {
            velocity = diff;
        }

        apply();
        faceAlongVelocity();
        return dist;
    }

    /** Brakes to a stop in place. */
    public void hold() {
        double speed = velocity.length();
        velocity = speed <= ACCELERATION ? Vec3.ZERO : velocity.scale((speed - ACCELERATION) / speed);
        apply();
    }

    private void apply() {
        if (mode != Mode.GROUND) {
            applyTreeFlags();
        }
        rat.setDeltaMovement(velocity);
        rat.fallDistance = 0.0F;
    }

    // ---------------------------------------------------------------- routes

    /**
     * Replaces the route.
     *
     * <p>Leading waypoints that the rat is already on or past are dropped, so a fresh plan
     * that starts at the rat's own block never drags it backwards.</p>
     */
    public void setPath(List<BlockPos> nodes) {
        List<Vec3> points = new ArrayList<>(nodes.size());
        for (BlockPos node : nodes) {
            points.add(feetPoint(node));
        }
        this.pathNodes = List.copyOf(nodes);
        this.path = points;
        this.index = 0;

        Vec3 pos = rat.position();
        while (index < path.size() - 1) {
            Vec3 current = path.get(index);
            Vec3 next = path.get(index + 1);
            boolean onIt = pos.distanceTo(current) < 0.9D;
            boolean past = next.subtract(current).dot(pos.subtract(current)) > 0.0D;
            if (onIt || past) {
                index++;
            } else {
                break;
            }
        }
    }

    public void clearPath() {
        this.path = List.of();
        this.pathNodes = List.of();
        this.index = 0;
    }

    public boolean hasPath() {
        return !path.isEmpty();
    }

    /**
     * Follows the current route.
     *
     * @return {@code true} once the final waypoint is reached (or there is no route)
     */
    public boolean followPath(double maxSpeed) {
        if (path.isEmpty()) {
            hold();
            return true;
        }

        Vec3 pos = rat.position();
        int last = path.size() - 1;
        while (index < last && pos.distanceTo(path.get(index)) < PASS_DISTANCE) {
            index++;
        }
        // Look ahead: cut corners when the straight line is clear.
        for (int j = Math.min(last, index + 3); j > index; j--) {
            if (lineClear(pos, path.get(j))) {
                index = j;
                break;
            }
        }

        Vec3 target = path.get(index);
        boolean isFinal = index == last;
        double dist = steerTowards(target, maxSpeed, isFinal);

        if (SquirrelConfig.debugParticles() && rat.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.x, target.y + 0.2D, target.z, 1, 0, 0, 0, 0);
        }
        return isFinal && dist < 0.15D;
    }

    /** Returns {@code true} if the next waypoint block has become impassable. */
    public boolean isPathBlocked() {
        if (pathNodes.isEmpty() || index >= pathNodes.size()) {
            return false;
        }
        return !CanopyBlocks.isPassable(rat.level().getBlockState(pathNodes.get(index)));
    }

    private boolean lineClear(Vec3 from, Vec3 to) {
        Vec3 diff = to.subtract(from);
        double length = diff.length();
        int steps = Math.max(1, (int) Math.ceil(length / 0.3D));
        BlockPos previous = null;
        for (int i = 1; i <= steps; i++) {
            Vec3 sample = from.add(diff.scale((double) i / steps)).add(0.0D, 0.2D, 0.0D);
            BlockPos block = BlockPos.containing(sample);
            if (block.equals(previous)) {
                continue;
            }
            previous = block;
            if (!CanopyBlocks.isClingNode(rat.level(), block)) {
                return false;
            }
        }
        return true;
    }

    public static Vec3 feetPoint(BlockPos node) {
        return new Vec3(node.getX() + 0.5D, node.getY() + FEET_OFFSET, node.getZ() + 0.5D);
    }

    // ---------------------------------------------------------------- facing

    private void faceAlongVelocity() {
        double horizontal = velocity.horizontalDistanceSqr();
        if (horizontal > 1.0E-4D) {
            turnTo((float) (Mth.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG) - 90.0F);
        }
    }

    /** Turns the body smoothly toward a point. */
    public void faceTowards(Vec3 point) {
        double dx = point.x - rat.getX();
        double dz = point.z - rat.getZ();
        if (dx * dx + dz * dz > 1.0E-4D) {
            turnTo((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F);
        }
    }

    private void turnTo(float yaw) {
        float next = Mth.approachDegrees(rat.getYRot(), yaw, TURN_RATE);
        rat.setYRot(next);
        rat.yBodyRot = next;
        rat.setYHeadRot(next);
    }

    // ---------------------------------------------------------------- safety

    /** Returns {@code true} when the rat's feet are inside solid terrain. */
    public boolean isInsideTerrain() {
        return CanopyBlocks.isTerrain(rat.level().getBlockState(rat.blockPosition()));
    }

    // ---------------------------------------------------------------- debug

    public int waypointIndex() {
        return index;
    }

    public List<BlockPos> waypoints() {
        return pathNodes;
    }

    public boolean isActive() {
        return mode == Mode.CANOPY;
    }

    public boolean ownsNoPhysics() {
        return mode != Mode.GROUND;
    }

    public TamedRat rat() {
        return rat;
    }
}
