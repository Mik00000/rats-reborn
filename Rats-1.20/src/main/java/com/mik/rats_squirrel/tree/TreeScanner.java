package com.mik.rats_squirrel.tree;

import com.mik.rats_squirrel.fruit.FruitClassifier;
import com.mik.rats_squirrel.fruit.FruitKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Incremental, level-shared tree and cluster scanner.
 *
 * <p>Phase 2 implementation:
 * <ul>
 *   <li>BFS over canopy parts (max 512 nodes, radius 12, height 32).</li>
 *   <li>Collects adjacent logs, builds continuous trunk columns, resolves canopy entry and stand pos.</li>
 *   <li>Low bushes (no logs) are identified and built without trunk routes.</li>
 *   <li>Stores and retrieves clusters through {@link ClusterCache}.</li>
 * </ul>
 * </p>
 */
public final class TreeScanner {
    private static final int MAX_CANOPY_NODES = 512;
    private static final int MAX_CANOPY_RADIUS = 12;
    private static final int MAX_CANOPY_HEIGHT = 32;

    private final ServerLevel level;
    private final ClusterCache cache;
    private final FruitClassifier classifier;
    private final int sectionsPerTick;

    public TreeScanner(ServerLevel level, ClusterCache cache, FruitClassifier classifier, int sectionsPerTick) {
        if (sectionsPerTick <= 0) {
            throw new IllegalArgumentException("sectionsPerTick must be positive");
        }
        this.level = level;
        this.cache = cache;
        this.classifier = classifier;
        this.sectionsPerTick = sectionsPerTick;
    }

    /** Advances scanner cleanup by the current game time. */
    public void tick(int gameTime) {
        cache.cleanup(gameTime);
    }

    /**
     * Returns a cached cluster, or builds one from the supplied mature fruit.
     */
    public Optional<TreeCluster> getOrBuild(BlockPos fruitPos, int gameTime) {
        Optional<TreeCluster> cached = cache.get(fruitPos, gameTime);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<TreeCluster> built = buildCluster(fruitPos, gameTime);
        built.ifPresent(cluster -> cache.putCluster(cluster, gameTime));
        return built;
    }

    /** Builds a cluster from a starting mature fruit position using BFS. */
    public Optional<TreeCluster> buildCluster(BlockPos startFruit, int gameTime) {
        BlockState startState = level.getBlockState(startFruit);
        Optional<FruitKind> startKind = classifier.classify(level, startFruit, startState);
        if (startKind.isEmpty()) {
            return Optional.empty();
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> canopyBlocks = new LinkedHashSet<>();
        Set<BlockPos> adjacentLogs = new LinkedHashSet<>();
        Map<FruitKind, List<BlockPos>> fruits = new HashMap<>();

        queue.add(startFruit);
        canopyBlocks.add(startFruit);
        fruits.computeIfAbsent(startKind.get(), k -> new ArrayList<>()).add(startFruit);

        // 1. BFS over canopy
        while (!queue.isEmpty() && canopyBlocks.size() < MAX_CANOPY_NODES) {
            BlockPos current = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);

                if (Math.abs(neighbor.getX() - startFruit.getX()) > MAX_CANOPY_RADIUS
                        || Math.abs(neighbor.getZ() - startFruit.getZ()) > MAX_CANOPY_RADIUS
                        || Math.abs(neighbor.getY() - startFruit.getY()) > MAX_CANOPY_HEIGHT) {
                    continue;
                }

                BlockState nState = level.getBlockState(neighbor);
                if (isLog(nState)) {
                    adjacentLogs.add(neighbor);
                } else if (isCanopyPart(nState)) {
                    if (canopyBlocks.add(neighbor)) {
                        Optional<FruitKind> optKind = classifier.classify(level, neighbor, nState);
                        optKind.ifPresent(k -> fruits.computeIfAbsent(k, key -> new ArrayList<>()).add(neighbor));
                        queue.add(neighbor);
                    }
                }
            }
        }

        // 2. If no logs -> low bush/vine cluster
        if (adjacentLogs.isEmpty()) {
            for (List<BlockPos> fruitList : fruits.values()) {
                fruitList.sort((a, b) -> Double.compare(a.distSqr(startFruit), b.distSqr(startFruit)));
            }
            AABB bounds = calculateBounds(canopyBlocks, List.of());
            TreeCluster bushCluster = new TreeCluster(startFruit.asLong(), startFruit, bounds, canopyBlocks, List.of(), fruits, gameTime);
            return Optional.of(bushCluster);
        }

        // 3. Tree: group logs by (X, Z) column and build trunk routes
        Map<Long, List<BlockPos>> columns = new LinkedHashMap<>();
        for (BlockPos log : adjacentLogs) {
            long columnKey = (((long) log.getX()) << 32) | (log.getZ() & 0xFFFFFFFFL);
            columns.computeIfAbsent(columnKey, k -> new ArrayList<>()).add(log);
        }

