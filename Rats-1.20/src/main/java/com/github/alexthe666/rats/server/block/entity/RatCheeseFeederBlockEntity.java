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

	// Создаёт хранилище кормушки; принимает позицию и состояние блока.
	public RatCheeseFeederBlockEntity(BlockPos pos, BlockState state) {
		super(RatsBlockEntityRegistry.RAT_CHEESE_FEEDER.get(), pos, state);
	}

	// Проверяет, является ли предмет допустимым сыром; принимает стек, отдаёт true для сырных предметов.
	public static boolean isAllowedCheese(ItemStack stack) {
		try {
			return !stack.isEmpty() && (stack.is(RatsItemTags.CHEESE_ITEMS) || stack.is(RatsItemRegistry.CREATIVE_CHEESE.get()));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить сырный предмет {}", stack, exception);
			throw exception;
		}
	}

	// Тикает кормушку; принимает мир, позицию, состояние и block entity, ничего не отдаёт.
	public static void tick(Level level, BlockPos pos, BlockState state, RatCheeseFeederBlockEntity feeder) {
		try {
			if (!level.isClientSide() && state.getValue(RatCheeseFeederBlock.HAS_CHEESE) != feeder.hasCheese()) {
				feeder.updateCheeseState();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось обновить состояние кормушки {}", pos, exception);
			throw exception;
		}
	}

	// Добавляет сыр из стека игрока; принимает стек, отдаёт количество перемещённых предметов.
	public int addCheeseFrom(ItemStack source) {
		try {
			if (!isAllowedCheese(source)) {
				throw new IllegalArgumentException("Кормушка принимает только сыр: " + source);
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
			RatsMod.LOGGER.error("Не удалось добавить сыр в кормушку {}", this.getBlockPos(), exception);
			throw exception;
		}
	}
		// Забирает сыр для крысы; принимает нужное количество, отдаёт стек сыра или пустой стек.
	public ItemStack takeCheeseForRat(int amount) {
		try {
			if (amount <= 0) {
				throw new IllegalArgumentException("Количество сыра для выдачи должно быть положительным: " + amount);
			}
			for (int slot = 0; slot < this.getContainerSize(); slot++) {
				ItemStack stack = this.getItem(slot);
				if (!stack.isEmpty() && isAllowedCheese(stack)) {
					ItemStack removed = this.removeItem(slot, Math.min(amount, stack.getCount()));
					if (removed.isEmpty()) {
						throw new IllegalStateException("Кормушка показала сыр, но не смогла выдать его из слота " + slot);
					}
					return removed;
				}
			}
			return ItemStack.EMPTY;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выдать сыр крысе из кормушки {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Забирает один сыр для кормления крысы; ничего не принимает, отдаёт стек из одного сыра или пустой стек.
	public ItemStack takeOneCheeseForRat() {
		try {
			return this.takeCheeseForRat(1);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выдать один сыр крысе из кормушки {}", this.getBlockPos(), exception);
			throw exception;
		}
	}// Проверяет наличие сыра в кормушке; ничего не принимает, отдаёт true если есть хотя бы один сыр.
	public boolean hasCheese() {
		try {
			for (ItemStack stack : this.inventory) {
				if (!stack.isEmpty()) {
					return true;
				}
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить наличие сыра в кормушке {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает предметный обработчик кормушки; принимает сторону, отдаёт IItemHandler.
	public IItemHandler getItemHandler(@Nullable Direction side) {
		try {
			return this.itemHandler;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить item capability кормушки {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает число слотов кормушки; ничего не принимает.
	@Override
	public int getContainerSize() {
		return this.inventory.size();
	}

	// Проверяет пустоту кормушки; ничего не принимает.
	@Override
	public boolean isEmpty() {
		return !this.hasCheese();
	}

	// Возвращает предмет из слота; принимает индекс слота.
	@Override
	public ItemStack getItem(int index) {
		return this.inventory.get(index);
	}

	// Удаляет часть предметов из слота; принимает индекс и количество.
	@Override
	public ItemStack removeItem(int index, int count) {
		try {
			ItemStack removed = ContainerHelper.removeItem(this.inventory, index, count);
			if (!removed.isEmpty()) {
				this.setChanged();
			}
			return removed;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось забрать сыр из кормушки {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Удаляет весь стек из слота без дополнительных проверок; принимает индекс слота.
	@Override
	public ItemStack removeItemNoUpdate(int index) {
		try {
			return ContainerHelper.takeItem(this.inventory, index);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось тихо забрать сыр из кормушки {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Записывает предмет в слот; принимает индекс и стек, ничего не отдаёт.
	@Override
	public void setItem(int index, ItemStack stack) {
		try {
			if (!stack.isEmpty() && !isAllowedCheese(stack)) {
				throw new IllegalArgumentException("Кормушка принимает только сыр: " + stack);
			}
			this.inventory.set(index, stack);
			if (stack.getCount() > this.getMaxStackSize()) {
				stack.setCount(this.getMaxStackSize());
			}
			this.setChanged();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось положить предмет в кормушку {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет, можно ли положить предмет в слот; принимает индекс и стек.
	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return isAllowedCheese(stack);
	}

	// Возвращает слоты, доступные со стороны; принимает сторону.
	@Override
	public int[] getSlotsForFace(Direction direction) {
		return SLOTS;
	}

	// Проверяет вставку автоматизацией; принимает слот, стек и сторону.
	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
		try {
			boolean allowed = this.canPlaceItem(index, stack);
			if (!allowed) {
				RatsMod.LOGGER.debug("Кормушка {} отказала автоматизации со стороны {}: предмет {} не является сыром", this.getBlockPos(), direction, stack);
			}
			return allowed;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить вставку автоматизацией в кормушку {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет извлечение автоматизацией; принимает слот, стек и сторону.
	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
		return true;
	}

	// Проверяет доступ игрока к кормушке; принимает игрока.
	@Override
	public boolean stillValid(Player player) {
		return this.getLevel() != null
				&& this.getLevel().getBlockEntity(this.getBlockPos()) == this
				&& player.distanceToSqr(this.getBlockPos().getX() + 0.5D, this.getBlockPos().getY() + 0.5D, this.getBlockPos().getZ() + 0.5D) <= 64.0D;
	}

	// Очищает инвентарь кормушки; ничего не принимает.
	@Override
	public void clearContent() {
		try {
			this.inventory.clear();
			this.setChanged();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось очистить кормушку {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает список предметов для BaseContainerBlockEntity; ничего не принимает.
	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.inventory;
	}

	// Заменяет список предметов при загрузке; принимает новый список, ничего не отдаёт.
	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.inventory = items;
	}

	// Загружает кормушку из NBT; принимает тег и доступ к реестрам, ничего не отдаёт.
	@Override
	protected void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {
		try {
			super.loadAdditional(compound, registries);
			this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
			ContainerHelper.loadAllItems(compound, this.inventory, registries);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось загрузить кормушку {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Сохраняет кормушку в NBT; принимает тег и доступ к реестрам, ничего не отдаёт.
	@Override
	protected void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
		try {
			super.saveAdditional(compound, registries);
			ContainerHelper.saveAllItems(compound, this.inventory, registries);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось сохранить кормушку {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает имя меню кормушки; ничего не принимает.
	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.rats.rat_cheese_feeder");
	}

	// Создаёт серверное меню кормушки; принимает id и инвентарь игрока.
	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory player) {
		return new ChestMenu(MenuType.GENERIC_9x1, id, player, this, 1);
	}

	// Создаёт меню для игрока; принимает id, инвентарь игрока и игрока.
	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new ChestMenu(MenuType.GENERIC_9x1, id, playerInventory, this, 1);
	}

	// Помечает кормушку изменённой и обновляет видимость сыра; ничего не принимает.
	@Override
	public void setChanged() {
		super.setChanged();
		try {
			this.updateCheeseState();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось применить состояние сыра кормушки {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Обновляет blockstate has_cheese по содержимому; ничего не принимает.
	private void updateCheeseState() {
		try {
			Level level = this.getLevel();
			if (level == null || level.isClientSide()) {
				return;
			}
			BlockState state = this.getBlockState();
			if (!state.hasProperty(RatCheeseFeederBlock.HAS_CHEESE)) {
				throw new IllegalStateException("У блока кормушки нет свойства has_cheese");
			}
			boolean hasCheese = this.hasCheese();
			if (state.getValue(RatCheeseFeederBlock.HAS_CHEESE) != hasCheese) {
				level.setBlock(this.getBlockPos(), state.setValue(RatCheeseFeederBlock.HAS_CHEESE, hasCheese), Block.UPDATE_ALL);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось обновить модель кормушки {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Создаёт пакет синхронизации block entity; ничего не принимает.
	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag);
	}

	// Обрабатывает пакет синхронизации block entity; принимает соединение, пакет и доступ к реестрам.
	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
		super.onDataPacket(net, packet, registries);
		this.handleUpdateTag(packet.getTag(), registries);
	}

	// Возвращает тег синхронизации для клиента; принимает доступ к реестрам.
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithoutMetadata(registries);
	}
}
