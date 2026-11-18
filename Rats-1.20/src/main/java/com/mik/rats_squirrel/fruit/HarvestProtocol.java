package com.mik.rats_squirrel.fruit;

import com.mik.rats_squirrel.tree.FailureBlacklist;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;

/**
 * Runs one harvest transaction: snapshot, interact once, capture, verify.
 *
 * <p>Rules:
 * <ul>
 *   <li>Never call both a block event and {@code useWithoutItem} for the same block.</li>
 *   <li>Winery adapters are event-only.</li>
 *   <li>Generic adapters use {@code useWithoutItem} only; no loot-table fallback and no manual
 *       state reset.</li>
 *   <li>Fallback drops are allowed only for exact adapters with a known reset state.</li>
 *   <li>Re-read the block state after the interaction; never use the pre-interaction state for
 *       drops.</li>
 *   <li>If a successful interaction produced no items, blacklist the fruit for
 *       {@link FailureBlacklist.Reason#HARVEST_FAILED} instead of retrying a fallback.</li>
 * </ul>
 * </p>
 */
public final class HarvestProtocol {
    private final FailureBlacklist blacklist;

    public HarvestProtocol(FailureBlacklist blacklist) {
        this.blacklist = blacklist;
    }

    /**
     * Performs exactly one harvest attempt.
     */
    public HarvestOutcome harvest(ServerLevel level, BlockPos pos, BlockState state, FruitAdapter adapter, FakePlayer player) {
        BlockPos actual = adapter.actualPos(level, pos, state);
        BlockState actualState = level.getBlockState(actual);

        FakePlayer fakePlayer = player != null ? player : FakePlayerFactory.getMinecraft(level);
        fakePlayer.setPos(actual.getX() + 0.5D, actual.getY(), actual.getZ() + 0.5D);

        ItemCapture capture = ItemCapture.begin(level, actual, 1.5D);

        InteractionResult result = adapter.interact(level, actual, actualState, fakePlayer);

        List<ItemStack> items = capture.end(level, actual, 1.5D);
        BlockState resultingState = level.getBlockState(actual);

        if (items.isEmpty() && adapter.supportsExactFallback(actualState)
                && (result == InteractionResult.PASS || result == InteractionResult.FAIL)) {
            items = adapter.exactDrops(level, actual, actualState, fakePlayer);
            if (!items.isEmpty()) {
                adapter.applyExactReset(level, actual, actualState);
                resultingState = level.getBlockState(actual);
            }
        }

        if (items.isEmpty() && adapter.isSuccess(result)) {
            blacklist.addBlock(actual, FailureBlacklist.Reason.HARVEST_FAILED, (int) level.getGameTime(), 600);
        }

        return new HarvestOutcome(items, result, resultingState);
    }

    public FailureBlacklist blacklist() {
        return blacklist;
    }
}
