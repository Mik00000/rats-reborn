package com.github.alexthe666.rats.server.items.upgrades;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.items.OreRatNuggetItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

public class OreDoublingRatUpgradeItem extends BaseRatUpgradeItem {

	public OreDoublingRatUpgradeItem(Item.Properties properties) {
		super(properties, 2, 4);
	}

	@Override
	public boolean shouldDepositItem(TamedRat rat, ItemStack stack) {
		return !isProcessable(rat.level(), stack);
	}

	@Override
	public boolean isRatHoldingFood(TamedRat rat) {
		return isProcessable(rat.level(), rat.getMainHandItem());
	}

	// Checks whether the rat can eat this stack and produce ore chunks; takes the level and item, returns true/false.
	public static boolean isProcessable(Level level, ItemStack stack) {
		try {
			if (stack.isEmpty()) {
				return false;
			}
			if (!(stack.is(Tags.Items.ORES) || stack.is(Tags.Items.RAW_MATERIALS) || stack.is(Tags.Items.STORAGE_BLOCKS))) {
				return false;
			}
			return !OreRatNuggetItem.getIngot(level, stack).isEmpty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error checking ore for doubling {}: {}", stack.getItem(), exception.getMessage(), exception);
			throw exception;
		}
	}
}
