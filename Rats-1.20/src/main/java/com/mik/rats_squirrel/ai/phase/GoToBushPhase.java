package com.mik.rats_squirrel.ai.phase;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.tree.FailureBlacklist;
import com.mik.rats_squirrel.tree.HarvestTask;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * {@code GO_TO_BUSH}: walk to a low fruit bush on ground.
 *
 * <p>Walks to the bush fruit nearest to the rat (so a berry patch is picked in a sweep instead
 * of zig-zagging), and on arrival hands over to local harvest. A fruit is blacklisted when the
 * rat stops getting closer to it for 100 ticks.</p>
 */
public final class GoToBushPhase extends AbstractHarvestPhase {
    private static final int STALL_LIMIT = 100;

    private double bestDistance;
    private int stalledTicks;

    @Override
    public void onEnter(HarvestContext context) {
        context.movement().enterGround();
        bestDistance = Double.MAX_VALUE;
        stalledTicks = 0;
        HarvestTask task = context.task();
        if (task != null) {
            task.pruneUnripe(context.level(), com.mik.rats_squirrel.canopy.CanopyBlocks.classifier());
        }
        if (task == null || !task.hasFruit()) {
            return;
        }
        TamedRat rat = context.rat();
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (BlockPos candidate : task.fruitQueue()) {
            double d = rat.distanceToSqr(Vec3.atCenterOf(candidate));
            if (d < nearestDist) {
                nearestDist = d;
                nearest = candidate;
            }
        }
        if (nearest != null) {
            task.prioritizeFruit(nearest);
        }
        BlockPos fruit = task.currentFruit();
        rat.getNavigation().moveTo(fruit.getX() + 0.5D, fruit.getY(), fruit.getZ() + 0.5D, rat.getHungerAdjustedWorkSpeed(1.1D));
    }

    @Override
    public PhaseResult tick(HarvestContext context) {
        HarvestTask task = context.task();
        if (task == null || !task.hasFruit()) {
            return PhaseResult.finish();
        }

        BlockPos fruit = task.currentFruit();
        TamedRat rat = context.rat();

        double distance = rat.position().distanceTo(Vec3.atCenterOf(fruit));
        if (distance < bestDistance - 0.5D) {
            bestDistance = distance;
            stalledTicks = 0;
        } else {
            stalledTicks++;
        }
        if (stalledTicks > STALL_LIMIT || context.ticksInPhase() > 600) {
            context.blacklist().add(fruit, FailureBlacklist.Reason.NO_PATH, context.gameTime(), 600);
            task.pollFruit();
            if (task.hasFruit()) {
                context.resetPhaseTicks();
                onEnter(context);
                return PhaseResult.stay();
            }
            return PhaseResult.finish();
        }

        Vec3 fruitCenter = Vec3.atCenterOf(fruit);
        if (rat.distanceToSqr(fruitCenter) <= 4.0D) {
            rat.getNavigation().stop();
            return PhaseResult.transition(Phase.HARVEST);
        }

        if (rat.getNavigation().isDone()) {
            rat.getNavigation().moveTo(fruit.getX() + 0.5D, fruit.getY(), fruit.getZ() + 0.5D, rat.getHungerAdjustedWorkSpeed(1.1D));
        }

        return PhaseResult.stay();
    }
}
