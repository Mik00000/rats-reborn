package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Диагностика и принудительная перезагрузка NBT инвентаря и улучшений крысы.
 */
public final class RatPersistenceHelper {

	private RatPersistenceHelper() {
	}

	// Логирует содержимое слотов при сохранении; принимает крысу и тег сущности, ничего не возвращает.
	public static void logSave(TamedRat rat, CompoundTag tag) {
		try {
			if (!tag.contains("Items", Tag.TAG_LIST) && !tag.contains("RatUpgradeSlots", Tag.TAG_LIST)) {
				RatsMod.LOGGER.debug("Сохранение крысы {}: в NBT нет Items и RatUpgradeSlots", rat.getUUID());
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
			RatsMod.LOGGER.debug("Сохранение крысы {}: Items={}, RatUpgradeSlots={}, слоты:{}",
					rat.getUUID(),
					tag.contains("Items", Tag.TAG_LIST) ? tag.getList("Items", Tag.TAG_COMPOUND).size() : 0,
					tag.contains("RatUpgradeSlots", Tag.TAG_LIST) ? tag.getList("RatUpgradeSlots", Tag.TAG_COMPOUND).size() : 0,
					builder);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка логирования сохранения крысы {}: {}", rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Логирует результат загрузки; принимает крысу и флаги отложенных данных, ничего не возвращает.
	public static void logLoadResult(TamedRat rat, boolean hadPendingInventory, boolean hadPendingUpgrades) {
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
			RatsMod.LOGGER.debug("Загрузка крысы {}: pendingItems={}, pendingUpgrades={}, слоты после загрузки:{}",
					rat.getUUID(), hadPendingInventory, hadPendingUpgrades, builder);
			if (hadPendingUpgrades && builder.toString().isBlank()) {
				RatsMod.LOGGER.error("Загрузка крысы {}: RatUpgradeSlots были в NBT, но слоты 3-5 пусты после parse", rat.getUUID());
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка логирования загрузки крысы {}: {}", rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}
}