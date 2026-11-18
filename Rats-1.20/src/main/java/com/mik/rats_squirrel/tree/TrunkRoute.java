package com.mik.rats_squirrel.tree;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A validated, climbable path along one trunk column.
 *
 * <p>{@code logs} is ordered from the base log to the highest log that touches the canopy.
 * {@code canopyEntry} is the passable/leaf position where the rat leaves the trunk and enters
 * the canopy. {@code standPos} is the ground position next to {@code base} from which the rat
 * starts climbing.</p>
 *
 * @param base        lowest continuous log of the column
 * @param logs        ordered logs from {@code base} to the canopy contact point
 * @param canopyEntry first passable/leaf position that connects the trunk to the canopy
 * @param standPos    validated ground position adjacent to the base
 * @param height      number of blocks in the route, or the vertical extent used for scoring
 */
public record TrunkRoute(
        BlockPos base,
        List<BlockPos> logs,
        BlockPos canopyEntry,
        BlockPos standPos,
        int height
) {
    public TrunkRoute {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(canopyEntry, "canopyEntry");
        Objects.requireNonNull(standPos, "standPos");
        logs = List.copyOf(logs);
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative");
        }
    }

    /** A route must contain at least two logs and a canopy entry. */
    public boolean isValid() {
        return height >= 2 && logs.size() >= 2 && !base.equals(canopyEntry);
    }

    /** Highest log in the route, if any. */
    public Optional<BlockPos> topLog() {
        return logs.isEmpty() ? Optional.empty() : Optional.of(logs.get(logs.size() - 1));
    }

    /** Returns a copy of this route with a different stand position. */
    public TrunkRoute withStandPos(BlockPos newStandPos) {
        return new TrunkRoute(base, logs, canopyEntry, newStandPos, height);
    }
}
