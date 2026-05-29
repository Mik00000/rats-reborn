package com.github.alexthe666.rats.server.block.entity;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.data.tags.RatsBlockTags;
import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

public class RatQuarryBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	private static final int[] STACKS = IntStream.range(0, 64).toArray();
	private static final int RESERVATION_TIMEOUT_TICKS = 200;
	// Возраст брони, после которого цель слоя считается зависшей и снимается для добивки слоя.
	private static final int STALE_LAYER_RESERVATION_TICKS = 80;
	// Порог оставшихся блоков слоя, при котором включается режим добивки с поиском любой достижимой стойки.
	private static final int LAYER_CLEANUP_UNMINED_THRESHOLD = 6;
	// Максимум одновременно назначенных крыс на добычу одного слоя карьера.
	private static final int MAX_PARALLEL_LAYER_WORKERS = 10;
	// Максимальная дистанция добычи блока слоя от позиции ног крысы при добивке.
	private static final double LAYER_CLEANUP_REACH_DISTANCE_SQR = 20.0D;
	// Дополнительный радиус герметизации жидкостей вокруг рабочей области 5x5: даёт защитный контур 7x7.
	private static final int FLUID_SEAL_EXTRA_RADIUS = 1;
	private static final String TAG_PLAYER_PLACED_BLOCKS = "PlayerPlacedBlocks";
	// Допуск по высоте, в пределах которого крыса сходит с шахты обычной навигацией, без привязки к столбу.
	public static final int COLUMN_EXIT_HEIGHT_DELTA = 1;
	private final IItemHandler topItemHandler = new SidedInvWrapper(this, Direction.UP);
	private final IItemHandler bottomItemHandler = new SidedInvWrapper(this, Direction.DOWN);
	private final IItemHandler sideItemHandler = new SidedInvWrapper(this, Direction.NORTH);
	private final Map<UUID, BlockPos> reservedTargets = new HashMap<>();
	private final Map<UUID, Integer> reservedAges = new HashMap<>();
	private final Set<Long> playerPlacedBlocks = new HashSet<>();
	private NonNullList<ItemStack> inventory = NonNullList.withSize(64, ItemStack.EMPTY);
	private int tick;

	public RatQuarryBlockEntity(BlockPos pos, BlockState state) {
		super(RatsBlockEntityRegistry.RAT_QUARRY.get(), pos, state);
	}

	@Override
	public int getContainerSize() {
		return this.inventory.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.inventory) {
			if (!itemstack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getItem(int index) {
		return this.inventory.get(index);
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		return ContainerHelper.removeItem(this.inventory, index, count);
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		return ContainerHelper.takeItem(this.inventory, index);
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return STACKS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
		return this.canPlaceItem(index, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
		return true;
	}

	public void setItem(int index, ItemStack stack) {
		this.inventory.set(index, stack);

		if (stack.getCount() > this.getMaxStackSize()) {
			stack.setCount(this.getMaxStackSize());
		}
		this.setChanged();
	}

	public boolean stillValid(Player player) {
		if (player.level().getBlockEntity(this.getBlockPos()) != this) {
			return false;
		} else {
			return player.distanceToSqr((double) this.getBlockPos().getX() + 0.5D, (double) this.getBlockPos().getY() + 0.5D, (double) this.getBlockPos().getZ() + 0.5D) <= 64.0D;
		}
	}

	@Override
	public void clearContent() {
		this.inventory.clear();
	}


	public static void tick(Level level, BlockPos pos, BlockState state, RatQuarryBlockEntity te) {
		if (!level.isClientSide()) {
			te.tick++;
			if (te.tick % 20 == 0) {
				te.cleanupReservations(level);
				te.maintainSupportShaft(level);
			}
		}
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.inventory;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.inventory = items;
	}

	@Override
	protected void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {
		super.loadAdditional(compound, registries);
		this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.inventory, registries);
		this.playerPlacedBlocks.clear();
		if (compound.contains(TAG_PLAYER_PLACED_BLOCKS)) {
			for (long packedPos : compound.getLongArray(TAG_PLAYER_PLACED_BLOCKS)) {
				this.playerPlacedBlocks.add(packedPos);
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
		super.saveAdditional(compound, registries);
		ContainerHelper.saveAllItems(compound, this.inventory, registries);
		compound.putLongArray(TAG_PLAYER_PLACED_BLOCKS, this.playerPlacedBlocks.stream().mapToLong(Long::longValue).toArray());
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable(RatsLangConstants.QUARRY);
	}

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory player) {
		return new ChestMenu(MenuType.GENERIC_9x6, id, player, this, 6);
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new ChestMenu(MenuType.GENERIC_9x6, id, playerInventory, this, 6);
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	// Возвращает предметный обработчик карьера для указанной стороны; принимает сторону, отдаёт IItemHandler.
	public IItemHandler getItemHandler(@Nullable Direction facing) {
		try {
			if (this.remove) {
				throw new IllegalStateException("Нельзя получить item capability удалённого карьера");
			}
			if (facing == Direction.UP) {
				return this.topItemHandler;
			}
			if (facing == Direction.DOWN) {
				return this.bottomItemHandler;
			}
			return this.sideItemHandler;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить item capability карьера на {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
		super.onDataPacket(net, packet, registries);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithId(registries);
	}

	public int getRadius() {
		return 2;
	}

	// Проверяет, находится ли позиция в рабочем объёме карьера; принимает позицию блока, отдаёт true для зоны 5x5 под карьером.
	public boolean isInsideWorkVolume(BlockPos pos) {
		try {
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius();
			return pos.getY() < quarry.getY()
					&& Math.abs(pos.getX() - quarry.getX()) <= radius
					&& Math.abs(pos.getZ() - quarry.getZ()) <= radius;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить рабочий объём карьера {} для {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Проверяет объём герметизации жидкостей; принимает позицию блока, отдаёт true для защитного контура 7x7 под карьером.
	public boolean isInsideFluidSealVolume(BlockPos pos) {
		try {
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius() + FLUID_SEAL_EXTRA_RADIUS;
			return pos.getY() < quarry.getY()
					&& Math.abs(pos.getX() - quarry.getX()) <= radius
					&& Math.abs(pos.getZ() - quarry.getZ()) <= radius;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить объём герметизации карьера {} для {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Проверяет служебный блок карьера; принимает позицию, отдаёт true для карьера, платформы или столба поддержки.
	public boolean isQuarryServiceBlock(Level level, BlockPos pos) {
		try {
			if (pos.equals(this.getBlockPos())) {
				return true;
			}
			if (this.isSupportColumnPos(pos)) {
				return true;
			}
			return level.getBlockState(pos).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить служебный блок {} карьера {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет блок, поставленный игроком в зоне карьера; принимает позицию, отдаёт true если крысы должны его пропускать.
	public boolean isPlayerPlacedBlock(BlockPos pos) {
		try {
			return this.playerPlacedBlocks.contains(pos.asLong());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить блок игрока {} карьера {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Запоминает блок, поставленный игроком в рабочей зоне; принимает уровень и позицию, ничего не возвращает.
	public void markPlayerPlacedBlock(Level level, BlockPos pos) {
		try {
			if (!this.isInsideWorkVolume(pos) || this.isQuarryServiceBlock(level, pos)) {
				return;
			}
			if (this.playerPlacedBlocks.add(pos.asLong())) {
				this.reservedTargets.entrySet().removeIf(entry -> entry.getValue().equals(pos));
				this.setChanged();
				RatsMod.LOGGER.info("Карьер {} пропускает блок игрока на {}", this.getBlockPos(), pos);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось пометить блок игрока {} для карьера {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Снимает пометку блока игрока после разрушения; принимает позицию, ничего не возвращает.
	public void clearPlayerPlacedBlock(BlockPos pos) {
		try {
			if (this.playerPlacedBlocks.remove(pos.asLong())) {
				this.setChanged();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось снять пометку блока игрока {} у карьера {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Находит безопасный горизонтальный шаг внутри карьера; принимает уровень, текущую и целевую позиции ног, отдаёт соседнюю клетку или null.
	public @Nullable BlockPos findSafeHorizontalGridStep(Level level, BlockPos current, BlockPos target) {
		try {
			if (current.equals(target)) {
				return null;
			}
			int stepX = Integer.compare(target.getX(), current.getX());
			int stepZ = Integer.compare(target.getZ(), current.getZ());
			BlockPos first = Math.abs(target.getX() - current.getX()) >= Math.abs(target.getZ() - current.getZ()) && stepX != 0 ? current.offset(stepX, 0, 0) : current.offset(0, 0, stepZ);
			BlockPos second = first.getX() == current.getX() && stepX != 0 ? current.offset(stepX, 0, 0) : current.offset(0, 0, stepZ);
			if (this.isValidGridStep(level, first)) {
				return first;
			}
			if (!second.equals(current) && this.isValidGridStep(level, second)) {
				return second;
			}
			BlockPos best = null;
			double bestDistance = Double.MAX_VALUE;
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				BlockPos candidate = current.relative(direction);
				if (!this.isValidGridStep(level, candidate)) {
					continue;
				}
				double distance = candidate.distSqr(target);
				if (distance < current.distSqr(target) && distance < bestDistance) {
					best = candidate;
					bestDistance = distance;
				}
			}
			if (best != null) {
				return best;
			}
			BlockPos columnStep = this.getPrimaryAccessColumnStandPos(current.getY());
			if (!columnStep.equals(current) && this.isValidGridStep(level, columnStep)) {
				return columnStep;
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти горизонтальный шаг карьера {} из {} к {}", this.getBlockPos(), current, target, exception);
			throw exception;
		}
	}

	// Проверяет безопасную клетку для сеточного шага; принимает уровень и позицию ног, отдаёт true если шаг допустим.
	private boolean isValidGridStep(Level level, BlockPos next) {
		try {
			if (this.isAccessColumnStandPos(next)) {
				return this.hasAccessColumnPlatformBelow(level, next);
			}
			return this.isSafeQuarryStandPos(level, next);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить сеточный шаг {} карьера {}", next, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Удаляет устаревшие брони целей; принимает уровень, ничего не возвращает.
	private void cleanupReservations(Level level) {
		try {
			this.reservedAges.replaceAll((id, age) -> age + 20);
			this.reservedAges.entrySet().removeIf(entry -> entry.getValue() > RESERVATION_TIMEOUT_TICKS || !this.isReservedTargetStillValid(level, this.reservedTargets.get(entry.getKey()), entry.getKey()));
			this.reservedTargets.keySet().removeIf(id -> !this.reservedAges.containsKey(id));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось очистить брони целей карьера {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет защиту столба поддержки; принимает позицию, отдаёт true для служебного блока карьера.
	public boolean isQuarryProtected(BlockPos pos) {
		try {
			return this.isSupportColumnPos(pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить защиту карьера на {}", pos, exception);
			throw exception;
		}
	}

	// Проверяет защиту лестницы любым карьером в соседних чанках; принимает уровень и позицию, отдаёт true если блок нельзя ломать.
	public static boolean isProtectedByNearbyQuarry(Level level, BlockPos pos) {
		try {
			int chunkX = pos.getX() >> 4;
			int chunkZ = pos.getZ() >> 4;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (!level.hasChunk(chunkX + dx, chunkZ + dz)) {
						continue;
					}
					for (BlockEntity blockEntity : level.getChunk(chunkX + dx, chunkZ + dz).getBlockEntities().values()) {
						if (blockEntity instanceof RatQuarryBlockEntity quarry && quarry.isQuarryLadderProtected(level, pos)) {
							return true;
						}
					}
				}
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить защиту лестницы карьера на {}", pos, exception);
			throw exception;
		}
	}

	// Проверяет защиту платформ лестницы; принимает уровень и позицию, отдаёт true только для rat_quarry_platform на координатах шахты (блок карьера не защищается).
	public boolean isQuarryLadderProtected(Level level, BlockPos pos) {
		try {
			return level.getBlockState(pos).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) && this.isSupportColumnXZ(pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить защиту лестницы карьера {} на {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Бронирует следующую задачу для крысы; принимает уровень, UUID и позицию ног крысы, отдаёт позицию блока или null.
	public @Nullable BlockPos claimNextTarget(Level level, UUID ratId, BlockPos ratPos) {
		try {
			BlockPos existing = this.reservedTargets.get(ratId);
			if (this.isReservedTargetStillValid(level, existing, ratId)) {
				this.reservedAges.put(ratId, 0);
				return existing;
			}
			this.releaseTarget(ratId);
			BlockPos supportRepair = this.findSupportRepairTarget(level, ratId, ratPos);
			if (supportRepair != null) {
				return this.reserveTarget(ratId, supportRepair);
			}
			for (int y = this.getBlockPos().getY() - 1; y > level.getMinBuildHeight(); y--) {
				if (!this.layerHasPendingWork(level, y)) {
					continue;
				}
				this.releaseStaleLayerReservations(level, y);
				for (BlockPos support : this.getSupportColumnPositions(y)) {
					if (!level.getBlockState(support).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) {
						if (!this.isReservedByOther(support, ratId)) {
							return this.reserveTarget(ratId, support);
						}
					}
				}
				BlockPos fluidTarget = this.findNextFluidSealTarget(level, y, ratId);
				if (fluidTarget != null) {
					return this.reserveTarget(ratId, fluidTarget);
				}
				if (this.countActiveLayerReservations(level, y, ratId) >= MAX_PARALLEL_LAYER_WORKERS) {
					RatsMod.LOGGER.info("Карьер {} уже держит {} крыс на слое {}, крыса {} ждёт свободную цель", this.getBlockPos(), MAX_PARALLEL_LAYER_WORKERS, y, ratId);
					return null;
				}
				BlockPos layerTarget = this.findNextSnakeTarget(level, y, ratId, ratPos);
				if (layerTarget == null) {
					layerTarget = this.findLayerStragglerTarget(level, y, ratId);
				}
				if (layerTarget == null) {
					layerTarget = this.findLayerForcedCleanupTarget(level, y, ratId);
				}
				if (layerTarget != null) {
					return this.reserveTarget(ratId, layerTarget);
				}
				if (this.layerHasPendingWork(level, y)) {
					RatsMod.LOGGER.warn("Карьер {} не смог выдать достижимую цель на слое {} (осталось {} блоков и {} жидкостей), крыса {} ждёт", this.getBlockPos(), y, this.countUnminedBlocksOnLayer(level, y), this.countFluidSealTargetsOnLayer(level, y), ratId);
					return null;
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выдать цель карьера {} крысе {}", this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Снимает бронь крысы с недостижимой цели и освобождает слой для добивки; принимает уровень, UUID и позицию, ничего не возвращает.
	public void abandonTarget(Level level, UUID ratId, BlockPos target) {
		try {
			if (target != null) {
				this.releaseStaleLayerReservations(level, target.getY());
			}
			this.releaseTarget(ratId);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось сбросить цель {} карьера {} для крысы {}", target, this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Освобождает бронь цели крысы; принимает UUID крысы, ничего не возвращает.
	public void releaseTarget(UUID ratId) {
		try {
			this.reservedTargets.remove(ratId);
			this.reservedAges.remove(ratId);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось освободить бронь цели карьера {} для крысы {}", this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Завершает цель крысы; принимает UUID и позицию, снимает бронь при совпадении.
	public void completeTarget(UUID ratId, BlockPos target) {
		try {
			if (target.equals(this.reservedTargets.get(ratId))) {
				this.releaseTarget(ratId);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось завершить цель {} карьера {} для крысы {}", target, this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Принудительно восстанавливает всю шахту поддержки; принимает уровень, ничего не возвращает.
	private void maintainSupportShaft(Level level) {
		try {
			int topY = this.getBlockPos().getY() - 1;
			int bottomY = this.getShaftMaintenanceBottomY(level);
			int restored = 0;
			for (int y = topY; y >= bottomY; y--) {
				for (BlockPos support : this.getSupportColumnPositions(y)) {
					if (!level.getBlockState(support).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) && this.placeSupport(level, support)) {
						restored++;
					}
				}
			}
			if (restored > 0) {
				RatsMod.LOGGER.info("Карьер {} принудительно восстановил {} платформ шахты между Y {} и {}", this.getBlockPos(), restored, bottomY, topY);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось принудительно восстановить шахту карьера {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает нижнюю границу принудительного обслуживания шахты; принимает уровень, отдаёт Y самого глубокого обслуживаемого слоя.
	private int getShaftMaintenanceBottomY(Level level) {
		try {
			BlockPos quarry = this.getBlockPos();
			int topY = quarry.getY() - 1;
			int activeY = this.findTopPendingWorkLayer(level);
			int bottom = activeY > level.getMinBuildHeight() ? Math.min(topY, activeY) : topY;
			while (bottom > level.getMinBuildHeight()) {
				boolean extend = false;
				for (BlockPos support : this.getSupportColumnPositions(bottom - 1)) {
					BlockPos above = support.above();
					if (level.getBlockState(above).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) && !level.getBlockState(support).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) {
						extend = true;
						break;
					}
				}
				if (!extend) {
					break;
				}
				bottom--;
			}
			return bottom;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось вычислить нижнюю границу обслуживания шахты карьера {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Находит недостающую платформу для параллельного ремонта; принимает уровень, UUID и позицию крысы, отдаёт ближайшую свободную цель или null.
	private @Nullable BlockPos findSupportRepairTarget(Level level, @Nullable UUID ratId, BlockPos ratPos) {
		try {
			List<BlockPos> candidates = this.collectSupportRepairCandidates(level, ratId);
			if (candidates.isEmpty()) {
				return null;
			}
			int start = ratId == null ? 0 : Math.floorMod(ratId.hashCode(), candidates.size());
			BlockPos best = null;
			double bestDistance = Double.MAX_VALUE;
			for (int index = 0; index < candidates.size(); index++) {
				BlockPos candidate = candidates.get((start + index) % candidates.size());
				double distance = ratPos.distSqr(candidate);
				if (best == null || distance < bestDistance) {
					best = candidate;
					bestDistance = distance;
				}
			}
			if (best != null) {
				RatsMod.LOGGER.info("Карьер {} выдал крысе {} ремонт лестницы {} со стойки {}", this.getBlockPos(), ratId, best, this.getSupportPlacementStandPos(level, best, ratPos));
			}
			return best;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти ремонт лестницы карьера {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Собирает все цели ремонта шахты; принимает уровень и UUID крысы, отдаёт список позиций без чужой брони.
	private List<BlockPos> collectSupportRepairCandidates(Level level, @Nullable UUID ratId) {
		try {
			List<BlockPos> candidates = new ArrayList<>();
			int topY = this.getBlockPos().getY() - 1;
			int bottomY = this.getShaftMaintenanceBottomY(level);
			for (int y = topY; y >= bottomY; y--) {
				for (BlockPos support : this.getSupportColumnPositions(y)) {
					if (this.needsSupportRepair(level, support, ratId)) {
						candidates.add(support);
					}
				}
			}
			return candidates;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось собрать цели ремонта шахты карьера {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Находит верхний слой, где ещё есть работа; принимает уровень, отдаёт Y слоя или minBuildHeight - 1.
	private int findTopPendingWorkLayer(Level level) {
		try {
			for (int y = this.getBlockPos().getY() - 1; y > level.getMinBuildHeight(); y--) {
				if (this.layerHasPendingWork(level, y)) {
					return y;
				}
			}
			return level.getMinBuildHeight() - 1;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти верхний рабочий слой карьера {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет, нужна ли починка платформы; принимает уровень, позицию и UUID крысы, отдаёт true для свободной ремонтной цели.
	private boolean needsSupportRepair(Level level, BlockPos support, @Nullable UUID ratId) {
		try {
			BlockState state = level.getBlockState(support);
			return !state.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())
					&& !this.isReservedByOther(support, ratId)
					&& this.canReplaceWithSupport(level, support, state)
					&& this.getSupportPlacementStandPos(level, support, null) != null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить починку платформы {} карьера {}", support, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает безопасную стойку для установки платформы; принимает уровень, цель и позицию крысы, отдаёт стойку снизу или сверху.
	public @Nullable BlockPos getSupportPlacementStandPos(Level level, BlockPos support, BlockPos ratPos) {
		try {
			if (!this.isSupportColumnPos(support)) {
				return null;
			}
			if (level.getBlockState(support).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) {
				return support.above();
			}
			BlockPos reference = ratPos == null ? support : ratPos;
			BlockPos best = null;
			double bestDistance = Double.MAX_VALUE;
			for (int floorY = support.getY() - 1; floorY > level.getMinBuildHeight(); floorY--) {
				BlockPos floor = new BlockPos(support.getX(), floorY, support.getZ());
				if (level.getBlockState(floor).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) {
					BlockPos stand = floor.above();
					double distance = reference.distSqr(stand);
					if (best == null || distance < bestDistance) {
						best = stand;
						bestDistance = distance;
					}
					break;
				}
			}
			BlockPos above = support.above();
			if (level.getBlockState(above).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) {
				BlockPos stand = above.above();
				double distance = reference.distSqr(stand);
				if (best == null || distance < bestDistance) {
					best = stand;
					bestDistance = distance;
				}
			} else if (level.getBlockState(above).isAir() || level.getBlockState(above).canBeReplaced()) {
				BlockState floor = level.getBlockState(support.below());
				if (floor.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) || (!floor.isAir() && !floor.canBeReplaced() && !floor.liquid())) {
					double distance = reference.distSqr(above);
					if (best == null || distance < bestDistance) {
						best = above;
						bestDistance = distance;
					}
				}
			}
			return best;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти стойку установки поддержки {} карьера {}", support, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет, может ли крыса поставить платформу с позиции ног; принимает стойку и цель, отдаёт true при досягаемости.
	public boolean canPlaceSupportFromStand(BlockPos standPos, BlockPos support) {
		try {
			if (standPos.equals(support)) {
				return false;
			}
			int dx = Math.abs(support.getX() - standPos.getX());
			int dy = support.getY() - standPos.getY();
			int dz = Math.abs(support.getZ() - standPos.getZ());
			return dx <= 1 && dz <= 1 && dy >= -1 && dy <= 1;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить установку поддержки {} с позиции {} карьера {}", support, standPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Ставит крысу на только что установленную платформу; принимает уровень, крысу и позицию поддержки, ничего не возвращает.
	public void snapRatOntoSupportPlatform(Level level, net.minecraft.world.entity.Entity rat, BlockPos support) {
		try {
			BlockPos stand = support.above();
			rat.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			rat.fallDistance = 0.0F;
			rat.setPos(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
			if (!level.getBlockState(stand.below()).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) {
				RatsMod.LOGGER.warn("Карьер {} не нашёл платформу под крысой после установки на {}", this.getBlockPos(), support);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось поставить крысу на платформу {} карьера {}", support, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет, есть ли под стойкой платформа шахты; принимает уровень и позицию ног, отдаёт true для безопасной клетки колонны.
	public boolean hasAccessColumnPlatformBelow(Level level, BlockPos standPos) {
		try {
			if (!this.isAccessColumnStandPos(standPos)) {
				return false;
			}
			return level.getBlockState(standPos.below()).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить опору колонны {} карьера {}", standPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Ставит поддержку в столбе; принимает уровень и позицию, отдаёт true если поддержка стоит.
	public boolean placeSupport(Level level, BlockPos pos) {
		try {
			BlockState state = level.getBlockState(pos);
			if (state.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) {
				return true;
			}
			if (!this.canReplaceWithSupport(level, pos, state)) {
				RatsMod.LOGGER.warn("Столб поддержки карьера {} заблокирован на {} блоком {}", this.getBlockPos(), pos, state.getBlock());
				return false;
			}
			level.setBlockAndUpdate(pos, RatsBlockRegistry.RAT_QUARRY_PLATFORM.get().defaultBlockState());
			this.sealFluidsAroundSupport(level, pos);
			level.playSound(null, pos, SoundEvents.BAMBOO_PLACE, SoundSource.BLOCKS);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось поставить поддержку карьера {} на {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Заменяет целевую жидкость булыжником; принимает уровень и позицию, отдаёт true если на позиции стоит твёрдая пробка.
	public boolean sealFluidBlock(Level level, BlockPos pos) {
		try {
			BlockState state = level.getBlockState(pos);
			if (!this.isFluidSealTarget(level, pos)) {
				RatsMod.LOGGER.warn("Карьер {} не стал герметизировать {}, потому что там нет жидкости в объёме 7x7: {}", this.getBlockPos(), pos, state.getBlock());
				return false;
			}
			level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
			level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS);
			this.clearPlayerPlacedBlock(pos);
			RatsMod.LOGGER.info("Карьер {} закупорил жидкость булыжником на {}", this.getBlockPos(), pos);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось герметизировать жидкость карьером {} на {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Герметизирует жидкости, открытые добычей; принимает уровень и позицию добычи, отдаёт true если поставлена хотя бы одна пробка.
	public boolean sealExposedFluidsAfterMining(Level level, BlockPos minedPos) {
		try {
			boolean sealed = false;
			sealed |= this.sealFluidForSafety(level, minedPos.below(), "под выкопанным блоком");
			sealed |= this.sealFluidForSafety(level, minedPos, "в ячейке добычи после обновления жидкости");
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				sealed |= this.sealFluidForSafety(level, minedPos.relative(direction), "рядом с выкопанным блоком");
				sealed |= this.sealFluidForSafety(level, minedPos.below().relative(direction), "рядом с жидкостью под выкопанным блоком");
			}
			if (sealed) {
				RatsMod.LOGGER.info("Карьер {} экстренно закрыл жидкость вокруг выкопанного блока {}", this.getBlockPos(), minedPos);
			}
			return sealed;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось закрыть жидкости вокруг выкопанного блока {} карьера {}", minedPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Герметизирует жидкость вокруг платформы поддержки; принимает уровень и позицию платформы, отдаёт true если поставлена пробка.
	private boolean sealFluidsAroundSupport(Level level, BlockPos supportPos) {
		try {
			boolean sealed = false;
			sealed |= this.sealFluidForSafety(level, supportPos.below(), "под платформой поддержки");
			sealed |= this.sealFluidForSafety(level, supportPos.above(), "над платформой поддержки");
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				sealed |= this.sealFluidForSafety(level, supportPos.relative(direction), "рядом с платформой поддержки");
				sealed |= this.sealFluidForSafety(level, supportPos.below().relative(direction), "рядом с основанием платформы поддержки");
			}
			return sealed;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось закрыть жидкости вокруг платформы {} карьера {}", supportPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Ставит булыжник вместо аварийной жидкости; принимает уровень, позицию и причину, отдаёт true если блок заменён.
	private boolean sealFluidForSafety(Level level, BlockPos pos, String reason) {
		try {
			BlockState state = level.getBlockState(pos);
			if (!this.isInsideFluidSealVolume(pos) || state.getFluidState().isEmpty() || state.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) || level.getBlockEntity(pos) != null) {
				return false;
			}
			level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
			this.clearPlayerPlacedBlock(pos);
			RatsMod.LOGGER.info("Карьер {} поставил аварийную пробку из булыжника на {}: {}", this.getBlockPos(), pos, reason);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось поставить аварийную пробку на {} карьером {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет, можно ли заменить блок поддержкой; принимает уровень, позицию и состояние.
	public boolean canReplaceWithSupport(Level level, BlockPos pos, BlockState state) {
		try {
			return level.getBlockEntity(pos) == null && (state.isAir() || state.canBeReplaced() || state.liquid() || state.getDestroySpeed(level, pos) >= 0.0F);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить замену блока {} поддержкой карьера {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет, является ли цель поддержкой; принимает позицию, отдаёт true для единственного столба.
	public boolean isSupportTarget(BlockPos pos) {
		try {
			return this.isSupportColumnPos(pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить цель поддержки {} карьера {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает позицию стойки для работы с целью; принимает цель, отдаёт место ног крысы.
	public BlockPos getStandPosForTarget(BlockPos target) {
		return this.getSupportStandPosForTarget(target);
	}

	// Возвращает позицию стойки для работы с целью; принимает уровень, цель и текущую позицию крысы.
	public BlockPos getStandPosForTarget(Level level, BlockPos target, BlockPos ratPos) {
		try {
			if (this.isSupportTarget(target)) {
				BlockPos placementStand = this.getSupportPlacementStandPos(level, target, ratPos);
				if (placementStand != null) {
					return placementStand;
				}
				return this.getSupportStandPosForTarget(target);
			}
			if (this.isFluidSealTarget(level, target)) {
				BlockPos fluidStand = this.findReachableFluidSealStand(level, target, ratPos);
				if (fluidStand == null) {
					throw new IllegalStateException("Нет достижимой стойки для герметизации жидкости " + target + " карьером " + this.getBlockPos());
				}
				return fluidStand;
			}
			if (this.isLastSnakeBlockOnLayer(target)) {
				BlockPos adjacentStand = this.findAdjacentLayerMiningStand(level, target, ratPos);
				if (adjacentStand != null) {
					return adjacentStand;
				}
			}
			boolean cleanupStandNeeded = this.shouldUseLayerCleanupStand(level, target);
			if (cleanupStandNeeded) {
				BlockPos adjacentStand = this.findAdjacentLayerMiningStand(level, target, ratPos);
				if (adjacentStand != null) {
					return adjacentStand;
				}
				BlockPos columnAdjacentStand = this.getColumnAdjacentMiningStandPos(level, target);
				if (columnAdjacentStand != null) {
					return columnAdjacentStand;
				}
				BlockPos cleanupStand = this.findReachableMiningStand(level, target, ratPos);
				if (cleanupStand != null) {
					return cleanupStand;
				}
				RatsMod.LOGGER.warn("Карьер {} не нашёл достижимую стойку добивки для блока {} на слое {}", this.getBlockPos(), target, target.getY());
			}
			BlockPos pathStandFloor = this.getNextSnakePathFloor(target);
			if (pathStandFloor != null) {
				BlockPos pathStand = pathStandFloor.above();
				if (this.isSafeMiningStandPos(level, pathStand) && this.canReachLayerBlockFromStand(pathStand, target)) {
					return pathStand;
				}
				RatsMod.LOGGER.warn("Карьер {} не смог использовать следующую точку змейки {} для цели {}", this.getBlockPos(), pathStand, target);
			}
			BlockPos reachable = this.findReachableMiningStand(level, target, ratPos);
			if (reachable != null) {
				return reachable;
			}
			BlockPos columnAdjacentStand = this.getColumnAdjacentMiningStandPos(level, target);
			if (columnAdjacentStand != null) {
				return columnAdjacentStand;
			}
			BlockPos adjacentStand = this.findAdjacentLayerMiningStand(level, target, ratPos);
			if (adjacentStand != null) {
				return adjacentStand;
			}
			throw new IllegalStateException("Нет достижимой стойки на слое для блока " + target + " карьером " + this.getBlockPos());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось вычислить позицию крысы для цели {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет, нужен ли режим добивки слоя для выбора стойки; принимает уровень и цель, отдаёт true при малом остатке или без следующей клетки змейки.
	private boolean shouldUseLayerCleanupStand(Level level, BlockPos target) {
		try {
			return this.getNextSnakePathFloor(target) == null || this.countUnminedBlocksOnLayer(level, target.getY()) <= LAYER_CLEANUP_UNMINED_THRESHOLD;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить режим добивки для {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Считает недобытые блоки на слое; принимает уровень и Y, отдаёт количество.
	private int countUnminedBlocksOnLayer(Level level, int layerY) {
		try {
			int count = 0;
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius();
			for (int dz = -radius; dz <= radius; dz++) {
				boolean reverse = (dz + radius) % 2 == 1;
				for (int ix = 0; ix <= radius * 2; ix++) {
					int dx = reverse ? radius - ix : -radius + ix;
					BlockPos pos = new BlockPos(quarry.getX() + dx, layerY, quarry.getZ() + dz);
					if (!this.isSupportColumnPos(pos) && this.isLayerMineableState(level, pos, level.getBlockState(pos))) {
						count++;
					}
				}
			}
			return count;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось посчитать блоки слоя {} карьера {}", layerY, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет, есть ли на слое любая работа карьера; принимает уровень и Y, отдаёт true для жидкости 7x7 или добычи 5x5.
	private boolean layerHasPendingWork(Level level, int y) {
		try {
			return this.layerHasFluidSealTargets(level, y) || this.layerHasUnminedBlocks(level, y);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить работу слоя {} карьера {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет жидкость на слое защитного контура; принимает уровень и Y, отдаёт true если есть вода или лава в 7x7.
	private boolean layerHasFluidSealTargets(Level level, int y) {
		try {
			return this.countFluidSealTargetsOnLayer(level, y) > 0;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить жидкости слоя {} карьера {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Считает жидкие блоки на слое защитного контура; принимает уровень и Y, отдаёт количество целей герметизации.
	private int countFluidSealTargetsOnLayer(Level level, int layerY) {
		try {
			int count = 0;
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius() + FLUID_SEAL_EXTRA_RADIUS;
			for (int dz = -radius; dz <= radius; dz++) {
				for (int dx = -radius; dx <= radius; dx++) {
					BlockPos pos = new BlockPos(quarry.getX() + dx, layerY, quarry.getZ() + dz);
					if (this.isFluidSealTarget(level, pos)) {
						count++;
					}
				}
			}
			return count;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось посчитать жидкости слоя {} карьера {}", layerY, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет, есть ли достижимая стойка для добычи блока; принимает уровень и цель, отдаёт true если крыса может подойти и копать.
	public boolean hasReachableStandForTarget(Level level, BlockPos target) {
		try {
			if (this.isFluidSealTarget(level, target)) {
				return this.findReachableFluidSealStand(level, target, target) != null;
			}
			return this.findAdjacentLayerMiningStand(level, target, target) != null
					|| this.getColumnAdjacentMiningStandPos(level, target) != null
					|| this.findReachableMiningStand(level, target, target) != null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить достижимую стойку для {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет досягаемость жидкого блока с позиции ног; принимает стойку и цель, отдаёт true если крыса может поставить булыжник.
	public boolean canReachFluidSealBlockFromStand(BlockPos standPos, BlockPos target) {
		try {
			if (!this.isInsideFluidSealVolume(target) || (!this.isInsideWorkVolume(standPos.below()) && !this.isAccessColumnStandPos(standPos))) {
				return false;
			}
			if (standPos.equals(target) || standPos.below().equals(target)) {
				return false;
			}
			double reachX = target.getX() + 0.5D - (standPos.getX() + 0.5D);
			double reachY = target.getY() + 0.5D - standPos.getY();
			double reachZ = target.getZ() + 0.5D - (standPos.getZ() + 0.5D);
			return reachX * reachX + reachY * reachY + reachZ * reachZ <= LAYER_CLEANUP_REACH_DISTANCE_SQR;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить досягаемость жидкости {} с позиции {} карьера {}", target, standPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Находит достижимую стойку для герметизации жидкости; принимает уровень, цель и опорную позицию, отдаёт ноги крысы или null.
	private @Nullable BlockPos findReachableFluidSealStand(Level level, BlockPos target, BlockPos referencePos) {
		try {
			BlockPos best = null;
			double bestDistance = Double.MAX_VALUE;
			for (BlockPos candidate : this.collectFluidSealStandCandidates(target)) {
				if (!this.isValidFluidSealStandPos(level, candidate, target) || !this.canReachFluidSealBlockFromStand(candidate, target)) {
					continue;
				}
				double distance = referencePos.distSqr(candidate);
				if (best == null || distance < bestDistance) {
					best = candidate;
					bestDistance = distance;
				}
			}
			return best;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти стойку герметизации для {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Собирает кандидатов стоек для герметизации жидкости; принимает цель, отдаёт список позиций ног внутри карьера.
	private List<BlockPos> collectFluidSealStandCandidates(BlockPos target) {
		try {
			List<BlockPos> candidates = new ArrayList<>();
			Set<Long> seen = new HashSet<>();
			int feetY = target.getY() + 1;
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius();
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				this.addStandCandidate(candidates, seen, target.relative(direction).above());
			}
			for (int dz = -radius; dz <= radius; dz++) {
				for (int dx = -radius; dx <= radius; dx++) {
					this.addStandCandidate(candidates, seen, new BlockPos(quarry.getX() + dx, feetY, quarry.getZ() + dz));
				}
			}
			for (BlockPos support : this.getSupportColumnPositions(target.getY())) {
				this.addStandCandidate(candidates, seen, support.above());
			}
			return candidates;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось собрать стойки герметизации для {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет стойку герметизации жидкости; принимает уровень, ноги крысы и цель, отдаёт true если можно безопасно поставить булыжник.
	private boolean isValidFluidSealStandPos(Level level, BlockPos standPos, BlockPos target) {
		try {
			if (!this.isInsideFluidSealVolume(target) || (!this.isInsideWorkVolume(standPos.below()) && !this.isAccessColumnStandPos(standPos))) {
				return false;
			}
			if (standPos.equals(target) || standPos.below().equals(target)) {
				return false;
			}
			boolean accessColumnStand = this.isAccessColumnStandPos(standPos);
			BlockState feet = level.getBlockState(standPos);
			BlockState head = level.getBlockState(standPos.above());
			boolean feetBlocked = !feet.isAir() && !feet.canBeReplaced() && !(accessColumnStand && feet.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()));
			boolean headBlocked = !head.isAir() && !head.canBeReplaced() && !(accessColumnStand && head.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()));
			if (feetBlocked || !feet.getFluidState().isEmpty() || headBlocked || !head.getFluidState().isEmpty()) {
				return false;
			}
			BlockPos floor = standPos.below();
			BlockState floorState = level.getBlockState(floor);
			if (floorState.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) || accessColumnStand) {
				return true;
			}
			return this.isInsideWorkVolume(floor) && !floorState.isAir() && floorState.getFluidState().isEmpty() && floorState.isFaceSturdy(level, floor, Direction.UP);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить стойку {} для герметизации {} карьером {}", standPos, target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает стойку на колонне лестницы для соседнего блока слоя; принимает уровень и цель, отдаёт ноги крысы или null.
	private @Nullable BlockPos getColumnAdjacentMiningStandPos(Level level, BlockPos target) {
		try {
			if (!this.isInsideWorkVolume(target) || this.isSupportColumnPos(target)) {
				return null;
			}
			for (BlockPos support : this.getSupportColumnPositions(target.getY())) {
				int dx = Math.abs(target.getX() - support.getX());
				int dz = Math.abs(target.getZ() - support.getZ());
				if (dx + dz != 1) {
					continue;
				}
				BlockPos stand = support.above();
				if (this.isAccessColumnStandPos(stand)
						&& this.isValidLayerCleanupStandPos(level, stand, target)
						&& this.canReachLayerBlockFromStand(stand, target)) {
					return stand;
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти стойку у колонны для блока {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Находит стойку на соседней клетке слоя (не на столбе); принимает уровень, цель и опорную позицию, отдаёт ноги крысы или null.
	public @Nullable BlockPos findAdjacentLayerMiningStand(Level level, BlockPos target, BlockPos referencePos) {
		try {
			BlockPos best = null;
			double bestDistance = Double.MAX_VALUE;
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				BlockPos stand = target.relative(direction).above();
				if (this.isAccessColumnStandPos(stand)) {
					continue;
				}
				if (!this.isValidLayerCleanupStandPos(level, stand, target) || !this.canReachLayerBlockFromStand(stand, target)) {
					continue;
				}
				double distance = referencePos.distSqr(stand);
				if (best == null || distance < bestDistance) {
					best = stand;
					bestDistance = distance;
				}
			}
			return best;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти соседнюю стойку слоя для {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Находит ближайшую достижимую стойку для добычи блока слоя; принимает уровень, цель и опорную позицию, отдаёт ноги крысы или null.
	public @Nullable BlockPos findReachableMiningStand(Level level, BlockPos target, BlockPos referencePos) {
		try {
			BlockPos best = null;
			double bestScore = Double.MAX_VALUE;
			for (BlockPos candidate : this.collectLayerStandCandidates(target)) {
				if (!this.isValidLayerCleanupStandPos(level, candidate, target) || !this.canReachLayerBlockFromStand(candidate, target)) {
					continue;
				}
				double score = referencePos.distSqr(candidate);
				if (this.isAccessColumnStandPos(candidate)) {
					score += 256.0D;
				}
				if (this.isHorizontalAdjacentLayerStand(candidate, target)) {
					score -= 32.0D;
				}
				if (best == null || score < bestScore) {
					best = candidate;
					bestScore = score;
				}
			}
			if (best == null) {
				best = this.findReachableMiningStandByWalk(level, target, referencePos);
			}
			return best;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти достижимую стойку для {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Собирает кандидатов стоек вокруг блока слоя; принимает цель, отдаёт список позиций ног.
	private List<BlockPos> collectLayerStandCandidates(BlockPos target) {
		try {
			List<BlockPos> candidates = new ArrayList<>();
			Set<Long> seen = new HashSet<>();
			int feetY = target.getY() + 1;
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius();
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				this.addStandCandidate(candidates, seen, target.relative(direction).offset(0, 1, 0));
			}
			for (int dz = -radius; dz <= radius; dz++) {
				boolean reverse = (dz + radius) % 2 == 1;
				for (int ix = 0; ix <= radius * 2; ix++) {
					int dx = reverse ? radius - ix : -radius + ix;
					this.addStandCandidate(candidates, seen, new BlockPos(quarry.getX() + dx, feetY, quarry.getZ() + dz));
				}
			}
			for (BlockPos support : this.getSupportColumnPositions(target.getY())) {
				int dx = Math.abs(target.getX() - support.getX());
				int dz = Math.abs(target.getZ() - support.getZ());
				if (dx + dz == 1) {
					this.addStandCandidate(candidates, seen, new BlockPos(support.getX(), feetY, support.getZ()));
				}
			}
			return candidates;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось собрать кандидатов стоек для {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Добавляет уникальную позицию стойки в список; принимает список, множество и позицию, ничего не возвращает.
	private void addStandCandidate(List<BlockPos> candidates, Set<Long> seen, BlockPos candidate) {
		if (seen.add(candidate.asLong())) {
			candidates.add(candidate);
		}
	}

	// Проверяет допустимую стойку при добивке слоя; принимает уровень, ноги крысы и цель, отдаёт true если можно стоять и копать.
	private boolean isValidLayerCleanupStandPos(Level level, BlockPos standPos, BlockPos target) {
		try {
			if (!this.isInsideWorkVolume(target) || (!this.isInsideWorkVolume(standPos.below()) && !this.isAccessColumnStandPos(standPos))) {
				return false;
			}
			if (standPos.equals(target) || standPos.below().equals(target)) {
				return false;
			}
			boolean accessColumnStand = this.isAccessColumnStandPos(standPos);
			BlockState feet = level.getBlockState(standPos);
			BlockState head = level.getBlockState(standPos.above());
			boolean feetBlocked = !feet.isAir() && !feet.canBeReplaced() && !(accessColumnStand && feet.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()));
			boolean headBlocked = !head.isAir() && !head.canBeReplaced() && !(accessColumnStand && head.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()));
			if (feetBlocked || feet.liquid() || headBlocked || head.liquid()) {
				return false;
			}
			return this.hasLayerMiningFooting(level, standPos, target);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить стойку добивки {} для цели {} карьера {}", standPos, target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет досягаемость блока слоя с позиции ног; принимает стойку и цель, отдаёт true если блок в радиусе добычи.
	public boolean canReachLayerBlockFromStand(BlockPos standPos, BlockPos target) {
		try {
			if (!this.isInsideWorkVolume(target) || (!this.isInsideWorkVolume(standPos.below()) && !this.isAccessColumnStandPos(standPos))) {
				return false;
			}
			if (standPos.equals(target) || standPos.below().equals(target)) {
				return false;
			}
			int dx = Math.abs(target.getX() - standPos.getX());
			int dy = Math.abs(target.getY() - standPos.getY());
			int dz = Math.abs(target.getZ() - standPos.getZ());
			if (dx > this.getRadius() * 2 || dz > this.getRadius() * 2 || dy > 2) {
				return false;
			}
			double reachX = target.getX() + 0.5D - (standPos.getX() + 0.5D);
			double reachY = target.getY() + 0.5D - standPos.getY();
			double reachZ = target.getZ() + 0.5D - (standPos.getZ() + 0.5D);
			return reachX * reachX + reachY * reachY + reachZ * reachZ <= LAYER_CLEANUP_REACH_DISTANCE_SQR;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить досягаемость {} с стойки {} карьера {}", target, standPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Ищет достижимую стойку обходом по безопасным клеткам слоя; принимает уровень, цель и опорную позицию, отдаёт ноги крысы или null.
	private @Nullable BlockPos findReachableMiningStandByWalk(Level level, BlockPos target, BlockPos referencePos) {
		try {
			int feetY = target.getY() + 1;
			List<BlockPos> queue = new ArrayList<>();
			Set<Long> seen = new HashSet<>();
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius();
			for (int dz = -radius; dz <= radius; dz++) {
				boolean reverse = (dz + radius) % 2 == 1;
				for (int ix = 0; ix <= radius * 2; ix++) {
					int dx = reverse ? radius - ix : -radius + ix;
					BlockPos stand = new BlockPos(quarry.getX() + dx, feetY, quarry.getZ() + dz);
					if (this.isValidLayerCleanupStandPos(level, stand, target) && seen.add(stand.asLong())) {
						queue.add(stand);
					}
				}
			}
			BlockPos best = null;
			double bestDistance = Double.MAX_VALUE;
			for (int index = 0; index < queue.size(); index++) {
				BlockPos stand = queue.get(index);
				if (this.canReachLayerBlockFromStand(stand, target)) {
					double distance = referencePos.distSqr(stand);
					if (best == null || distance < bestDistance) {
						best = stand;
						bestDistance = distance;
					}
				}
				for (Direction direction : Direction.Plane.HORIZONTAL) {
					BlockPos next = stand.relative(direction);
					if (next.getY() != feetY || !seen.add(next.asLong()) || !this.isValidLayerCleanupStandPos(level, next, target)) {
						continue;
					}
					queue.add(next);
				}
			}
			return best;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти стойку обходом для {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет, можно ли выдать блок слоя крысе; принимает уровень, позицию, UUID, отдаёт true если цель добываема и достижима.
	private boolean isAssignableLayerTarget(Level level, BlockPos pos, @Nullable UUID ratId) {
		try {
			return !this.isSupportColumnPos(pos)
					&& this.isLayerMineableState(level, pos, level.getBlockState(pos))
					&& !this.hasReservationConflictForCandidate(level, pos, ratId)
					&& this.hasReachableStandForTarget(level, pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить назначение цели {} карьера {} крысе {}", pos, this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Возвращает стойку на поддержке слоя; принимает цель, отдаёт позицию ног над столбом.
	private BlockPos getSupportStandPosForTarget(BlockPos target) {
		try {
			return this.isSupportTarget(target) ? target.above() : this.getSupportColumnPos(target.getY()).above();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось вычислить стойку поддержки для цели {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет, что стойка на одной клетке по горизонтали с целью слоя; принимает ноги и цель, отдаёт true для соседней клетки.
	private boolean isHorizontalAdjacentLayerStand(BlockPos standPos, BlockPos target) {
		try {
			return standPos.getY() == target.getY() + 1
					&& Math.abs(standPos.getX() - target.getX()) + Math.abs(standPos.getZ() - target.getZ()) == 1;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить соседство стойки {} и цели {} карьера {}", standPos, target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет опору под ногами на слое (включая клетку над выкопанным соседом); принимает уровень, ноги и цель, отдаёт true если крыса может стоять.
	private boolean hasLayerMiningFooting(Level level, BlockPos standPos, BlockPos target) {
		try {
			BlockPos floor = standPos.below();
			if (floor.equals(target)) {
				return false;
			}
			BlockState floorState = level.getBlockState(floor);
			if (floorState.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) {
				return true;
			}
			if (this.isAccessColumnStandPos(standPos) && this.hasAccessColumnPlatformBelow(level, standPos)) {
				return true;
			}
			if (floor.getY() == target.getY() && this.isLayerMineableState(level, floor, floorState)) {
				return true;
			}
			if (floor.getY() == target.getY() && floorState.isAir() && this.isInsideWorkVolume(floor)) {
				BlockPos footing = floor.below();
				BlockState footingState = level.getBlockState(footing);
				return !footingState.isAir() && !footingState.liquid() && footingState.isFaceSturdy(level, footing, Direction.UP);
			}
			if (this.isInsideWorkVolume(floor) || this.isAccessColumnStandPos(standPos)) {
				return !floorState.isAir() && !floorState.liquid() && floorState.isFaceSturdy(level, floor, Direction.UP);
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить опору стойки {} для цели {} карьера {}", standPos, target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет безопасную позицию рядом с добываемым блоком; принимает уровень и позицию ног.
	private boolean isSafeMiningStandPos(Level level, BlockPos standPos) {
		try {
			BlockState feet = level.getBlockState(standPos);
			BlockState head = level.getBlockState(standPos.above());
			if ((!feet.isAir() && !feet.canBeReplaced() && !feet.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) || feet.liquid()
					|| (!head.isAir() && !head.canBeReplaced()) || head.liquid()) {
				return false;
			}
			BlockPos floor = standPos.below();
			BlockState floorState = level.getBlockState(floor);
			if (floorState.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) || this.isAccessColumnStandPos(standPos)) {
				return this.hasAccessColumnPlatformBelow(level, standPos);
			}
			if (this.isInsideWorkVolume(floor) && floorState.isAir()) {
				BlockPos footing = floor.below();
				BlockState footingState = level.getBlockState(footing);
				return !footingState.isAir() && !footingState.liquid() && footingState.isFaceSturdy(level, footing, Direction.UP);
			}
			return !floorState.isAir() && !floorState.liquid() && floorState.isFaceSturdy(level, floor, Direction.UP);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить позицию добычи {} карьера {}", standPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет безопасную клетку движения внутри карьера; принимает уровень и позицию ног, отдаёт true если крыса может стоять.
	public boolean isSafeQuarryStandPos(Level level, BlockPos standPos) {
		try {
			return this.isSafeMiningStandPos(level, standPos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить безопасную клетку карьера {} на {}", this.getBlockPos(), standPos, exception);
			throw exception;
		}
	}

	// Проверяет, является ли блок последним в порядке змейки на слое; принимает позицию цели, отдаёт true для финального блока слоя.
	public boolean isLastSnakeBlockOnLayer(BlockPos target) {
		try {
			BlockPos last = this.getLastSnakeBlockOnLayer(target.getY());
			return last != null && last.equals(target);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить последний блок змейки {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает последний блок слоя в порядке змейки; принимает Y слоя, отдаёт позицию или null.
	private @Nullable BlockPos getLastSnakeBlockOnLayer(int layerY) {
		try {
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius();
			BlockPos last = null;
			for (int dz = -radius; dz <= radius; dz++) {
				boolean reverse = (dz + radius) % 2 == 1;
				for (int ix = 0; ix <= radius * 2; ix++) {
					int dx = reverse ? radius - ix : -radius + ix;
					BlockPos pos = new BlockPos(quarry.getX() + dx, layerY, quarry.getZ() + dz);
					if (!this.isSupportColumnPos(pos)) {
						last = pos;
					}
				}
			}
			return last;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти последний блок змейки слоя {} карьера {}", layerY, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает стойку на угловом столбе для добычи последнего блока слоя; принимает цель, отдаёт позицию ног над платформой столба.
	public BlockPos getColumnLayerMiningStandPos(BlockPos target) {
		try {
			return this.getPrimaryAccessColumnStandPos(target.getY() + 1);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось вычислить стойку столба для последнего блока {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает пол стойки для проверки конфликтов брони; принимает цель, отдаёт следующую клетку змейки или пол столба для последнего блока.
	private @Nullable BlockPos getSnakeStandFloorForConflict(BlockPos target) {
		try {
			BlockPos next = this.getNextSnakePathFloor(target);
			if (next != null) {
				return next;
			}
			if (this.isLastSnakeBlockOnLayer(target)) {
				return this.getSupportColumnPos(target.getY());
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось вычислить пол стойки для конфликта {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает следующий блок змейки как пол для крысы; принимает цель, отдаёт пол стойки или null для последнего блока слоя.
	private @Nullable BlockPos getNextSnakePathFloor(BlockPos target) {
		try {
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius();
			boolean foundTarget = false;
			for (int dz = -radius; dz <= radius; dz++) {
				boolean reverse = (dz + radius) % 2 == 1;
				for (int ix = 0; ix <= radius * 2; ix++) {
					int dx = reverse ? radius - ix : -radius + ix;
					BlockPos pos = new BlockPos(quarry.getX() + dx, target.getY(), quarry.getZ() + dz);
					if (this.isSupportColumnPos(pos)) {
						continue;
					}
					if (foundTarget) {
						return pos;
					}
					if (pos.equals(target)) {
						foundTarget = true;
					}
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти следующую точку змейки для цели {} карьера {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает верхний незавершённый слой; принимает уровень, отдаёт Y или minBuildHeight - 1 если добывать нечего.
	public int getActiveLayerY(Level level) {
		try {
			BlockPos quarry = this.getBlockPos();
			for (int y = quarry.getY() - 1; y > level.getMinBuildHeight(); y--) {
				if (!level.getBlockState(this.getSupportColumnPos(y)).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) || this.layerHasPendingWork(level, y)) {
					return y;
				}
			}
			return level.getMinBuildHeight() - 1;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти активный слой карьера {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет, остались ли на слое недобытые блоки без учёта брони; принимает уровень и Y, отдаёт true если слой не закончен.
	public boolean layerHasUnminedBlocks(Level level, int y) {
		try {
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius();
			for (int dz = -radius; dz <= radius; dz++) {
				boolean reverse = (dz + radius) % 2 == 1;
				for (int ix = 0; ix <= radius * 2; ix++) {
					int dx = reverse ? radius - ix : -radius + ix;
					BlockPos pos = new BlockPos(quarry.getX() + dx, y, quarry.getZ() + dz);
					if (!this.isSupportColumnPos(pos) && this.isLayerMineableState(level, pos, level.getBlockState(pos))) {
						return true;
					}
				}
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить недобитый слой {} карьера {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Снимает зависшие брони на слое, чтобы другие крысы могли добить оставшиеся блоки; принимает уровень и Y, ничего не возвращает.
	private void releaseStaleLayerReservations(Level level, int y) {
		try {
			List<UUID> staleRats = new ArrayList<>();
			for (Map.Entry<UUID, BlockPos> entry : this.reservedTargets.entrySet()) {
				BlockPos target = entry.getValue();
				if (target == null || target.getY() != y || (!this.isLayerMineableState(level, target, level.getBlockState(target)) && !this.isFluidSealTarget(level, target))) {
					continue;
				}
				int age = this.reservedAges.getOrDefault(entry.getKey(), 0);
				if (age >= STALE_LAYER_RESERVATION_TICKS) {
					staleRats.add(entry.getKey());
				}
			}
			for (UUID staleRat : staleRats) {
				BlockPos released = this.reservedTargets.get(staleRat);
				this.releaseTarget(staleRat);
				RatsMod.LOGGER.warn("Карьер {} снял зависшую бронь крысы {} на слое {} для блока {}", this.getBlockPos(), staleRat, y, released);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось снять зависшие брони слоя {} карьера {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Находит оставшийся блок слоя для добивки; принимает уровень, слой и UUID, отдаёт позицию без учёта конфликта стоек.
	private @Nullable BlockPos findLayerStragglerTarget(Level level, int y, UUID ratId) {
		try {
			BlockPos columnAdjacent = this.findColumnAdjacentLayerTarget(level, y, ratId);
			if (columnAdjacent != null) {
				return columnAdjacent;
			}
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius();
			for (int dz = -radius; dz <= radius; dz++) {
				boolean reverse = (dz + radius) % 2 == 1;
				for (int ix = 0; ix <= radius * 2; ix++) {
					int dx = reverse ? radius - ix : -radius + ix;
					BlockPos pos = new BlockPos(quarry.getX() + dx, y, quarry.getZ() + dz);
					if (this.isAssignableLayerTarget(level, pos, ratId)) {
						return pos;
					}
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти блок добивки слоя {} карьера {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Находит недобитый блок рядом с колонной лестницы; принимает уровень, слой и UUID, отдаёт позицию только внутри 5x5.
	private @Nullable BlockPos findColumnAdjacentLayerTarget(Level level, int y, UUID ratId) {
		try {
			for (BlockPos support : this.getSupportColumnPositions(y)) {
				for (Direction direction : Direction.Plane.HORIZONTAL) {
					BlockPos candidate = support.relative(direction);
					if (this.isAssignableColumnAdjacentLayerTarget(level, candidate, ratId)) {
						return candidate;
					}
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти блок рядом с лестницей слоя {} карьера {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет блок рядом с колонной лестницы; принимает уровень, позицию и UUID, отдаёт true если его можно добить.
	private boolean isAssignableColumnAdjacentLayerTarget(Level level, BlockPos candidate, @Nullable UUID ratId) {
		try {
			return this.isInsideWorkVolume(candidate)
					&& !this.isSupportColumnPos(candidate)
					&& this.isLayerMineableState(level, candidate, level.getBlockState(candidate))
					&& !this.isReservedByOther(candidate, ratId)
					&& this.getColumnAdjacentMiningStandPos(level, candidate) != null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить блок рядом с лестницей {} карьера {}", candidate, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Принудительно выдаёт оставшийся блок слоя, снимая чужую бронь; принимает уровень, слой и UUID, отдаёт позицию или null.
	private @Nullable BlockPos findLayerForcedCleanupTarget(Level level, int y, UUID ratId) {
		try {
			this.releaseStaleLayerReservations(level, y);
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius();
			BlockPos firstUnmined = null;
			for (int dz = -radius; dz <= radius; dz++) {
				boolean reverse = (dz + radius) % 2 == 1;
				for (int ix = 0; ix <= radius * 2; ix++) {
					int dx = reverse ? radius - ix : -radius + ix;
					BlockPos pos = new BlockPos(quarry.getX() + dx, y, quarry.getZ() + dz);
					if (!this.isSupportColumnPos(pos) && this.isLayerMineableState(level, pos, level.getBlockState(pos))) {
						if (firstUnmined == null) {
							firstUnmined = pos;
						}
						if (this.isAssignableLayerTarget(level, pos, ratId)) {
							return pos;
						}
					}
				}
			}
			if (firstUnmined == null) {
				return null;
			}
			for (UUID otherRat : new ArrayList<>(this.reservedTargets.keySet())) {
				if (otherRat.equals(ratId)) {
					continue;
				}
				BlockPos reserved = this.reservedTargets.get(otherRat);
				if (reserved != null && reserved.getY() == y && this.isLayerMineableState(level, reserved, level.getBlockState(reserved))) {
					this.releaseTarget(otherRat);
					RatsMod.LOGGER.warn("Карьер {} снял бронь крысы {} на слое {} для принудительной добивки {}", this.getBlockPos(), otherRat, y, reserved);
				}
			}
			return this.hasReachableStandForTarget(level, firstUnmined) && !this.isReservedByOther(firstUnmined, ratId) ? firstUnmined : null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось принудительно выдать блок добивки слоя {} карьера {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает глубину слоя относительно карьера; принимает Y слоя, отдаёт неотрицательную глубину.
	public int getDepthForLayerY(int layerY) {
		return Math.max(0, this.getBlockPos().getY() - 1 - layerY);
	}

	// Возвращает Y слоя по глубине; принимает глубину, отдаёт Y.
	public int getLayerYForDepth(int depth) {
		return this.getBlockPos().getY() - 1 - Math.max(0, depth);
	}

	// Возвращает позицию платформы поддержки для глубины; принимает глубину, отдаёт блок единственного столба.
	public BlockPos getAccessPlatformForDepth(int depth) {
		try {
			return this.getSupportColumnPos(this.getLayerYForDepth(depth));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось вычислить позицию пути карьера {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает позицию поддержки на слое; принимает Y слоя, отдаёт угол единственного столба.
	private BlockPos getSupportColumnPos(int layerY) {
		try {
			BlockPos quarry = this.getBlockPos();
			return new BlockPos(quarry.getX() + this.getRadius(), layerY, quarry.getZ() + this.getRadius());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось вычислить позицию столба карьера {} на слое {}", this.getBlockPos(), layerY, exception);
			throw exception;
		}
	}

	// Возвращает позиции поддержек на слое; принимает Y слоя, отдаёт угол и две внутренние прилегающие колонны.
	private BlockPos[] getSupportColumnPositions(int layerY) {
		try {
			BlockPos primary = this.getSupportColumnPos(layerY);
			return new BlockPos[]{primary, primary.west(), primary.north()};
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось вычислить позиции столбов карьера {} на слое {}", this.getBlockPos(), layerY, exception);
			throw exception;
		}
	}

	// Проверяет горизонтальную позицию шахты поддержки; принимает позицию, отдаёт true для любого из трёх столбов.
	private boolean isSupportColumnXZ(BlockPos pos) {
		try {
			for (BlockPos support : this.getSupportColumnPositions(Math.min(pos.getY(), this.getBlockPos().getY() - 1))) {
				if (pos.getX() == support.getX() && pos.getZ() == support.getZ()) {
					return true;
				}
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить горизонталь столба {} карьера {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет позицию столба; принимает позицию, отдаёт true для одной из трёх поддержек.
	private boolean isSupportColumnPos(BlockPos pos) {
		try {
			return this.isSupportColumnXZ(pos) && pos.getY() <= this.getBlockPos().getY() - 1;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить позицию столба {} карьера {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет позицию ног в зоне шахты; принимает позицию, отдаёт true для угла и вспомогательных колонн.
	public boolean isAccessColumnStandPos(BlockPos pos) {
		try {
			return this.isSupportColumnXZ(pos) && pos.getY() <= this.getBlockPos().getY();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить позицию крысы на столбе карьера {} на {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Проверяет угловую колонну подъёма; принимает позицию, отдаёт true только для основного углового столба.
	public boolean isPrimaryAccessColumnStandPos(BlockPos pos) {
		try {
			BlockPos primary = this.getSupportColumnPos(Math.min(pos.getY(), this.getBlockPos().getY() - 1));
			return pos.getX() == primary.getX() && pos.getZ() == primary.getZ() && pos.getY() <= this.getBlockPos().getY();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить угловую колонну карьера {} на {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Проверяет, можно ли сойти с шахты вниз; принимает текущую и целевую Y ног, отдаёт true в пределах ±1 блока при спуске.
	public boolean isWithinColumnExitHeight(int currentY, int targetY) {
		try {
			return targetY < currentY && Math.abs(currentY - targetY) <= COLUMN_EXIT_HEIGHT_DELTA;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить порог выхода с шахты карьера {} для Y {} -> {}", this.getBlockPos(), currentY, targetY, exception);
			throw exception;
		}
	}

	// Проверяет, нужен ли пошаговый подъём или спуск по угловой колонне; принимает текущую и целевую Y ног, отдаёт true если нужна привязка.
	public boolean shouldUseColumnVerticalAssist(int currentY, int targetY) {
		try {
			if (targetY > currentY) {
				return currentY < targetY;
			}
			if (targetY < currentY) {
				return !this.isWithinColumnExitHeight(currentY, targetY);
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить необходимость подъёма по шахте карьера {} для Y {} -> {}", this.getBlockPos(), currentY, targetY, exception);
			throw exception;
		}
	}

	// Проверяет, стоит ли крыса на угловой платформе подъёма; принимает уровень и позицию ног, отдаёт true только над угловым столбом.
	public boolean isOnPrimaryAccessColumnSurface(Level level, BlockPos ratPos) {
		try {
			BlockPos floor = ratPos.below();
			BlockPos primary = this.getSupportColumnPos(floor.getY());
			return floor.getX() == primary.getX() && floor.getZ() == primary.getZ() && level.getBlockState(floor).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить опору крысы на угловой шахте карьера {} на {}", this.getBlockPos(), ratPos, exception);
			throw exception;
		}
	}

	// Возвращает точку подъёма на угловой колонне; принимает целевую высоту ног, отдаёт позицию только углового столба.
	public BlockPos getPrimaryAccessColumnStandPos(int targetY) {
		try {
			BlockPos primary = this.getSupportColumnPos(Math.min(targetY, this.getBlockPos().getY() - 1));
			return new BlockPos(primary.getX(), targetY, primary.getZ());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось вычислить угловую колонну карьера {} на высоте {}", this.getBlockPos(), targetY, exception);
			throw exception;
		}
	}

	// Проверяет платформу вертикального столба; принимает позицию, отдаёт true для поддержки.
	public boolean isAccessColumnPlatform(BlockPos pos) {
		try {
			return this.isSupportColumnPos(pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить платформу столба карьера {} на {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Находит следующую жидкость для герметизации; принимает уровень, слой и UUID крысы, отдаёт позицию воды или лавы в 7x7.
	private @Nullable BlockPos findNextFluidSealTarget(Level level, int y, @Nullable UUID ratId) {
		try {
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius() + FLUID_SEAL_EXTRA_RADIUS;
			for (int dz = -radius; dz <= radius; dz++) {
				boolean reverse = (dz + radius) % 2 == 1;
				for (int ix = 0; ix <= radius * 2; ix++) {
					int dx = reverse ? radius - ix : -radius + ix;
					BlockPos pos = new BlockPos(quarry.getX() + dx, y, quarry.getZ() + dz);
					if (this.isAssignableFluidSealTarget(level, pos, ratId)) {
						return pos;
					}
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти жидкость для герметизации карьером {} на слое {}", this.getBlockPos(), y, exception);
			throw exception;
		}
	}

	// Проверяет цель герметизации; принимает уровень, позицию и UUID, отдаёт true для достижимой воды или лавы без чужой брони.
	private boolean isAssignableFluidSealTarget(Level level, BlockPos pos, @Nullable UUID ratId) {
		try {
			return this.isFluidSealTarget(level, pos)
					&& !this.isReservedByOther(pos, ratId)
					&& this.findReachableFluidSealStand(level, pos, pos) != null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить жидкость {} для герметизации карьером {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Находит следующую цель слоя для параллельной змейки; принимает уровень, слой, UUID и позицию крысы, отдаёт позицию или null.
	private @Nullable BlockPos findNextSnakeTarget(Level level, int y, @Nullable UUID ratId, BlockPos ratPos) {
		try {
			if (this.countActiveLayerReservations(level, y, ratId) >= MAX_PARALLEL_LAYER_WORKERS) {
				return null;
			}
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius();
			List<BlockPos> candidates = new ArrayList<>();
			for (int dz = -radius; dz <= radius; dz++) {
				boolean reverse = (dz + radius) % 2 == 1;
				for (int ix = 0; ix <= radius * 2; ix++) {
					int dx = reverse ? radius - ix : -radius + ix;
					BlockPos pos = new BlockPos(quarry.getX() + dx, y, quarry.getZ() + dz);
					if (this.isAssignableLayerTarget(level, pos, ratId)) {
						candidates.add(pos);
					}
				}
			}
			return this.selectParallelLayerTarget(level, candidates, ratId, ratPos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти цель змейкой для карьера {} на слое {}", this.getBlockPos(), y, exception);
			throw exception;
		}
	}

	// Считает активные брони добычи слоя; принимает уровень, слой и текущую крысу, отдаёт количество других рабочих.
	private int countActiveLayerReservations(Level level, int y, @Nullable UUID ratId) {
		try {
			int count = 0;
			for (Map.Entry<UUID, BlockPos> entry : this.reservedTargets.entrySet()) {
				if (entry.getKey().equals(ratId)) {
					continue;
				}
				BlockPos reserved = entry.getValue();
				if (reserved != null && reserved.getY() == y && this.isLayerMineableState(level, reserved, level.getBlockState(reserved))) {
					count++;
				}
			}
			return count;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось посчитать рабочих слоя {} карьера {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Выбирает лучшую цель из пула слоя; принимает уровень, кандидатов, UUID и позицию крысы, отдаёт ближайшую цель с небольшим смещением по UUID.
	private @Nullable BlockPos selectParallelLayerTarget(Level level, List<BlockPos> candidates, @Nullable UUID ratId, BlockPos ratPos) {
		try {
			if (candidates.isEmpty()) {
				return null;
			}
			int start = ratId == null ? 0 : Math.floorMod(ratId.hashCode(), candidates.size());
			BlockPos best = null;
			double bestScore = Double.MAX_VALUE;
			for (int index = 0; index < candidates.size(); index++) {
				BlockPos candidate = candidates.get((start + index) % candidates.size());
				BlockPos stand = this.getStandPosForTarget(level, candidate, ratPos);
				double score = ratPos.distSqr(stand) + index * 0.05D;
				if (this.isAccessColumnStandPos(stand) && this.getColumnAdjacentMiningStandPos(level, candidate) == null) {
					score += 512.0D;
				} else if (this.isAccessColumnStandPos(stand)) {
					score += 16.0D;
				}
				if (this.isHorizontalAdjacentLayerStand(stand, candidate)) {
					score -= 48.0D;
				}
				if (best == null || score < bestScore) {
					best = candidate;
					bestScore = score;
				}
			}
			if (best != null) {
				RatsMod.LOGGER.info("Карьер {} выдал крысе {} цель слоя {} со стойки {}", this.getBlockPos(), ratId, best, this.getStandPosForTarget(level, best, ratPos));
			}
			return best;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выбрать параллельную цель карьера {} для крысы {}", this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Проверяет конфликт брони цели и рабочей стойки; принимает уровень, кандидата и UUID, отдаёт true если крысы помешают друг другу.
	private boolean hasReservationConflictForCandidate(Level level, BlockPos candidate, @Nullable UUID ratId) {
		try {
			if (this.countUnminedBlocksOnLayer(level, candidate.getY()) <= LAYER_CLEANUP_UNMINED_THRESHOLD) {
				return this.isReservedByOther(candidate, ratId);
			}
			BlockPos candidateStandFloor = this.getSnakeStandFloorForConflict(candidate);
			for (Map.Entry<UUID, BlockPos> entry : this.reservedTargets.entrySet()) {
				if (entry.getKey().equals(ratId)) {
					continue;
				}
				BlockPos reserved = entry.getValue();
				if (reserved == null || reserved.getY() != candidate.getY() || !this.isReservedTargetStillValid(level, reserved, entry.getKey())) {
					continue;
				}
				if (reserved.equals(candidate)) {
					return true;
				}
				BlockPos reservedStandFloor = this.getSnakeStandFloorForConflict(reserved);
				if (candidate.equals(reservedStandFloor) || reserved.equals(candidateStandFloor)) {
					return true;
				}
				if (candidateStandFloor != null && candidateStandFloor.equals(reservedStandFloor)) {
					return true;
				}
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить конфликт брони {} карьера {} для крысы {}", candidate, this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Проверяет цель герметизации жидкости; принимает уровень и позицию, отдаёт true для воды или лавы в защитном объёме 7x7.
	public boolean isFluidSealTarget(Level level, BlockPos pos) {
		try {
			BlockState state = level.getBlockState(pos);
			return this.isInsideFluidSealVolume(pos)
					&& !this.isSupportColumnPos(pos)
					&& !state.getFluidState().isEmpty()
					&& !state.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())
					&& level.getBlockEntity(pos) == null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить жидкость {} карьера {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет добываемый блок слоя; принимает уровень, позицию и состояние, отдаёт true только для обычной добычи.
	private boolean isLayerMineableState(Level level, BlockPos pos, BlockState state) {
		if (!this.isInsideWorkVolume(pos) || this.isSupportColumnPos(pos) || this.isPlayerPlacedBlock(pos) || state.isAir() || !state.getFluidState().isEmpty() || state.is(RatsBlockTags.QUARRY_IGNORABLES) || state.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) || level.getBlockEntity(pos) != null) {
			return false;
		}
		float hardness = state.getDestroySpeed(level, pos);
		return hardness >= 0.0F && hardness <= RatConfig.ratStrengthThreshold;
	}

	// Проверяет чужую бронь цели; принимает позицию и UUID текущей крысы, отдаёт true если цель занята другой крысой.
	private boolean isReservedByOther(BlockPos pos, @Nullable UUID ratId) {
		return this.reservedTargets.entrySet().stream().anyMatch(entry -> !entry.getKey().equals(ratId) && entry.getValue().equals(pos));
	}

	// Бронирует цель; принимает UUID и позицию, отдаёт эту же позицию.
	private BlockPos reserveTarget(UUID ratId, BlockPos target) {
		this.reservedTargets.put(ratId, target);
		this.reservedAges.put(ratId, 0);
		return target;
	}

	// Проверяет актуальность брони; принимает уровень, позицию и UUID, отдаёт true если цель ещё нужна.
	private boolean isReservedTargetStillValid(Level level, @Nullable BlockPos target, UUID ratId) {
		if (target == null || this.isReservedByOther(target, ratId)) {
			return false;
		}
		if (this.isSupportColumnPos(target)) {
			return !level.getBlockState(target).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get());
		}
		if (this.isFluidSealTarget(level, target)) {
			return true;
		}
		return this.isLayerMineableState(level, target, level.getBlockState(target));
	}

	// Возвращает позицию у карьера для сдачи добычи; принимает уровень, отдаёт точку у блока карьера.
	public BlockPos getDepositApproachPos(Level level) {
		try {
			return this.getQuarryDepositWaitPos(level);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти точку сдачи у карьера {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает позицию ног крысы для сдачи в карьер; принимает уровень, отдаёт точку рядом с блоком карьера на его высоте.
	public BlockPos getQuarryDepositStandPos(Level level) {
		try {
			return this.getQuarryDepositWaitPos(level);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось вычислить внутреннюю точку сдачи карьера {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает устойчивую точку ожидания сдачи; принимает уровень, отдаёт верхнюю точку дорожки у спуска.
	public BlockPos getQuarryDepositWaitPos(Level level) {
		try {
			return this.getPrimaryAccessColumnStandPos(this.getBlockPos().getY());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось вычислить точку ожидания сдачи карьера {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет допустимую позицию сдачи добычи; принимает уровень и позицию крысы, отдаёт true в верхней док-зоне столба.
	public boolean isValidQuarryDepositStand(Level level, BlockPos ratPos) {
		try {
			BlockPos waitPos = this.getQuarryDepositWaitPos(level);
			if (ratPos.getY() != waitPos.getY() || !this.hasSafeDepositFloor(level, ratPos)) {
				return false;
			}
			int dx = Math.abs(ratPos.getX() - waitPos.getX());
			int dz = Math.abs(ratPos.getZ() - waitPos.getZ());
			return dx <= 1 && dz <= 1;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить позицию сдачи {} для карьера {}", ratPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет опору под точкой сдачи; принимает уровень и позицию ног крысы, отдаёт true если крыса не висит в воздухе.
	private boolean hasSafeDepositFloor(Level level, BlockPos ratPos) {
		try {
			BlockPos floorPos = ratPos.below();
			BlockState floor = level.getBlockState(floorPos);
			return floor.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) || (!floor.isAir() && !floor.liquid() && floor.isFaceSturdy(level, floorPos, Direction.UP));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить опору точки сдачи {} карьера {}", ratPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

}
