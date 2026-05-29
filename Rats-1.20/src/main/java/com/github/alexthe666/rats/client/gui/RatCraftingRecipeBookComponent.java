package com.github.alexthe666.rats.client.gui;

import com.github.alexthe666.rats.server.inventory.RatCraftingTableMenu;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Iterator;

public class RatCraftingRecipeBookComponent extends RecipeBookComponent {

	@Override
	public void placeRecipe(int width, int height, int result, RecipeHolder<?> recipe, Iterator<Ingredient> iterator, int zeroidk) {
		if (this.menu instanceof RatCraftingTableMenu crafting) {
			for (int slot = 0; slot < 9; slot++) {
				crafting.getCraftingTable().matrixHandler.setStackInSlot(slot, ItemStack.EMPTY);
			}
			super.placeRecipe(width, height, result, recipe, iterator, zeroidk);
		}
	}

	public void addItemToSlot(Iterator<Ingredient> iterator, int slotIndex, int zeroidk, int width, int height) {
		if (this.menu instanceof RatCraftingTableMenu crafting) {
			Ingredient ingredient = iterator.next();
			if (!ingredient.isEmpty()) {
				Slot slot = this.menu.slots.get(slotIndex);
				crafting.getCraftingTable().matrixHandler.setStackInSlot(slot.index - 1, ingredient.getItems()[0]);
			}
		}
	}
}
