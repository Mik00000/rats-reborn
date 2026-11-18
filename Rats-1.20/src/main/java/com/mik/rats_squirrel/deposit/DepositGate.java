package com.mik.rats_squirrel.deposit;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatInventoryAccess;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Precondition gate for deposit handoff.
 *
 * <p>The gate answers one question: can the rat's configured deposit accept the next item?
 * It must be called at most once every 20 ticks per rat and its result may be cached by
 * {@code (item, count)} until the next probe.</p>
 *
 * <p>This class deliberately does not move the rat or touch the deposit. The base
 * {@code RatDepositGoal} remains responsible for the actual handoff; the result is only used
 * for diagnostics ({@code /squirrel debug}, {@code /squirrel deposit}).</p>
 */
public final class DepositGate {
    private static final String ACTION = "squirrel deposit probe";

    private record CacheKey(UUID ratId, Item item, int count) {}
    private record CacheVal(DepositStatus status, int tick) {}
    private static final Map<CacheKey, CacheVal> CACHE = new ConcurrentHashMap<>();

    private DepositGate() {
    }

    /** Clears the probe cache; used by tests. */
    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Checks the configured deposit for the supplied stack with 20-tick caching.
     *
     * <p>An empty stack is treated as {@link DepositStatus#OK} because there is nothing to
     * insert yet; callers that need a capacity precheck must pass a representative fruit stack.
     * Reachability is intentionally not checked here because pathfinding is expensive. The
     * state machine should map a failed navigation attempt to {@link DepositStatus#UNREACHABLE}.</p>
     *
     * @param rat   rat whose deposit position and deposit facing should be used
     * @param stack stack that is about to be inserted, or a representative fruit stack
     * @return the current deposit status; never {@code null}
     */
    public static DepositStatus check(TamedRat rat, ItemStack stack) {
        int now = (int) rat.level().getGameTime();
        CacheKey key = new CacheKey(rat.getUUID(), stack.getItem(), stack.getCount());
        CacheVal cached = CACHE.get(key);
        if (cached != null && now - cached.tick < 20 && now >= cached.tick) {
            return cached.status;
        }

        DepositStatus status = computeStatus(rat, stack);
        if (CACHE.size() > 256) {
            pruneCache(now, 200);
        }
        CACHE.put(key, new CacheVal(status, now));
        return status;
    }

    private static DepositStatus computeStatus(TamedRat rat, ItemStack stack) {
        if (rat.getDepositPos().isEmpty()) {
            return DepositStatus.MISSING;
        }

        GlobalPos deposit = rat.getDepositPos().get();
        Level level = rat.level();
        if (!deposit.dimension().equals(level.dimension())) {
            return DepositStatus.WRONG_DIM;
        }

        BlockPos pos = deposit.pos();
        if (RatUtils.isBlockProtected(level, pos, rat)) {
            return DepositStatus.PROTECTED;
        }

        if (level.getBlockEntity(pos) == null) {
            return DepositStatus.NO_CAPABILITY;
        }

        if (stack.isEmpty()) {
            return DepositStatus.OK;
        }

        if (!RatInventoryAccess.canInsert(level, pos, rat.depositFacing, stack, rat, ACTION)) {
            return DepositStatus.FULL;
        }

        return DepositStatus.OK;
    }

    /** Removes cache entries older than {@code maxAge} ticks. */
    public static void pruneCache(int now, int maxAge) {
        CACHE.values().removeIf(v -> now - v.tick > maxAge || now < v.tick);
    }
}
