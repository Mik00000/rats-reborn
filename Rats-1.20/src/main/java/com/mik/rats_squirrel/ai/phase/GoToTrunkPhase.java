package com.mik.rats_squirrel.ai.phase;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.canopy.TrunkRail;
import com.mik.rats_squirrel.tree.FailureBlacklist;
import com.mik.rats_squirrel.tree.HarvestTask;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * {@code GO_TO_TRUNK}: walk to the trunk's stand position with vanilla navigation.
 *
 * <p>Arrival is reached next to the stand position or at the foot of the rail. The cluster is
 * blacklisted only when the rat stops making progress for 100 ticks (or after 900 ticks in
 * total), so a long walk to a distant tree is no longer abandoned half way.</p>
 */
public final class GoToTrunkPhase extends AbstractHarvestPhase {
    /** Give up only when the rat stops getting closer for this long, not on total walk time. */
    private static final int STALL_LIMIT = 100;
    private static final int HARD_LIMIT = 900;

    private double bestDistance;
    private int stalledTicks;

    @Override
    public void onEnter(HarvestContext context) {
        context.movement().enterGround();
        bestDistance = Double.MAX_VALUE;
        stalledTicks = 0;
        moveToStand(context);
    }

    @Override
    public PhaseResult tick(HarvestContext context) {
        HarvestTask task = context.task();
        TrunkRail rail = context.rail();
        if (task == null || task.route() == null || rail == null) {
            return PhaseResult.finish();
        }

        TamedRat rat = context.rat();
        Vec3 standCenter = Vec3.atBottomCenterOf(task.route().standPos());
        double distance = rat.position().distanceTo(standCenter);
        if (distance < bestDistance - 0.5D) {
            bestDistance = distance;
            stalledTicks = 0;
        } else {
            stalledTicks++;
        }
        if (stalledTicks > STALL_LIMIT || context.ticksInPhase() > HARD_LIMIT) {
            context.debug("walk to trunk {} stalled", task.route().base());
            context.blacklist().add(task.cluster().id(), FailureBlacklist.Reason.NO_PATH, context.gameTime(), 1200);
            return PhaseResult.finish();
        }

        boolean atStand = rat.distanceToSqr(standCenter) <= 1.0D;
        boolean atRail = rail.horizontalDistance(rat.position()) <= 0.8D && Math.abs(rat.getY() - rail.floorY()) <= 1.0D;
        if ((atStand || atRail) && rat.onGround()) {
            rat.getNavigation().stop();
            return PhaseResult.transition(Phase.CLIMB_TRUNK);
        }

        if (rat.getNavigation().isDone() || rat.getNavigation().isStuck()) {
            moveToStand(context);
        }
        return PhaseResult.stay();
    }

    private static void moveToStand(HarvestContext context) {
        HarvestTask task = context.task();
        if (task == null || task.route() == null) {
            return;
        }
        TamedRat rat = context.rat();
        BlockPos stand = task.route().standPos();
        rat.getNavigation().moveTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, rat.getHungerAdjustedWorkSpeed(1.1D));
    }
}
