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

	// Prevents instantiating the service; takes nothing and always reports an error.
	private RatInventoryAccess() {
		throw new IllegalStateException("The inventory access service cannot be instantiated");
	}

	// Checks item insertion; takes the level, position, side, stack, rat and action, returns true if the container will accept part of the stack.
	public static boolean canInsert(Level level, BlockPos pos, Direction preferredSide, ItemStack stack, TamedRat rat, String action) {
		try {
			if (stack.isEmpty()) {
				RatsMod.LOGGER.debug("Rat {} does not check insertion into {}: the stack is empty, action {}", rat.getUUID(), pos, action);
				return false;
			}
			for (Direction side : orderedSides(preferredSide)) {
				IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
				if (handler == null) {
					RatsMod.LOGGER.debug("Rat {}: block {} has no inventory on side {} for {}", rat.getUUID(), pos, side, action);
					continue;
				}
				ItemStack simulated = ItemHandlerHelper.insertItem(handler, stack.copy(), true);
				if (simulated.getCount() < stack.getCount()) {
					RatsMod.LOGGER.debug("Rat {}: block {} will accept items from side {} for {}", rat.getUUID(), pos, side, action);
					return true;
				}
				RatsMod.LOGGER.debug("Rat {}: block {} refused inserting {} from side {} for {}", rat.getUUID(), pos, stack, side, action);
			}
			if (canUseCreateProxy(level, pos, rat, action)) {
				RatsMod.LOGGER.debug("Rat {}: Create block {} will be used as a manual insertion point for {}", rat.getUUID(), pos, action);
				return true;
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to check inserting items into block {} for {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Inserts items into a container; takes the level, position, side, stack, rat and action, returns the stack remainder.
	public static ItemStack insert(Level level, BlockPos pos, Direction preferredSide, ItemStack stack, TamedRat rat, String action) {
		try {
			if (stack.isEmpty()) {
				return ItemStack.EMPTY;
			}
			ItemStack remaining = stack.copy();
			for (Direction side : orderedSides(preferredSide)) {
				IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
				if (handler == null) {
					RatsMod.LOGGER.debug("Rat {}: cannot insert into {} from side {} for {}: no handler", rat.getUUID(), pos, side, action);
					continue;
				}
				ItemStack before = remaining.copy();
				remaining = ItemHandlerHelper.insertItem(handler, remaining, false);
				int moved = before.getCount() - remaining.getCount();
				if (moved > 0) {
					RatsMod.LOGGER.debug("Rat {} put {} items into {} via side {} for {}", rat.getUUID(), moved, pos, side, action);
				}
				if (remaining.isEmpty()) {
					return ItemStack.EMPTY;
				}
			}
			ItemStack createRemaining = insertWithCreateProxy(level, pos, remaining, rat, action);
			if (createRemaining.getCount() < remaining.getCount()) {
				return createRemaining;
			}
			RatsMod.LOGGER.debug("Rat {} could not fully put items into {} for {}, remainder {}", rat.getUUID(), pos, action, remaining);
			return remaining;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to insert items into block {} for {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Checks extraction of suitable items; takes the level, position, side, rat and action, returns true if there is a suitable slot.
	public static boolean canExtract(Level level, BlockPos pos, Direction preferredSide, TamedRat rat, String action) {
		try {
			for (Direction side : orderedSides(preferredSide)) {
				IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
				if (handler == null) {
					RatsMod.LOGGER.debug("Rat {}: block {} has no inventory on side {} for {}", rat.getUUID(), pos, side, action);
					continue;
				}
				int slot = RatUtils.getItemSlotFromItemHandler(rat, handler, level.getRandom());
				if (slot >= 0) {
					RatsMod.LOGGER.debug("Rat {}: block {} will provide items from side {} from slot {} for {}", rat.getUUID(), pos, side, slot, action);
					return true;
				}
				RatsMod.LOGGER.debug("Rat {}: block {} has no suitable items on side {} for {}", rat.getUUID(), pos, side, action);
			}
			if (canUseCreateProxy(level, pos, rat, action)) {
				RatsMod.LOGGER.debug("Rat {}: Create block {} will be used as a manual pickup point for {}", rat.getUUID(), pos, action);
				return true;
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to check extracting items from block {} for {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Extracts suitable items; takes the level, position, side, rat, stack size and action, returns the extracted stack.
	public static ItemStack extract(Level level, BlockPos pos, Direction preferredSide, TamedRat rat, int amount, String action) {
		try {
			if (amount <= 0) {
				throw new IllegalArgumentException("The extraction amount must be positive: " + amount);
			}
			for (Direction side : orderedSides(preferredSide)) {
				IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
				if (handler == null) {
					RatsMod.LOGGER.debug("Rat {}: cannot take from {} on side {} for {}: no handler", rat.getUUID(), pos, side, action);
					continue;
				}
				int slot = RatUtils.getItemSlotFromItemHandler(rat, handler, level.getRandom());
				if (slot < 0) {
					RatsMod.LOGGER.debug("Rat {}: no suitable slot in {} on side {} for {}", rat.getUUID(), pos, side, action);
					continue;
				}
				ItemStack simulated = handler.extractItem(slot, amount, true);
				if (simulated.isEmpty()) {
					RatsMod.LOGGER.debug("Rat {}: slot {} in {} on side {} refused extraction for {}", rat.getUUID(), slot, pos, side, action);
					continue;
				}
				ItemStack extracted = handler.extractItem(slot, amount, false);
				if (!extracted.isEmpty()) {
					RatsMod.LOGGER.debug("Rat {} took {} items from {} via side {} for {}", rat.getUUID(), extracted.getCount(), pos, side, action);
					return extracted;
				}
			}
			ItemStack createExtracted = extractWithCreateProxy(level, pos, rat, amount, action);
			if (!createExtracted.isEmpty()) {
				return createExtracted;
			}
			return ItemStack.EMPTY;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to extract items from block {} for {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Builds the side order; takes the preferred side, returns an array with it first and the rest after it.
	private static Direction[] orderedSides(Direction preferredSide) {
		try {
			return ORDERED_SIDES[preferredSide.ordinal()];
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to build the inventory side order, preferred side {}", preferredSide, exception);
			throw exception;
		}
	}

	// Builds the side order cache for every preferred side; takes nothing, returns a direction table.
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
			RatsMod.LOGGER.error("Failed to build the inventory side order cache", exception);
			throw exception;
		}
	}

	// Checks manual Create access; takes the level, position, rat and action, returns true if the worker can use the block.
	private static boolean canUseCreateProxy(Level level, BlockPos pos, TamedRat rat, String action) {
		try {
			boolean canUse = rat.hasWorkerUpgrade()
					&& CreateCompat.canWorkerUse(level.getBlockState(pos));
			if (!canUse) {
				RatsMod.LOGGER.debug("Rat {} does not use the Create proxy for {}: no worker upgrade or the block is not supported", rat.getUUID(), action);
			}
			return canUse;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to check the Create proxy of block {} for {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Inserts an item via a manual Create action; takes the level, position, stack, rat and action, returns the remainder.
	private static ItemStack insertWithCreateProxy(Level level, BlockPos pos, ItemStack stack, TamedRat rat, String action) {
		try {
			if (!canUseCreateProxy(level, pos, rat, action)) {
				return stack;
			}
			rat.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack.copy());
			boolean worked = CreateCompat.workBlock(rat, pos);
			ItemStack remaining = rat.getMainHandItem().copy();
			if (worked && remaining.getCount() < stack.getCount()) {
				RatsMod.LOGGER.debug("Rat {} put items into Create block {} via a manual action for {}", rat.getUUID(), pos, action);
				return remaining;
			}
			return stack;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to insert items via Create proxy {} for {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}

	// Takes an item via a manual Create action; takes the level, position, rat, amount and action, returns the obtained stack.
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
				RatsMod.LOGGER.debug("Rat {} took items from Create block {} via a manual action for {}", rat.getUUID(), pos, action);
				return extracted;
			}
			return ItemStack.EMPTY;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to take items via Create proxy {} for {}", rat.getUUID(), pos, action, exception);
			throw exception;
		}
	}
}