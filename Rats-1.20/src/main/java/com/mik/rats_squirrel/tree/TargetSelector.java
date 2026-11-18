package com.mik.rats_squirrel.tree;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.config.SquirrelConfig;
import com.mik.rats_squirrel.fruit.FruitKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Chooses the best reachable task from nearby clusters.
 *
 * <ul>
 *   <li>Candidates are scored with one consistent estimate: capped fruit value minus walking
 *       distance, a light climb cost and the canopy distance to the nearest fruit.</li>
 *   <li>Fruit trees get a configurable priority bonus over bushes.</li>
 *   <li>The cluster worked last gets a small bonus so the rat does not flip-flop.</li>
 *   <li>Vanilla paths are checked best-first, at most 4 per selection.</li>
 * </ul>
 */
public final class TargetSelector {
    private final TreeScanner scanner;
    private final FailureBlacklist blacklist;

    public TargetSelector(TreeScanner scanner, FailureBlacklist blacklist) {
        this.scanner = scanner;
        this.blacklist = blacklist;
    }

    /** Score bonus for the cluster worked last, so near-equal candidates do not flip-flop. */
    private static final double STICKY_BONUS = 12.0D;
    /** Fruit beyond this count no longer raises the score, so one huge patch cannot outweigh distance. */
    private static final int FRUIT_CAP = 8;
    private static final double FRUIT_VALUE = 6.0D;
    private static final int MAX_PATH_QUERIES = 4;

    private record Candidate(TreeCluster cluster, @Nullable TrunkRoute route, FruitKind kind, List<BlockPos> fruits, BlockPos walkTarget, double score) {
    }

    /** Selects the next harvest task for the rat. */
    public Optional<HarvestTask> select(TamedRat rat, int gameTime) {
        return select(rat, gameTime, null);
    }

