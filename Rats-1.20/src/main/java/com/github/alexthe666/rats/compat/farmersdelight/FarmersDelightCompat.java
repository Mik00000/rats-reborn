package com.github.alexthe666.rats.compat.farmersdelight;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatInventoryAccess;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import vectorwing.farmersdelight.common.block.AbstractStoveBlock;
import vectorwing.farmersdelight.common.block.SkilletBlock;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;
import vectorwing.farmersdelight.common.block.entity.StoveBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipeInput;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class FarmersDelightCompat {
	private static final int COOKING_POT_INPUT_SLOTS = 6;

	// Запрещает создание сервиса совместимости; параметров нет, ничего не отдаёт.
	private FarmersDelightCompat() {
		throw new IllegalStateException("Сервис совместимости Farmer's Delight не создаётся как объект");
	}

	// Проверяет, является ли блок рабочим блоком Farmer's Delight; принимает сущность блока, отдаёт true для плиты, котла или доски.
	public static boolean isSupportedWorkBlock(BlockEntity blockEntity) {
		try {
			return blockEntity instanceof StoveBlockEntity
					|| blockEntity instanceof CookingPotBlockEntity
					|| blockEntity instanceof CuttingBoardBlockEntity
					|| blockEntity instanceof SkilletBlockEntity;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить рабочий блок Farmer's Delight {}", blockEntity, exception);
			throw exception;
		}
	}

	// Выполняет один цикл работы шефа с Farmer's Delight; принимает крысу и три точки, отдаёт true если действие выполнено.
	public static boolean tryRunChefCycle(TamedRat rat, BlockPos workPos, BlockPos pickupPos, BlockPos depositPos) {
		try {
			BlockEntity blockEntity = rat.level().getBlockEntity(workPos);
			if (blockEntity instanceof StoveBlockEntity stove) {
				return tryRunStoveCycle(rat, stove, pickupPos, depositPos);
			}
			if (blockEntity instanceof SkilletBlockEntity skillet) {
				return tryRunSkilletCycle(rat, skillet, pickupPos, depositPos);
			}
			if (blockEntity instanceof CookingPotBlockEntity pot) {
				return tryRunCookingPotCycle(rat, pot, pickupPos, depositPos);
			}
			if (blockEntity instanceof CuttingBoardBlockEntity board) {
				return tryRunCuttingBoardCycle(rat, board, pickupPos, depositPos);
			}
			RatsMod.LOGGER.debug("Крыса {} не работает с Farmer's Delight: блок {} не поддержан", rat.getUUID(), workPos);
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла выполнить цикл Farmer's Delight у блока {}", rat.getUUID(), workPos, exception);
			throw exception;
		}
	}

	// Обрабатывает кухонную плиту; принимает крысу, плиту и точки сундуков, отдаёт true если снят результат или поставлен новый предмет.
	private static boolean tryRunStoveCycle(TamedRat rat, StoveBlockEntity stove, BlockPos pickupPos, BlockPos depositPos) {
		try {
			FdFilter filter = FdFilter.fromRat(rat);
			if (tryCollectFinishedStoveItem(rat, stove, depositPos, filter)) {
				return true;
			}
			BlockState state = rat.level().getBlockState(stove.getBlockPos());
			if (!state.hasProperty(AbstractStoveBlock.LIT) || !state.getValue(AbstractStoveBlock.LIT)) {
				RatsMod.LOGGER.debug("Крыса {} не кладёт предмет на плиту FD {}: плита не нагрета", rat.getUUID(), stove.getBlockPos());
				return false;
			}
			if (stove.isFull()) {
				RatsMod.LOGGER.debug("Крыса {} не кладёт предмет на плиту FD {}: все места заняты", rat.getUUID(), stove.getBlockPos());
				return false;
			}
			Optional<InventoryView> pickup = resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "забор для плиты FD");
			if (pickup.isEmpty()) {
				RatsMod.LOGGER.debug("Крыса {} не нашла сундук забора для плиты FD {}", rat.getUUID(), pickupPos);
				return false;
			}
			for (int slot = 0; slot < pickup.get().handler().getSlots(); slot++) {
				ItemStack candidate = pickup.get().handler().getStackInSlot(slot);
				if (candidate.isEmpty() || filter.isBlacklisted(candidate)) {
					continue;
				}
				Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> recipe = stove.getCookingRecipe(candidate);
				if (recipe.isEmpty()) {
					continue;
				}
				ItemStack output = recipe.get().value().getResultItem(rat.level().registryAccess()).copy();
				if (output.isEmpty() || !filter.allowsProcessing(candidate, output)) {
					continue;
				}
				if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, output, rat, "проверка склада для результата плиты FD")) {
					RatsMod.LOGGER.debug("Крыса {} не кладёт {} на плиту FD {}: склад {} не примет {}", rat.getUUID(), candidate, stove.getBlockPos(), depositPos, output);
					return false;
				}
				ItemStack extracted = extractExact(pickup.get(), slot, 1, stack -> ItemStack.isSameItemSameComponents(stack, candidate), rat, "забор сырья для плиты FD");
				if (extracted.isEmpty()) {
					return false;
				}
				ItemStack oneItem = extracted.copyWithCount(1);
				boolean placed = stove.placeFood(rat, oneItem, recipe.get());
				if (!placed) {
					returnToPickupOrThrow(rat, pickupPos, extracted, "возврат сырья после отказа плиты FD");
					RatsMod.LOGGER.debug("Крыса {} не смогла положить {} на плиту FD {}", rat.getUUID(), extracted, stove.getBlockPos());
					return false;
				}
				markChanged(stove);
				RatsMod.LOGGER.debug("Крыса {} положила {} на плиту FD {}, ожидаемый результат {}", rat.getUUID(), extracted, stove.getBlockPos(), output);
				return true;
			}
			RatsMod.LOGGER.debug("Крыса {} не нашла подходящее сырьё для плиты FD в {}", rat.getUUID(), pickupPos);
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла обработать плиту FD {}", rat.getUUID(), stove.getBlockPos(), exception);
			throw exception;
		}
	}

	// Забирает готовый предмет с плиты; принимает крысу, плиту, склад и фильтр, отдаёт true если предмет перенесён.
	private static boolean tryCollectFinishedStoveItem(TamedRat rat, StoveBlockEntity stove, BlockPos depositPos, FdFilter filter) {
		try {
			ItemStackHandler items = stove.getItems();
			for (int slot = 0; slot < items.getSlots(); slot++) {
				ItemStack stack = items.getStackInSlot(slot);
				if (stack.isEmpty() || stove.getCookingRecipe(stack).isPresent()) {
					continue;
				}
				if (filter.isBlacklisted(stack)) {
					RatsMod.LOGGER.debug("Крыса {} не снимает {} с плиты FD {}: результат в чёрном списке", rat.getUUID(), stack, stove.getBlockPos());
					continue;
				}
				if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, stack.copyWithCount(1), rat, "проверка склада для готовой плиты FD")) {
					RatsMod.LOGGER.debug("Крыса {} не снимает {} с плиты FD {}: склад {} полон", rat.getUUID(), stack, stove.getBlockPos(), depositPos);
					return false;
				}
				ItemStack extracted = items.extractItem(slot, 1, false);
				ItemStack remaining = RatInventoryAccess.insert(rat.level(), depositPos, rat.depositFacing, extracted.copy(), rat, "сдача результата плиты FD");
				if (!remaining.isEmpty()) {
					ItemStack rollback = ItemHandlerHelper.insertItem(items, extracted, false);
					if (!rollback.isEmpty()) {
						throw new IllegalStateException("Склад отказал после симуляции, а вернуть результат плиты не удалось: " + rollback);
					}
					return false;
				}
				markChanged(stove);
				RatsMod.LOGGER.debug("Крыса {} сняла {} с плиты FD {} и отнесла в {}", rat.getUUID(), extracted, stove.getBlockPos(), depositPos);
				return true;
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла снять результат с плиты FD {}", rat.getUUID(), stove.getBlockPos(), exception);
			throw exception;
		}
	}

	// Обрабатывает кухонный котёл; принимает крысу, котёл и точки сундуков, отдаёт true если подана порция или заложен рецепт.

	// Обрабатывает сковороду FD; принимает крысу, сковороду и сундуки, отдаёт true если предмет заложен или готовый результат сдан.
	private static boolean tryRunSkilletCycle(TamedRat rat, SkilletBlockEntity skillet, BlockPos pickupPos, BlockPos depositPos) {
		try {
			FdFilter filter = FdFilter.fromRat(rat);
			if (tryCollectDroppedSkilletResult(rat, skillet, depositPos, filter)) {
				return true;
			}
			BlockState state = rat.level().getBlockState(skillet.getBlockPos());
			if (state.hasProperty(SkilletBlock.WATERLOGGED) && state.getValue(SkilletBlock.WATERLOGGED)) {
				RatsMod.LOGGER.debug("Крыса {} не кладёт предмет на сковороду FD {}: сковорода залита водой", rat.getUUID(), skillet.getBlockPos());
				return false;
			}
			if (!skillet.isHeated()) {
				RatsMod.LOGGER.debug("Крыса {} не кладёт предмет на сковороду FD {}: сковорода не нагрета", rat.getUUID(), skillet.getBlockPos());
				return false;
			}
			if (skillet.hasStoredStack()) {
				RatsMod.LOGGER.debug("Крыса {} ждёт готовку на сковороде FD {}: внутри уже есть {}", rat.getUUID(), skillet.getBlockPos(), skillet.getStoredStack());
				return false;
			}
			Optional<InventoryView> pickup = resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "забор для сковороды FD");
			if (pickup.isEmpty()) {
				return false;
			}
			for (int slot = 0; slot < pickup.get().handler().getSlots(); slot++) {
				ItemStack candidate = pickup.get().handler().getStackInSlot(slot);
				if (candidate.isEmpty() || filter.isBlacklisted(candidate)) {
					continue;
				}
				ItemStack output = getCampfireCookingResult(rat.level(), candidate);
				if (output.isEmpty() || !filter.allowsProcessing(candidate, output)) {
					continue;
				}
				if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, output, rat, "проверка склада для результата сковороды FD")) {
					RatsMod.LOGGER.debug("Крыса {} не кладёт {} на сковороду FD {}: склад {} не примет {}", rat.getUUID(), candidate, skillet.getBlockPos(), depositPos, output);
					return false;
				}
				ItemStack extracted = extractExact(pickup.get(), slot, 1, stack -> ItemStack.isSameItemSameComponents(stack, candidate), rat, "забор сырья для сковороды FD");
				if (extracted.isEmpty()) {
					return false;
				}
				ItemStack oneItem = extracted.copyWithCount(1);
				ItemStack remaining = skillet.addItemToCook(oneItem, null);
				if (!remaining.isEmpty()) {
					returnToPickupOrThrow(rat, pickupPos, extracted, "возврат сырья после отказа сковороды FD");
					RatsMod.LOGGER.debug("Крыса {} не смогла положить {} на сковороду FD {}", rat.getUUID(), extracted, skillet.getBlockPos());
					return false;
				}
				markChanged(skillet);
				RatsMod.LOGGER.debug("Крыса {} положила {} на сковороду FD {}, ожидаемый результат {}", rat.getUUID(), extracted, skillet.getBlockPos(), output);
				return true;
			}
			RatsMod.LOGGER.debug("Крыса {} не нашла подходящее сырьё для сковороды FD в {}", rat.getUUID(), pickupPos);
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла обработать сковороду FD {}", rat.getUUID(), skillet.getBlockPos(), exception);
			throw exception;
		}
	}

	// Забирает готовый предмет, который сковорода FD выбросила в мир; принимает крысу, сковороду, склад и фильтр, отдаёт true если результат сдан.
	private static boolean tryCollectDroppedSkilletResult(TamedRat rat, SkilletBlockEntity skillet, BlockPos depositPos, FdFilter filter) {
		try {
			AABB searchBox = new AABB(skillet.getBlockPos()).inflate(1.0D, 0.75D, 1.0D);
			List<ItemEntity> items = rat.level().getEntitiesOfClass(ItemEntity.class, searchBox, entity -> entity.isAlive() && !entity.getItem().isEmpty());
			for (ItemEntity entity : items) {
				ItemStack stack = entity.getItem();
				if (!isCampfireCookingResult(rat.level(), stack) || !filter.allowsCollectedResult(stack)) {
					continue;
				}
				ItemStack single = stack.copyWithCount(1);
				if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, single, rat, "проверка склада для выпавшей сковороды FD")) {
					RatsMod.LOGGER.debug("Крыса {} не забирает {} у сковороды FD {}: склад {} не примет предмет", rat.getUUID(), single, skillet.getBlockPos(), depositPos);
					return false;
				}
				ItemStack remaining = RatInventoryAccess.insert(rat.level(), depositPos, rat.depositFacing, single.copy(), rat, "сдача результата сковороды FD");
				if (!remaining.isEmpty()) {
					throw new IllegalStateException("Склад отказал после симуляции результата сковороды FD, остаток: " + remaining);
				}
				stack.shrink(1);
				if (stack.isEmpty()) {
					entity.discard();
				} else {
					entity.setItem(stack);
				}
				RatsMod.LOGGER.debug("Крыса {} забрала {} у сковороды FD {} и отнесла в {}", rat.getUUID(), single, skillet.getBlockPos(), depositPos);
				return true;
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла забрать результат сковороды FD {}", rat.getUUID(), skillet.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает результат рецепта костра для сковороды; принимает мир и сырьё, отдаёт готовый стек или пустой стек.
	private static ItemStack getCampfireCookingResult(Level level, ItemStack input) {
		try {
			Optional<RecipeHolder<CampfireCookingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(input.copyWithCount(1)), level);
			if (recipe.isEmpty()) {
				return ItemStack.EMPTY;
			}
			return recipe.get().value().getResultItem(level.registryAccess()).copy();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти рецепт костра FD для {}", input, exception);
			throw exception;
		}
	}

	// Проверяет, является ли предмет результатом рецепта костра; принимает мир и стек, отдаёт true для готовых предметов сковороды.
	private static boolean isCampfireCookingResult(Level level, ItemStack stack) {
		try {
			List<RecipeHolder<CampfireCookingRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(RecipeType.CAMPFIRE_COOKING);
			for (RecipeHolder<CampfireCookingRecipe> recipe : recipes) {
				ItemStack output = recipe.value().getResultItem(level.registryAccess());
				if (ItemStack.isSameItemSameComponents(output, stack)) {
					return true;
				}
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить результат костра FD {}", stack, exception);
			throw exception;
		}
	}

	private static boolean tryRunCookingPotCycle(TamedRat rat, CookingPotBlockEntity pot, BlockPos pickupPos, BlockPos depositPos) {
		try {
			FdFilter filter = FdFilter.fromRat(rat);
			if (tryServeCookingPotMeal(rat, pot, pickupPos, depositPos, filter)) {
				return true;
			}
			if (!pot.isHeated()) {
				RatsMod.LOGGER.debug("Крыса {} не загружает котёл FD {}: котёл не нагрет", rat.getUUID(), pot.getBlockPos());
				return false;
			}
			if (!isCookingPotInputClear(pot)) {
				RatsMod.LOGGER.debug("Крыса {} ждёт котёл FD {}: внутри уже есть ингредиенты или блюдо", rat.getUUID(), pot.getBlockPos());
				return false;
			}
			if (!filter.hasWhitelist()) {
				RatsMod.LOGGER.debug("Крыса {} не готовит в котле FD {}: для котла нужна цель-рецепт в белом списке", rat.getUUID(), pot.getBlockPos());
				return false;
			}
			Optional<InventoryView> pickup = resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "забор для котла FD");
			if (pickup.isEmpty()) {
				return false;
			}
			Optional<CookingPotPlan> plan = findCookingPotPlan(rat, pot, pickup.get(), depositPos, filter);
			if (plan.isEmpty()) {
				RatsMod.LOGGER.debug("Крыса {} не нашла полный набор для целевого рецепта котла FD у {}", rat.getUUID(), pickupPos);
				return false;
			}
			List<ItemStack> extracted = extractPlans(pickup.get(), plan.get().ingredients(), rat, "забор ингредиентов для котла FD");
			if (extracted.size() != plan.get().ingredients().size()) {
				throw new IllegalStateException("Не удалось извлечь полный набор ингредиентов котла FD для " + plan.get().recipeId());
			}
			try {
				insertCookingPotIngredients(pot, extracted, rat);
			} catch (RuntimeException exception) {
				for (ItemStack stack : extracted) {
					returnToPickupOrThrow(rat, pickupPos, stack, "возврат ингредиентов котла FD");
				}
				throw exception;
			}
			markChanged(pot);
			RatsMod.LOGGER.debug("Крыса {} заложила рецепт котла FD {} в {}", rat.getUUID(), plan.get().recipeId(), pot.getBlockPos());
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла обработать котёл FD {}", rat.getUUID(), pot.getBlockPos(), exception);
			throw exception;
		}
	}

	// Подаёт готовую порцию из котла; принимает крысу, котёл, сундуки и фильтр, отдаёт true если порция снята.
	private static boolean tryServeCookingPotMeal(TamedRat rat, CookingPotBlockEntity pot, BlockPos pickupPos, BlockPos depositPos, FdFilter filter) {
		try {
			ItemStack meal = pot.getMeal();
			if (meal.isEmpty()) {
				return false;
			}
			if (filter.isBlacklisted(meal)) {
				RatsMod.LOGGER.debug("Крыса {} не снимает блюдо {} из котла FD {}: результат в чёрном списке", rat.getUUID(), meal, pot.getBlockPos());
				return false;
			}
			ItemStack container = pot.getContainer().copyWithCount(1);
			if (container.isEmpty()) {
				RatsMod.LOGGER.debug("Крыса {} не снимает блюдо {} из котла FD {}: у блюда нет контейнера для подачи", rat.getUUID(), meal, pot.getBlockPos());
				return false;
			}
			if (filter.isBlacklisted(container)) {
				RatsMod.LOGGER.debug("Крыса {} не снимает блюдо из котла FD {}: контейнер {} в чёрном списке", rat.getUUID(), pot.getBlockPos(), container);
				return false;
			}
			if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, meal.copyWithCount(1), rat, "проверка склада для блюда котла FD")) {
				RatsMod.LOGGER.debug("Крыса {} не снимает блюдо {} из котла FD {}: склад {} не примет результат", rat.getUUID(), meal, pot.getBlockPos(), depositPos);
				return false;
			}
			Optional<InventoryView> pickup = resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "забор контейнера для котла FD");
			if (pickup.isEmpty()) {
				return false;
			}
			Optional<InventoryPlan> containerPlan = reserveSingle(pickup.get(), stack -> ItemStack.isSameItemSameComponents(stack, container), filter::allowsContainer);
			if (containerPlan.isEmpty()) {
				RatsMod.LOGGER.debug("Крыса {} не снимает блюдо {} из котла FD {}: нет контейнера {}", rat.getUUID(), meal, pot.getBlockPos(), container);
				return false;
			}
			ItemStack extractedContainer = extractPlan(pickup.get(), containerPlan.get(), rat, "забор контейнера для котла FD");
			ItemStack served = pot.useHeldItemOnMeal(extractedContainer);
			if (served.isEmpty()) {
				returnToPickupOrThrow(rat, pickupPos, container, "возврат контейнера после отказа котла FD");
				RatsMod.LOGGER.debug("Крыса {} не смогла снять порцию из котла FD {}", rat.getUUID(), pot.getBlockPos());
				return false;
			}
			ItemStack remaining = RatInventoryAccess.insert(rat.level(), depositPos, rat.depositFacing, served.copy(), rat, "сдача блюда котла FD");
			if (!remaining.isEmpty()) {
				throw new IllegalStateException("Склад отказал после симуляции подачи котла FD, остаток: " + remaining);
			}
			markChanged(pot);
			RatsMod.LOGGER.debug("Крыса {} сняла {} из котла FD {} и отнесла в {}", rat.getUUID(), served, pot.getBlockPos(), depositPos);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла снять блюдо из котла FD {}", rat.getUUID(), pot.getBlockPos(), exception);
			throw exception;
		}
	}

	// Ищет транзакционный план котла; принимает крысу, котёл, сундук, склад и фильтр, отдаёт найденный план или пусто.
	private static Optional<CookingPotPlan> findCookingPotPlan(TamedRat rat, CookingPotBlockEntity pot, InventoryView pickup, BlockPos depositPos, FdFilter filter) {
		try {
			List<RecipeHolder<CookingPotRecipe>> recipes = rat.level().getRecipeManager().getAllRecipesFor(ModRecipeTypes.COOKING.get());
			for (RecipeHolder<CookingPotRecipe> holder : recipes) {
				CookingPotRecipe recipe = holder.value();
				ItemStack result = recipe.getResultItem(rat.level().registryAccess()).copy();
				if (result.isEmpty() || !filter.isWhitelisted(result) || filter.isBlacklisted(result)) {
					continue;
				}
				ItemStack container = recipe.getOutputContainer().copyWithCount(1);
				if (!container.isEmpty() && !filter.allowsContainer(container)) {
					RatsMod.LOGGER.debug("Крыса {} пропускает рецепт котла FD {}: контейнер {} запрещён", rat.getUUID(), holder.id(), container);
					continue;
				}
				List<InventoryPlan> ingredients = reserveIngredients(pickup, recipe.getIngredients(), stack -> filter.allowsIngredientForTargetRecipe(stack));
				if (ingredients.size() != recipe.getIngredients().size()) {
					RatsMod.LOGGER.debug("Крыса {} не собрала полный набор ингредиентов для рецепта котла FD {}", rat.getUUID(), holder.id());
					continue;
				}
				if (!container.isEmpty() && reserveSingle(pickup, stack -> ItemStack.isSameItemSameComponents(stack, container), filter::allowsContainer).isEmpty()) {
					RatsMod.LOGGER.debug("Крыса {} не нашла контейнер {} для рецепта котла FD {}", rat.getUUID(), container, holder.id());
					continue;
				}
				if (!matchesCookingPotRecipe(recipe, ingredients, rat.level())) {
					RatsMod.LOGGER.debug("Крыса {} собрала ингредиенты, но рецепт котла FD {} не совпал при проверке", rat.getUUID(), holder.id());
					continue;
				}
				if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, result, rat, "проверка склада для рецепта котла FD")) {
					RatsMod.LOGGER.debug("Крыса {} не готовит рецепт котла FD {}: склад {} не примет {}", rat.getUUID(), holder.id(), depositPos, result);
					continue;
				}
				return Optional.of(new CookingPotPlan(holder.id().toString(), ingredients));
			}
			return Optional.empty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла подобрать рецепт котла FD {}", rat.getUUID(), pot.getBlockPos(), exception);
			throw exception;
		}
	}

	// Обрабатывает разделочную доску; принимает крысу, доску и точки сундуков, отдаёт true если предмет нарезан.
	private static boolean tryRunCuttingBoardCycle(TamedRat rat, CuttingBoardBlockEntity board, BlockPos pickupPos, BlockPos depositPos) {
		try {
			if (!board.isEmpty()) {
				RatsMod.LOGGER.debug("Крыса {} не работает с доской FD {}: доска занята предметом {}", rat.getUUID(), board.getBlockPos(), board.getStoredItem());
				return false;
			}
			FdFilter filter = FdFilter.fromRat(rat);
			Optional<InventoryView> pickup = resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "забор для доски FD");
			if (pickup.isEmpty()) {
				return false;
			}
			Optional<CuttingBoardPlan> plan = findCuttingBoardPlan(rat, pickup.get(), depositPos, filter);
			if (plan.isEmpty()) {
				RatsMod.LOGGER.debug("Крыса {} не нашла полный набор для доски FD в {}", rat.getUUID(), pickupPos);
				return false;
			}
			ItemStack ingredient = extractPlan(pickup.get(), plan.get().ingredient(), rat, "забор ингредиента для доски FD");
			ItemStack tool;
			try {
				tool = extractPlan(pickup.get(), plan.get().tool(), rat, "забор ножа для доски FD");
			} catch (RuntimeException exception) {
				returnToPickupOrThrow(rat, pickupPos, ingredient, "возврат ингредиента после отказа ножа FD");
				throw exception;
			}
			boolean ingredientConsumed = false;
			try {
				ItemStack leftover = board.addItem(ingredient.copy());
				if (!leftover.isEmpty()) {
					throw new IllegalStateException("Доска FD отказала после симуляции, остаток: " + leftover);
				}
				ItemStack stored = board.removeItem();
				if (stored.isEmpty()) {
					throw new IllegalStateException("Доска FD не вернула предмет для нарезки");
				}
				ingredientConsumed = true;
				damageToolOrBreak(rat, tool);
				for (ItemStack output : plan.get().outputs()) {
					ItemStack remaining = RatInventoryAccess.insert(rat.level(), depositPos, rat.depositFacing, output.copy(), rat, "сдача результата доски FD");
					if (!remaining.isEmpty()) {
						throw new IllegalStateException("Склад отказал после симуляции доски FD, остаток: " + remaining);
					}
				}
				if (!tool.isEmpty()) {
					returnToPickupOrThrow(rat, pickupPos, tool, "возврат ножа после доски FD");
				}
				markChanged(board);
				RatsMod.LOGGER.debug("Крыса {} нарезала {} на доске FD {} по рецепту {}", rat.getUUID(), stored, board.getBlockPos(), plan.get().recipeId());
				return true;
			} catch (RuntimeException exception) {
				if (!ingredientConsumed) {
					returnToPickupOrThrow(rat, pickupPos, ingredient, "возврат ингредиента после ошибки доски FD");
				}
				if (!tool.isEmpty()) {
					returnToPickupOrThrow(rat, pickupPos, tool, "возврат ножа после ошибки доски FD");
				}
				throw exception;
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла обработать доску FD {}", rat.getUUID(), board.getBlockPos(), exception);
			throw exception;
		}
	}

	// Ищет транзакционный план доски; принимает крысу, сундук, склад и фильтр, отдаёт план или пусто.
	private static Optional<CuttingBoardPlan> findCuttingBoardPlan(TamedRat rat, InventoryView pickup, BlockPos depositPos, FdFilter filter) {
		try {
			List<RecipeHolder<CuttingBoardRecipe>> recipes = rat.level().getRecipeManager().getAllRecipesFor(ModRecipeTypes.CUTTING.get());
			for (RecipeHolder<CuttingBoardRecipe> holder : recipes) {
				CuttingBoardRecipe recipe = holder.value();
				List<ItemStack> outputs = getCuttingBoardOutputs(recipe);
				if (outputs.isEmpty() || outputs.stream().anyMatch(filter::isBlacklisted)) {
					continue;
				}
				boolean outputTargeted = outputs.stream().anyMatch(filter::isWhitelisted);
				Optional<InventoryPlan> ingredient = reserveSingle(pickup, stack -> recipe.getIngredients().get(0).test(stack), stack -> filter.allowsIngredient(stack, outputTargeted));
				if (ingredient.isEmpty()) {
					continue;
				}
				Optional<InventoryPlan> tool = reserveSingle(pickup, stack -> recipe.getTool().test(stack), stack -> filter.allowsTool(stack) && !samePlanItem(ingredient.get(), stack));
				if (tool.isEmpty()) {
					RatsMod.LOGGER.debug("Крыса {} не нашла нож или инструмент для рецепта доски FD {}", rat.getUUID(), holder.id());
					continue;
				}
				if (!matchesCuttingBoardRecipe(recipe, ingredient.get().stack(), tool.get().stack(), rat.level())) {
					continue;
				}
				if (!canInsertAll(rat, depositPos, outputs, "проверка склада для результата доски FD")) {
					RatsMod.LOGGER.debug("Крыса {} не режет рецепт доски FD {}: склад {} не примет результат", rat.getUUID(), holder.id(), depositPos);
					continue;
				}
				return Optional.of(new CuttingBoardPlan(holder.id().toString(), ingredient.get(), tool.get(), outputs));
			}
			return Optional.empty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла подобрать рецепт доски FD", rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет, пусты ли входные слоты котла; принимает котёл, отдаёт true только если можно загрузить новый рецепт.
	private static boolean isCookingPotInputClear(CookingPotBlockEntity pot) {
		try {
			ItemStackHandler inventory = pot.getInventory();
			if (!pot.getMeal().isEmpty()) {
				return false;
			}
			for (int slot = 0; slot < COOKING_POT_INPUT_SLOTS; slot++) {
				if (!inventory.getStackInSlot(slot).isEmpty()) {
					return false;
				}
			}
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить занятость котла FD {}", pot.getBlockPos(), exception);
			throw exception;
		}
	}

	// Вставляет ингредиенты в котёл; принимает котёл, список стеков и крысу, ничего не отдаёт.
	private static void insertCookingPotIngredients(CookingPotBlockEntity pot, List<ItemStack> ingredients, TamedRat rat) {
		try {
			ItemStackHandler inventory = pot.getInventory();
			for (int index = 0; index < ingredients.size(); index++) {
				ItemStack remaining = inventory.insertItem(index, ingredients.get(index).copy(), false);
				if (!remaining.isEmpty()) {
					throw new IllegalStateException("Котёл FD отказал ингредиенту " + ingredients.get(index) + ", остаток: " + remaining);
				}
			}
			RatsMod.LOGGER.debug("Крыса {} загрузила {} ингредиентов в котёл FD {}", rat.getUUID(), ingredients.size(), pot.getBlockPos());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла загрузить ингредиенты в котёл FD {}", rat.getUUID(), pot.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет рецепт котла на выбранных ингредиентах; принимает рецепт, планы и мир, отдаёт true при совпадении.
	private static boolean matchesCookingPotRecipe(CookingPotRecipe recipe, List<InventoryPlan> ingredients, Level level) {
		try {
			ItemStackHandler handler = new ItemStackHandler(COOKING_POT_INPUT_SLOTS);
			for (int index = 0; index < ingredients.size(); index++) {
				handler.setStackInSlot(index, ingredients.get(index).stack().copy());
			}
			return recipe.matches(new RecipeWrapper(handler), level);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить рецепт котла FD", exception);
			throw exception;
		}
	}

	// Проверяет рецепт доски на выбранных предмете и инструменте; принимает рецепт, предмет, инструмент и мир, отдаёт true при совпадении.
	private static boolean matchesCuttingBoardRecipe(CuttingBoardRecipe recipe, ItemStack item, ItemStack tool, Level level) {
		try {
			return recipe.matches(new CuttingBoardRecipeInput(item.copy(), tool.copy()), level);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить рецепт доски FD", exception);
			throw exception;
		}
	}

	// Получает результаты доски; принимает рецепт, отдаёт список стеков без пустых значений.
	private static List<ItemStack> getCuttingBoardOutputs(CuttingBoardRecipe recipe) {
		try {
			List<ItemStack> outputs = new ArrayList<>();
			for (ItemStack stack : recipe.getResults()) {
				if (!stack.isEmpty()) {
					outputs.add(stack.copy());
				}
			}
			return outputs;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось прочитать результаты рецепта доски FD", exception);
			throw exception;
		}
	}

	// Резервирует ингредиенты по списку Ingredient; принимает сундук, ингредиенты и фильтр, отдаёт планы извлечения.
	private static List<InventoryPlan> reserveIngredients(InventoryView inventory, NonNullList<Ingredient> ingredients, Predicate<ItemStack> allowed) {
		try {
			List<VirtualSlot> virtualSlots = createVirtualSlots(inventory);
			List<InventoryPlan> plans = new ArrayList<>();
			for (Ingredient ingredient : ingredients) {
				Optional<InventoryPlan> plan = reserveFromVirtualSlots(inventory, virtualSlots, stack -> ingredient.test(stack), allowed);
				if (plan.isEmpty()) {
					return List.of();
				}
				plans.add(plan.get());
			}
			return plans;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось зарезервировать ингредиенты FD", exception);
			throw exception;
		}
	}

	// Резервирует один предмет из сундука; принимает сундук, условие предмета и фильтр, отдаёт план или пусто.
	private static Optional<InventoryPlan> reserveSingle(InventoryView inventory, Predicate<ItemStack> matcher, Predicate<ItemStack> allowed) {
		try {
			return reserveFromVirtualSlots(inventory, createVirtualSlots(inventory), matcher, allowed);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось зарезервировать предмет FD", exception);
			throw exception;
		}
	}

	// Резервирует предмет из виртуальных слотов; принимает сундук, слоты, условие и фильтр, отдаёт план или пусто.
	private static Optional<InventoryPlan> reserveFromVirtualSlots(InventoryView inventory, List<VirtualSlot> virtualSlots, Predicate<ItemStack> matcher, Predicate<ItemStack> allowed) {
		try {
			for (VirtualSlot virtualSlot : virtualSlots) {
				ItemStack available = virtualSlot.stack();
				if (available.isEmpty() || !matcher.test(available) || !allowed.test(available)) {
					continue;
				}
				ItemStack planned = available.copyWithCount(1);
				available.shrink(1);
				return Optional.of(new InventoryPlan(inventory.side(), virtualSlot.slot(), planned));
			}
			return Optional.empty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выбрать виртуальный слот FD", exception);
			throw exception;
		}
	}

	// Создаёт снимок инвентаря; принимает представление сундука, отдаёт виртуальные слоты.
	private static List<VirtualSlot> createVirtualSlots(InventoryView inventory) {
		try {
			List<VirtualSlot> slots = new ArrayList<>();
			for (int slot = 0; slot < inventory.handler().getSlots(); slot++) {
				ItemStack stack = inventory.handler().getStackInSlot(slot);
				if (!stack.isEmpty()) {
					slots.add(new VirtualSlot(slot, stack.copy()));
				}
			}
			return slots;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось создать снимок инвентаря FD со стороны {}", inventory.side(), exception);
			throw exception;
		}
	}

	// Извлекает набор планов; принимает сундук, планы, крысу и действие, отдаёт извлечённые стеки.
	private static List<ItemStack> extractPlans(InventoryView inventory, List<InventoryPlan> plans, TamedRat rat, String action) {
		try {
			List<ItemStack> extracted = new ArrayList<>();
			for (InventoryPlan plan : plans) {
				extracted.add(extractPlan(inventory, plan, rat, action));
			}
			return extracted;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла извлечь набор предметов FD для {}", rat.getUUID(), action, exception);
			throw exception;
		}
	}

	// Извлекает один план; принимает сундук, план, крысу и действие, отдаёт извлечённый стек.
	private static ItemStack extractPlan(InventoryView inventory, InventoryPlan plan, TamedRat rat, String action) {
		try {
			return extractExact(inventory, plan.slot(), plan.stack().getCount(), stack -> ItemStack.isSameItemSameComponents(stack, plan.stack()), rat, action);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла извлечь план {} для {}", rat.getUUID(), plan, action, exception);
			throw exception;
		}
	}

	// Извлекает точный предмет из слота; принимает сундук, слот, количество, проверку, крысу и действие, отдаёт стек.
	private static ItemStack extractExact(InventoryView inventory, int slot, int amount, Predicate<ItemStack> matcher, TamedRat rat, String action) {
		try {
			ItemStack current = inventory.handler().getStackInSlot(slot);
			if (current.isEmpty() || !matcher.test(current)) {
				throw new IllegalStateException("Слот " + slot + " изменился перед извлечением FD для " + action + ": " + current);
			}
			ItemStack simulated = inventory.handler().extractItem(slot, amount, true);
			if (simulated.getCount() != amount || !matcher.test(simulated)) {
				throw new IllegalStateException("Слот " + slot + " не отдаёт нужный предмет FD для " + action + ": " + simulated);
			}
			ItemStack extracted = inventory.handler().extractItem(slot, amount, false);
			RatsMod.LOGGER.debug("Крыса {} забрала {} из слота {} со стороны {} для {}", rat.getUUID(), extracted, slot, inventory.side(), action);
			return extracted;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла извлечь предмет FD из слота {} для {}", rat.getUUID(), slot, action, exception);
			throw exception;
		}
	}

	// Ищет доступный инвентарь блока; принимает мир, позицию, сторону, крысу и действие, отдаёт обработчик с выбранной стороной.
	private static Optional<InventoryView> resolveInventory(Level level, BlockPos pos, Direction preferredSide, TamedRat rat, String action) {
		try {
			for (Direction side : orderedSides(preferredSide)) {
				IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
				if (handler != null) {
					return Optional.of(new InventoryView(handler, side));
				}
			}
			RatsMod.LOGGER.debug("Крыса {} не нашла инвентарь {} для {}", rat.getUUID(), pos, action);
			return Optional.empty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла найти инвентарь {} для {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Формирует порядок сторон; принимает предпочитаемую сторону, отдаёт массив направлений.
	private static Direction[] orderedSides(Direction preferredSide) {
		try {
			Direction[] directions = Direction.values();
			Direction[] ordered = new Direction[directions.length];
			ordered[0] = preferredSide;
			int index = 1;
			for (Direction direction : directions) {
				if (direction != preferredSide) {
					ordered[index++] = direction;
				}
			}
			return ordered;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось собрать порядок сторон FD для {}", preferredSide, exception);
			throw exception;
		}
	}

	// Проверяет вставку всех результатов в склад; принимает крысу, склад, стеки и действие, отдаёт true если каждый стек принят.
	private static boolean canInsertAll(TamedRat rat, BlockPos depositPos, List<ItemStack> stacks, String action) {
		try {
			for (ItemStack stack : stacks) {
				if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, stack, rat, action)) {
					return false;
				}
			}
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить вставку результатов FD в {}", rat.getUUID(), depositPos, exception);
			throw exception;
		}
	}

	// Возвращает предмет в сундук забора или сообщает жёсткую ошибку; принимает крысу, сундук, стек и действие, ничего не отдаёт.
	private static void returnToPickupOrThrow(TamedRat rat, BlockPos pickupPos, ItemStack stack, String action) {
		try {
			if (stack.isEmpty()) {
				return;
			}
			ItemStack remaining = RatInventoryAccess.insert(rat.level(), pickupPos, rat.pickupFacing, stack.copy(), rat, action);
			if (!remaining.isEmpty()) {
				throw new IllegalStateException("Не удалось вернуть предмет FD в сундук забора, остаток: " + remaining);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла вернуть предмет {} в {}", rat.getUUID(), stack, pickupPos, exception);
			throw exception;
		}
	}

	// Тратит прочность инструмента; принимает крысу и инструмент, изменяет стек и ничего не отдаёт.
	private static void damageToolOrBreak(TamedRat rat, ItemStack tool) {
		try {
			if (tool.isDamageableItem()) {
				ItemStack previousHand = rat.getMainHandItem().copy();
				rat.setItemInHand(InteractionHand.MAIN_HAND, tool);
				tool.hurtAndBreak(1, rat, EquipmentSlot.MAINHAND);
				rat.setItemInHand(InteractionHand.MAIN_HAND, previousHand);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла потратить прочность инструмента FD {}", rat.getUUID(), tool, exception);
			throw exception;
		}
	}

	// Сравнивает план с предметом; принимает план и стек, отдаёт true если это тот же предмет.
	private static boolean samePlanItem(InventoryPlan plan, ItemStack stack) {
		try {
			return ItemStack.isSameItemSameComponents(plan.stack(), stack);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось сравнить план FD {} со стеком {}", plan, stack, exception);
			throw exception;
		}
	}

	// Помечает блок изменённым; принимает сущность блока, ничего не отдаёт.
	private static void markChanged(BlockEntity blockEntity) {
		try {
			blockEntity.setChanged();
			if (blockEntity.getLevel() != null) {
				BlockState state = blockEntity.getBlockState();
				blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), state, state, 3);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось пометить блок FD {} изменённым", blockEntity.getBlockPos(), exception);
			throw exception;
		}
	}

	private record InventoryView(IItemHandler handler, Direction side) {
	}

	private record VirtualSlot(int slot, ItemStack stack) {
	}

	private record InventoryPlan(Direction side, int slot, ItemStack stack) {
	}

	private record CookingPotPlan(String recipeId, List<InventoryPlan> ingredients) {
	}

	private record CuttingBoardPlan(String recipeId, InventoryPlan ingredient, InventoryPlan tool, List<ItemStack> outputs) {
	}

	private record FdFilter(boolean whitelistPresent, Set<String> whitelistIds, Set<String> blacklistIds) {
		// Создаёт фильтр из улучшений крысы; принимает крысу, отдаёт снимок белого и чёрного списка.
		private static FdFilter fromRat(TamedRat rat) {
			try {
				Set<String> whitelist = readFilterIds(rat, RatsItemRegistry.RAT_UPGRADE_WHITELIST.get());
				Set<String> blacklist = readFilterIds(rat, RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get());
				return new FdFilter(RatUpgradeUtils.hasUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_WHITELIST.get()), whitelist, blacklist);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Крыса {} не смогла прочитать списки FD", rat.getUUID(), exception);
				throw exception;
			}
		}

		// Сообщает, включён ли белый список; параметров нет, отдаёт true при наличии улучшения whitelist.
		private boolean hasWhitelist() {
			try {
				return this.whitelistPresent;
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось проверить наличие whitelist FD", exception);
				throw exception;
			}
		}

		// Проверяет предмет в белом списке; принимает стек, отдаёт true если его идентификатор записан.
		private boolean isWhitelisted(ItemStack stack) {
			try {
				return this.whitelistIds.contains(getItemId(stack));
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось проверить whitelist FD для {}", stack, exception);
				throw exception;
			}
		}

		// Проверяет предмет в чёрном списке; принимает стек, отдаёт true если его идентификатор записан.
		private boolean isBlacklisted(ItemStack stack) {
			try {
				return this.blacklistIds.contains(getItemId(stack));
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось проверить blacklist FD для {}", stack, exception);
				throw exception;
			}
		}

		// Проверяет вход и выход рецепта плиты или доски; принимает вход и выход, отдаёт true если операция разрешена.
		private boolean allowsProcessing(ItemStack input, ItemStack output) {
			try {
				if (this.isBlacklisted(input) || this.isBlacklisted(output)) {
					return false;
				}
				if (!this.whitelistPresent) {
					return true;
				}
				return this.isWhitelisted(output) || this.isWhitelisted(input);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось проверить обработку FD для входа {} и выхода {}", input, output, exception);
				throw exception;
			}
		}

		// Проверяет ингредиент обычного рецепта; принимает стек и признак целевого выхода, отдаёт true если ингредиент можно брать.
		private boolean allowsIngredient(ItemStack stack, boolean outputTargeted) {
			try {
				if (this.isBlacklisted(stack)) {
					return false;
				}
				return !this.whitelistPresent || outputTargeted || this.isWhitelisted(stack);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось проверить ингредиент FD {}", stack, exception);
				throw exception;
			}
		}

		// Проверяет ингредиент целевого рецепта котла; принимает стек, отдаёт true если он не запрещён blacklist.
		private boolean allowsIngredientForTargetRecipe(ItemStack stack) {
			try {
				return !this.isBlacklisted(stack);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось проверить ингредиент целевого рецепта FD {}", stack, exception);
				throw exception;
			}
		}

		// Проверяет контейнер блюда; принимает стек, отдаёт true если контейнер не в blacklist.
		private boolean allowsContainer(ItemStack stack) {
			try {
				return !this.isBlacklisted(stack);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось проверить контейнер FD {}", stack, exception);
				throw exception;
			}
		}

		// Проверяет инструмент доски; принимает стек, отдаёт true если инструмент не в blacklist.

		// Проверяет готовый результат сковороды; принимает стек, отдаёт true если фильтры разрешают сдачу.
		private boolean allowsCollectedResult(ItemStack stack) {
			try {
				if (this.isBlacklisted(stack)) {
					return false;
				}
				return !this.whitelistPresent || this.isWhitelisted(stack);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось проверить готовый результат FD {}", stack, exception);
				throw exception;
			}
		}

		private boolean allowsTool(ItemStack stack) {
			try {
				return !this.isBlacklisted(stack);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось проверить инструмент FD {}", stack, exception);
				throw exception;
			}
		}

		// Читает идентификаторы предметов из улучшения списка; принимает крысу и предмет улучшения, отдаёт набор id.
		private static Set<String> readFilterIds(TamedRat rat, net.minecraft.world.item.Item upgradeItem) {
			try {
				ItemStack upgrade = RatUpgradeUtils.getUpgrade(rat, upgradeItem);
				if (upgrade.isEmpty()) {
					return Set.of();
				}
				CompoundTag tag = upgrade.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
				if (!tag.contains("Items", 9)) {
					return Set.of();
				}
				ListTag list = tag.getList("Items", 10);
				Set<String> ids = new HashSet<>();
				for (int index = 0; index < list.size(); index++) {
					String id = list.getCompound(index).getString("id");
					if (!id.isBlank()) {
						ids.add(ResourceLocation.parse(id).toString());
					}
				}
				return ids;
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Крыса {} не смогла прочитать список улучшения {}", rat.getUUID(), upgradeItem, exception);
				throw exception;
			}
		}

		// Получает идентификатор предмета; принимает стек, отдаёт строку вида namespace:path.
		private static String getItemId(ItemStack stack) {
			try {
				return Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(stack.getItem())).toString();
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось получить идентификатор предмета FD {}", stack, exception);
				throw exception;
			}
		}
	}
}
