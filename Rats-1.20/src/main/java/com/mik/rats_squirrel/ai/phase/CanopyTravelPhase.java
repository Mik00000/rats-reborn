package com.mik.rats_squirrel.ai.phase;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.canopy.CanopyBlocks;
import com.mik.rats_squirrel.canopy.CanopyGraph;
import com.mik.rats_squirrel.canopy.CanopyPathfinder;
import com.mik.rats_squirrel.canopy.TrunkRail;
import com.mik.rats_squirrel.config.SquirrelConfig;
import com.mik.rats_squirrel.tree.FailureBlacklist;
import com.mik.rats_squirrel.tree.HarvestTask;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code CANOPY_TRAVEL}: move through the canopy to the nearest reachable fruit.
 *
 * <ul>
 *   <li>One multi-goal search picks the fruit with the cheapest real route, so the queue is
 *       effectively sorted by path distance and unreachable fruit is skipped cheaply.</li>
 *   <li>The route is re-planned only when the target changes, the next waypoint is blocked,
 *       or the rat stops making progress; never on a fixed timer.</li>
 *   <li>A fruit is blacklisted only after {@value #MAX_FAILURES} failed attempts.</li>
 *   <li>When the queue runs dry the canopy is re-read for fruit that was missed before the
 *       rat leaves the tree.</li>
 * </ul>
 */
public final class CanopyTravelPhase extends AbstractHarvestPhase {
    static final int MAX_FAILURES = 3;
    static final double HARVEST_REACH = 2.0D;
    private static final double SPEED = 0.2D;
    private static final int NO_PROGRESS_LIMIT = 30;
    private static final int TARGET_TIMEOUT = 200;

    private final CanopyPathfinder pathfinder = new CanopyPathfinder(new CanopyGraph());

    private BlockPos target;
    private double bestDistance;
    private int noProgressTicks;
    private int ticksOnTarget;
    private int replans;
    private int failedSearches;
    private int searchCooldown;
    private boolean refreshed;

    @Override
    public void onEnter(HarvestContext context) {
        context.movement().enterCanopy();
        target = null;
        failedSearches = 0;
        searchCooldown = 0;
        refreshed = false;
    }

    @Override
    public PhaseResult tick(HarvestContext context) {
        HarvestTask task = context.task();
        if (task == null) {
            return PhaseResult.transition(Phase.RETURN_TO_TRUNK);
        }
        if (context.movement().isInsideTerrain()) {
            context.debug("inside terrain during canopy travel, emergency descent");
            return PhaseResult.transition(Phase.EMERGENCY_DESCENT);
        }

        TamedRat rat = context.rat();
        if (context.ticksInPhase() % 20 == 0) {
            int dropped = task.pruneUnripe(context.level(), CanopyBlocks.classifier());
            if (dropped > 0) {
                context.debug("dropped {} fruit that is no longer ripe", dropped);
                if (target != null && !task.contains(target)) {
                    target = null;
                }
            }
        }
        if (!task.hasFruit()) {
            if (!refreshed) {
                refreshed = true;
                int added = task.refreshFruits(context.level(), CanopyBlocks.classifier(), context.blacklist(), context.gameTime());
                context.debug("fruit queue empty, re-read canopy: {} more fruit", added);
            }
            if (!task.hasFruit()) {
                return PhaseResult.transition(Phase.LEAP_TO_TREE);
            }
        }

        // Anything already within reach is picked right away.
        for (BlockPos candidate : task.fruitQueue()) {
            if (withinReach(rat, candidate)) {
                task.prioritizeFruit(candidate);
                return PhaseResult.transition(Phase.HARVEST);
            }
        }

        if (target == null || !task.contains(target) || context.movement().isPathBlocked()) {
            PhaseResult planned = planToNearest(context, task);
            if (planned != null) {
                return planned;
            }
            if (target == null) {
                return PhaseResult.stay();
            }
        }

        ticksOnTarget++;
        double distance = rat.position().distanceTo(Vec3.atCenterOf(target));
        if (distance < bestDistance - 0.1D) {
            bestDistance = distance;
            noProgressTicks = 0;
        } else {
            noProgressTicks++;
        }

        if (ticksOnTarget > TARGET_TIMEOUT || noProgressTicks > NO_PROGRESS_LIMIT) {
            if (++replans > 2 || ticksOnTarget > TARGET_TIMEOUT) {
                giveUpOn(context, task, target, "no progress");
            }
            target = null;
            return PhaseResult.stay();
        }

        boolean arrived = context.movement().followPath(SPEED);
        if (arrived) {
            if (withinReach(rat, target) || rat.blockPosition().distSqr(target) <= CanopyPathfinder.FRUIT_REACH_SQR) {
                return PhaseResult.transition(Phase.HARVEST);
            }
            // Route ended short of the fruit: plan again from here.
            target = null;
        }
        return PhaseResult.stay();
    }

    /** Returns a transition when planning decided to leave the phase, otherwise {@code null}. */
    private PhaseResult planToNearest(HarvestContext context, HarvestTask task) {
        if (searchCooldown > 0) {
            searchCooldown--;
            context.movement().hold();
            target = null;
            return null;
        }

        int now = context.gameTime();
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos fruit : task.fruitQueue()) {
            if (!context.blacklist().isBlocked(fruit, now)) {
                candidates.add(fruit);
            }
        }
        if (candidates.isEmpty()) {
            task.fruitQueue().clear();
            target = null;
            return null;
        }

        BlockPos start = context.rat().blockPosition();
        Optional<CanopyPathfinder.Route> route = pathfinder.findNearest(
                context.level(), start, candidates, searchBounds(context, task), SquirrelConfig.canopyPathNodeLimit());

        if (route.isEmpty()) {
            failedSearches++;
            context.debug("no canopy route to any of {} fruit (attempt {})", candidates.size(), failedSearches);
            if (failedSearches >= MAX_FAILURES) {
                for (BlockPos fruit : candidates) {
                    context.blacklist().add(fruit, FailureBlacklist.Reason.NO_CANOPY_PATH, now, SquirrelConfig.blacklistTtl());
                    task.removeFruit(fruit);
                }
                return PhaseResult.transition(Phase.RETURN_TO_TRUNK);
            }
            searchCooldown = 10;
            target = null;
            context.movement().hold();
            return null;
        }

        failedSearches = 0;
        if (!route.get().goal().equals(target)) {
            replans = 0;
            ticksOnTarget = 0;
        }
        target = route.get().goal();
        task.prioritizeFruit(target);
        context.movement().setPath(route.get().path());
        bestDistance = Double.MAX_VALUE;
        noProgressTicks = 0;
        return null;
    }

    private void giveUpOn(HarvestContext context, HarvestTask task, BlockPos fruit, String why) {
        int failures = task.recordFailure(fruit);
        context.debug("fruit {} failed ({}), attempt {}", fruit, why, failures);
        if (failures >= MAX_FAILURES) {
            context.blacklist().add(fruit, FailureBlacklist.Reason.NO_CANOPY_PATH, context.gameTime(), SquirrelConfig.blacklistTtl());
            task.removeFruit(fruit);
        } else {
            task.deferFruit(fruit);
        }
    }

    static AABB searchBounds(HarvestContext context, HarvestTask task) {
        AABB box = task.cluster().bounds();
        TrunkRail rail = context.rail();
        if (rail != null) {
            box = box.minmax(new AABB(rail.entryNode()));
        }
        return box.inflate(2.0D);
    }

    static boolean withinReach(TamedRat rat, BlockPos fruit) {
        Vec3 body = rat.position().add(0.0D, rat.getBbHeight() * 0.5D, 0.0D);
        return body.distanceToSqr(Vec3.atCenterOf(fruit)) <= HARVEST_REACH * HARVEST_REACH;
    }
}
