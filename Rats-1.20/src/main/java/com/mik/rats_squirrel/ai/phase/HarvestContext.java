package com.mik.rats_squirrel.ai.phase;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.RatsSquirrelMod;
import com.mik.rats_squirrel.canopy.CanopyMovementController;
import com.mik.rats_squirrel.canopy.TrunkRail;
import com.mik.rats_squirrel.config.SquirrelConfig;
import com.mik.rats_squirrel.tree.FailureBlacklist;
import com.mik.rats_squirrel.tree.HarvestTask;
import com.mik.rats_squirrel.tree.TrunkRoute;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

/**
 * Shared mutable context passed to every phase handler.
 *
 * <p>Holds the active task, the trunk rail derived from the task's route, and the phase timer.</p>
 */
public final class HarvestContext {
    private final TamedRat rat;
    private final ServerLevel level;
    private final CanopyMovementController movement;
    private final FailureBlacklist blacklist;
    @Nullable
    private HarvestTask task;
    @Nullable
    private TrunkRail rail;
    @Nullable
    private TrunkRoute railRoute;
    private int ticksInPhase;

    public HarvestContext(TamedRat rat, ServerLevel level, CanopyMovementController movement, FailureBlacklist blacklist) {
        this.rat = rat;
        this.level = level;
        this.movement = movement;
        this.blacklist = blacklist;
    }

    public TamedRat rat() {
        return rat;
    }

    public ServerLevel level() {
        return level;
    }

    public CanopyMovementController movement() {
        return movement;
    }

    public FailureBlacklist blacklist() {
        return blacklist;
    }

    @Nullable
    public HarvestTask task() {
        return task;
    }

    public void setTask(@Nullable HarvestTask task) {
        this.task = task;
        this.rail = null;
        this.railRoute = null;
    }

    /**
     * Rail for the task's current trunk, recomputed whenever the route changes (for example
     * after a leap to another tree).
     */
    @Nullable
    public TrunkRail rail() {
        TrunkRoute route = task == null ? null : task.route();
        if (route == null) {
            return null;
        }
        if (rail == null || railRoute != route) {
            rail = TrunkRail.compute(level, route);
            railRoute = route;
        }
        return rail;
    }

    public int ticksInPhase() {
        return ticksInPhase;
    }

    public void advancePhaseTick() {
        this.ticksInPhase++;
    }

    public void resetPhaseTicks() {
        this.ticksInPhase = 0;
    }

    /** Current game time as an {@code int}; TTL arithmetic is wrap-safe for real sessions. */
    public int gameTime() {
        return (int) level.getGameTime();
    }

    /** Debug log line tagged with the rat id; no-op unless debug logging is enabled. */
    public void debug(String message, Object... args) {
        if (SquirrelConfig.debugLogging()) {
            RatsSquirrelMod.LOGGER.info("[squirrel {}] " + message, prepend(rat.getId(), args));
        }
    }

    private static Object[] prepend(Object first, Object[] rest) {
        Object[] all = new Object[rest.length + 1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }
}
