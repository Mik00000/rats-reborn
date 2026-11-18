package com.mik.rats_squirrel.fruit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;

/**
 * Mod-specific bridge between a block state and the harvest protocol.
 *
 * <p>An adapter owns exactly one collection strategy. It must not rely on a generic
 * {@code Block.getDrops} fallback unless {@link #supportsExactFallback()} is {@code true} and
 * it knows the exact reset state. Winery-style blocks that only react to a block event return
 * {@link #usesBlockEvent()} {@code = true} and must not also receive {@code useWithoutItem}.</p>
 *
 * <p>Phase 0 adapters are registered as stubs: {@link #matches(BlockState)} returns
 * {@code false} until the classifier work in Phase 1.</p>
 */
public interface FruitAdapter {
    /** Returns {@code true} when this adapter owns the supplied block state. */
    boolean matches(BlockState state);

    /** Returns {@code true} when the supplied block is currently harvestable. */
    boolean isMature(ServerLevel level, BlockPos pos, BlockState state);

    /** Returns the stable kind for a matched, mature block. */
    FruitKind kind(ServerLevel level, BlockPos pos, BlockState state);

    /**
     * Resolves the actual block to interact with, for example the lower half of a double bush.
     */
    default BlockPos actualPos(ServerLevel level, BlockPos pos, BlockState state) {
        return pos;
    }

    /**
     * Performs exactly one interaction with the block.
     *
     * <p>Adapters that return {@code true} from {@link #usesBlockEvent()} may ignore this method
     * and let {@link HarvestProtocol} drive the block event.</p>
     */
    InteractionResult interact(ServerLevel level, BlockPos pos, BlockState state, FakePlayer player);

    /** {@code true} when the block must be harvested through a right-click block event. */
    default boolean usesBlockEvent() {
        return false;
    }

    /** {@code true} when the adapter has a known exact drop list and reset state. */
    default boolean supportsExactFallback() {
        return false;
    }

    /** {@code true} when the adapter supports exact fallback for the specific block state. */
    default boolean supportsExactFallback(BlockState state) {
        return supportsExactFallback();
    }

    /**
     * Exact fallback drops. Only called when {@link #supportsExactFallback()} is {@code true}
     * and the primary interaction produced no items.
     */
    default List<ItemStack> exactDrops(ServerLevel level, BlockPos pos, BlockState state, FakePlayer player) {
        return List.of();
    }

    /** Exact state reset paired with {@link #exactDrops}. */
    default void applyExactReset(ServerLevel level, BlockPos pos, BlockState state) {
    }

    /** Default success test for the interaction result. */
    default boolean isSuccess(InteractionResult result) {
        return result.consumesAction();
    }
}
