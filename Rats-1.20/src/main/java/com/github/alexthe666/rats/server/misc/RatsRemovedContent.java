package com.github.alexthe666.rats.server.misc;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class RatsRemovedContent {
    public static final Set<ResourceLocation> REMOVED_ITEMS = Set.of();

    private RatsRemovedContent() {
    }

    public static boolean isRemoved(Item item) {
        return false;
    }

    public static boolean isRemoved(Block block) {
        return false;
    }
}
