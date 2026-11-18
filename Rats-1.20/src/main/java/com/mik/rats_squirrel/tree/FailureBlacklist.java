package com.mik.rats_squirrel.tree;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * TTL blacklist for trees, fruits and clusters that failed validation or harvest.
 *
 * <p>Keys are independent: a fruit position and a cluster id may both be blacklisted. Entries
 * are stored with an absolute expiry tick so that no per-entry tick loop is needed. Cleanup is
 * amortised by calling {@link #cleanup(int)} during target scans.</p>
 */
public final class FailureBlacklist {
    private final Map<Long, Entry> blocks = new HashMap<>();
    private final Map<Long, Entry> clusters = new HashMap<>();

    /** Reason why a target was blacklisted. */
    public enum Reason {
        NO_STAND,
        NO_PATH,
        NO_TRUNK,
        NO_CANOPY_PATH,
        HARVEST_FAILED,
        NO_DEPOSIT
    }

    /**
     * Blacklists a fruit or stand position.
     *
     * @param pos    position to blacklist
     * @param reason failure reason used by debug output
     * @param now    current game time
     * @param ttl    lifetime in ticks
     */
    public void addBlock(BlockPos pos, Reason reason, int now, int ttl) {
        blocks.put(pos.asLong(), new Entry(reason, now + Math.max(1, ttl)));
    }

    /** Alias for {@link #addBlock}. */
    public void add(BlockPos pos, Reason reason, int now, int ttl) {
        addBlock(pos, reason, now, ttl);
    }

    /**
     * Blacklists a whole tree cluster.
     *
     * @param clusterId stable cluster identity
     * @param reason    failure reason used by debug output
     * @param now       current game time
     * @param ttl       lifetime in ticks
     */
    public void addCluster(long clusterId, Reason reason, int now, int ttl) {
        clusters.put(clusterId, new Entry(reason, now + Math.max(1, ttl)));
    }

    /** Alias for {@link #addCluster}. */
    public void add(long clusterId, Reason reason, int now, int ttl) {
        addCluster(clusterId, reason, now, ttl);
    }

    /** Returns {@code true} if the position is currently blacklisted. */
    public boolean isBlocked(BlockPos pos, int now) {
        Entry entry = blocks.get(pos.asLong());
        if (entry == null) {
            return false;
        }
        if (entry.expiresAt() <= now) {
            blocks.remove(pos.asLong());
            return false;
        }
        return true;
    }

    /** Returns {@code true} if the cluster is currently blacklisted. */
    public boolean isBlocked(long clusterId, int now) {
        Entry entry = clusters.get(clusterId);
        if (entry == null) {
            return false;
        }
        if (entry.expiresAt() <= now) {
            clusters.remove(clusterId);
            return false;
        }
        return true;
    }

    /** Returns the active reason for a position, if any. */
    public Optional<Reason> reasonFor(BlockPos pos, int now) {
        Entry entry = blocks.get(pos.asLong());
        if (entry == null || entry.expiresAt() <= now) {
            return Optional.empty();
        }
        return Optional.of(entry.reason());
    }

    /** Removes all expired entries. */
    public void cleanup(int now) {
        removeExpired(blocks, now);
        removeExpired(clusters, now);
    }

    /** Removes every entry. Used by the debug command. */
    public void clear() {
        blocks.clear();
        clusters.clear();
    }

    /** Number of tracked entries, including expired entries that have not been cleaned yet. */
    public int size() {
        return blocks.size() + clusters.size();
    }

    private static void removeExpired(Map<Long, Entry> entries, int now) {
        Iterator<Map.Entry<Long, Entry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt() <= now) {
                iterator.remove();
            }
        }
    }

    private record Entry(Reason reason, int expiresAt) {
    }
}
