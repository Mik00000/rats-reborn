package com.github.alexthe666.rats.server.inventory;

import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.registry.RatsMenuRegistry;
import com.github.alexthe666.rats.server.block.entity.RatCraftingTableBlockEntity;
import com.github.alexthe666.rats.server.inventory.container.TableItemHandlers;
import com.github.alexthe666.rats.server.inventory.slot.ImprovedSlotItemHandler;
import com.github.alexthe666.rats.server.inventory.slot.RatCraftingResultSlot;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

public class RatCraftingTableMenu extends RecipeBookMenu<CraftingInput, CraftingRecipe> {
   private final RatCraftingTableBlockEntity table;
   private final ContainerData data;

   public RatCraftingTableMenu(int id, Inventory playerInventory, RatCraftingTableBlockEntity table, ContainerData data) {
      super((MenuType)RatsMenuRegistry.RAT_CRAFTING_TABLE_CONTAINER.get(), id);
      this.table = table;
      this.data = data;
      this.addSlot(new RatCraftingResultSlot(table.resultHandler, playerInventory.player, table, 0, 130, 40));

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 3; j++) {
            this.addSlot(new ImprovedSlotItemHandler(table, table.matrixHandler, j + i * 3, 36 + j * 18, 22 + i * 18, true));
         }
      }

      for (int k = 0; k < 9; k++) {
         this.addSlot(new ImprovedSlotItemHandler(table, table.bufferHandler, k, k * 18 + 8, 96, false));
      }

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 129 + i * 18));
         }
      }

      for (int k = 0; k < 9; k++) {
         this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 187));
      }

      this.addDataSlots(data);
   }

   public RatCraftingTableMenu(int i, Inventory playerInventory, FriendlyByteBuf buf) {
      this(
         i,
         playerInventory,
         Objects.requireNonNull((RatCraftingTableBlockEntity)playerInventory.player.level().getBlockEntity(buf.readBlockPos())),
         new SimpleContainerData(2)
      );
   }

   public boolean stillValid(Player player) {
      return stillValid(ContainerLevelAccess.create(this.table.getLevel(), this.table.getBlockPos()), player, (Block)RatsBlockRegistry.RAT_CRAFTING_TABLE.get());
   }

   public RatCraftingTableBlockEntity getCraftingTable() {
      return this.table;
   }

   public int getCookProgressionScaled() {
      int i = this.data.get(0);
      int j = this.data.get(1);
      return i != 0 ? i * 23 / j : 0;
   }

   public void incrementRecipeIndex(boolean negative) {
      this.table.incrementSelectedRecipe(negative);
   }

   public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(slotIndex);
      if (slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (slotIndex == 0) {
            itemstack1.getItem().onCraftedBy(itemstack1, player.level(), player);
            if (!this.moveItemStackTo(itemstack1, 19, 55, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(itemstack1, itemstack);
         } else if (slotIndex >= 1 && slotIndex < 10) {
            if (!this.moveItemStackTo(itemstack1, 10, 19, false) && !this.moveItemStackTo(itemstack1, 19, 55, true)) {
               return ItemStack.EMPTY;
            }
         } else if (slotIndex >= 10 && slotIndex < 19) {
            if (!this.moveItemStackTo(itemstack1, 19, 55, true)) {
               return ItemStack.EMPTY;
            }
         } else if (slotIndex >= 19 && slotIndex < 46) {
            if (!this.moveItemStackTo(itemstack1, 10, 19, false) && !this.moveItemStackTo(itemstack1, 46, 55, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemstack1, 10, 19, false) && !this.moveItemStackTo(itemstack1, 19, 46, false)) {
            return ItemStack.EMPTY;
         }

         if (itemstack1.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(player, itemstack1);
         if (slotIndex == 0) {
            player.drop(itemstack1, false);
         }
      }

      return itemstack;
   }

   public void clicked(int slotIndex, int mouseButton, ClickType type, Player player) {
      if (slotIndex > 0 && slotIndex < 10) {
         ItemStack stack = this.getCarried().copyWithCount(1);
         ((Slot)this.slots.get(slotIndex)).set(stack);
         this.table.setChanged();
      } else {
         super.clicked(slotIndex, mouseButton, type, player);
      }
   }

   public void fillCraftSlotsStackedContents(StackedContents contents) {
      ((TableItemHandlers.BufferHandler)this.table.bufferHandler).fillStackedContents(contents);
   }

   public void clearCraftingContent() {
      for (int i = 0; i < this.table.matrixHandler.getSlots(); i++) {
         this.table.matrixHandler.setStackInSlot(i, ItemStack.EMPTY);
      }
   }

   public boolean recipeMatches(RecipeHolder<CraftingRecipe> recipe) {
      return true;
   }

   public int getResultSlotIndex() {
      return 0;
   }

   public int getGridWidth() {
      return 3;
   }

   public int getGridHeight() {
      return 3;
   }

   public int getSize() {
      return 10;
   }

   public RecipeBookType getRecipeBookType() {
      return RecipeBookType.CRAFTING;
   }

   public boolean shouldMoveToInventory(int index) {
      return false;
   }
}
