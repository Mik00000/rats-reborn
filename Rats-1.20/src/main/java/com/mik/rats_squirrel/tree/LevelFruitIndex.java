package com.mik.rats_squirrel.tree;

import com.mik.rats_squirrel.config.SquirrelConfig;
import com.mik.rats_squirrel.fruit.FruitClassifier;
import com.mik.rats_squirrel.fruit.FruitKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Level-shared spatial index and budgeted section scanner.
 *
 * <p>Shared across all rats in one {@link ServerLevel}. Inspects at most {@code scanSectionsPerTick}
 * sections per tick, filtering empty and non-relevant sections via {@link LevelChunkSection#maybeHas}.
 * Builds and caches {@link TreeCluster}s from discovered mature fruits.</p>
 */
public final class LevelFruitIndex {
    private static final Map<ResourceKey<Level>, LevelFruitIndex> INSTANCES = new ConcurrentHashMap<>();

    private final ServerLevel level;
    private final TreeScanner scanner;
    private final FruitClassifier classifier;
    private final ClusterCache cache;
    private final int sectionsPerTick;

    private final Map<BlockPos, FruitKind> knownFruits = new ConcurrentHashMap<>();
    private final Deque<SectionPos> scanQueue = new ArrayDeque<>();
    private final Set<SectionPos> queuedSections = new HashSet<>();

    public LevelFruitIndex(ServerLevel level, TreeScanner scanner, FruitClassifier classifier, ClusterCache cache, int sectionsPerTick) {
        this.level = level;
        this.scanner = scanner;
        this.classifier = classifier;
        this.cache = cache;
        this.sectionsPerTick = Math.max(1, sectionsPerTick);
    }

    /** Returns or creates the level-shared fruit index for the supplied server level. */
    public static LevelFruitIndex get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level.dimension(), dim -> {
            ClusterCache cache = new ClusterCache(2048, SquirrelConfig.clusterCacheTtl());
            FruitClassifier classifier = FruitClassifier.createDefault();
            int sections = SquirrelConfig.scanSectionsPerTick();
            TreeScanner scanner = new TreeScanner(level, cache, classifier, sections);
            return new LevelFruitIndex(level, scanner, classifier, cache, sections);
        });
    }

    /** Clears all instances (e.g. on server reload or shutdown). */
    public static void clearAll() {
        INSTANCES.clear();
    }

    /** Enqueues chunk sections in the horizontal radius around the centre for scanning. */
    public synchronized void enqueueArea(BlockPos center, int radius) {
        int minSecX = SectionPos.blockToSectionCoord(center.getX() - radius);
        int maxSecX = SectionPos.blockToSectionCoord(center.getX() + radius);
        int minSecZ = SectionPos.blockToSectionCoord(center.getZ() - radius);
        int maxSecZ = SectionPos.blockToSectionCoord(center.getZ() + radius);

        int minSecY = Math.max(level.getMinSection(), SectionPos.blockToSectionCoord(center.getY() - 16));
        int maxSecY = Math.min(level.getMaxSection(), SectionPos.blockToSectionCoord(center.getY() + Math.max(32, com.mik.rats_squirrel.config.SquirrelConfig.maxTreeHeight())));

        for (int sx = minSecX; sx <= maxSecX; sx++) {
            for (int sz = minSecZ; sz <= maxSecZ; sz++) {
                for (int sy = minSecY; sy <= maxSecY; sy++) {
                    if (scanQueue.size() >= 512) {
                        return;
                    }
                    SectionPos sp = SectionPos.of(sx, sy, sz);
                    if (!queuedSections.contains(sp)) {
                        queuedSections.add(sp);
                        scanQueue.add(sp);
                    }
                }
            }
        }
    }

    /**
     * Ticks the scanner and processes up to {@code sectionsPerTick} chunk sections.
     */
    public synchronized void tick(int gameTime) {
        scanner.tick(gameTime);

        int budget = sectionsPerTick;
        while (budget > 0 && !scanQueue.isEmpty()) {
            SectionPos sp = scanQueue.poll();
            queuedSections.remove(sp);
            budget--;

            scanSection(sp, gameTime);
        }
    }

    private void scanSection(SectionPos sp, int gameTime) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(sp.x(), sp.z());
        if (chunk == null) {
            return;
        }

        int secIdx = level.getSectionIndexFromSectionY(sp.y());
        if (secIdx < 0 || secIdx >= chunk.getSections().length) {
            return;
        }

        LevelChunkSection section = chunk.getSection(secIdx);
        if (section == null || section.hasOnlyAir()) {
            return;
        }

        if (!section.maybeHas(scanner::maybeRelevant)) {
            return;
        }

        BlockPos origin = sp.origin();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = section.getBlockState(x, y, z);
                    if (classifier.maybeFruit(state)) {
                        mutable.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                        Optional<FruitKind> kind = classifier.classify(level, mutable, state);
                        if (kind.isPresent()) {
                            BlockPos fruitPos = mutable.immutable();
                            knownFruits.put(fruitPos, kind.get());
                            scanner.getOrBuild(fruitPos, gameTime);
                        } else {
                            knownFruits.remove(mutable);
                        }
                    }
                }
            }
        }
    }

    /** Immediately scans sections around a position if knownFruits has no candidate. */
    public void probeImmediate(BlockPos center, int gameTime) {
        SectionPos centerSection = SectionPos.of(center);
        for (int dy = -1; dy <= 2; dy++) {
            int sy = centerSection.y() + dy;
            if (sy >= level.getMinSection() && sy <= level.getMaxSection()) {
                scanSection(SectionPos.of(centerSection.x(), sy, centerSection.z()), gameTime);
            }
        }
    }

    /** Queries clusters with a fresh blacklist and current game time. */
    public List<TreeCluster> query(BlockPos center, int radius, ItemStack held) {
        return query(center, radius, held, new FailureBlacklist(), (int) level.getGameTime());
    }

    /**
     * Queries clusters within the given radius of the centre matching the held item.
     *
     * @param center    search centre
     * @param radius    search radius
     * @param held      item held by the rat (or empty)
     * @param blacklist failure blacklist
     * @param gameTime  current game time
     * @return candidate clusters (at most 12)
     */
    public List<TreeCluster> query(BlockPos center, int radius, ItemStack held, FailureBlacklist blacklist, int gameTime) {
        if (knownFruits.isEmpty()) {
            probeImmediate(center, gameTime);
        }

        List<TreeCluster> result = new ArrayList<>();
        Set<Long> seenClusterIds = new HashSet<>();
        double radiusSqr = (double) radius * radius;
        int maxRise = com.mik.rats_squirrel.config.SquirrelConfig.maxTreeHeight();

        // Horizontal radius: fruit high up in a tall tree is still "near" the rat below it.
        for (Map.Entry<BlockPos, FruitKind> entry : knownFruits.entrySet()) {
            BlockPos fruitPos = entry.getKey();
            double dx = fruitPos.getX() - center.getX();
            double dz = fruitPos.getZ() - center.getZ();
            if (dx * dx + dz * dz > radiusSqr || Math.abs(fruitPos.getY() - center.getY()) > maxRise) {
                continue;
            }
            if (blacklist.isBlocked(fruitPos, gameTime)) {
                continue;
            }
            FruitKind kind = entry.getValue();
            if (!held.isEmpty() && !kind.accepts(held)) {
                continue;
            }

            Optional<TreeCluster> optCluster = scanner.getOrBuild(fruitPos, gameTime);
            if (optCluster.isPresent()) {
                TreeCluster cluster = optCluster.get();
                if (blacklist.isBlocked(cluster.id(), gameTime)) {
                    continue;
                }
                if (seenClusterIds.add(cluster.id())) {
                    result.add(cluster);
                    if (result.size() >= 12) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /** Records a directly discovered fruit (e.g. from tests or events). */
    public void trackFruit(BlockPos pos, FruitKind kind) {
        knownFruits.put(pos.immutable(), kind);
    }

    /** Invalidates a block position. */
    public void invalidate(BlockPos pos) {
        knownFruits.remove(pos);
        scanner.invalidate(pos);
    }

    public ServerLevel level() {
        return level;
    }

    public TreeScanner scanner() {
        return scanner;
    }

    public FruitClassifier classifier() {
        return classifier;
    }

    public ClusterCache cache() {
        return cache;
    }

    public int knownFruitsCount() {
        return knownFruits.size();
    }

    public int queuedSectionsCount() {
        return scanQueue.size();
    }

    public Map<BlockPos, FruitKind> knownFruits() {
        return Collections.unmodifiableMap(knownFruits);
    }
}
