package com.mik.rats_squirrel.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common configuration for the squirrel harvest addon.
 *
 * <p>The values in this class are intentionally read-through: gameplay code should call the
 * static accessors instead of capturing config values at class-load time. This keeps server
 * reloads and config edits predictable.</p>
 */
public final class SquirrelConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue SEARCH_RADIUS;
    public static final ModConfigSpec.IntValue MAX_TREE_HEIGHT;
    public static final ModConfigSpec.IntValue SCAN_SECTIONS_PER_TICK;
    public static final ModConfigSpec.IntValue CLUSTER_CACHE_TTL;
    public static final ModConfigSpec.IntValue BLACKLIST_TTL;
    public static final ModConfigSpec.DoubleValue TREE_PRIORITY_BONUS;
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING;
    public static final ModConfigSpec.BooleanValue DEBUG_PARTICLES;
    public static final ModConfigSpec.DoubleValue MAX_LEAP_DISTANCE;
    public static final ModConfigSpec.IntValue CANOPY_PATH_NODE_LIMIT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Tree search and target acquisition").push("search");
        SEARCH_RADIUS = builder
                .comment("Horizontal radius, in blocks, used when looking for harvestable trees.")
                .defineInRange("searchRadius", 32, 8, 96);
        MAX_TREE_HEIGHT = builder
                .comment("Maximum trunk height the scanner may consider.")
                .defineInRange("maxTreeHeight", 48, 8, 128);
        SCAN_SECTIONS_PER_TICK = builder
                .comment("Maximum chunk sections inspected per tick by the shared fruit index.")
                .defineInRange("scanSectionsPerTick", 6, 1, 32);
        CLUSTER_CACHE_TTL = builder
                .comment("Lifetime of a cached tree cluster, in ticks.")
                .defineInRange("clusterCacheTtl", 400, 20, 2400);
        TREE_PRIORITY_BONUS = builder
                .comment("Score bonus for fruit trees over bushes, roughly in blocks of extra walking a squirrel accepts to climb a tree instead of picking a bush.")
                .defineInRange("treePriorityBonus", 32.0D, 0.0D, 256.0D);
        BLACKLIST_TTL = builder
                .comment("Lifetime of a failed target entry, in ticks.")
                .defineInRange("blacklistTtl", 900, 20, 7200);
        builder.pop();

        builder.comment("Canopy movement limits").push("movement");
        MAX_LEAP_DISTANCE = builder
                .comment("Maximum horizontal distance for a validated leap between canopies.")
                .defineInRange("maxLeapDistance", 3.0D, 1.0D, 5.0D);
        CANOPY_PATH_NODE_LIMIT = builder
                .comment("Maximum number of nodes expanded by the local canopy A* search.")
                .defineInRange("canopySearchNodeLimit", 2048, 64, 8192);
        builder.pop();

        builder.comment("Diagnostics").push("debug");
        DEBUG_LOGGING = builder
                .comment("Emit detailed squirrel decision logs. Keep disabled on production servers.")
                .define("debugLogging", false);
        DEBUG_PARTICLES = builder
                .comment("Render debug particles for routes, paths and selected targets.")
                .define("debugParticles", false);
        builder.pop();

        SPEC = builder.build();
    }

    private SquirrelConfig() {
    }

    public static int searchRadius() {
        return SPEC != null && SPEC.isLoaded() ? SEARCH_RADIUS.get() : 32;
    }

    public static int maxTreeHeight() {
        return SPEC != null && SPEC.isLoaded() ? MAX_TREE_HEIGHT.get() : 48;
    }

    public static int scanSectionsPerTick() {
        return SPEC != null && SPEC.isLoaded() ? SCAN_SECTIONS_PER_TICK.get() : 6;
    }

    public static int clusterCacheTtl() {
        return SPEC != null && SPEC.isLoaded() ? CLUSTER_CACHE_TTL.get() : 400;
    }

    public static int blacklistTtl() {
        return SPEC != null && SPEC.isLoaded() ? BLACKLIST_TTL.get() : 900;
    }

    public static double treePriorityBonus() {
        return SPEC != null && SPEC.isLoaded() ? TREE_PRIORITY_BONUS.get() : 32.0D;
    }

    public static boolean debugLogging() {
        return SPEC != null && SPEC.isLoaded() ? DEBUG_LOGGING.get() : false;
    }

    public static boolean debugParticles() {
        return SPEC != null && SPEC.isLoaded() ? DEBUG_PARTICLES.get() : false;
    }

    public static double maxLeapDistance() {
        return SPEC != null && SPEC.isLoaded() ? MAX_LEAP_DISTANCE.get() : 3.0D;
    }

    public static int canopyPathNodeLimit() {
        return SPEC != null && SPEC.isLoaded() ? CANOPY_PATH_NODE_LIMIT.get() : 2048;
    }
}
