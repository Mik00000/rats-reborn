package com.mik.rats_squirrel.ai.phase;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.canopy.TrunkRail;
import net.minecraft.world.phys.Vec3;

/**
 * {@code DESCEND_TRUNK}: climb down the trunk rail and hand the rat back to ground physics.
 *
 * <p>The rat first steps onto the rail at its current height, then descends while facing the
 * bark. On reaching the foot of the trunk it is placed exactly on the floor and released.</p>
 */
public final class DescendTrunkPhase extends AbstractHarvestPhase {
    private static final double DESCEND_SPEED = 0.2D;

    @Override
    public void onEnter(HarvestContext context) {
        context.movement().enterRail();
    }

    @Override
    public PhaseResult tick(HarvestContext context) {
        TrunkRail rail = context.rail();
        if (rail == null) {
            return PhaseResult.transition(Phase.EMERGENCY_DESCENT);
        }
        if (context.ticksInPhase() > 80 + Math.max(2, rail.height()) * 15) {
            return PhaseResult.transition(Phase.EMERGENCY_DESCENT);
        }

        TamedRat rat = context.rat();
        Vec3 bottom = rail.bottom();
        boolean aligned = rail.horizontalDistance(rat.position()) <= 0.2D;
        Vec3 target = aligned ? bottom : rail.at(Math.max(bottom.y, rat.getY()));

        double remaining = context.movement().steerTowards(target, DESCEND_SPEED, true);
        context.movement().faceTowards(rail.trunkCentre(rat.getY()));

        if (aligned && remaining < 0.08D) {
            rat.setPos(bottom.x, bottom.y, bottom.z);
            rat.setDeltaMovement(Vec3.ZERO);
            context.movement().enterGround();
            return PhaseResult.finish();
        }
        return PhaseResult.stay();
    }
}
