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
 * Чтение и запись пользовательских NBT-данных предмета через {@link DataComponents#CUSTOM_DATA} (API 1.21.1).
 */
public final class RatsItemStackData {

	private RatsItemStackData() {
	}

	// Возвращает копию тега предмета или пустой тег, если данных нет.
	public static CompoundTag getTag(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	// Возвращает тег предмета для изменения; при отсутствии создаёт пустой тег на стеке.
	public static CompoundTag getOrCreateTag(ItemStack stack) {
		CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
		if (existing != null) {
			return existing.copyTag();
		}
		CompoundTag tag = new CompoundTag();
		setTag(stack, tag);
		return tag;
	}

	// Записывает тег в компонент CUSTOM_DATA стека.
	public static void setTag(ItemStack stack, CompoundTag tag) {
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	// true, если у стека есть ненулевой пользовательский тег.
	public static boolean hasTag(ItemStack stack) {
		return !getTag(stack).isEmpty();
	}

	/**
	 * Десериализует предмет из NBT; пустой или битый тег даёт EMPTY без спама в лог Minecraft.
	 * Параметры: provider реестров, тег NBT. Отдаёт стек или EMPTY.
	 */
	public static ItemStack parse(HolderLookup.Provider provider, CompoundTag tag) {
		try {
			if (tag == null || tag.isEmpty() || !tag.contains("id")) {
				return ItemStack.EMPTY;
			}
			return ItemStack.parse(provider, tag).orElse(ItemStack.EMPTY);
		} catch (Exception e) {
			RatsMod.LOGGER.error("Не удалось разобрать ItemStack из NBT: {}", e.getMessage(), e);
			return ItemStack.EMPTY;
		}
	}

	/**
	 * Сериализует предмет в NBT; пустой стек не записывается (избегаем Cannot encode empty ItemStack).
	 */
	public static CompoundTag save(HolderLookup.Provider provider, ItemStack stack) {
		try {
			if (stack.isEmpty()) {
				return new CompoundTag();
			}
			CompoundTag tag = new CompoundTag();
			Tag encoded = stack.save(provider, tag);
			if (!(encoded instanceof CompoundTag compoundTag)) {
				throw new IllegalStateException("ItemStack.save вернул не CompoundTag: " + encoded.getClass().getSimpleName());
			}
			if (!compoundTag.contains("id")) {
				throw new IllegalStateException("ItemStack.save не записал id предмета: " + stack);
			}
			return compoundTag;
		} catch (Exception e) {
			RatsMod.LOGGER.error("Не удалось сохранить ItemStack в NBT: {}", e.getMessage(), e);
			throw new IllegalStateException("Ошибка сохранения ItemStack в NBT: " + e.getMessage(), e);
		}
	}

	/**
	 * Реестры для загрузки предметов из NBT на сервере/клиенте; на клиенте без мира — EMPTY.
	 */
	public static HolderLookup.Provider resolveProvider(HolderLookup.Provider preferred) {
		if (preferred != null && preferred != RegistryAccess.EMPTY) {
			return preferred;
		}
		return RegistryAccess.EMPTY;
	}
}
