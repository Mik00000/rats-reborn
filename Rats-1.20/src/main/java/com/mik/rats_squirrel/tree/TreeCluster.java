package com.mik.rats_squirrel.tree;

import com.mik.rats_squirrel.fruit.FruitKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable snapshot of one harvestable tree or bush cluster.
 *
 * <p>A cluster is built from a mature fruit: a bounded flood fill collects the connected
 * canopy, then the adjacent logs are grouped into validated {@link TrunkRoute}s. A cluster
 * with no routes is a low bush and is handled by the bush path of the state machine.</p>
 *
 * @param id                stable identity used by the failure blacklist
 * @param anchor            lowest log, or the fruit block for a bush
 * @param bounds            inclusive-ish bounding box of the scanned cluster
 * @param canopyBlocks      all leaves, fruit blocks and other canopy parts in the cluster
 * @param trunks            validated trunk routes; empty for a low bush
 * @param fruits            mature fruits grouped by their classified kind
 * @param lastValidatedTick game time at which this snapshot was validated
 */
public record TreeCluster(
        long id,
        BlockPos anchor,
        AABB bounds,
        Set<BlockPos> canopyBlocks,
        List<TrunkRoute> trunks,
        Map<FruitKind, List<BlockPos>> fruits,
        int lastValidatedTick
) {
    public TreeCluster {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(bounds, "bounds");
        canopyBlocks = Set.copyOf(canopyBlocks);
        trunks = List.copyOf(trunks);

        Map<FruitKind, List<BlockPos>> fruitCopy = new HashMap<>();
        fruits.forEach((kind, positions) -> fruitCopy.put(kind, List.copyOf(positions)));
        fruits = Map.copyOf(fruitCopy);
    }

    /** Returns {@code true} when this cluster has no climbable trunk and must be treated as a bush. */
    public boolean isBush() {
        return trunks.isEmpty();
    }

    /** Returns the mature fruits of the requested kind, or an empty list. */
    public List<BlockPos> fruitsFor(FruitKind kind) {
        return fruits.getOrDefault(kind, List.of());
    }

    /** Total number of mature fruits in the cluster. */
    public int fruitCount() {
        return fruits.values().stream().mapToInt(List::size).sum();
    }

    /** Returns {@code true} if this cluster's canopy contains the given position. */
    public boolean containsCanopy(BlockPos pos) {
        return canopyBlocks.contains(pos);
    }

    /** Returns {@code true} when the snapshot is older than the supplied TTL. */
    public boolean isExpired(int gameTime, int ttl) {
        return gameTime - lastValidatedTick > ttl;
    }

    /** Returns a copy of this cluster with a refreshed validation tick. */
    public TreeCluster revalidated(int gameTime) {
        return new TreeCluster(id, anchor, bounds, canopyBlocks, trunks, fruits, gameTime);
    }
}
