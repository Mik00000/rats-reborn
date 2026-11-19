package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Diagnostics and forced reload of the rat inventory and upgrade NBT.
 */
public final class RatPersistenceHelper {

	private RatPersistenceHelper() {
	}

	// Logs slot contents on save; takes the rat and entity tag, returns nothing.
	public static void logSave(TamedRat rat, CompoundTag tag) {
		try {
			if (!tag.contains("Items", Tag.TAG_LIST) && !tag.contains("RatUpgradeSlots", Tag.TAG_LIST)) {
				RatsMod.LOGGER.debug("Saving rat {}: NBT has no Items and RatUpgradeSlots", rat.getUUID());
				return;
			}
			StringBuilder builder = new StringBuilder();
			if (tag.contains("Items", Tag.TAG_LIST)) {
				ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
				for (int i = 0; i < items.size(); i++) {
					CompoundTag slotTag = items.getCompound(i);
					int slot = slotTag.getByte("Slot") & 255;
					ItemStack parsed = ItemStack.parse(rat.registryAccess(), slotTag).orElse(ItemStack.EMPTY);
					if (parsed.isEmpty()) {
						parsed = RatsItemStackData.parse(rat.registryAccess(), slotTag);
					}
					if (!parsed.isEmpty()) {
						builder.append(" [").append(slot).append("=").append(BuiltInRegistries.ITEM.getKey(parsed.getItem())).append("]");
					}
				}
			}
			RatsMod.LOGGER.debug("Saving rat {}: Items={}, RatUpgradeSlots={}, slots:{}",
					rat.getUUID(),
					tag.contains("Items", Tag.TAG_LIST) ? tag.getList("Items", Tag.TAG_COMPOUND).size() : 0,
					tag.contains("RatUpgradeSlots", Tag.TAG_LIST) ? tag.getList("RatUpgradeSlots", Tag.TAG_COMPOUND).size() : 0,
					builder);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error logging the save of rat {}: {}", rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Logs the load result; takes the rat and pending data flags, returns nothing.
	public static void logLoadResult(TamedRat rat, boolean hadPendingInventory, int expectedUpgrades) {
		try {
			StringBuilder builder = new StringBuilder();
			if (rat.getInventory() != null) {
				for (int slot = 0; slot < rat.getInventory().getContainerSize(); slot++) {
					ItemStack stack = rat.getInventory().getItem(slot);
					if (!stack.isEmpty()) {
						builder.append(" [").append(slot).append("=").append(BuiltInRegistries.ITEM.getKey(stack.getItem())).append("]");
					}
				}
			}
			RatsMod.LOGGER.debug("Loading rat {}: pendingItems={}, pendingUpgrades={}, slots after load:{}",
					rat.getUUID(), hadPendingInventory, expectedUpgrades, builder);
			// An empty RatUpgradeSlots list is saved for every rat without upgrades; only complain when
			// upgrades were actually stored but none of them made it into slots 3-5.
			if (expectedUpgrades > 0 && !rat.hasAnyUpgradeSlotFilled()) {
				RatsMod.LOGGER.warn("Loading rat {}: {} upgrade(s) were stored in NBT but slots 3-5 are empty after parsing", rat.getUUID(), expectedUpgrades);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error logging the load of rat {}: {}", rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}
}