        List<TrunkRoute> routes = new ArrayList<>();
        for (List<BlockPos> logCol : columns.values()) {
            // Find highest log touching canopy
            BlockPos topLog = logCol.stream().max((a, b) -> Integer.compare(a.getY(), b.getY())).orElse(logCol.getFirst());

            // Descend downwards to base log
            List<BlockPos> columnLogs = new ArrayList<>();
            columnLogs.add(topLog);
            BlockPos curr = topLog;
            while (true) {
                BlockPos below = curr.below();
                BlockState belowState = level.getBlockState(below);
                if (isLog(belowState)) {
                    columnLogs.add(below);
                    curr = below;
                } else {
                    break;
                }
            }
            BlockPos base = curr;
            Collections.reverse(columnLogs);

            int height = columnLogs.size();
            if (height < 2) {
                continue;
            }

            // Resolve canopy entry
            BlockPos canopyEntry = findCanopyEntry(columnLogs, canopyBlocks);
            if (canopyEntry == null) {
                continue;
            }

            // Resolve stand position
            BlockPos standPos = findStandPos(base);
            if (standPos == null) {
                continue;
            }

            TrunkRoute route = new TrunkRoute(base, columnLogs, canopyEntry, standPos, height);
            if (route.isValid()) {
                routes.add(route);
            }
        }

        if (routes.isEmpty()) {
            return Optional.empty();
        }

        BlockPos refPos = routes.getFirst().canopyEntry();
        for (List<BlockPos> fruitList : fruits.values()) {
            fruitList.sort((a, b) -> Double.compare(a.distSqr(refPos), b.distSqr(refPos)));
        }

        BlockPos anchor = routes.getFirst().base();
        AABB bounds = calculateBounds(canopyBlocks, routes);
        TreeCluster cluster = new TreeCluster(anchor.asLong(), anchor, bounds, canopyBlocks, routes, fruits, gameTime);
        return Optional.of(cluster);
    }

    private BlockPos findCanopyEntry(List<BlockPos> columnLogs, Set<BlockPos> canopyBlocks) {
        // 1. Iterate from lowest log to top log: find first log touching canopy
        for (BlockPos log : columnLogs) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos side = log.relative(dir);
                if (canopyBlocks.contains(side) || isCanopyPart(level.getBlockState(side))) {
                    return side;
                }
            }
        }

        // 2. If no horizontal canopy blocks found, check above topLog
        BlockPos topLog = columnLogs.getLast();
        BlockPos above = topLog.above();
        if (canopyBlocks.contains(above) || isPassableCanopy(level.getBlockState(above))) {
            return above;
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = topLog.relative(dir);
            if (canopyBlocks.contains(side) || isPassableCanopy(level.getBlockState(side))) {
                return side;
            }
        }
        return topLog.above();
    }

    private BlockPos findStandPos(BlockPos base) {
        int[][] offsets = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };

        for (int[] offset : offsets) {
            int ox = offset[0];
            int oz = offset[1];

            for (int dy = 0; dy >= -1; dy--) {
                BlockPos candidate = base.offset(ox, dy, oz);
                BlockState inState = level.getBlockState(candidate);
                BlockState headState = level.getBlockState(candidate.above());
                BlockState floorState = level.getBlockState(candidate.below());

                if (isPassableStand(inState) && isPassableStand(headState) && isValidFloor(floorState)) {
                    return candidate;
                }
            }
            // Also check step-up
            BlockPos stepUp = base.offset(ox, 1, oz);
            if (isPassableStand(level.getBlockState(stepUp))
                    && isPassableStand(level.getBlockState(stepUp.above()))
                    && isValidFloor(level.getBlockState(stepUp.below()))) {
                return stepUp;
            }
        }
        return null;
    }

    private static boolean isValidFloor(BlockState floor) {
        if (!floor.isSolid()) {
            return false;
        }
        if (floor.is(BlockTags.LEAVES) || isLog(floor)) {
            return false;
        }
        if (floor.is(Blocks.LAVA) || floor.is(Blocks.FIRE) || floor.is(Blocks.CACTUS)) {
            return false;
        }
        return true;
    }

    public static boolean isPassableStand(BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (state.is(BlockTags.REPLACEABLE)) {
            return true;
        }
        if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.CACTUS)) {
            return false;
        }
        return !state.isSolid();
    }

    public static boolean isPassableCanopy(BlockState state) {
        if (state.isAir() || state.is(BlockTags.REPLACEABLE)) {
            return true;
        }
        if (state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock) {
            return true;
        }
        return !state.isSolid();
    }

    public static boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS)
                || (state.getBlock() instanceof RotatedPillarBlock && state.is(BlockTags.MINEABLE_WITH_AXE));
    }

    public boolean isCanopyPart(BlockState state) {
        if (state.isAir() || isLog(state)) {
            return false;
        }
        if (state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock) {
            return true;
        }
        return classifier.maybeFruit(state);
    }

    public boolean maybeRelevant(BlockState state) {
        return isCanopyPart(state) || isLog(state);
    }

    private static AABB calculateBounds(Set<BlockPos> canopy, List<TrunkRoute> routes) {
        if (canopy.isEmpty() && routes.isEmpty()) {
            return new AABB(0, 0, 0, 1, 1, 1);
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos p : canopy) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }

        for (TrunkRoute r : routes) {
            minX = Math.min(minX, r.standPos().getX());
            minY = Math.min(minY, r.standPos().getY());
            minZ = Math.min(minZ, r.standPos().getZ());
            maxX = Math.max(maxX, r.standPos().getX());
            maxY = Math.max(maxY, r.standPos().getY());
            maxZ = Math.max(maxZ, r.standPos().getZ());
        }

        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    public void invalidate(BlockPos pos) {
        cache.invalidate(pos);
    }

    public ServerLevel level() {
        return level;
    }

    public ClusterCache cache() {
        return cache;
    }

    public FruitClassifier classifier() {
        return classifier;
    }

    public int sectionsPerTick() {
        return sectionsPerTick;
    }
}
