package com.mik.rats_squirrel.tree;

import com.mik.rats_squirrel.fruit.FruitClassifier;
import com.mik.rats_squirrel.fruit.FruitKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One active harvest assignment: a tree (or bush), the trunk used to reach it, and the queue
 * of fruits still to pick.
 *
 * <p>The task is deliberately mutable and owned by one goal. A fruit that cannot be reached is
 * not dropped at the first failure: {@link #recordFailure} counts attempts and only the caller
 * decides when a fruit is given up.</p>
 */
public final class HarvestTask {
    private TreeCluster cluster;
    @Nullable
    private TrunkRoute route;
    private final FruitKind kind;
    private final Deque<BlockPos> fruitQueue;
    private final Map<BlockPos, Integer> failures = new HashMap<>();

    public HarvestTask(TreeCluster cluster, @Nullable TrunkRoute route, FruitKind kind, Collection<BlockPos> fruits) {
        this.cluster = Objects.requireNonNull(cluster, "cluster");
        this.route = route;
        this.kind = Objects.requireNonNull(kind, "kind");
        this.fruitQueue = new ArrayDeque<>(fruits);
    }

    public TreeCluster cluster() {
        return cluster;
    }

    @Nullable
    public TrunkRoute route() {
        return route;
    }

    public FruitKind kind() {
        return kind;
    }

    /** Live queue; callers may iterate it but should mutate through the task methods. */
    public Deque<BlockPos> fruitQueue() {
        return fruitQueue;
    }

    public boolean hasFruit() {
        return !fruitQueue.isEmpty();
    }

    @Nullable
    public BlockPos currentFruit() {
        return fruitQueue.peekFirst();
    }

    @Nullable
    public BlockPos pollFruit() {
        return fruitQueue.pollFirst();
    }

    public boolean contains(BlockPos fruit) {
        return fruitQueue.contains(fruit);
    }

    /** Moves the fruit to the front of the queue if present. */
    public void prioritizeFruit(BlockPos fruit) {
        if (fruitQueue.remove(fruit)) {
            fruitQueue.addFirst(fruit);
        }
    }

    /** Moves the fruit to the back of the queue so other fruits are tried first. */
    public void deferFruit(BlockPos fruit) {
        if (fruitQueue.remove(fruit)) {
            fruitQueue.addLast(fruit);
        }
    }

    public void removeFruit(BlockPos fruit) {
        fruitQueue.remove(fruit);
        failures.remove(fruit);
    }

    /** Records a failed attempt on a fruit and returns the total failure count. */
    public int recordFailure(BlockPos fruit) {
        return failures.merge(fruit.immutable(), 1, Integer::sum);
    }

    /** Moves the task to a neighbouring tree after a leap. */
    public void switchCluster(TreeCluster newCluster, @Nullable TrunkRoute newRoute, Collection<BlockPos> fruits) {
        this.cluster = Objects.requireNonNull(newCluster, "newCluster");
        this.route = newRoute;
        this.fruitQueue.clear();
        this.fruitQueue.addAll(fruits);
        this.failures.clear();
    }

    /**
     * Drops queued fruit that is no longer ripe (picked by someone, or regrowing).
     *
     * @return number of fruits removed
     */
    public int pruneUnripe(ServerLevel level, FruitClassifier classifier) {
        int before = fruitQueue.size();
        fruitQueue.removeIf(pos -> !level.isLoaded(pos)
                || classifier.classify(level, pos, level.getBlockState(pos)).filter(kind::equals).isEmpty());
        return before - fruitQueue.size();
    }

    /**
     * Re-reads the cluster's canopy for mature fruit of this task's kind that is not queued.
     *
     * @return number of fruits added
     */
    public int refreshFruits(ServerLevel level, FruitClassifier classifier, FailureBlacklist blacklist, int now) {
        int added = 0;
        for (BlockPos pos : cluster.canopyBlocks()) {
            if (fruitQueue.contains(pos) || blacklist.isBlocked(pos, now) || failures.getOrDefault(pos, 0) > 0) {
                continue;
            }
            Optional<FruitKind> found = classifier.classify(level, pos, level.getBlockState(pos));
            if (found.isPresent() && found.get().equals(kind)) {
                fruitQueue.addLast(pos);
                added++;
            }
        }
        return added;
    }
}
