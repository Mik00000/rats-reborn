package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.compat.create.CreateCompat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class RatInventoryAccess {
	private static final Direction[] DIRECTIONS = Direction.values();
	private static final Direction[][] ORDERED_SIDES = createOrderedSides();

	// Запрещает создание сервиса; ничего не принимает и всегда сообщает об ошибке.
	private RatInventoryAccess() {
		throw new IllegalStateException("Сервис доступа к инвентарям не создаётся как объект");
	}

	// Проверяет вставку предметов; принимает мир, позицию, сторону, стек, крысу и действие, отдаёт true если контейнер примет часть стека.
	public static boolean canInsert(Level level, BlockPos pos, Direction preferredSide, ItemStack stack, TamedRat rat, String action) {
		try {
			if (stack.isEmpty()) {
				RatsMod.LOGGER.debug("Крыса {} не проверяет вставку в {}: стек пустой, действие {}", rat.getUUID(), pos, action);
				return false;
			}
			for (Direction side : orderedSides(preferredSide)) {
				IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
				if (handler == null) {
					RatsMod.LOGGER.debug("Крыса {}: у блока {} нет инвентаря со стороны {} для {}", rat.getUUID(), pos, side, action);
					continue;
				}
				ItemStack simulated = ItemHandlerHelper.insertItem(handler, stack.copy(), true);
				if (simulated.getCount() < stack.getCount()) {
					RatsMod.LOGGER.debug("Крыса {}: блок {} примет предметы со стороны {} для {}", rat.getUUID(), pos, side, action);
					return true;
				}
				RatsMod.LOGGER.debug("Крыса {}: блок {} отказал во вставке {} со стороны {} для {}", rat.getUUID(), pos, stack, side, action);
			}
			if (canUseCreateProxy(level, pos, rat, action)) {
				RatsMod.LOGGER.debug("Крыса {}: блок Create {} будет использован как ручная точка вставки для {}", rat.getUUID(), pos, action);
				return true;
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить вставку предметов в блок {} для {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Вставляет предметы в контейнер; принимает мир, позицию, сторону, стек, крысу и действие, отдаёт остаток стека.
	public static ItemStack insert(Level level, BlockPos pos, Direction preferredSide, ItemStack stack, TamedRat rat, String action) {
		try {
			if (stack.isEmpty()) {
				return ItemStack.EMPTY;
			}
			ItemStack remaining = stack.copy();
			for (Direction side : orderedSides(preferredSide)) {
				IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
				if (handler == null) {
					RatsMod.LOGGER.debug("Крыса {}: нельзя вставить в {} со стороны {} для {}: нет обработчика", rat.getUUID(), pos, side, action);
					continue;
				}
				ItemStack before = remaining.copy();
				remaining = ItemHandlerHelper.insertItem(handler, remaining, false);
				int moved = before.getCount() - remaining.getCount();
				if (moved > 0) {
					RatsMod.LOGGER.debug("Крыса {} положила {} предметов в {} через сторону {} для {}", rat.getUUID(), moved, pos, side, action);
				}
				if (remaining.isEmpty()) {
					return ItemStack.EMPTY;
				}
			}
			ItemStack createRemaining = insertWithCreateProxy(level, pos, remaining, rat, action);
			if (createRemaining.getCount() < remaining.getCount()) {
				return createRemaining;
			}
			RatsMod.LOGGER.debug("Крыса {} не смогла полностью положить предметы в {} для {}, остаток {}", rat.getUUID(), pos, action, remaining);
			return remaining;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла вставить предметы в блок {} для {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Проверяет извлечение подходящих предметов; принимает мир, позицию, сторону, крысу и действие, отдаёт true если есть подходящий слот.
	public static boolean canExtract(Level level, BlockPos pos, Direction preferredSide, TamedRat rat, String action) {
		try {
			for (Direction side : orderedSides(preferredSide)) {
				IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
				if (handler == null) {
					RatsMod.LOGGER.debug("Крыса {}: у блока {} нет инвентаря со стороны {} для {}", rat.getUUID(), pos, side, action);
					continue;
				}
				int slot = RatUtils.getItemSlotFromItemHandler(rat, handler, level.getRandom());
				if (slot >= 0) {
					RatsMod.LOGGER.debug("Крыса {}: блок {} отдаст предметы со стороны {} из слота {} для {}", rat.getUUID(), pos, side, slot, action);
					return true;
				}
				RatsMod.LOGGER.debug("Крыса {}: в блоке {} нет подходящих предметов со стороны {} для {}", rat.getUUID(), pos, side, action);
			}
			if (canUseCreateProxy(level, pos, rat, action)) {
				RatsMod.LOGGER.debug("Крыса {}: блок Create {} будет использован как ручная точка забора для {}", rat.getUUID(), pos, action);
				return true;
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить извлечение предметов из блока {} для {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Извлекает подходящие предметы; принимает мир, позицию, сторону, крысу, размер стека и действие, отдаёт извлечённый стек.
	public static ItemStack extract(Level level, BlockPos pos, Direction preferredSide, TamedRat rat, int amount, String action) {
		try {
			if (amount <= 0) {
				throw new IllegalArgumentException("Количество извлечения должно быть положительным: " + amount);
			}
			for (Direction side : orderedSides(preferredSide)) {
				IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
				if (handler == null) {
					RatsMod.LOGGER.debug("Крыса {}: нельзя забрать из {} со стороны {} для {}: нет обработчика", rat.getUUID(), pos, side, action);
					continue;
				}
				int slot = RatUtils.getItemSlotFromItemHandler(rat, handler, level.getRandom());
				if (slot < 0) {
					RatsMod.LOGGER.debug("Крыса {}: нет подходящего слота в {} со стороны {} для {}", rat.getUUID(), pos, side, action);
					continue;
				}
				ItemStack simulated = handler.extractItem(slot, amount, true);
				if (simulated.isEmpty()) {
					RatsMod.LOGGER.debug("Крыса {}: слот {} в {} со стороны {} отказал в извлечении для {}", rat.getUUID(), slot, pos, side, action);
					continue;
				}
				ItemStack extracted = handler.extractItem(slot, amount, false);
				if (!extracted.isEmpty()) {
					RatsMod.LOGGER.debug("Крыса {} забрала {} предметов из {} через сторону {} для {}", rat.getUUID(), extracted.getCount(), pos, side, action);
					return extracted;
				}
			}
			ItemStack createExtracted = extractWithCreateProxy(level, pos, rat, amount, action);
			if (!createExtracted.isEmpty()) {
				return createExtracted;
			}
			return ItemStack.EMPTY;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла извлечь предметы из блока {} для {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Формирует порядок сторон; принимает предпочитаемую сторону, отдаёт массив с ней первой и остальными после неё.
	private static Direction[] orderedSides(Direction preferredSide) {
		try {
			return ORDERED_SIDES[preferredSide.ordinal()];
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось собрать порядок сторон инвентаря, предпочитаемая сторона {}", preferredSide, exception);
			throw exception;
		}
	}

	// Создаёт кеш порядка сторон для каждой предпочтительной стороны; ничего не принимает, отдаёт таблицу направлений.
	private static Direction[][] createOrderedSides() {
		try {
			Direction[][] ordered = new Direction[DIRECTIONS.length][DIRECTIONS.length];
			for (Direction preferredSide : DIRECTIONS) {
				Direction[] sides = ordered[preferredSide.ordinal()];
				sides[0] = preferredSide;
				int index = 1;
				for (Direction direction : DIRECTIONS) {
					if (direction != preferredSide) {
						sides[index++] = direction;
					}
				}
			}
			return ordered;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось создать кеш порядка сторон инвентаря", exception);
			throw exception;
		}
	}

	// Проверяет ручной Create-доступ; принимает мир, позицию, крысу и действие, отдаёт true если работяга может использовать блок.
	private static boolean canUseCreateProxy(Level level, BlockPos pos, TamedRat rat, String action) {
		try {
			boolean canUse = rat.hasWorkerUpgrade()
					&& CreateCompat.canWorkerUse(level.getBlockState(pos));
			if (!canUse) {
				RatsMod.LOGGER.debug("Крыса {} не использует Create-прокси для {}: нет улучшения работяги или блок не поддержан", rat.getUUID(), action);
			}
			return canUse;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить Create-прокси блока {} для {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Вставляет предмет через ручное действие Create; принимает мир, позицию, стек, крысу и действие, отдаёт остаток.
	private static ItemStack insertWithCreateProxy(Level level, BlockPos pos, ItemStack stack, TamedRat rat, String action) {
		try {
			if (!canUseCreateProxy(level, pos, rat, action)) {
				return stack;
			}
			rat.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack.copy());
			boolean worked = CreateCompat.workBlock(rat, pos);
			ItemStack remaining = rat.getMainHandItem().copy();
			if (worked && remaining.getCount() < stack.getCount()) {
				RatsMod.LOGGER.debug("Крыса {} положила предметы в Create-блок {} через ручное действие для {}", rat.getUUID(), pos, action);
				return remaining;
			}
			return stack;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла вставить предметы через Create-прокси {} для {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Забирает предмет через ручное действие Create; принимает мир, позицию, крысу, количество и действие, отдаёт полученный стек.
	private static ItemStack extractWithCreateProxy(Level level, BlockPos pos, TamedRat rat, int amount, String action) {
		try {
			if (!canUseCreateProxy(level, pos, rat, action)) {
				return ItemStack.EMPTY;
			}
			rat.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			boolean worked = CreateCompat.workBlock(rat, pos);
			ItemStack extracted = rat.getMainHandItem().copy();
			if (worked && !extracted.isEmpty()) {
				if (extracted.getCount() > amount) {
					ItemStack limited = extracted.copyWithCount(amount);
					ItemStack remainder = extracted.copyWithCount(extracted.getCount() - amount);
					rat.spawnAtLocation(remainder, 0.0F);
					return limited;
				}
				RatsMod.LOGGER.debug("Крыса {} забрала предметы из Create-блока {} через ручное действие для {}", rat.getUUID(), pos, action);
				return extracted;
			}
			return ItemStack.EMPTY;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла забрать предметы через Create-прокси {} для {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}
}