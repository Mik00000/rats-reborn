/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.InventoryMenu
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.BannerItem
 *  net.minecraft.world.item.ItemStack
 */
package com.github.alexthe666.rats.server.inventory;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsMenuRegistry;
import com.github.alexthe666.rats.registry.RatsUpgradeConflictRegistry;
import com.github.alexthe666.rats.server.entity.rat.InventoryRat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.inventory.container.RatContainer;
import com.github.alexthe666.rats.server.items.upgrades.BaseRatUpgradeItem;
import com.github.alexthe666.rats.server.misc.RatsRemovedContent;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;

public class RatMenu
extends AbstractContainerMenu {
    private final Container ratInventory;
    private final int ratEntityId;
    private static final ResourceLocation EMPTY_HELMET_SLOT = ResourceLocation.fromNamespaceAndPath((String)"rats", (String)"item/empty_rat_helmet_slot");
    private static final ResourceLocation EMPTY_BANNER_SLOT = ResourceLocation.fromNamespaceAndPath((String)"rats", (String)"item/empty_rat_banner_slot");
    private static final ResourceLocation EMPTY_ITEM_SLOT = ResourceLocation.fromNamespaceAndPath((String)"rats", (String)"item/empty_rat_item_slot");
    private static final ResourceLocation EMPTY_UPGRADE_SLOT = ResourceLocation.fromNamespaceAndPath((String)"rats", (String)"item/empty_rat_upgrade_slot");

    public RatMenu(int id, Container ratInventory, Inventory playerInventory) {
        this(id, ratInventory, playerInventory, -1);
    }

    public static RatMenu fromNetwork(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        return new RatMenu(id, RatMenu.resolveRatInventory(playerInventory, entityId), playerInventory, entityId);
    }

    private static Container resolveRatInventory(Inventory playerInventory, int entityId) {
        Entity entity = playerInventory.player.level().getEntity(entityId);
        if (!(entity instanceof InventoryRat)) {
            RatsMod.LOGGER.error("Rat menu: entity id={} not found or not an InventoryRat", (Object)entityId);
            throw new IllegalStateException("Rat entity for the menu not found: id=" + entityId);
        }
        InventoryRat inventoryRat = (InventoryRat)entity;
        SimpleContainer container = inventoryRat.getInventory();
        if (container == null) {
            RatsMod.LOGGER.error("Rat menu: entity id={} has no inventory", (Object)entityId);
            throw new IllegalStateException("Rat inventory is missing: id=" + entityId);
        }
        return container;
    }

    private RatMenu(int id, Container ratInventory, final Inventory playerInventory, int ratEntityId) {
        super((MenuType)RatsMenuRegistry.RAT_CONTAINER.get(), id);
        this.ratInventory = ratInventory;
        this.ratEntityId = ratEntityId;
        ratInventory.startOpen(playerInventory.player);
        this.addSlot(new Slot(ratInventory, 0, 70, 54){

            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_ITEM_SLOT);
            }
        });
        this.addSlot(new Slot(ratInventory, 1, 70, 18){

            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && !stack.isEmpty() && (stack.getItem().canEquip(stack, EquipmentSlot.HEAD, (LivingEntity)playerInventory.player) || RatMenu.isHatItem(stack));
            }

            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_HELMET_SLOT);
            }
        });
        this.addSlot(new Slot(ratInventory, 2, 70, 36){

            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && !stack.isEmpty() && (TamedRat.isCheeseReserveItem(stack) || stack.getItem() instanceof BannerItem);
            }

            public int getMaxStackSize(ItemStack stack) {
                return TamedRat.isCheeseReserveItem(stack) ? 16 : 1;
            }

            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_BANNER_SLOT);
            }
        });
        int i = 0;
        while (i <= 2) {
            final int finalI = i++;
            this.addSlot(new Slot(ratInventory, 3 + finalI, -2, 18 + finalI * 18){

                public int getMaxStackSize() {
                    return 1;
                }

                public boolean mayPlace(ItemStack stack) {
                    return super.mayPlace(stack) && !stack.isEmpty() && stack.getItem() instanceof BaseRatUpgradeItem && !RatsRemovedContent.isRemoved(stack.getItem()) && RatMenu.canCoexistUpgrade(this.container, stack, finalI);
                }

                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_UPGRADE_SLOT);
                }
            });
        }
        for (int j = 0; j < 3; ++j) {
            for (int k1 = 0; k1 < 9; ++k1) {
                this.addSlot(new Slot((Container)playerInventory, k1 + j * 9 + 9, 8 + k1 * 18, 102 + j * 18 - 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot((Container)playerInventory, k, 8 + k * 18, 142));
        }
    }

    public RatMenu(int i, Inventory inventory) {
        this(i, (Container)new SimpleContainer(6), inventory);
    }

    public static boolean isHatItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null) {
            String ns = id.getNamespace();
            String path = id.getPath();
            if ("simplehats".equals(ns) || path.contains("hat") || path.contains("cap") || path.contains("crown") || path.contains("beanie")) {
                return true;
            }
        }
        return stack.getTags().anyMatch(tag -> {
            String path = tag.location().getPath();
            return path.contains("hat") || path.contains("head") || path.contains("helm");
        });
    }

    public int getRatEntityId() {
        return this.ratEntityId;
    }

    public static boolean canCoexistUpgrade(Container inventory, ItemStack upgrade, int ourSlot) {
        return switch (ourSlot) {
            case 0 -> {
                if (!RatsUpgradeConflictRegistry.doesConflict(upgrade, inventory.getItem(4)) && !RatsUpgradeConflictRegistry.doesConflict(upgrade, inventory.getItem(5))) {
                    yield true;
                }
                yield false;
            }
            case 1 -> {
                if (!RatsUpgradeConflictRegistry.doesConflict(upgrade, inventory.getItem(3)) && !RatsUpgradeConflictRegistry.doesConflict(upgrade, inventory.getItem(5))) {
                    yield true;
                }
                yield false;
            }
            case 2 -> {
                if (!RatsUpgradeConflictRegistry.doesConflict(upgrade, inventory.getItem(3)) && !RatsUpgradeConflictRegistry.doesConflict(upgrade, inventory.getItem(4))) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    public boolean stillValid(Player player) {
        return this.ratInventory.stillValid(player);
    }

    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < this.ratInventory.getContainerSize() ? !this.moveItemStackTo(itemstack1, this.ratInventory.getContainerSize(), this.slots.size(), true) : (this.getSlot(1).mayPlace(itemstack1) && !this.getSlot(1).hasItem() ? !this.moveItemStackTo(itemstack1, 1, 2, false) : (this.getSlot(2).mayPlace(itemstack1) ? !this.moveItemStackTo(itemstack1, 2, 3, false) : (this.getSlot(3).mayPlace(itemstack1) && !this.getSlot(3).hasItem() ? !this.moveItemStackTo(itemstack1, 3, 4, false) : (this.getSlot(4).mayPlace(itemstack1) && !this.getSlot(4).hasItem() ? !this.moveItemStackTo(itemstack1, 4, 5, false) : (this.getSlot(5).mayPlace(itemstack1) && !this.getSlot(5).hasItem() ? !this.moveItemStackTo(itemstack1, 5, 6, false) : (this.getSlot(0).mayPlace(itemstack1) ? !this.moveItemStackTo(itemstack1, 0, 1, false) : this.ratInventory.getContainerSize() <= 6 || !this.moveItemStackTo(itemstack1, 6, this.ratInventory.getContainerSize(), false)))))))) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    public void removed(Player player) {
        super.removed(player);
        this.ratInventory.stopOpen(player);
        Container container = this.ratInventory;
        if (container instanceof RatContainer) {
            RatContainer container2 = (RatContainer)container;
            container2.getRat().closeInventory();
        }
    }
}

