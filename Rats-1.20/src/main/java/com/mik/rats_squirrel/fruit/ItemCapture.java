package com.mik.rats_squirrel.fruit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Merge-safe snapshot of item entities dropped during one interaction.
 *
 * <p>Phase 1 implementation:</p>
 * <ol>
 *   <li>Before the interaction, records {@code Map<UUID, Integer>} for item entities within
 *       a 1.5-2 block radius of the target centre. Also records existing falling block entities.</li>
 *   <li>After the interaction, a new UUID contributes its full stack. An existing UUID whose
 *       count increased contributes only the delta and is reduced back to its previous count.
 *       A decreased count is ignored.</li>
 *   <li>Never discards unrelated entities in the area. Only newly generated entities or the
 *       exact consumed delta are taken.</li>
 *   <li>Handles Durian-style {@link FallingBlockEntity}: extracts loot drops and discards the
 *       falling entity to prevent duplication or loot loss.</li>
 * </ol>
 */
public final class ItemCapture {
    private static final double MAX_DISTANCE = 2.0D;
    private static final double MAX_DISTANCE_SQR = MAX_DISTANCE * MAX_DISTANCE;

    private final Map<UUID, Integer> beforeItems;
    private final Set<UUID> beforeFallingBlocks;

    private ItemCapture(Map<UUID, Integer> beforeItems, Set<UUID> beforeFallingBlocks) {
        this.beforeItems = beforeItems;
        this.beforeFallingBlocks = beforeFallingBlocks;
    }

    /** Starts a capture around the supplied block position. */
    public static ItemCapture begin(ServerLevel level, BlockPos pos, double radius) {
        AABB box = new AABB(pos).inflate(radius);
        Vec3 center = Vec3.atCenterOf(pos);

        Map<UUID, Integer> beforeItems = new HashMap<>();
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box,
                e -> e.distanceToSqr(center.x, center.y, center.z) <= MAX_DISTANCE_SQR);
        for (ItemEntity item : items) {
            beforeItems.put(item.getUUID(), item.getItem().getCount());
        }

        Set<UUID> beforeFalling = new HashSet<>();
        List<FallingBlockEntity> falling = level.getEntitiesOfClass(FallingBlockEntity.class, box,
                e -> e.distanceToSqr(center.x, center.y, center.z) <= MAX_DISTANCE_SQR);
        for (FallingBlockEntity f : falling) {
            beforeFalling.add(f.getUUID());
        }

        return new ItemCapture(beforeItems, beforeFalling);
    }

    /**
     * Ends the capture and returns only the item deltas produced by the interaction.
     */
    public List<ItemStack> end(ServerLevel level, BlockPos pos, double radius) {
        AABB box = new AABB(pos).inflate(radius);
        Vec3 center = Vec3.atCenterOf(pos);

        List<ItemStack> captured = new ArrayList<>();

        // 1. Process ItemEntities
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box,
                e -> e.distanceToSqr(center.x, center.y, center.z) <= MAX_DISTANCE_SQR);
        for (ItemEntity item : items) {
            UUID id = item.getUUID();
            ItemStack currentStack = item.getItem();
            if (currentStack.isEmpty()) {
                continue;
            }

            if (!beforeItems.containsKey(id)) {
                // New entity dropped during interaction
                captured.add(currentStack.copy());
                item.discard();
            } else {
                int oldCount = beforeItems.get(id);
                int newCount = currentStack.getCount();
                if (newCount > oldCount) {
                    int delta = newCount - oldCount;
                    captured.add(currentStack.copyWithCount(delta));
                    int remaining = oldCount;
                    if (remaining <= 0) {
                        item.discard();
                    } else {
                        item.setItem(currentStack.copyWithCount(remaining));
                    }
                }
            }
        }

        // 2. Process FallingBlockEntities (Durian)
        List<FallingBlockEntity> fallingEntities = level.getEntitiesOfClass(FallingBlockEntity.class, box,
                e -> e.distanceToSqr(center.x, center.y, center.z) <= MAX_DISTANCE_SQR);
        for (FallingBlockEntity falling : fallingEntities) {
            UUID id = falling.getUUID();
            if (!beforeFallingBlocks.contains(id)) {
                BlockState fallingState = falling.getBlockState();
                List<ItemStack> drops = Block.getDrops(fallingState, level, pos, null, null, ItemStack.EMPTY);
                if (!drops.isEmpty()) {
                    for (ItemStack drop : drops) {
                        if (!drop.isEmpty()) {
                            captured.add(drop.copy());
                        }
                    }
                } else {
                    Item blockItem = fallingState.getBlock().asItem();
                    if (blockItem != Items.AIR) {
                        captured.add(new ItemStack(blockItem));
                    }
                }
                falling.discard();
            }
        }

        return compact(captured);
    }

    /** Merges stacks of the same item into fewer entries. */
    public static List<ItemStack> compact(List<ItemStack> items) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack toInsert = stack.copy();
            for (ItemStack existing : result) {
                if (ItemStack.isSameItemSameComponents(existing, toInsert)) {
                    int maxAdd = existing.getMaxStackSize() - existing.getCount();
                    int adding = Math.min(toInsert.getCount(), maxAdd);
                    if (adding > 0) {
                        existing.grow(adding);
                        toInsert.shrink(adding);
                        if (toInsert.isEmpty()) {
                            break;
                        }
                    }
                }
            }
            if (!toInsert.isEmpty()) {
                result.add(toInsert);
            }
        }
        return result;
    }
}
