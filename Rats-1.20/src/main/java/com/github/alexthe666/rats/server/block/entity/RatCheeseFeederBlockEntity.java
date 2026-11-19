package com.github.alexthe666.rats.server.block.entity;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.data.tags.RatsItemTags;
import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.block.RatCheeseFeederBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public class RatCheeseFeederBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	private static final int SLOT_COUNT = 9;
	private static final int[] SLOTS = IntStream.range(0, SLOT_COUNT).toArray();
	private final IItemHandler itemHandler = new SidedInvWrapper(this, Direction.UP);
	private NonNullList<ItemStack> inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

	// Creates the feeder storage; takes the block position and state.
	public RatCheeseFeederBlockEntity(BlockPos pos, BlockState state) {
		super(RatsBlockEntityRegistry.RAT_CHEESE_FEEDER.get(), pos, state);
	}

	// Checks whether an item is valid cheese; takes the stack, returns true for cheese items.
	public static boolean isAllowedCheese(ItemStack stack) {
		try {
			return !stack.isEmpty() && (stack.is(RatsItemTags.CHEESE_ITEMS) || stack.is(RatsItemRegistry.CREATIVE_CHEESE.get()));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check cheese item {}", stack, exception);
			throw exception;
		}
	}

	// Ticks the feeder; takes the level, position, state and block entity, returns nothing.
	public static void tick(Level level, BlockPos pos, BlockState state, RatCheeseFeederBlockEntity feeder) {
		try {
			if (!level.isClientSide() && state.getValue(RatCheeseFeederBlock.HAS_CHEESE) != feeder.hasCheese()) {
				feeder.updateCheeseState();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to update the feeder state {}", pos, exception);
			throw exception;
		}
	}

	// Adds cheese from the player's stack; takes the stack, returns the number of items moved.
	public int addCheeseFrom(ItemStack source) {
		try {
			if (!isAllowedCheese(source)) {
				throw new IllegalArgumentException("The feeder only accepts cheese: " + source);
			}
			int moved = 0;
			for (int slot = 0; slot < this.getContainerSize() && !source.isEmpty(); slot++) {
				ItemStack existing = this.getItem(slot);
				if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, source) && existing.getCount() < existing.getMaxStackSize()) {
					int amount = Math.min(source.getCount(), existing.getMaxStackSize() - existing.getCount());
					existing.grow(amount);
					source.shrink(amount);
					moved += amount;
					this.setChanged();
				}
			}
			for (int slot = 0; slot < this.getContainerSize() && !source.isEmpty(); slot++) {
				if (this.getItem(slot).isEmpty()) {
					int amount = Math.min(source.getCount(), source.getMaxStackSize());
					ItemStack inserted = source.split(amount);
					this.setItem(slot, inserted);
					moved += amount;
				}
			}
			return moved;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to add cheese to feeder {}", this.getBlockPos(), exception);
			throw exception;
		}
	}
		// Takes cheese for a rat; takes the needed amount, returns a cheese stack or an empty stack.
	public ItemStack takeCheeseForRat(int amount) {
		try {
			if (amount <= 0) {
				throw new IllegalArgumentException("The amount of cheese to give must be positive: " + amount);
			}
			for (int slot = 0; slot < this.getContainerSize(); slot++) {
				ItemStack stack = this.getItem(slot);
				if (!stack.isEmpty() && isAllowedCheese(stack)) {
					ItemStack removed = this.removeItem(slot, Math.min(amount, stack.getCount()));
					if (removed.isEmpty()) {
						throw new IllegalStateException("The feeder reported cheese but could not give it from the slot " + slot);
					}
					return removed;
				}
			}
			return ItemStack.EMPTY;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to give cheese to a rat from feeder {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Takes one cheese to feed a rat; takes nothing, returns a single-cheese stack or an empty stack.
	public ItemStack takeOneCheeseForRat() {
		try {
			return this.takeCheeseForRat(1);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to give one cheese to a rat from feeder {}", this.getBlockPos(), exception);
			throw exception;
		}
	}// Checks whether the feeder has cheese; takes nothing, returns true if there is at least one cheese.
	public boolean hasCheese() {
		try {
			for (ItemStack stack : this.inventory) {
				if (!stack.isEmpty()) {
					return true;
				}
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check whether feeder {} has cheese", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the feeder item handler; takes the side, returns an IItemHandler.
	public IItemHandler getItemHandler(@Nullable Direction side) {
		try {
			return this.itemHandler;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to get the item capability of feeder {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the number of feeder slots; takes nothing.
	@Override
	public int getContainerSize() {
		return this.inventory.size();
	}

	// Checks whether the feeder is empty; takes nothing.
	@Override
	public boolean isEmpty() {
		return !this.hasCheese();
	}

	// Returns the item in a slot; takes the slot index.
	@Override
	public ItemStack getItem(int index) {
		return this.inventory.get(index);
	}

	// Removes some items from a slot; takes the index and count.
	@Override
	public ItemStack removeItem(int index, int count) {
		try {
			ItemStack removed = ContainerHelper.removeItem(this.inventory, index, count);
			if (!removed.isEmpty()) {
				this.setChanged();
			}
			return removed;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to take cheese from feeder {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Removes the whole stack from a slot without extra checks; takes the slot index.
	@Override
	public ItemStack removeItemNoUpdate(int index) {
		try {
			return ContainerHelper.takeItem(this.inventory, index);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to silently take cheese from feeder {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Writes an item into a slot; takes the index and stack, returns nothing.
	@Override
	public void setItem(int index, ItemStack stack) {
		try {
			if (!stack.isEmpty() && !isAllowedCheese(stack)) {
				throw new IllegalArgumentException("The feeder only accepts cheese: " + stack);
			}
			this.inventory.set(index, stack);
			if (stack.getCount() > this.getMaxStackSize()) {
				stack.setCount(this.getMaxStackSize());
			}
			this.setChanged();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to put an item into feeder {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks whether an item may be placed into a slot; takes the index and stack.
	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return isAllowedCheese(stack);
	}

	// Returns the slots accessible from a side; takes the side.
	@Override
	public int[] getSlotsForFace(Direction direction) {
		return SLOTS;
	}

	// Checks automated insertion; takes the slot, stack and side.
	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
		try {
			boolean allowed = this.canPlaceItem(index, stack);
			if (!allowed) {
				RatsMod.LOGGER.debug("Feeder {} refused automation from side {}: item {} is not cheese", this.getBlockPos(), direction, stack);
			}
			return allowed;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check automated insertion into feeder {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks automated extraction; takes the slot, stack and side.
	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
		return true;
	}

	// Checks player access to the feeder; takes the player.
	@Override
	public boolean stillValid(Player player) {
		return this.getLevel() != null
				&& this.getLevel().getBlockEntity(this.getBlockPos()) == this
				&& player.distanceToSqr(this.getBlockPos().getX() + 0.5D, this.getBlockPos().getY() + 0.5D, this.getBlockPos().getZ() + 0.5D) <= 64.0D;
	}

	// Clears the feeder inventory; takes nothing.
	@Override
	public void clearContent() {
		try {
			this.inventory.clear();
			this.setChanged();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to clear feeder {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the item list for BaseContainerBlockEntity; takes nothing.
	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.inventory;
	}

	// Replaces the item list on load; takes the new list, returns nothing.
	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.inventory = items;
	}

	// Loads the feeder from NBT; takes the tag and registry access, returns nothing.
	@Override
	protected void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {
		try {
			super.loadAdditional(compound, registries);
			this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
			ContainerHelper.loadAllItems(compound, this.inventory, registries);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to load feeder {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Saves the feeder to NBT; takes the tag and registry access, returns nothing.
	@Override
	protected void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
		try {
			super.saveAdditional(compound, registries);
			ContainerHelper.saveAllItems(compound, this.inventory, registries);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to save feeder {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the feeder menu name; takes nothing.
	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.rats.rat_cheese_feeder");
	}

	// Creates the server-side feeder menu; takes the id and player inventory.
	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory player) {
		return new ChestMenu(MenuType.GENERIC_9x1, id, player, this, 1);
	}

	// Creates the menu for a player; takes the id, player inventory and player.
	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new ChestMenu(MenuType.GENERIC_9x1, id, playerInventory, this, 1);
	}

	// Marks the feeder as changed and updates cheese visibility; takes nothing.
	@Override
	public void setChanged() {
		super.setChanged();
		try {
			this.updateCheeseState();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to apply the cheese state of feeder {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Updates the has_cheese blockstate from the contents; takes nothing.
	private void updateCheeseState() {
		try {
			Level level = this.getLevel();
			if (level == null || level.isClientSide()) {
				return;
			}
			BlockState state = this.getBlockState();
			if (!state.hasProperty(RatCheeseFeederBlock.HAS_CHEESE)) {
				throw new IllegalStateException("The feeder block has no has_cheese property");
			}
			boolean hasCheese = this.hasCheese();
			if (state.getValue(RatCheeseFeederBlock.HAS_CHEESE) != hasCheese) {
				level.setBlock(this.getBlockPos(), state.setValue(RatCheeseFeederBlock.HAS_CHEESE, hasCheese), Block.UPDATE_ALL);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to update the model of feeder {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Creates the block entity sync packet; takes nothing.
	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag);
	}

	// Handles the block entity sync packet; takes the connection, packet and registry access.
	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
		super.onDataPacket(net, packet, registries);
		this.handleUpdateTag(packet.getTag(), registries);
	}

	// Returns the sync tag for the client; takes registry access.
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithoutMetadata(registries);
	}
}
