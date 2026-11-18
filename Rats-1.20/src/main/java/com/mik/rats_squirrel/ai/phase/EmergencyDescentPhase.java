package com.mik.rats_squirrel.ai.phase;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.RatsSquirrelMod;
import com.mik.rats_squirrel.tree.HarvestTask;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * {@code EMERGENCY_DESCENT}: last-resort way down when the normal return route fails.
 *
 * <p>The rat slides straight down through the foliage to the first non-leaf surface below it,
 * then gets ground physics back. If that takes more than 200 ticks it is teleported to the
 * trunk's stand position, which is logged.</p>
 */
public final class EmergencyDescentPhase extends AbstractHarvestPhase {
    @Override
    public void onEnter(HarvestContext context) {
        context.movement().enterRail();
        context.debug("emergency descent from {}", context.rat().blockPosition());
    }

    @Override
    public PhaseResult tick(HarvestContext context) {
        TamedRat rat = context.rat();
        BlockPos column = rat.blockPosition();
        int surface = context.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());

        if (context.ticksInPhase() > 200) {
            HarvestTask task = context.task();
            BlockPos safe = task != null && task.route() != null
                    ? task.route().standPos()
                    : new BlockPos(column.getX(), surface, column.getZ());
            RatsSquirrelMod.LOGGER.warn("Emergency descent timed out for rat {}; teleporting to {}", rat.getUUID(), safe);
            rat.teleportTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
            context.movement().enterGround();
            return PhaseResult.finish();
        }

        Vec3 landing = new Vec3(rat.getX(), surface, rat.getZ());
        if (rat.getY() <= surface + 0.05D) {
            rat.setPos(landing.x, Math.max(rat.getY(), surface), landing.z);
            rat.setDeltaMovement(Vec3.ZERO);
            context.movement().enterGround();
            return PhaseResult.finish();
        }
        context.movement().steerTowards(landing, 0.25D, true);
        return PhaseResult.stay();
    }
}