    /**
     * Selects the next harvest task for the rat.
     *
     * <p>Every candidate is scored with the same distance estimate first, then vanilla paths are
     * checked best-first until one is reachable. Scoring all candidates the same way (rather
     * than mixing path lengths and straight-line distances) plus a bonus for the last worked
     * cluster keeps the choice stable, so the rat does not run back and forth between two
     * similar sources.</p>
     *
     * @param lastAnchor anchor of the cluster the rat worked last, or {@code null}
     */
    public Optional<HarvestTask> select(TamedRat rat, int gameTime, @Nullable BlockPos lastAnchor) {
        blacklist.cleanup(gameTime);
        if (!(rat.level() instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }

        LevelFruitIndex index = LevelFruitIndex.get(serverLevel);
        int radius = SquirrelConfig.searchRadius();
        index.enqueueArea(rat.blockPosition(), radius);
        index.tick(gameTime);

        // With a deposit the rat only collects what it already holds (it will carry it to the
        // chest); without one, like the original harvesters, any fruit is fair game and
        // whatever does not fit in the hand just drops.
        ItemStack held = rat.getDepositPos().isPresent() ? rat.getMainHandItem() : ItemStack.EMPTY;
        List<TreeCluster> clusters = index.query(rat.blockPosition(), radius, held, blacklist, gameTime);
        if (clusters.isEmpty()) {
            return Optional.empty();
        }

        Vec3 ratPos = rat.position();
        double treeBonus = SquirrelConfig.treePriorityBonus();
        List<Candidate> candidates = new ArrayList<>();
        for (TreeCluster cluster : clusters) {
            if (blacklist.isBlocked(cluster.id(), gameTime)) {
                continue;
            }
            Map.Entry<FruitKind, List<BlockPos>> pick = pickKind(serverLevel, index, cluster, held, gameTime);
            if (pick == null) {
                continue;
            }
            FruitKind kind = pick.getKey();
            List<BlockPos> fruits = pick.getValue();
            boolean sticky = lastAnchor != null && cluster.anchor().distManhattan(lastAnchor) <= 2;
            double bonus = sticky ? STICKY_BONUS : 0.0D;

            if (cluster.isBush()) {
                BlockPos nearest = nearestTo(fruits, ratPos);
                double ground = ratPos.distanceTo(Vec3.atCenterOf(nearest));
                candidates.add(new Candidate(cluster, null, kind, fruits, nearest,
                        fruitValue(fruits.size()) - ground + bonus));
                continue;
            }

            for (TrunkRoute route : cluster.trunks()) {
                if (blacklist.isBlocked(route.standPos(), gameTime)) {
                    continue;
                }
                double ground = ratPos.distanceTo(Vec3.atBottomCenterOf(route.standPos()));
                double canopy = Math.sqrt(nearestTo(fruits, Vec3.atCenterOf(route.canopyEntry())).distSqr(route.canopyEntry()));
                double utility = score(cluster, route, ground, canopy) + treeBonus + bonus;
                candidates.add(new Candidate(cluster, route, kind, fruits, route.standPos(), utility));
            }
        }

        candidates.sort(Comparator.comparingDouble(Candidate::score).reversed());

        PathNavigation navigation = rat.getNavigation();
        int pathQueries = 0;
        Set<Long> rejectedClusters = new HashSet<>();
        for (Candidate candidate : candidates) {
            if (rejectedClusters.contains(candidate.cluster().id())) {
                continue;
            }
            if (pathQueries >= MAX_PATH_QUERIES) {
                break;
            }
            if (navigation != null && !alreadyThere(rat, candidate)) {
                Path path = navigation.createPath(candidate.walkTarget(), 1);
                pathQueries++;
                if (path == null || !path.canReach()) {
                    blacklist.add(candidate.walkTarget(), FailureBlacklist.Reason.NO_PATH, gameTime, 1200);
                    if (candidate.cluster().isBush()) {
                        rejectedClusters.add(candidate.cluster().id());
                    }
                    continue;
                }
            }
            return Optional.of(new HarvestTask(candidate.cluster(), candidate.route(), candidate.kind(), candidate.fruits()));
        }
        return Optional.empty();
    }

    /**
     * Tree utility: capped fruit value minus walking distance, a light climb cost and the
     * canopy distance from the entry to the nearest fruit. The tree priority bonus is added by
     * the caller.
     */
    public double score(TreeCluster cluster, TrunkRoute route, double groundCost, double canopyCost) {
        double climbCost = route.height() * 0.25D;
        return fruitValue(cluster.fruitCount()) - groundCost - climbCost - canopyCost;
    }

    private static double fruitValue(int count) {
        return Math.min(count, FRUIT_CAP) * FRUIT_VALUE;
    }

    @Nullable
    private Map.Entry<FruitKind, List<BlockPos>> pickKind(ServerLevel level, LevelFruitIndex index, TreeCluster cluster, ItemStack held, int gameTime) {
        Map.Entry<FruitKind, List<BlockPos>> best = null;
        int bestCount = 0;
        for (Map.Entry<FruitKind, List<BlockPos>> entry : cluster.fruits().entrySet()) {
            if (!held.isEmpty() && !entry.getKey().accepts(held)) {
                continue;
            }
            // Cluster fruit lists are cached; re-check every position against the live block so
            // already picked or regrowing fruit never sends the rat on an empty trip.
            List<BlockPos> usable = new ArrayList<>();
            for (BlockPos p : entry.getValue()) {
                if (blacklist.isBlocked(p, gameTime)) {
                    continue;
                }
                if (isRipe(level, index, p, entry.getKey())) {
                    usable.add(p);
                } else {
                    index.invalidate(p);
                }
            }
            if (usable.size() > bestCount) {
                bestCount = usable.size();
                best = Map.entry(entry.getKey(), usable);
            }
        }
        return best;
    }

    /** Live maturity check of one cached fruit position. */
    public static boolean isRipe(ServerLevel level, LevelFruitIndex index, BlockPos pos, FruitKind kind) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        return index.classifier().classify(level, pos, level.getBlockState(pos)).filter(kind::equals).isPresent();
    }

    private static BlockPos nearestTo(List<BlockPos> positions, Vec3 from) {
        BlockPos best = positions.getFirst();
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : positions) {
            double d = Vec3.atCenterOf(pos).distanceToSqr(from);
            if (d < bestDist) {
                bestDist = d;
                best = pos;
            }
        }
        return best;
    }

    private static boolean alreadyThere(TamedRat rat, Candidate candidate) {
        if (candidate.route() != null && candidate.cluster().containsCanopy(rat.blockPosition())) {
            return true;
        }
        return rat.position().distanceToSqr(Vec3.atBottomCenterOf(candidate.walkTarget())) <= 2.25D;
    }

    public TreeScanner scanner() {
        return scanner;
    }

    public FailureBlacklist blacklist() {
        return blacklist;
    }
}
