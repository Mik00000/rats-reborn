package com.mik.rats_squirrel.ai.phase;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.canopy.CanopyGraph;
import com.mik.rats_squirrel.canopy.CanopyPathfinder;
import com.mik.rats_squirrel.canopy.TrunkRail;
import com.mik.rats_squirrel.config.SquirrelConfig;
import com.mik.rats_squirrel.tree.HarvestTask;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * {@code RETURN_TO_TRUNK}: travel through the canopy back to the top of the trunk rail.
 *
 * <p>Cannot be interrupted by hunger, schedule or command changes. After three failed searches
 * or 300 ticks the rat falls back to {@code EMERGENCY_DESCENT}.</p>
 */
public final class ReturnToTrunkPhase extends AbstractHarvestPhase {
    private static final double SPEED = 0.2D;

    private final CanopyPathfinder pathfinder = new CanopyPathfinder(new CanopyGraph());
    private boolean planned;
    private boolean onFinalApproach;
    private int failedSearches;
    private int retryCooldown;

    @Override
    public void onEnter(HarvestContext context) {
        context.movement().enterCanopy();
        planned = false;
        onFinalApproach = false;
        failedSearches = 0;
        retryCooldown = 0;
    }

    @Override
    public PhaseResult tick(HarvestContext context) {
        HarvestTask task = context.task();
        TrunkRail rail = context.rail();
        if (task == null || rail == null) {
            return PhaseResult.transition(Phase.EMERGENCY_DESCENT);
        }
        if (context.ticksInPhase() > 300) {
            context.debug("return to trunk timed out");
            return PhaseResult.transition(Phase.EMERGENCY_DESCENT);
        }

        TamedRat rat = context.rat();
        Vec3 top = rail.top();

        // Already on the rail (for example interrupted while climbing): just descend.
        if (rail.horizontalDistance(rat.position()) < 0.3D && rat.getY() <= top.y + 0.5D) {
            return PhaseResult.transition(Phase.DESCEND_TRUNK);
        }

        if (onFinalApproach) {
            double remaining = context.movement().steerTowards(top, 0.15D, true);
            return remaining < 0.15D ? PhaseResult.transition(Phase.DESCEND_TRUNK) : PhaseResult.stay();
        }

        if (!planned || context.movement().isPathBlocked()) {
            if (retryCooldown > 0) {
                retryCooldown--;
                context.movement().hold();
                return PhaseResult.stay();
            }
            Optional<List<BlockPos>> path = pathfinder.findPathTo(
                    context.level(), rat.blockPosition(), rail.entryNode(),
                    CanopyTravelPhase.searchBounds(context, task), SquirrelConfig.canopyPathNodeLimit(), 0.0D);
            if (path.isEmpty()) {
                if (++failedSearches >= 3) {
                    context.debug("no canopy route back to trunk {}", rail.base());
                    return PhaseResult.transition(Phase.EMERGENCY_DESCENT);
                }
                retryCooldown = 10;
                context.movement().hold();
                return PhaseResult.stay();
            }
            context.movement().setPath(path.get());
            planned = true;
        }

        if (context.movement().followPath(SPEED)) {
            onFinalApproach = true;
        }
        return PhaseResult.stay();
    }
}
