package com.mik.rats_squirrel.ai.phase;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.canopy.TrunkRail;
import com.mik.rats_squirrel.config.SquirrelConfig;
import com.mik.rats_squirrel.tree.FailureBlacklist;
import com.mik.rats_squirrel.tree.HarvestTask;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/**
 * {@code CLIMB_TRUNK}: climb the trunk rail up to the canopy entry node.
 *
 * <p>The climb is kinematic with collision off, so leaves hanging over the trunk no longer
 * block the rat's head. The rat first steps onto the rail, then climbs straight up while
 * facing the bark, and switches to canopy travel on reaching the entry node.</p>
 */
public final class ClimbTrunkPhase extends AbstractHarvestPhase {
    private static final double CLIMB_SPEED = 0.22D;

    @Override
    public void onEnter(HarvestContext context) {
        context.movement().enterRail();
    }

    @Override
    public PhaseResult tick(HarvestContext context) {
        HarvestTask task = context.task();
        TrunkRail rail = context.rail();
        if (task == null || rail == null) {
            return PhaseResult.transition(Phase.EMERGENCY_DESCENT);
        }

        int timeout = 60 + Math.max(2, rail.height()) * 12;
        if (context.ticksInPhase() > timeout) {
            context.debug("climb timed out on trunk {}", rail.base());
            context.blacklist().add(task.cluster().id(), FailureBlacklist.Reason.NO_TRUNK, context.gameTime(), 1200);
            return PhaseResult.transition(Phase.DESCEND_TRUNK);
        }

        TamedRat rat = context.rat();
        Vec3 top = rail.top();
        Vec3 target = rail.horizontalDistance(rat.position()) > 0.2D
                ? rail.at(Math.min(top.y, rat.getY() + 0.05D))
                : top;

        double remaining = context.movement().steerTowards(target, CLIMB_SPEED, true);
        context.movement().faceTowards(rail.trunkCentre(rat.getY()));

        if (SquirrelConfig.debugParticles()) {
            context.level().sendParticles(ParticleTypes.CRIT, rat.getX(), rat.getY(), rat.getZ(), 1, 0, 0, 0, 0);
        }

        if (target == top && remaining < 0.15D) {
            context.movement().enterCanopy();
            return PhaseResult.transition(Phase.CANOPY_TRAVEL);
        }
        return PhaseResult.stay();
    }
}
