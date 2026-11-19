package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatsMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Reads and writes custom item NBT data via {@link DataComponents#CUSTOM_DATA} (1.21.1 API).
 */
public final class RatsItemStackData {

	private RatsItemStackData() {
	}

	// Returns a copy of the item tag or an empty tag if there is no data.
	public static CompoundTag getTag(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	// Returns the item tag for modification; creates an empty tag on the stack if missing.
	public static CompoundTag getOrCreateTag(ItemStack stack) {
		CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
		if (existing != null) {
			return existing.copyTag();
		}
		CompoundTag tag = new CompoundTag();
		setTag(stack, tag);
		return tag;
	}

	// Writes the tag into the stack's CUSTOM_DATA component.
	public static void setTag(ItemStack stack, CompoundTag tag) {
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	// true if the stack has a non-empty custom tag.
	public static boolean hasTag(ItemStack stack) {
		return !getTag(stack).isEmpty();
	}

	/**
	 * Deserializes an item from NBT; an empty or broken tag gives EMPTY without spamming the Minecraft log.
	 * Parameters: registry provider, NBT tag. Returns a stack or EMPTY.
	 */
	public static ItemStack parse(HolderLookup.Provider provider, CompoundTag tag) {
		try {
			if (tag == null || tag.isEmpty() || !tag.contains("id")) {
				return ItemStack.EMPTY;
			}
			return ItemStack.parse(provider, tag).orElse(ItemStack.EMPTY);
		} catch (Exception e) {
			RatsMod.LOGGER.error("Failed to parse ItemStack from NBT: {}", e.getMessage(), e);
			return ItemStack.EMPTY;
		}
	}

	/**
	 * Serializes an item to NBT; an empty stack is not written (avoids Cannot encode empty ItemStack).
	 */
	public static CompoundTag save(HolderLookup.Provider provider, ItemStack stack) {
		try {
			if (stack.isEmpty()) {
				return new CompoundTag();
			}
			CompoundTag tag = new CompoundTag();
			Tag encoded = stack.save(provider, tag);
			if (!(encoded instanceof CompoundTag compoundTag)) {
				throw new IllegalStateException("ItemStack.save did not return a CompoundTag: " + encoded.getClass().getSimpleName());
			}
			if (!compoundTag.contains("id")) {
				throw new IllegalStateException("ItemStack.save did not write the item id: " + stack);
			}
			return compoundTag;
		} catch (Exception e) {
			RatsMod.LOGGER.error("Failed to save ItemStack to NBT: {}", e.getMessage(), e);
			throw new IllegalStateException("Error saving ItemStack to NBT: " + e.getMessage(), e);
		}
	}

	/**
	 * Registries for loading items from NBT on the server/client; on a client without a world - EMPTY.
	 */
	public static HolderLookup.Provider resolveProvider(HolderLookup.Provider preferred) {
		if (preferred != null && preferred != RegistryAccess.EMPTY) {
			return preferred;
		}
		return RegistryAccess.EMPTY;
	}
}
