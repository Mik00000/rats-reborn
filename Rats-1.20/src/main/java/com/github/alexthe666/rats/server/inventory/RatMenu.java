package com.github.alexthe666.rats.server.inventory;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsMenuRegistry;
import com.github.alexthe666.rats.registry.RatsUpgradeConflictRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.entity.rat.InventoryRat;
import com.github.alexthe666.rats.server.inventory.container.RatContainer;
import com.github.alexthe666.rats.server.items.upgrades.BaseRatUpgradeItem;
import com.github.alexthe666.rats.server.misc.RatsRemovedContent;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;

public class RatMenu extends AbstractContainerMenu {
	private final Container ratInventory;
	private final int ratEntityId;
	private static final ResourceLocation EMPTY_HELMET_SLOT = ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "item/empty_rat_helmet_slot");
	private static final ResourceLocation EMPTY_BANNER_SLOT = ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "item/empty_rat_banner_slot");
	private static final ResourceLocation EMPTY_ITEM_SLOT = ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "item/empty_rat_item_slot");
	private static final ResourceLocation EMPTY_UPGRADE_SLOT = ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "item/empty_rat_upgrade_slot");

	public RatMenu(int id, Container ratInventory, Inventory playerInventory) {
		this(id, ratInventory, playerInventory, -1);
	}

	/**
	 * Фабрика для клиента: читает id крысы из буфера openMenu и создаёт меню.
	 * Параметры: id окна, инвентарь игрока, extra-данные (varInt id крысы). Возвращает RatMenu.
	 */
	public static RatMenu fromNetwork(int id, Inventory playerInventory, FriendlyByteBuf buf) {
		int entityId = buf.readVarInt();
		return new RatMenu(id, resolveRatInventory(playerInventory, entityId), playerInventory, entityId);
	}

	private static Container resolveRatInventory(Inventory playerInventory, int entityId) {
		Entity entity = playerInventory.player.level().getEntity(entityId);
		if (!(entity instanceof InventoryRat inventoryRat)) {
			RatsMod.LOGGER.error("Меню крысы: сущность id={} не найдена или не InventoryRat", entityId);
			throw new IllegalStateException("Сущность крысы для меню не найдена: id=" + entityId);
		}
		Container container = inventoryRat.getInventory();
		if (container == null) {
			RatsMod.LOGGER.error("Меню крысы: у сущности id={} нет инвентаря", entityId);
			throw new IllegalStateException("Инвентарь крысы отсутствует: id=" + entityId);
		}
		return container;
	}

	private RatMenu(int id, Container ratInventory, Inventory playerInventory, int ratEntityId) {
		super(RatsMenuRegistry.RAT_CONTAINER.get(), id);
		this.ratInventory = ratInventory;
		this.ratEntityId = ratEntityId;
		ratInventory.startOpen(playerInventory.player);
		this.addSlot(new Slot(ratInventory, 0, 70, 54) {
			@Override
			public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
				return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_ITEM_SLOT);
			}
		});
		this.addSlot(new Slot(ratInventory, 1, 70, 18) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return super.mayPlace(stack) && !stack.isEmpty() && stack.getItem().canEquip(stack, EquipmentSlot.HEAD, playerInventory.player);
			}

			@Override
			public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
				return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_HELMET_SLOT);
			}
		});
		this.addSlot(new Slot(ratInventory, 2, 70, 36) {
			public boolean mayPlace(ItemStack stack) {
				return super.mayPlace(stack) && !stack.isEmpty() && (TamedRat.isCheeseReserveItem(stack) || stack.getItem() instanceof BannerItem);
			}

			@Override
			public int getMaxStackSize(ItemStack stack) {
				return TamedRat.isCheeseReserveItem(stack) ? 16 : 1;
			}

			@Override
			public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
				return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_BANNER_SLOT);
			}
		});

		for (int i = 0; i <= 2; ++i) {
			int finalI = i;
			this.addSlot(new Slot(ratInventory, 3 + finalI, -2, 18 + finalI * 18) {

				@Override
				public int getMaxStackSize() {
					return 1;
				}

				public boolean mayPlace(ItemStack stack) {
					return super.mayPlace(stack) && !stack.isEmpty() && stack.getItem() instanceof BaseRatUpgradeItem && !RatsRemovedContent.isRemoved(stack.getItem()) && RatMenu.canCoexistUpgrade(this.container, stack, finalI);
				}

				@Override
				public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
					return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_UPGRADE_SLOT);
				}
			});
		}

		for (int j = 0; j < 3; ++j) {
			for (int k1 = 0; k1 < 9; ++k1) {
				this.addSlot(new Slot(playerInventory, k1 + j * 9 + 9, 8 + k1 * 18, 102 + j * 18 - 18));
			}
		}

		for (int k = 0; k < 9; ++k) {
			this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
		}
	}

	public RatMenu(int i, Inventory inventory) {
		this(i, new SimpleContainer(6), inventory);
	}

	public int getRatEntityId() {
		return this.ratEntityId;
	}

	public static boolean canCoexistUpgrade(Container inventory, ItemStack upgrade, int ourSlot) {
		return switch (ourSlot) {
			case 0 ->
					!RatsUpgradeConflictRegistry.doesConflict(upgrade, inventory.getItem(4)) && !RatsUpgradeConflictRegistry.doesConflict(upgrade, inventory.getItem(5));
			case 1 ->
					!RatsUpgradeConflictRegistry.doesConflict(upgrade, inventory.getItem(3)) && !RatsUpgradeConflictRegistry.doesConflict(upgrade, inventory.getItem(5));
			case 2 ->
					!RatsUpgradeConflictRegistry.doesConflict(upgrade, inventory.getItem(3)) && !RatsUpgradeConflictRegistry.doesConflict(upgrade, inventory.getItem(4));
			default -> false;
		};
	}

	@Override
	public boolean stillValid(Player player) {
		return this.ratInventory.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();
			if (index < this.ratInventory.getContainerSize()) {
				if (!this.moveItemStackTo(itemstack1, this.ratInventory.getContainerSize(), this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (this.getSlot(1).mayPlace(itemstack1) && !this.getSlot(1).hasItem()) {
				if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
					return ItemStack.EMPTY;
				}
			} else if (this.getSlot(2).mayPlace(itemstack1)) {
				if (!this.moveItemStackTo(itemstack1, 2, 3, false)) {
					return ItemStack.EMPTY;
				}
			} else if (this.getSlot(3).mayPlace(itemstack1) && !this.getSlot(3).hasItem()) {
				if (!this.moveItemStackTo(itemstack1, 3, 4, false)) {
					return ItemStack.EMPTY;
				}
			} else if (this.getSlot(4).mayPlace(itemstack1) && !this.getSlot(4).hasItem()) {
				if (!this.moveItemStackTo(itemstack1, 4, 5, false)) {
					return ItemStack.EMPTY;
				}
			} else if (this.getSlot(5).mayPlace(itemstack1) && !this.getSlot(5).hasItem()) {
				if (!this.moveItemStackTo(itemstack1, 5, 6, false)) {
					return ItemStack.EMPTY;
				}
			} else if (this.getSlot(0).mayPlace(itemstack1)) {
				if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (this.ratInventory.getContainerSize() <= 6 || !this.moveItemStackTo(itemstack1, 6, this.ratInventory.getContainerSize(), false)) {
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

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.ratInventory.stopOpen(player);
		if (this.ratInventory instanceof RatContainer container) {
			container.getRat().closeInventory();
		}
	}
}
