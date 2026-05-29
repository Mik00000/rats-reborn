package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.registry.RatlantisItemRegistry;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public final class RatsRemovedContent {

	private RatsRemovedContent() {
	}

	public static final Set<ResourceLocation> REMOVED_ITEMS = Set.of(
			RatsBlockRegistry.RAT_TRAP.getId(),
			RatsItemRegistry.RAT_ARROW.getId(),
			RatsItemRegistry.CONTAMINATED_FOOD.getId(),
			RatsItemRegistry.LITTLE_BLACK_WORM.getId(),
			RatsItemRegistry.CENTIPEDE.getId(),
			RatsItemRegistry.RAT_UPGRADE_AQUATIC.getId(),
			RatsItemRegistry.RAT_UPGRADE_COMBINED.getId(),
			RatsItemRegistry.RAT_UPGRADE_COMBINED_CREATIVE.getId(),
			RatsBlockRegistry.UPGRADE_COMBINER.getId(),
			RatsBlockRegistry.UPGRADE_SEPARATOR.getId(),
			RatsItemRegistry.RAT_UPGRADE_TICK_ACCELERATOR.getId(),
			RatsItemRegistry.RAT_UPGRADE_TIME_MANIPULATOR.getId(),
			RatsItemRegistry.RAT_UPGRADE_BASIC_ENERGY.getId(),
			RatsItemRegistry.RAT_UPGRADE_ADVANCED_ENERGY.getId(),
			RatsItemRegistry.RAT_UPGRADE_ELITE_ENERGY.getId(),
			RatsItemRegistry.RAT_UPGRADE_EXTREME_ENERGY.getId(),
			RatlantisItemRegistry.RAT_UPGRADE_ARCHEOLOGIST.getId()
	);

	public static boolean isRemoved(Item item) {
		return REMOVED_ITEMS.contains(BuiltInRegistries.ITEM.getKey(item));
	}

	public static boolean isRemoved(Block block) {
		return REMOVED_ITEMS.contains(BuiltInRegistries.BLOCK.getKey(block));
	}
}