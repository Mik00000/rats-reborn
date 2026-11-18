package com.mik.rats_squirrel.ai.phase;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.canopy.CanopyBlocks;
import com.mik.rats_squirrel.config.SquirrelConfig;
import com.mik.rats_squirrel.fruit.FruitAdapter;
import com.mik.rats_squirrel.fruit.HarvestOutcome;
import com.mik.rats_squirrel.fruit.HarvestProtocol;
import com.mik.rats_squirrel.fruit.SquirrelHand;
import com.mik.rats_squirrel.tree.FailureBlacklist;
import com.mik.rats_squirrel.tree.HarvestTask;
import com.mik.rats_squirrel.tree.LevelFruitIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.Optional;

/**
 * {@code HARVEST}: pick the current fruit.
 *
 * <p>The rat brakes, turns to the fruit, waits a few ticks so the motion reads as "reaching",
 * then runs one adapter interaction. Drops follow {@link SquirrelHand}: into the hand up to a
 * full stack, the rest falls to the ground.</p>
 *
 * <p>Afterwards the squirrel keeps harvesting unless its hand is full <em>and</em> a deposit
 * is bound, in which case it climbs down so the base deposit goal can empty the hand. Without
 * a deposit it never stops for a full hand, like the original harvester rats.</p>
 */
public final class HarvestActionPhase extends AbstractHarvestPhase {
    private static final int REACH_DELAY = 4;
    private static final int TIMEOUT = 20;

    @Override
    public PhaseResult tick(HarvestContext context) {
        HarvestTask task = context.task();
        if (task == null) {
            return PhaseResult.finish();
        }
        boolean bush = task.cluster().isBush();
        BlockPos fruit = task.currentFruit();
        if (fruit == null) {
            return next(context, task, bush);
        }

        TamedRat rat = context.rat();
        Vec3 fruitCentre = Vec3.atCenterOf(fruit);
        if (!bush) {
            context.movement().hold();
            context.movement().faceTowards(fruitCentre);
        }
        rat.getLookControl().setLookAt(fruitCentre.x, fruitCentre.y, fruitCentre.z);

        if (context.ticksInPhase() < REACH_DELAY) {
            return PhaseResult.stay();
        }
        if (context.ticksInPhase() > TIMEOUT) {
            fail(context, task, fruit, "harvest timed out");
            return next(context, task, bush);
        }

        ServerLevel level = context.level();
        BlockState state = level.getBlockState(fruit);
        Optional<FruitAdapter> adapter = CanopyBlocks.classifier().findMatureAdapter(level, fruit, state);
        if (adapter.isEmpty()) {
            // Picked by someone else or no longer ripe: just forget it.
            task.removeFruit(fruit);
            LevelFruitIndex.get(level).invalidate(fruit);
            return next(context, task, bush);
        }

        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(level);
        HarvestOutcome outcome = new HarvestProtocol(context.blacklist()).harvest(level, fruit, state, adapter.get(), fakePlayer);
        if (!outcome.harvestedAnything() && !adapter.get().isSuccess(outcome.result())) {
            fail(context, task, fruit, "adapter returned nothing");
            return next(context, task, bush);
        }

        rat.crafting = true;
        level.broadcastEntityEvent(rat, (byte) 86);
        SquirrelHand.store(rat, level, fruit, outcome.items());
        task.removeFruit(fruit);
        LevelFruitIndex.get(level).invalidate(fruit);
        context.debug("harvested {} -> hand {}x{}", fruit, rat.getMainHandItem().getCount(), rat.getMainHandItem().getItem());
        return next(context, task, bush);
    }

    private static void fail(HarvestContext context, HarvestTask task, BlockPos fruit, String why) {
        int failures = task.recordFailure(fruit);
        context.debug("harvest of {} failed ({}), attempt {}", fruit, why, failures);
        if (failures >= CanopyTravelPhase.MAX_FAILURES) {
            context.blacklist().add(fruit, FailureBlacklist.Reason.HARVEST_FAILED, context.gameTime(), SquirrelConfig.blacklistTtl());
            task.removeFruit(fruit);
        } else {
            task.deferFruit(fruit);
        }
    }

    private static PhaseResult next(HarvestContext context, HarvestTask task, boolean bush) {
        TamedRat rat = context.rat();
        boolean hasDeposit = rat.getDepositPos().isPresent();
        if (hasDeposit && SquirrelHand.isFull(rat)) {
            context.debug("hand full, heading to deposit");
            return bush ? PhaseResult.finish() : PhaseResult.transition(Phase.RETURN_TO_TRUNK);
        }
        if (bush) {
            return task.hasFruit() ? PhaseResult.transition(Phase.GO_TO_BUSH) : PhaseResult.finish();
        }
        // Canopy travel re-reads the canopy and decides between leap and return when empty.
        return PhaseResult.transition(Phase.CANOPY_TRAVEL);
    }
}
