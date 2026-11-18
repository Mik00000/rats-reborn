package com.mik.rats_squirrel.fruit;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Stable description of one harvestable fruit type.
 *
 * <p>Identity is the {@link #id} only; the representative stack is mutable and must not be
 * used as a map key. The classifier caches kinds per block type/state so the same instance can
 * be reused for every tree of that fruit.</p>
 *
 * @param id             registry-style identity used by the classifier and debug output
 * @param representative sample stack used for deposit-capacity probes
 * @param acceptedItems  items the rat's hand must contain for this kind to be harvested
 * @param mature         state-only maturity predicate; must not query loot tables
 * @param treeFruit      {@code true} when the fruit grows in a tree canopy, {@code false} for a bush
 */
public record FruitKind(
        ResourceLocation id,
        ItemStack representative,
        Set<Item> acceptedItems,
        Predicate<BlockState> mature,
        boolean treeFruit
) {
    public FruitKind {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(representative, "representative");
        Objects.requireNonNull(acceptedItems, "acceptedItems");
        Objects.requireNonNull(mature, "mature");
        representative = representative.copy();
        acceptedItems = Set.copyOf(acceptedItems);
    }

    /**
     * Creates a placeholder kind for an adapter that is not implemented yet.
     *
     * <p>Placeholders never accept items and their maturity predicate is always false, so they
     * cannot accidentally trigger a harvest.</p>
     */
    public static FruitKind placeholder(ResourceLocation id, boolean treeFruit) {
        return new FruitKind(id, ItemStack.EMPTY, Set.of(), state -> false, treeFruit);
    }

    /** Returns {@code true} when the supplied hand stack is compatible with this kind. */
    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        return acceptedItems.contains(stack.getItem())
                || (!representative.isEmpty() && stack.is(representative.getItem()));
    }

    /** Evaluates the state-only maturity predicate. */
    public boolean isMature(BlockState state) {
        return mature.test(state);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FruitKind kind && id.equals(kind.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
