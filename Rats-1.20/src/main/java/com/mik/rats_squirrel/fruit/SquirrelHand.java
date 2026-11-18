package com.mik.rats_squirrel.fruit;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Hand rules of the squirrel, modelled on the original Rats harvest goals with a built-in
 * Platter upgrade.
 *
 * <ul>
 *   <li>The rat holds one stack of one item, up to 64 (or the item's max stack size).</li>
 *   <li>A harvested drop goes into the hand when the hand is empty or holds the same item.</li>
 *   <li>Anything that does not fit falls to the ground at the fruit, exactly like a crop
 *       broken by a harvester rat. With a deposit bound, the base item-collector goal picks it
 *       up later; without one it simply stays there.</li>
 * </ul>
 */
public final class SquirrelHand {
    private SquirrelHand() {
    }

    public static int capacity(ItemStack stack) {
        return Math.min(64, stack.getMaxStackSize());
    }

    /** Built-in Platter: the hand can take more while it is empty or not yet full. */
    public static boolean canHoldMore(TamedRat rat) {
        ItemStack held = rat.getMainHandItem();
        return held.isEmpty() || held.getCount() < capacity(held);
    }

    public static boolean isFull(TamedRat rat) {
        return !canHoldMore(rat);
    }

    /** Stores drops in the hand where possible and pops the rest at {@code dropAt}. */
    public static void store(TamedRat rat, ServerLevel level, BlockPos dropAt, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            ItemStack remaining = drop.copy();
            ItemStack held = rat.getMainHandItem();
            if (held.isEmpty()) {
                if (rat.canRatPickupItem(remaining)) {
                    int take = Math.min(remaining.getCount(), capacity(remaining));
                    rat.setItemInHand(InteractionHand.MAIN_HAND, remaining.split(take));
                }
            } else if (ItemStack.isSameItemSameComponents(held, remaining)) {
                int space = capacity(held) - held.getCount();
                if (space > 0) {
                    int take = Math.min(space, remaining.getCount());
                    held.grow(take);
                    remaining.shrink(take);
                }
            }
            if (!remaining.isEmpty()) {
                Block.popResource(level, dropAt, remaining);
            }
        }
    }
}
