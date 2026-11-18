package com.mik.rats_squirrel.ai.phase;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.canopy.CanopyGraph;
import com.mik.rats_squirrel.canopy.CanopyMovementController;
import com.mik.rats_squirrel.canopy.LeapValidator;
import com.mik.rats_squirrel.config.SquirrelConfig;
import com.mik.rats_squirrel.tree.HarvestTask;
import com.mik.rats_squirrel.tree.LevelFruitIndex;
import com.mik.rats_squirrel.tree.TreeCluster;
import com.mik.rats_squirrel.tree.TrunkRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * {@code LEAP_TO_TREE}: jump across a short gap to a neighbouring canopy with the same fruit.
 *
 * <p>Only validated leaps are taken; if none exists the rat returns to its trunk. On landing
 * the task switches to the new tree, including its trunk, so the later descent uses the trunk
 * the rat is actually on.</p>
 */
public final class LeapToTreePhase extends AbstractHarvestPhase {
    private static final double LEAP_SPEED = 0.28D;

    private final LeapValidator validator = new LeapValidator(new CanopyGraph());
    private BlockPos landingPos;
    private TreeCluster targetCluster;

    @Override
    public void onEnter(HarvestContext context) {
        landingPos = null;
        targetCluster = null;

        HarvestTask task = context.task();
        if (task == null || task.cluster().isBush()) {
            return;
        }

        TamedRat rat = context.rat();
        ServerLevel level = context.level();
        Vec3 from = rat.position();
        double maxLeap = SquirrelConfig.maxLeapDistance();
        ItemStack filter = rat.getDepositPos().isPresent() ? rat.getMainHandItem() : ItemStack.EMPTY;

        List<TreeCluster> nearby = LevelFruitIndex.get(level).query(rat.blockPosition(), 6, filter, context.blacklist(), context.gameTime());
        for (TreeCluster cluster : nearby) {
            if (cluster.id() == task.cluster().id() || cluster.isBush() || cluster.fruitsFor(task.kind()).isEmpty()) {
                continue;
            }
            List<BlockPos> landings = cluster.canopyBlocks().stream()
                    .filter(pos -> Vec3.atCenterOf(pos).distanceTo(from) <= maxLeap + 0.5D)
                    .sorted(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(from)))
                    .toList();
            for (BlockPos landing : landings) {
                if (validator.canLeap(level, from, Vec3.atCenterOf(landing), maxLeap)) {
                    landingPos = landing;
                    targetCluster = cluster;
                    context.debug("leaping to tree {} via {}", cluster.id(), landing);
                    return;
                }
            }
        }
    }

    @Override
    public PhaseResult tick(HarvestContext context) {
        if (landingPos == null || targetCluster == null || context.ticksInPhase() > 40) {
            return PhaseResult.transition(Phase.RETURN_TO_TRUNK);
        }

        Vec3 landing = CanopyMovementController.feetPoint(landingPos);
        double remaining = context.movement().steerTowards(landing, LEAP_SPEED, true);
        if (remaining > 0.3D) {
            return PhaseResult.stay();
        }

        HarvestTask task = context.task();
        if (task != null) {
            TrunkRoute route = targetCluster.trunks().isEmpty() ? task.route() : targetCluster.trunks().getFirst();
            task.switchCluster(targetCluster, route, targetCluster.fruitsFor(task.kind()));
        }
        return PhaseResult.transition(Phase.CANOPY_TRAVEL);
    }
}
