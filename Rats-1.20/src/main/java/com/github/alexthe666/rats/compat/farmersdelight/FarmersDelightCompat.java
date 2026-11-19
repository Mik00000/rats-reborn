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

	// Prevents instantiating the compat service; no parameters, returns nothing.
	private FarmersDelightCompat() {
		throw new IllegalStateException("The Farmer's Delight compat service cannot be instantiated");
	}

	// Checks whether the block is a Farmer's Delight work block; takes the block entity, returns true for a stove, cooking pot or board.
	public static boolean isSupportedWorkBlock(BlockEntity blockEntity) {
		try {
			return blockEntity instanceof StoveBlockEntity
					|| blockEntity instanceof CookingPotBlockEntity
					|| blockEntity instanceof CuttingBoardBlockEntity
					|| blockEntity instanceof SkilletBlockEntity;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check Farmer's Delight work block {}", blockEntity, exception);
			throw exception;
		}
	}

	// Runs one chef work cycle with Farmer's Delight; takes the rat and three points, returns true if an action was performed.
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
			RatsMod.LOGGER.debug("Rat {} does not work with Farmer's Delight: block {} is not supported", rat.getUUID(), workPos);
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to run the Farmer's Delight cycle at block {}", rat.getUUID(), workPos, exception);
			throw exception;
		}
	}

	// Handles the stove; takes the rat, stove and chest points, returns true if a result was taken or a new item placed.
	private static boolean tryRunStoveCycle(TamedRat rat, StoveBlockEntity stove, BlockPos pickupPos, BlockPos depositPos) {
		try {
			FdFilter filter = FdFilter.fromRat(rat);
			if (tryCollectFinishedStoveItem(rat, stove, depositPos, filter)) {
				return true;
			}
			BlockState state = rat.level().getBlockState(stove.getBlockPos());
			if (!state.hasProperty(AbstractStoveBlock.LIT) || !state.getValue(AbstractStoveBlock.LIT)) {
				RatsMod.LOGGER.debug("Rat {} does not place an item on FD stove {}: the stove is not heated", rat.getUUID(), stove.getBlockPos());
				return false;
			}
			if (stove.isFull()) {
				RatsMod.LOGGER.debug("Rat {} does not place an item on FD stove {}: all spots are taken", rat.getUUID(), stove.getBlockPos());
				return false;
			}
			Optional<InventoryView> pickup = resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "pickup for FD stove");
			if (pickup.isEmpty()) {
				RatsMod.LOGGER.debug("Rat {} did not find a pickup chest for FD stove {}", rat.getUUID(), pickupPos);
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
				if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, output, rat, "deposit check for FD stove result")) {
					RatsMod.LOGGER.debug("Rat {} does not place {} on FD stove {}: deposit {} will not accept {}", rat.getUUID(), candidate, stove.getBlockPos(), depositPos, output);
					return false;
				}
				ItemStack extracted = extractExact(pickup.get(), slot, 1, stack -> ItemStack.isSameItemSameComponents(stack, candidate), rat, "raw ingredient pickup for FD stove");
				if (extracted.isEmpty()) {
					return false;
				}
				ItemStack oneItem = extracted.copyWithCount(1);
				boolean placed = stove.placeFood(rat, oneItem, recipe.get());
				if (!placed) {
					returnToPickupOrThrow(rat, pickupPos, extracted, "returning ingredient after FD stove refusal");
					RatsMod.LOGGER.debug("Rat {} failed to place {} on FD stove {}", rat.getUUID(), extracted, stove.getBlockPos());
					return false;
				}
				markChanged(stove);
				RatsMod.LOGGER.debug("Rat {} placed {} on FD stove {}, expected result {}", rat.getUUID(), extracted, stove.getBlockPos(), output);
				return true;
			}
			RatsMod.LOGGER.debug("Rat {} found no suitable ingredient for FD stove in {}", rat.getUUID(), pickupPos);
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to handle FD stove {}", rat.getUUID(), stove.getBlockPos(), exception);
			throw exception;
		}
	}

	// Takes a finished item off the stove; takes the rat, stove, deposit and filter, returns true if the item was moved.
	private static boolean tryCollectFinishedStoveItem(TamedRat rat, StoveBlockEntity stove, BlockPos depositPos, FdFilter filter) {
		try {
			ItemStackHandler items = stove.getItems();
			for (int slot = 0; slot < items.getSlots(); slot++) {
				ItemStack stack = items.getStackInSlot(slot);
				if (stack.isEmpty() || stove.getCookingRecipe(stack).isPresent()) {
					continue;
				}
				if (filter.isBlacklisted(stack)) {
					RatsMod.LOGGER.debug("Rat {} does not take {} off FD stove {}: result is blacklisted", rat.getUUID(), stack, stove.getBlockPos());
					continue;
				}
				if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, stack.copyWithCount(1), rat, "deposit check for finished FD stove")) {
					RatsMod.LOGGER.debug("Rat {} does not take {} off FD stove {}: deposit {} is full", rat.getUUID(), stack, stove.getBlockPos(), depositPos);
					return false;
				}
				ItemStack extracted = items.extractItem(slot, 1, false);
				ItemStack remaining = RatInventoryAccess.insert(rat.level(), depositPos, rat.depositFacing, extracted.copy(), rat, "delivering FD stove result");
				if (!remaining.isEmpty()) {
					ItemStack rollback = ItemHandlerHelper.insertItem(items, extracted, false);
					if (!rollback.isEmpty()) {
						throw new IllegalStateException("The deposit refused after simulation and the stove result could not be returned: " + rollback);
					}
					return false;
				}
				markChanged(stove);
				RatsMod.LOGGER.debug("Rat {} took {} off FD stove {} and carried it to {}", rat.getUUID(), extracted, stove.getBlockPos(), depositPos);
				return true;
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to take the result off FD stove {}", rat.getUUID(), stove.getBlockPos(), exception);
			throw exception;
		}
	}

	// Handles the cooking pot; takes the rat, pot and chest points, returns true if a serving was taken or a recipe loaded.

	// Handles the FD skillet; takes the rat, skillet and chests, returns true if an item was loaded or a finished result delivered.
	private static boolean tryRunSkilletCycle(TamedRat rat, SkilletBlockEntity skillet, BlockPos pickupPos, BlockPos depositPos) {
		try {
			FdFilter filter = FdFilter.fromRat(rat);
			if (tryCollectDroppedSkilletResult(rat, skillet, depositPos, filter)) {
				return true;
			}
			BlockState state = rat.level().getBlockState(skillet.getBlockPos());
			if (state.hasProperty(SkilletBlock.WATERLOGGED) && state.getValue(SkilletBlock.WATERLOGGED)) {
				RatsMod.LOGGER.debug("Rat {} does not place an item in FD skillet {}: the skillet is waterlogged", rat.getUUID(), skillet.getBlockPos());
				return false;
			}
			if (!skillet.isHeated()) {
				RatsMod.LOGGER.debug("Rat {} does not place an item in FD skillet {}: the skillet is not heated", rat.getUUID(), skillet.getBlockPos());
				return false;
			}
			if (skillet.hasStoredStack()) {
				RatsMod.LOGGER.debug("Rat {} is waiting for FD skillet {} to cook: it already contains {}", rat.getUUID(), skillet.getBlockPos(), skillet.getStoredStack());
				return false;
			}
			Optional<InventoryView> pickup = resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "pickup for FD skillet");
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
				if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, output, rat, "deposit check for FD skillet result")) {
					RatsMod.LOGGER.debug("Rat {} does not place {} in FD skillet {}: deposit {} will not accept {}", rat.getUUID(), candidate, skillet.getBlockPos(), depositPos, output);
					return false;
				}
				ItemStack extracted = extractExact(pickup.get(), slot, 1, stack -> ItemStack.isSameItemSameComponents(stack, candidate), rat, "raw ingredient pickup for FD skillet");
				if (extracted.isEmpty()) {
					return false;
				}
				ItemStack oneItem = extracted.copyWithCount(1);
				ItemStack remaining = skillet.addItemToCook(oneItem, null);
				if (!remaining.isEmpty()) {
					returnToPickupOrThrow(rat, pickupPos, extracted, "returning ingredient after FD skillet refusal");
					RatsMod.LOGGER.debug("Rat {} failed to place {} in FD skillet {}", rat.getUUID(), extracted, skillet.getBlockPos());
					return false;
				}
				markChanged(skillet);
				RatsMod.LOGGER.debug("Rat {} placed {} in FD skillet {}, expected result {}", rat.getUUID(), extracted, skillet.getBlockPos(), output);
				return true;
			}
			RatsMod.LOGGER.debug("Rat {} found no suitable ingredient for FD skillet in {}", rat.getUUID(), pickupPos);
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to handle FD skillet {}", rat.getUUID(), skillet.getBlockPos(), exception);
			throw exception;
		}
	}

	// Picks up a finished item the FD skillet dropped into the world; takes the rat, skillet, deposit and filter, returns true if the result was delivered.
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
				if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, single, rat, "deposit check for dropped FD skillet result")) {
					RatsMod.LOGGER.debug("Rat {} does not take {} from FD skillet {}: deposit {} will not accept the item", rat.getUUID(), single, skillet.getBlockPos(), depositPos);
					return false;
				}
				ItemStack remaining = RatInventoryAccess.insert(rat.level(), depositPos, rat.depositFacing, single.copy(), rat, "delivering FD skillet result");
				if (!remaining.isEmpty()) {
					throw new IllegalStateException("The deposit refused after simulating the FD skillet result, remainder: " + remaining);
				}
				stack.shrink(1);
				if (stack.isEmpty()) {
					entity.discard();
				} else {
					entity.setItem(stack);
				}
				RatsMod.LOGGER.debug("Rat {} took {} from FD skillet {} and carried it to {}", rat.getUUID(), single, skillet.getBlockPos(), depositPos);
				return true;
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to take the FD skillet result {}", rat.getUUID(), skillet.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the campfire recipe result for the skillet; takes the level and ingredient, returns the cooked stack or an empty stack.
	private static ItemStack getCampfireCookingResult(Level level, ItemStack input) {
		try {
			Optional<RecipeHolder<CampfireCookingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(input.copyWithCount(1)), level);
			if (recipe.isEmpty()) {
				return ItemStack.EMPTY;
			}
			return recipe.get().value().getResultItem(level.registryAccess()).copy();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to find an FD campfire recipe for {}", input, exception);
			throw exception;
		}
	}

	// Checks whether the item is a campfire recipe result; takes the level and stack, returns true for finished skillet items.
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
			RatsMod.LOGGER.error("Failed to check FD campfire result {}", stack, exception);
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
				RatsMod.LOGGER.debug("Rat {} does not load FD cooking pot {}: the pot is not heated", rat.getUUID(), pot.getBlockPos());
				return false;
			}
			if (!isCookingPotInputClear(pot)) {
				RatsMod.LOGGER.debug("Rat {} is waiting for FD cooking pot {}: it already contains ingredients or a meal", rat.getUUID(), pot.getBlockPos());
				return false;
			}
			if (!filter.hasWhitelist()) {
				RatsMod.LOGGER.debug("Rat {} does not cook in FD cooking pot {}: the pot needs a whitelisted target recipe", rat.getUUID(), pot.getBlockPos());
				return false;
			}
			Optional<InventoryView> pickup = resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "pickup for FD cooking pot");
			if (pickup.isEmpty()) {
				return false;
			}
			Optional<CookingPotPlan> plan = findCookingPotPlan(rat, pot, pickup.get(), depositPos, filter);
			if (plan.isEmpty()) {
				RatsMod.LOGGER.debug("Rat {} did not find a full set for the target FD cooking pot recipe at {}", rat.getUUID(), pickupPos);
				return false;
			}
			List<ItemStack> extracted = extractPlans(pickup.get(), plan.get().ingredients(), rat, "ingredient pickup for FD cooking pot");
			if (extracted.size() != plan.get().ingredients().size()) {
				throw new IllegalStateException("Failed to extract the full FD cooking pot ingredient set for " + plan.get().recipeId());
			}
			try {
				insertCookingPotIngredients(pot, extracted, rat);
			} catch (RuntimeException exception) {
				for (ItemStack stack : extracted) {
					returnToPickupOrThrow(rat, pickupPos, stack, "returning FD cooking pot ingredients");
				}
				throw exception;
			}
			markChanged(pot);
			RatsMod.LOGGER.debug("Rat {} loaded FD cooking pot recipe {} into {}", rat.getUUID(), plan.get().recipeId(), pot.getBlockPos());
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to handle FD cooking pot {}", rat.getUUID(), pot.getBlockPos(), exception);
			throw exception;
		}
	}

	// Serves a finished portion from the pot; takes the rat, pot, chests and filter, returns true if a portion was taken.
	private static boolean tryServeCookingPotMeal(TamedRat rat, CookingPotBlockEntity pot, BlockPos pickupPos, BlockPos depositPos, FdFilter filter) {
		try {
			ItemStack meal = pot.getMeal();
			if (meal.isEmpty()) {
				return false;
			}
			if (filter.isBlacklisted(meal)) {
				RatsMod.LOGGER.debug("Rat {} does not take meal {} from FD cooking pot {}: result is blacklisted", rat.getUUID(), meal, pot.getBlockPos());
				return false;
			}
			ItemStack container = pot.getContainer().copyWithCount(1);
			if (container.isEmpty()) {
				RatsMod.LOGGER.debug("Rat {} does not take meal {} from FD cooking pot {}: the meal has no serving container", rat.getUUID(), meal, pot.getBlockPos());
				return false;
			}
			if (filter.isBlacklisted(container)) {
				RatsMod.LOGGER.debug("Rat {} does not take the meal from FD cooking pot {}: container {} is blacklisted", rat.getUUID(), pot.getBlockPos(), container);
				return false;
			}
			if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, meal.copyWithCount(1), rat, "deposit check for FD cooking pot meal")) {
				RatsMod.LOGGER.debug("Rat {} does not take meal {} from FD cooking pot {}: deposit {} will not accept the result", rat.getUUID(), meal, pot.getBlockPos(), depositPos);
				return false;
			}
			Optional<InventoryView> pickup = resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "container pickup for FD cooking pot");
			if (pickup.isEmpty()) {
				return false;
			}
			Optional<InventoryPlan> containerPlan = reserveSingle(pickup.get(), stack -> ItemStack.isSameItemSameComponents(stack, container), filter::allowsContainer);
			if (containerPlan.isEmpty()) {
				RatsMod.LOGGER.debug("Rat {} does not take meal {} from FD cooking pot {}: no container {}", rat.getUUID(), meal, pot.getBlockPos(), container);
				return false;
			}
			ItemStack extractedContainer = extractPlan(pickup.get(), containerPlan.get(), rat, "container pickup for FD cooking pot");
			ItemStack served = pot.useHeldItemOnMeal(extractedContainer);
			if (served.isEmpty()) {
				returnToPickupOrThrow(rat, pickupPos, container, "returning the container after FD cooking pot refusal");
				RatsMod.LOGGER.debug("Rat {} failed to take a portion from FD cooking pot {}", rat.getUUID(), pot.getBlockPos());
				return false;
			}
			ItemStack remaining = RatInventoryAccess.insert(rat.level(), depositPos, rat.depositFacing, served.copy(), rat, "delivering FD cooking pot meal");
			if (!remaining.isEmpty()) {
				throw new IllegalStateException("The deposit refused after simulating FD cooking pot serving, remainder: " + remaining);
			}
			markChanged(pot);
			RatsMod.LOGGER.debug("Rat {} took {} from FD cooking pot {} and carried it to {}", rat.getUUID(), served, pot.getBlockPos(), depositPos);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to take the meal from FD cooking pot {}", rat.getUUID(), pot.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finds a transactional cooking pot plan; takes the rat, pot, chest, deposit and filter, returns the plan found or empty.
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
					RatsMod.LOGGER.debug("Rat {} skips FD cooking pot recipe {}: container {} is forbidden", rat.getUUID(), holder.id(), container);
					continue;
				}
				List<InventoryPlan> ingredients = reserveIngredients(pickup, recipe.getIngredients(), stack -> filter.allowsIngredientForTargetRecipe(stack));
				if (ingredients.size() != recipe.getIngredients().size()) {
					RatsMod.LOGGER.debug("Rat {} did not gather a full ingredient set for FD cooking pot recipe {}", rat.getUUID(), holder.id());
					continue;
				}
				if (!container.isEmpty() && reserveSingle(pickup, stack -> ItemStack.isSameItemSameComponents(stack, container), filter::allowsContainer).isEmpty()) {
					RatsMod.LOGGER.debug("Rat {} did not find container {} for FD cooking pot recipe {}", rat.getUUID(), container, holder.id());
					continue;
				}
				if (!matchesCookingPotRecipe(recipe, ingredients, rat.level())) {
					RatsMod.LOGGER.debug("Rat {} gathered the ingredients, but FD cooking pot recipe {} did not match on verification", rat.getUUID(), holder.id());
					continue;
				}
				if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, result, rat, "deposit check for FD cooking pot recipe")) {
					RatsMod.LOGGER.debug("Rat {} does not cook FD cooking pot recipe {}: deposit {} will not accept {}", rat.getUUID(), holder.id(), depositPos, result);
					continue;
				}
				return Optional.of(new CookingPotPlan(holder.id().toString(), ingredients));
			}
			return Optional.empty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to pick an FD cooking pot recipe {}", rat.getUUID(), pot.getBlockPos(), exception);
			throw exception;
		}
	}

	// Handles the cutting board; takes the rat, board and chest points, returns true if an item was cut.
	private static boolean tryRunCuttingBoardCycle(TamedRat rat, CuttingBoardBlockEntity board, BlockPos pickupPos, BlockPos depositPos) {
		try {
			if (!board.isEmpty()) {
				RatsMod.LOGGER.debug("Rat {} does not work with FD cutting board {}: the board holds item {}", rat.getUUID(), board.getBlockPos(), board.getStoredItem());
				return false;
			}
			FdFilter filter = FdFilter.fromRat(rat);
			Optional<InventoryView> pickup = resolveInventory(rat.level(), pickupPos, rat.pickupFacing, rat, "pickup for FD cutting board");
			if (pickup.isEmpty()) {
				return false;
			}
			Optional<CuttingBoardPlan> plan = findCuttingBoardPlan(rat, pickup.get(), depositPos, filter);
			if (plan.isEmpty()) {
				RatsMod.LOGGER.debug("Rat {} did not find a full set for FD cutting board in {}", rat.getUUID(), pickupPos);
				return false;
			}
			ItemStack ingredient = extractPlan(pickup.get(), plan.get().ingredient(), rat, "ingredient pickup for FD cutting board");
			ItemStack tool;
			try {
				tool = extractPlan(pickup.get(), plan.get().tool(), rat, "knife pickup for FD cutting board");
			} catch (RuntimeException exception) {
				returnToPickupOrThrow(rat, pickupPos, ingredient, "returning the ingredient after FD knife refusal");
				throw exception;
			}
			boolean ingredientConsumed = false;
			try {
				ItemStack leftover = board.addItem(ingredient.copy());
				if (!leftover.isEmpty()) {
					throw new IllegalStateException("FD cutting board refused after simulation, remainder: " + leftover);
				}
				ItemStack stored = board.removeItem();
				if (stored.isEmpty()) {
					throw new IllegalStateException("FD cutting board did not return the item to cut");
				}
				ingredientConsumed = true;
				damageToolOrBreak(rat, tool);
				for (ItemStack output : plan.get().outputs()) {
					ItemStack remaining = RatInventoryAccess.insert(rat.level(), depositPos, rat.depositFacing, output.copy(), rat, "delivering FD cutting board result");
					if (!remaining.isEmpty()) {
						throw new IllegalStateException("The deposit refused after simulating the FD cutting board, remainder: " + remaining);
					}
				}
				if (!tool.isEmpty()) {
					returnToPickupOrThrow(rat, pickupPos, tool, "returning the knife after FD cutting board");
				}
				markChanged(board);
				RatsMod.LOGGER.debug("Rat {} cut {} on FD cutting board {} using recipe {}", rat.getUUID(), stored, board.getBlockPos(), plan.get().recipeId());
				return true;
			} catch (RuntimeException exception) {
				if (!ingredientConsumed) {
					returnToPickupOrThrow(rat, pickupPos, ingredient, "returning the ingredient after an FD cutting board error");
				}
				if (!tool.isEmpty()) {
					returnToPickupOrThrow(rat, pickupPos, tool, "returning the knife after an FD cutting board error");
				}
				throw exception;
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to handle FD cutting board {}", rat.getUUID(), board.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finds a transactional cutting board plan; takes the rat, chest, deposit and filter, returns the plan or empty.
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
					RatsMod.LOGGER.debug("Rat {} did not find a knife or tool for FD cutting board recipe {}", rat.getUUID(), holder.id());
					continue;
				}
				if (!matchesCuttingBoardRecipe(recipe, ingredient.get().stack(), tool.get().stack(), rat.level())) {
					continue;
				}
				if (!canInsertAll(rat, depositPos, outputs, "deposit check for FD cutting board result")) {
					RatsMod.LOGGER.debug("Rat {} does not cut FD cutting board recipe {}: deposit {} will not accept the result", rat.getUUID(), holder.id(), depositPos);
					continue;
				}
				return Optional.of(new CuttingBoardPlan(holder.id().toString(), ingredient.get(), tool.get(), outputs));
			}
			return Optional.empty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to pick an FD cutting board recipe", rat.getUUID(), exception);
			throw exception;
		}
	}

	// Checks whether the pot input slots are empty; takes the pot, returns true only if a new recipe can be loaded.
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
			RatsMod.LOGGER.error("Failed to check whether FD cooking pot {} is occupied", pot.getBlockPos(), exception);
			throw exception;
		}
	}

	// Inserts ingredients into the pot; takes the pot, a list of stacks and the rat, returns nothing.
	private static void insertCookingPotIngredients(CookingPotBlockEntity pot, List<ItemStack> ingredients, TamedRat rat) {
		try {
			ItemStackHandler inventory = pot.getInventory();
			for (int index = 0; index < ingredients.size(); index++) {
				ItemStack remaining = inventory.insertItem(index, ingredients.get(index).copy(), false);
				if (!remaining.isEmpty()) {
					throw new IllegalStateException("FD cooking pot refused ingredient " + ingredients.get(index) + ", remainder: " + remaining);
				}
			}
			RatsMod.LOGGER.debug("Rat {} loaded {} ingredients into FD cooking pot {}", rat.getUUID(), ingredients.size(), pot.getBlockPos());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to load ingredients into FD cooking pot {}", rat.getUUID(), pot.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks the pot recipe against the selected ingredients; takes the recipe, plans and level, returns true on a match.
	private static boolean matchesCookingPotRecipe(CookingPotRecipe recipe, List<InventoryPlan> ingredients, Level level) {
		try {
			ItemStackHandler handler = new ItemStackHandler(COOKING_POT_INPUT_SLOTS);
			for (int index = 0; index < ingredients.size(); index++) {
				handler.setStackInSlot(index, ingredients.get(index).stack().copy());
			}
			return recipe.matches(new RecipeWrapper(handler), level);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the FD cooking pot recipe", exception);
			throw exception;
		}
	}

	// Checks the board recipe against the selected item and tool; takes the recipe, item, tool and level, returns true on a match.
	private static boolean matchesCuttingBoardRecipe(CuttingBoardRecipe recipe, ItemStack item, ItemStack tool, Level level) {
		try {
			return recipe.matches(new CuttingBoardRecipeInput(item.copy(), tool.copy()), level);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the FD cutting board recipe", exception);
			throw exception;
		}
	}

	// Gets the board results; takes the recipe, returns a list of non-empty stacks.
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
			RatsMod.LOGGER.error("Failed to read FD cutting board recipe results", exception);
			throw exception;
		}
	}

	// Reserves ingredients from an Ingredient list; takes the chest, ingredients and filter, returns extraction plans.
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
			RatsMod.LOGGER.error("Failed to reserve FD ingredients", exception);
			throw exception;
		}
	}

	// Reserves one item from the chest; takes the chest, item condition and filter, returns a plan or empty.
	private static Optional<InventoryPlan> reserveSingle(InventoryView inventory, Predicate<ItemStack> matcher, Predicate<ItemStack> allowed) {
		try {
			return reserveFromVirtualSlots(inventory, createVirtualSlots(inventory), matcher, allowed);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to reserve FD item", exception);
			throw exception;
		}
	}

	// Reserves an item from virtual slots; takes the chest, slots, condition and filter, returns a plan or empty.
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
			RatsMod.LOGGER.error("Failed to pick an FD virtual slot", exception);
			throw exception;
		}
	}

	// Creates an inventory snapshot; takes the chest view, returns virtual slots.
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
			RatsMod.LOGGER.error("Failed to snapshot the FD inventory from side {}", inventory.side(), exception);
			throw exception;
		}
	}

	// Extracts a set of plans; takes the chest, plans, rat and action, returns the extracted stacks.
	private static List<ItemStack> extractPlans(InventoryView inventory, List<InventoryPlan> plans, TamedRat rat, String action) {
		try {
			List<ItemStack> extracted = new ArrayList<>();
			for (InventoryPlan plan : plans) {
				extracted.add(extractPlan(inventory, plan, rat, action));
			}
			return extracted;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to extract the FD item set for {}", rat.getUUID(), action, exception);
			throw exception;
		}
	}

	// Extracts one plan; takes the chest, plan, rat and action, returns the extracted stack.
	private static ItemStack extractPlan(InventoryView inventory, InventoryPlan plan, TamedRat rat, String action) {
		try {
			return extractExact(inventory, plan.slot(), plan.stack().getCount(), stack -> ItemStack.isSameItemSameComponents(stack, plan.stack()), rat, action);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to extract plan {} for {}", rat.getUUID(), plan, action, exception);
			throw exception;
		}
	}

	// Extracts an exact item from a slot; takes the chest, slot, count, check, rat and action, returns the stack.
	private static ItemStack extractExact(InventoryView inventory, int slot, int amount, Predicate<ItemStack> matcher, TamedRat rat, String action) {
		try {
			ItemStack current = inventory.handler().getStackInSlot(slot);
			if (current.isEmpty() || !matcher.test(current)) {
				throw new IllegalStateException("Slot " + slot + " changed before FD extraction for " + action + ": " + current);
			}
			ItemStack simulated = inventory.handler().extractItem(slot, amount, true);
			if (simulated.getCount() != amount || !matcher.test(simulated)) {
				throw new IllegalStateException("Slot " + slot + " does not provide the required FD item for " + action + ": " + simulated);
			}
			ItemStack extracted = inventory.handler().extractItem(slot, amount, false);
			RatsMod.LOGGER.debug("Rat {} took {} from slot {} on side {} for {}", rat.getUUID(), extracted, slot, inventory.side(), action);
			return extracted;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to extract an FD item from slot {} for {}", rat.getUUID(), slot, action, exception);
			throw exception;
		}
	}

	// Finds an accessible block inventory; takes the level, position, side, rat and action, returns a handler with the chosen side.
	private static Optional<InventoryView> resolveInventory(Level level, BlockPos pos, Direction preferredSide, TamedRat rat, String action) {
		try {
			for (Direction side : orderedSides(preferredSide)) {
				IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
				if (handler != null) {
					return Optional.of(new InventoryView(handler, side));
				}
			}
			RatsMod.LOGGER.debug("Rat {} did not find inventory {} for {}", rat.getUUID(), pos, action);
			return Optional.empty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} could not find inventory {} for {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Builds the side order; takes the preferred side, returns an array of directions.
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
			RatsMod.LOGGER.error("Failed to build the FD side order for {}", preferredSide, exception);
			throw exception;
		}
	}

	// Checks inserting all results into the deposit; takes the rat, deposit, stacks and action, returns true if every stack is accepted.
	private static boolean canInsertAll(TamedRat rat, BlockPos depositPos, List<ItemStack> stacks, String action) {
		try {
			for (ItemStack stack : stacks) {
				if (!RatInventoryAccess.canInsert(rat.level(), depositPos, rat.depositFacing, stack, rat, action)) {
					return false;
				}
			}
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to check inserting FD results into {}", rat.getUUID(), depositPos, exception);
			throw exception;
		}
	}

	// Returns the item to the pickup chest or reports a hard error; takes the rat, chest, stack and action, returns nothing.
	private static void returnToPickupOrThrow(TamedRat rat, BlockPos pickupPos, ItemStack stack, String action) {
		try {
			if (stack.isEmpty()) {
				return;
			}
			ItemStack remaining = RatInventoryAccess.insert(rat.level(), pickupPos, rat.pickupFacing, stack.copy(), rat, action);
			if (!remaining.isEmpty()) {
				throw new IllegalStateException("Failed to return the FD item to the pickup chest, remainder: " + remaining);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to return item {} to {}", rat.getUUID(), stack, pickupPos, exception);
			throw exception;
		}
	}

	// Uses tool durability; takes the rat and the tool, modifies the stack and returns nothing.
	private static void damageToolOrBreak(TamedRat rat, ItemStack tool) {
		try {
			if (tool.isDamageableItem()) {
				ItemStack previousHand = rat.getMainHandItem().copy();
				rat.setItemInHand(InteractionHand.MAIN_HAND, tool);
				tool.hurtAndBreak(1, rat, EquipmentSlot.MAINHAND);
				rat.setItemInHand(InteractionHand.MAIN_HAND, previousHand);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to use durability of FD tool {}", rat.getUUID(), tool, exception);
			throw exception;
		}
	}

	// Compares a plan with an item; takes the plan and stack, returns true if it is the same item.
	private static boolean samePlanItem(InventoryPlan plan, ItemStack stack) {
		try {
			return ItemStack.isSameItemSameComponents(plan.stack(), stack);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to compare FD plan {} with stack {}", plan, stack, exception);
			throw exception;
		}
	}

	// Marks the block as changed; takes the block entity, returns nothing.
	private static void markChanged(BlockEntity blockEntity) {
		try {
			blockEntity.setChanged();
			if (blockEntity.getLevel() != null) {
				BlockState state = blockEntity.getBlockState();
				blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), state, state, 3);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to mark FD block {} as changed", blockEntity.getBlockPos(), exception);
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
		// Builds a filter from the rat's upgrades; takes the rat, returns a snapshot of the whitelist and blacklist.
		private static FdFilter fromRat(TamedRat rat) {
			try {
				Set<String> whitelist = readFilterIds(rat, RatsItemRegistry.RAT_UPGRADE_WHITELIST.get());
				Set<String> blacklist = readFilterIds(rat, RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get());
				return new FdFilter(RatUpgradeUtils.hasUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_WHITELIST.get()), whitelist, blacklist);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Rat {} failed to read the FD lists", rat.getUUID(), exception);
				throw exception;
			}
		}

		// Reports whether the whitelist is enabled; no parameters, returns true if a whitelist upgrade is present.
		private boolean hasWhitelist() {
			try {
				return this.whitelistPresent;
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to check for the FD whitelist", exception);
				throw exception;
			}
		}

		// Checks whether an item is whitelisted; takes the stack, returns true if its id is listed.
		private boolean isWhitelisted(ItemStack stack) {
			try {
				return this.whitelistIds.contains(getItemId(stack));
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to check the FD whitelist for {}", stack, exception);
				throw exception;
			}
		}

		// Checks whether an item is blacklisted; takes the stack, returns true if its id is listed.
		private boolean isBlacklisted(ItemStack stack) {
			try {
				return this.blacklistIds.contains(getItemId(stack));
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to check the FD blacklist for {}", stack, exception);
				throw exception;
			}
		}

		// Checks the input and output of a stove or board recipe; takes the input and output, returns true if the operation is allowed.
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
				RatsMod.LOGGER.error("Failed to check FD processing for input {} and output {}", input, output, exception);
				throw exception;
			}
		}

		// Checks an ingredient of a regular recipe; takes the stack and a target-output flag, returns true if the ingredient may be taken.
		private boolean allowsIngredient(ItemStack stack, boolean outputTargeted) {
			try {
				if (this.isBlacklisted(stack)) {
					return false;
				}
				return !this.whitelistPresent || outputTargeted || this.isWhitelisted(stack);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to check FD ingredient {}", stack, exception);
				throw exception;
			}
		}

		// Checks an ingredient of the target pot recipe; takes the stack, returns true if it is not blacklisted.
		private boolean allowsIngredientForTargetRecipe(ItemStack stack) {
			try {
				return !this.isBlacklisted(stack);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to check FD target recipe ingredient {}", stack, exception);
				throw exception;
			}
		}

		// Checks a meal container; takes the stack, returns true if the container is not blacklisted.
		private boolean allowsContainer(ItemStack stack) {
			try {
				return !this.isBlacklisted(stack);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to check FD container {}", stack, exception);
				throw exception;
			}
		}

		// Checks a board tool; takes the stack, returns true if the tool is not blacklisted.

		// Checks a finished skillet result; takes the stack, returns true if the filters allow delivering it.
		private boolean allowsCollectedResult(ItemStack stack) {
			try {
				if (this.isBlacklisted(stack)) {
					return false;
				}
				return !this.whitelistPresent || this.isWhitelisted(stack);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to check FD finished result {}", stack, exception);
				throw exception;
			}
		}

		private boolean allowsTool(ItemStack stack) {
			try {
				return !this.isBlacklisted(stack);
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to check FD tool {}", stack, exception);
				throw exception;
			}
		}

		// Reads item ids from a list upgrade; takes the rat and the upgrade item, returns a set of ids.
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
				RatsMod.LOGGER.error("Rat {} failed to read the list of upgrade {}", rat.getUUID(), upgradeItem, exception);
				throw exception;
			}
		}

		// Gets an item id; takes the stack, returns a namespace:path string.
		private static String getItemId(ItemStack stack) {
			try {
				return Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(stack.getItem())).toString();
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Failed to get FD item id {}", stack, exception);
				throw exception;
			}
		}
	}
}
