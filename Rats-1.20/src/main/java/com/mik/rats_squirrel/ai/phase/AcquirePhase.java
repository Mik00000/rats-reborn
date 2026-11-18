package com.mik.rats_squirrel.ai.phase;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.canopy.TrunkRail;
import com.mik.rats_squirrel.tree.HarvestTask;
import net.minecraft.world.phys.AABB;

/**
 * {@code ACQUIRE}: pick the first movement phase for a freshly acquired task.
 *
 * <p>Bushes are walked to. For trees the rat continues from where it already is: inside the
 * canopy it starts canopy travel, on the trunk rail it keeps climbing, otherwise it walks to
 * the trunk.</p>
 */
public final class AcquirePhase extends AbstractHarvestPhase {
    @Override
    public PhaseResult tick(HarvestContext context) {
        HarvestTask task = context.task();
        if (task == null) {
            return PhaseResult.finish();
        }
        if (task.cluster().isBush()) {
            return PhaseResult.transition(Phase.GO_TO_BUSH);
        }

        TrunkRail rail = context.rail();
        if (rail == null) {
            return PhaseResult.finish();
        }

        TamedRat rat = context.rat();
        boolean aboveGround = rat.getY() > rail.floorY() + 1.5D;
        AABB treeBox = task.cluster().bounds().inflate(1.0D);
        boolean inTree = task.cluster().containsCanopy(rat.blockPosition()) || treeBox.contains(rat.position());

        if (aboveGround && inTree) {
            context.movement().enterCanopy();
            return PhaseResult.transition(Phase.CANOPY_TRAVEL);
        }
        if (rat.getY() > rail.floorY() + 0.5D && rail.horizontalDistance(rat.position()) < 0.6D) {
            return PhaseResult.transition(Phase.CLIMB_TRUNK);
        }
        return PhaseResult.transition(Phase.GO_TO_TRUNK);
    }
}
