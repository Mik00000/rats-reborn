package com.mik.rats_squirrel.fruit;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Result of one harvest attempt.
 *
 * @param items          items captured from the interaction; may be empty
 * @param result         raw interaction result
 * @param resultingState block state re-read immediately after the interaction
 */
public record HarvestOutcome(List<ItemStack> items, InteractionResult result, BlockState resultingState) {
    public HarvestOutcome {
        items = List.copyOf(items);
    }

    /** Creates an outcome with no captured items. */
    public static HarvestOutcome none(InteractionResult result, BlockState resultingState) {
        return new HarvestOutcome(List.of(), result, resultingState);
    }

    /** Returns {@code true} when at least one item was captured. */
    public boolean harvestedAnything() {
        return !items.isEmpty();
    }
}
