package com.github.alexthe666.rats.server.items.upgrades;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CreateWorkerRatUpgradeItem extends BaseRatUpgradeItem {
	// Creates the Create worker upgrade; takes item properties and sets the description length.
	public CreateWorkerRatUpgradeItem(Item.Properties properties) {
		super(properties, 1, 3);
	}

	// Checks whether the rat should store the item; takes the rat and stack, returns true for regular transport.
	@Override
	public boolean shouldDepositItem(TamedRat rat, ItemStack stack) {
		try {
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check Create worker delivery of item {} for rat {}", stack, rat.getUUID(), exception);
			throw exception;
		}
	}
}
