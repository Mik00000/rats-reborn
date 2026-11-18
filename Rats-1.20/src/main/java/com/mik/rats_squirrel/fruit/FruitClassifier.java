package com.mik.rats_squirrel.fruit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/**
 * Expensive-state classifier: maps a mature block state to a {@link FruitKind}.
 *
 * <p>The classifier is adapter-driven and never calls {@code Block.getDrops}. Adapters are
 * checked in registration order, so the most specific optional-mod adapter must be added
 * before the generic vanilla adapter.</p>
 */
public final class FruitClassifier {
    private final List<FruitAdapter> adapters;

    public FruitClassifier(List<FruitAdapter> adapters) {
        this.adapters = List.copyOf(adapters);
    }

    /** Creates the default adapter chain for the currently loaded optional mods. */
    public static FruitClassifier createDefault() {
        return new FruitClassifier(com.mik.rats_squirrel.compat.CompatAdapters.createDefaults());
    }

    /**
     * Returns the kind for a mature fruit, or {@link Optional#empty()} for an unknown or
     * immature block. Implementations must not query loot tables.
     */
    public Optional<FruitKind> classify(ServerLevel level, BlockPos pos, BlockState state) {
        for (FruitAdapter adapter : adapters) {
            if (adapter.matches(state) && adapter.isMature(level, pos, state)) {
                return Optional.of(adapter.kind(level, pos, state));
            }
        }
        return Optional.empty();
    }

    /** Finds the first matching adapter for a block state. */
    public Optional<FruitAdapter> findAdapter(BlockState state) {
        for (FruitAdapter adapter : adapters) {
            if (adapter.matches(state)) {
                return Optional.of(adapter);
            }
        }
        return Optional.empty();
    }

    /** Returns {@code true} if any adapter matches this block state. */
    public boolean matches(BlockState state) {
        return findAdapter(state).isPresent();
    }

    /** Finds the matching adapter for a mature fruit block. */
    public Optional<FruitAdapter> findMatureAdapter(ServerLevel level, BlockPos pos, BlockState state) {
        for (FruitAdapter adapter : adapters) {
            if (adapter.matches(state) && adapter.isMature(level, pos, state)) {
                return Optional.of(adapter);
            }
        }
        return Optional.empty();
    }

    /** Cheap pre-filter used by section scans. */
    public boolean maybeFruit(BlockState state) {
        for (FruitAdapter adapter : adapters) {
            if (adapter.matches(state)) {
                return true;
            }
        }
        return false;
    }

    public List<FruitAdapter> adapters() {
        return adapters;
    }
}
