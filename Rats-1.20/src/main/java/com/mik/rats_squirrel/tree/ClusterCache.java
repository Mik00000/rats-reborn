package com.mik.rats_squirrel.tree;

import net.minecraft.core.BlockPos;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bounded TTL cache of {@link TreeCluster} snapshots, shared by all rats in one level.
 *
 * <p>The cache is deliberately simple: insertion-ordered eviction keeps the implementation
 * predictable and bounded. A production scanner may replace the eviction policy with an
 * access-order queue once profiling shows it matters.</p>
 */
public final class ClusterCache {
    private final int maxEntries;
    private final int ttl;
    private final Map<Long, Entry> entries = new LinkedHashMap<>();

    /**
     * @param maxEntries maximum number of cached clusters; must be positive
     * @param ttl        cluster lifetime in ticks; must be positive
     */
    public ClusterCache(int maxEntries, int ttl) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        if (ttl <= 0) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        this.maxEntries = maxEntries;
        this.ttl = ttl;
    }

    /** Returns a cached cluster when it exists and has not expired. */
    public Optional<TreeCluster> get(BlockPos fruitPos, int now) {
        long key = fruitPos.asLong();
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (now - entry.insertedAt() > ttl) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.cluster());
    }

    /** Stores or replaces a cluster and evicts the oldest entry when the cache is full. */
    public void put(BlockPos fruitPos, TreeCluster cluster, int now) {
        long key = fruitPos.asLong();
        entries.remove(key);
        entries.put(key, new Entry(cluster, now));
        while (entries.size() > maxEntries) {
            Iterator<Long> iterator = entries.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }

    /** Stores a cluster for its anchor position and for all its fruit positions. */
    public void putCluster(TreeCluster cluster, int now) {
        put(cluster.anchor(), cluster, now);
        for (java.util.List<BlockPos> fruitPositions : cluster.fruits().values()) {
            for (BlockPos p : fruitPositions) {
                put(p, cluster, now);
            }
        }
    }

    /** Removes the cluster keyed by the supplied fruit position. */
    public void invalidate(BlockPos fruitPos) {
        entries.remove(fruitPos.asLong());
    }

    /** Removes all expired entries. */
    public void cleanup(int now) {
        Iterator<Map.Entry<Long, Entry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().insertedAt() > ttl) {
                iterator.remove();
            }
        }
    }

    /** Removes every cached cluster. */
    public void clear() {
        entries.clear();
    }

    /** Number of cached clusters, including entries that have not been cleaned yet. */
    public int size() {
        return entries.size();
    }

    private record Entry(TreeCluster cluster, int insertedAt) {
    }
}
