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
	// Reservation age after which a layer target is considered stuck and released for the layer cleanup pass.
	private static final int STALE_LAYER_RESERVATION_TICKS = 80;
	// Remaining layer block threshold that enables the cleanup mode, which searches for any reachable standing spot.
	private static final int LAYER_CLEANUP_UNMINED_THRESHOLD = 6;
	// Maximum number of rats assigned to mine one quarry layer at the same time.
	private static final int MAX_PARALLEL_LAYER_WORKERS = 10;
	// Maximum distance from the rat's feet to a layer block during the cleanup pass.
	private static final double LAYER_CLEANUP_REACH_DISTANCE_SQR = 20.0D;
	// Extra fluid sealing radius around the 5x5 work area: gives a 7x7 protective ring.
	private static final int FLUID_SEAL_EXTRA_RADIUS = 1;
	private static final String TAG_PLAYER_PLACED_BLOCKS = "PlayerPlacedBlocks";
	// Height tolerance within which a rat steps off the shaft with regular navigation, without snapping to the pillar.
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

	// Returns the quarry item handler for the given side; takes the side, returns an IItemHandler.
	public IItemHandler getItemHandler(@Nullable Direction facing) {
		try {
			if (this.remove) {
				throw new IllegalStateException("Cannot get the item capability of a removed quarry");
			}
			if (facing == Direction.UP) {
				return this.topItemHandler;
			}
			if (facing == Direction.DOWN) {
				return this.bottomItemHandler;
			}
			return this.sideItemHandler;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to get the quarry item capability at {}", this.getBlockPos(), exception);
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

	// Checks whether a position is inside the quarry work volume; takes the block position, returns true for the 5x5 area below the quarry.
	public boolean isInsideWorkVolume(BlockPos pos) {
		try {
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius();
			return pos.getY() < quarry.getY()
					&& Math.abs(pos.getX() - quarry.getX()) <= radius
					&& Math.abs(pos.getZ() - quarry.getZ()) <= radius;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the work volume of quarry {} for {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Checks the fluid sealing volume; takes the block position, returns true for the 7x7 protective ring below the quarry.
	public boolean isInsideFluidSealVolume(BlockPos pos) {
		try {
			BlockPos quarry = this.getBlockPos();
			int radius = this.getRadius() + FLUID_SEAL_EXTRA_RADIUS;
			return pos.getY() < quarry.getY()
					&& Math.abs(pos.getX() - quarry.getX()) <= radius
					&& Math.abs(pos.getZ() - quarry.getZ()) <= radius;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the sealing volume of quarry {} for {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Checks for a quarry service block; takes the position, returns true for the quarry, platform or support pillar.
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
			RatsMod.LOGGER.error("Failed to check service block {} of quarry {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks for a player-placed block in the quarry area; takes the position, returns true if rats should skip it.
	public boolean isPlayerPlacedBlock(BlockPos pos) {
		try {
			return this.playerPlacedBlocks.contains(pos.asLong());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check player block {} of quarry {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Remembers a block placed by a player in the work area; takes the level and position, returns nothing.
	public void markPlayerPlacedBlock(Level level, BlockPos pos) {
		try {
			if (!this.isInsideWorkVolume(pos) || this.isQuarryServiceBlock(level, pos)) {
				return;
			}
			if (this.playerPlacedBlocks.add(pos.asLong())) {
				this.reservedTargets.entrySet().removeIf(entry -> entry.getValue().equals(pos));
				this.setChanged();
				RatsMod.LOGGER.info("Quarry {} skips the player block at {}", this.getBlockPos(), pos);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to mark player block {} for quarry {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Clears the player block mark after it is broken; takes the position, returns nothing.
	public void clearPlayerPlacedBlock(BlockPos pos) {
		try {
			if (this.playerPlacedBlocks.remove(pos.asLong())) {
				this.setChanged();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to clear player block mark {} of quarry {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finds a safe horizontal step inside the quarry; takes the level, current and target foot positions, returns a neighbouring cell or null.
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
			RatsMod.LOGGER.error("Failed to find a horizontal step for quarry {} from {} to {}", this.getBlockPos(), current, target, exception);
			throw exception;
		}
	}

	// Checks a safe cell for a grid step; takes the level and foot position, returns true if the step is allowed.
	private boolean isValidGridStep(Level level, BlockPos next) {
		try {
			if (this.isAccessColumnStandPos(next)) {
				return this.hasAccessColumnPlatformBelow(level, next);
			}
			return this.isSafeQuarryStandPos(level, next);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check grid step {} of quarry {}", next, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Removes stale target reservations; takes the level, returns nothing.
	private void cleanupReservations(Level level) {
		try {
			this.reservedAges.replaceAll((id, age) -> age + 20);
			this.reservedAges.entrySet().removeIf(entry -> entry.getValue() > RESERVATION_TIMEOUT_TICKS || !this.isReservedTargetStillValid(level, this.reservedTargets.get(entry.getKey()), entry.getKey()));
			this.reservedTargets.keySet().removeIf(id -> !this.reservedAges.containsKey(id));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to clear target reservations of quarry {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks support pillar protection; takes the position, returns true for a quarry service block.
	public boolean isQuarryProtected(BlockPos pos) {
		try {
			return this.isSupportColumnPos(pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check quarry protection at {}", pos, exception);
			throw exception;
		}
	}

	// Checks ladder protection by any quarry in neighbouring chunks; takes the level and position, returns true if the block must not be broken.
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
			RatsMod.LOGGER.error("Failed to check quarry ladder protection at {}", pos, exception);
			throw exception;
		}
	}

	// Checks ladder platform protection; takes the level and position, returns true only for rat_quarry_platform at shaft coordinates (the quarry block itself is not protected).
	public boolean isQuarryLadderProtected(Level level, BlockPos pos) {
		try {
			return level.getBlockState(pos).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) && this.isSupportColumnXZ(pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check ladder protection of quarry {} at {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Reserves the next task for a rat; takes the level, UUID and rat foot position, returns a block position or null.
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
					RatsMod.LOGGER.info("Quarry {} already holds {} rats on layer {}, rat {} is waiting for a free target", this.getBlockPos(), MAX_PARALLEL_LAYER_WORKERS, y, ratId);
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
					RatsMod.LOGGER.warn("Quarry {} could not hand out a reachable target on layer {} ({} blocks and {} fluids left), rat {} is waiting", this.getBlockPos(), y, this.countUnminedBlocksOnLayer(level, y), this.countFluidSealTargetsOnLayer(level, y), ratId);
					return null;
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to hand out a target of quarry {} to rat {}", this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Releases a rat's reservation on an unreachable target and frees the layer for cleanup; takes the level, UUID and position, returns nothing.
	public void abandonTarget(Level level, UUID ratId, BlockPos target) {
		try {
			if (target != null) {
				this.releaseStaleLayerReservations(level, target.getY());
			}
			this.releaseTarget(ratId);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to reset target {} of quarry {} for rat {}", target, this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Releases a rat's target reservation; takes the rat UUID, returns nothing.
	public void releaseTarget(UUID ratId) {
		try {
			this.reservedTargets.remove(ratId);
			this.reservedAges.remove(ratId);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to release the target reservation of quarry {} for rat {}", this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Completes a rat's target; takes the UUID and position, releases the reservation on a match.
	public void completeTarget(UUID ratId, BlockPos target) {
		try {
			if (target.equals(this.reservedTargets.get(ratId))) {
				this.releaseTarget(ratId);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to complete target {} of quarry {} for rat {}", target, this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Forcibly restores the whole support shaft; takes the level, returns nothing.
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
				RatsMod.LOGGER.info("Quarry {} forcibly restored {} shaft platforms between Y {} and {}", this.getBlockPos(), restored, bottomY, topY);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to forcibly restore the shaft of quarry {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the lower bound of forced shaft maintenance; takes the level, returns the Y of the deepest maintained layer.
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
			RatsMod.LOGGER.error("Failed to compute the shaft maintenance lower bound of quarry {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finds a missing platform for parallel repair; takes the level, UUID and rat position, returns the nearest free target or null.
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
				RatsMod.LOGGER.info("Quarry {} assigned rat {} to repair ladder {} from standing spot {}", this.getBlockPos(), ratId, best, this.getSupportPlacementStandPos(level, best, ratPos));
			}
			return best;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to find a ladder repair for quarry {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Collects all shaft repair targets; takes the level and rat UUID, returns positions not reserved by others.
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
			RatsMod.LOGGER.error("Failed to collect shaft repair targets of quarry {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finds the top layer that still has work; takes the level, returns the layer Y or minBuildHeight - 1.
	private int findTopPendingWorkLayer(Level level) {
		try {
			for (int y = this.getBlockPos().getY() - 1; y > level.getMinBuildHeight(); y--) {
				if (this.layerHasPendingWork(level, y)) {
					return y;
				}
			}
			return level.getMinBuildHeight() - 1;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to find the top work layer of quarry {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks whether a platform needs repair; takes the level, position and rat UUID, returns true for a free repair target.
	private boolean needsSupportRepair(Level level, BlockPos support, @Nullable UUID ratId) {
		try {
			BlockState state = level.getBlockState(support);
			return !state.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())
					&& !this.isReservedByOther(support, ratId)
					&& this.canReplaceWithSupport(level, support, state)
					&& this.getSupportPlacementStandPos(level, support, null) != null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check platform repair {} of quarry {}", support, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns a safe standing spot for placing a platform; takes the level, target and rat position, returns the spot below or above.
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
			RatsMod.LOGGER.error("Failed to find a standing spot to place support {} for quarry {}", support, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks whether a rat can place a platform from its foot position; takes the standing spot and target, returns true if within reach.
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
			RatsMod.LOGGER.error("Failed to check placing support {} from position {} for quarry {}", support, standPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Puts the rat onto the newly placed platform; takes the level, rat and support position, returns nothing.
	public void snapRatOntoSupportPlatform(Level level, net.minecraft.world.entity.Entity rat, BlockPos support) {
		try {
			BlockPos stand = support.above();
			rat.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			rat.fallDistance = 0.0F;
			rat.setPos(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
			if (!level.getBlockState(stand.below()).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) {
				RatsMod.LOGGER.warn("Quarry {} found no platform under the rat after placing at {}", this.getBlockPos(), support);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to put the rat onto platform {} of quarry {}", support, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks for a shaft platform under the standing spot; takes the level and foot position, returns true for a safe column cell.
	public boolean hasAccessColumnPlatformBelow(Level level, BlockPos standPos) {
		try {
			if (!this.isAccessColumnStandPos(standPos)) {
				return false;
			}
			return level.getBlockState(standPos.below()).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check column support {} of quarry {}", standPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Places a support in the pillar; takes the level and position, returns true if the support is in place.
	public boolean placeSupport(Level level, BlockPos pos) {
		try {
			BlockState state = level.getBlockState(pos);
			if (state.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) {
				return true;
			}
			if (!this.canReplaceWithSupport(level, pos, state)) {
				RatsMod.LOGGER.warn("Support pillar of quarry {} is blocked at {} by block {}", this.getBlockPos(), pos, state.getBlock());
				return false;
			}
			level.setBlockAndUpdate(pos, RatsBlockRegistry.RAT_QUARRY_PLATFORM.get().defaultBlockState());
			this.sealFluidsAroundSupport(level, pos);
			level.playSound(null, pos, SoundEvents.BAMBOO_PLACE, SoundSource.BLOCKS);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to place support of quarry {} at {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Replaces the target fluid with cobblestone; takes the level and position, returns true if a solid plug is at the position.
	public boolean sealFluidBlock(Level level, BlockPos pos) {
		try {
			BlockState state = level.getBlockState(pos);
			if (!this.isFluidSealTarget(level, pos)) {
				RatsMod.LOGGER.warn("Quarry {} did not seal {} because there is no fluid in the 7x7 volume: {}", this.getBlockPos(), pos, state.getBlock());
				return false;
			}
			level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
			level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS);
			this.clearPlayerPlacedBlock(pos);
			RatsMod.LOGGER.info("Quarry {} plugged fluid with cobblestone at {}", this.getBlockPos(), pos);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to seal fluid with quarry {} at {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Seals fluids exposed by mining; takes the level and mined position, returns true if at least one plug was placed.
	public boolean sealExposedFluidsAfterMining(Level level, BlockPos minedPos) {
		try {
			boolean sealed = false;
			sealed |= this.sealFluidForSafety(level, minedPos.below(), "below the mined block");
			sealed |= this.sealFluidForSafety(level, minedPos, "in the mined cell after the fluid update");
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				sealed |= this.sealFluidForSafety(level, minedPos.relative(direction), "next to the mined block");
				sealed |= this.sealFluidForSafety(level, minedPos.below().relative(direction), "next to the fluid below the mined block");
			}
			if (sealed) {
				RatsMod.LOGGER.info("Quarry {} emergency-sealed fluid around mined block {}", this.getBlockPos(), minedPos);
			}
			return sealed;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to seal fluids around mined block {} of quarry {}", minedPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Seals fluid around a support platform; takes the level and platform position, returns true if a plug was placed.
	private boolean sealFluidsAroundSupport(Level level, BlockPos supportPos) {
		try {
			boolean sealed = false;
			sealed |= this.sealFluidForSafety(level, supportPos.below(), "below the support platform");
			sealed |= this.sealFluidForSafety(level, supportPos.above(), "above the support platform");
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				sealed |= this.sealFluidForSafety(level, supportPos.relative(direction), "next to the support platform");
				sealed |= this.sealFluidForSafety(level, supportPos.below().relative(direction), "next to the support platform base");
			}
			return sealed;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to seal fluids around platform {} of quarry {}", supportPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Places cobblestone instead of an emergency fluid; takes the level, position and reason, returns true if the block was replaced.
	private boolean sealFluidForSafety(Level level, BlockPos pos, String reason) {
		try {
			BlockState state = level.getBlockState(pos);
			if (!this.isInsideFluidSealVolume(pos) || state.getFluidState().isEmpty() || state.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) || level.getBlockEntity(pos) != null) {
				return false;
			}
			level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
			this.clearPlayerPlacedBlock(pos);
			RatsMod.LOGGER.info("Quarry {} placed an emergency cobblestone plug at {}: {}", this.getBlockPos(), pos, reason);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to place an emergency plug at {} with quarry {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks whether a block can be replaced with a support; takes the level, position and state.
	public boolean canReplaceWithSupport(Level level, BlockPos pos, BlockState state) {
		try {
			return level.getBlockEntity(pos) == null && (state.isAir() || state.canBeReplaced() || state.liquid() || state.getDestroySpeed(level, pos) >= 0.0F);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check replacing block {} with support of quarry {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks whether the target is a support; takes the position, returns true for the single pillar.
	public boolean isSupportTarget(BlockPos pos) {
		try {
			return this.isSupportColumnPos(pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check support target {} of quarry {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the standing spot for working on a target; takes the target, returns the rat foot position.
	public BlockPos getStandPosForTarget(BlockPos target) {
		return this.getSupportStandPosForTarget(target);
	}

	// Returns the standing spot for working on a target; takes the level, target and current rat position.
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
					throw new IllegalStateException("No reachable standing spot for sealing fluid " + target + " with quarry " + this.getBlockPos());
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
				RatsMod.LOGGER.warn("Quarry {} found no reachable cleanup standing spot for block {} on layer {}", this.getBlockPos(), target, target.getY());
			}
			BlockPos pathStandFloor = this.getNextSnakePathFloor(target);
			if (pathStandFloor != null) {
				BlockPos pathStand = pathStandFloor.above();
				if (this.isSafeMiningStandPos(level, pathStand) && this.canReachLayerBlockFromStand(pathStand, target)) {
					return pathStand;
				}
				RatsMod.LOGGER.warn("Quarry {} could not use the next serpentine point {} for target {}", this.getBlockPos(), pathStand, target);
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
			throw new IllegalStateException("No reachable standing spot on the layer for block " + target + " with quarry " + this.getBlockPos());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to compute the rat position for target {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks whether layer cleanup mode is needed to pick a standing spot; takes the level and target, returns true when few blocks remain or there is no next serpentine cell.
	private boolean shouldUseLayerCleanupStand(Level level, BlockPos target) {
		try {
			return this.getNextSnakePathFloor(target) == null || this.countUnminedBlocksOnLayer(level, target.getY()) <= LAYER_CLEANUP_UNMINED_THRESHOLD;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check cleanup mode for {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Counts unmined blocks on a layer; takes the level and Y, returns the count.
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
			RatsMod.LOGGER.error("Failed to count blocks on layer {} of quarry {}", layerY, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks whether the layer has any quarry work; takes the level and Y, returns true for fluid in 7x7 or mining in 5x5.
	private boolean layerHasPendingWork(Level level, int y) {
		try {
			return this.layerHasFluidSealTargets(level, y) || this.layerHasUnminedBlocks(level, y);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check work on layer {} of quarry {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks for fluid on the protective ring layer; takes the level and Y, returns true if there is water or lava in 7x7.
	private boolean layerHasFluidSealTargets(Level level, int y) {
		try {
			return this.countFluidSealTargetsOnLayer(level, y) > 0;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check fluids on layer {} of quarry {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Counts fluid blocks on the protective ring layer; takes the level and Y, returns the number of sealing targets.
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
			RatsMod.LOGGER.error("Failed to count fluids on layer {} of quarry {}", layerY, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks for a reachable standing spot to mine a block; takes the level and target, returns true if the rat can walk up and dig.
	public boolean hasReachableStandForTarget(Level level, BlockPos target) {
		try {
			if (this.isFluidSealTarget(level, target)) {
				return this.findReachableFluidSealStand(level, target, target) != null;
			}
			return this.findAdjacentLayerMiningStand(level, target, target) != null
					|| this.getColumnAdjacentMiningStandPos(level, target) != null
					|| this.findReachableMiningStand(level, target, target) != null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check a reachable standing spot for {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks whether a fluid block is reachable from the foot position; takes the standing spot and target, returns true if the rat can place cobblestone.
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
			RatsMod.LOGGER.error("Failed to check fluid reach {} from position {} of quarry {}", target, standPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finds a reachable standing spot for sealing fluid; takes the level, target and reference position, returns the rat feet or null.
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
			RatsMod.LOGGER.error("Failed to find a sealing standing spot for {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Collects standing spot candidates for sealing fluid; takes the target, returns a list of foot positions inside the quarry.
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
			RatsMod.LOGGER.error("Failed to collect sealing standing spots for {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks a fluid sealing standing spot; takes the level, rat feet and target, returns true if cobblestone can be placed safely.
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
			RatsMod.LOGGER.error("Failed to check standing spot {} for sealing {} with quarry {}", standPos, target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns a standing spot on the ladder column for a neighbouring layer block; takes the level and target, returns the rat feet or null.
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
			RatsMod.LOGGER.error("Failed to find a column standing spot for block {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finds a standing spot on a neighbouring layer cell (not on the pillar); takes the level, target and reference position, returns the rat feet or null.
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
			RatsMod.LOGGER.error("Failed to find a neighbouring layer standing spot for {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finds the nearest reachable standing spot to mine a layer block; takes the level, target and reference position, returns the rat feet or null.
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
			RatsMod.LOGGER.error("Failed to find a reachable standing spot for {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Collects standing spot candidates around a layer block; takes the target, returns a list of foot positions.
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
			RatsMod.LOGGER.error("Failed to collect standing spot candidates for {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Adds a unique standing spot position to the list; takes the list, set and position, returns nothing.
	private void addStandCandidate(List<BlockPos> candidates, Set<Long> seen, BlockPos candidate) {
		if (seen.add(candidate.asLong())) {
			candidates.add(candidate);
		}
	}

	// Checks a valid standing spot during layer cleanup; takes the level, rat feet and target, returns true if the rat can stand and dig.
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
			RatsMod.LOGGER.error("Failed to check cleanup standing spot {} for target {} of quarry {}", standPos, target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks whether a layer block is reachable from the foot position; takes the standing spot and target, returns true if the block is within mining radius.
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
			RatsMod.LOGGER.error("Failed to check reach of {} from standing spot {} of quarry {}", target, standPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Searches for a reachable standing spot by walking safe layer cells; takes the level, target and reference position, returns the rat feet or null.
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
			RatsMod.LOGGER.error("Failed to find a standing spot by search for {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks whether a layer block can be assigned to a rat; takes the level, position and UUID, returns true if the target is mineable and reachable.
	private boolean isAssignableLayerTarget(Level level, BlockPos pos, @Nullable UUID ratId) {
		try {
			return !this.isSupportColumnPos(pos)
					&& this.isLayerMineableState(level, pos, level.getBlockState(pos))
					&& !this.hasReservationConflictForCandidate(level, pos, ratId)
					&& this.hasReachableStandForTarget(level, pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check assigning target {} of quarry {} to rat {}", pos, this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Returns the standing spot on the layer support; takes the target, returns the foot position above the pillar.
	private BlockPos getSupportStandPosForTarget(BlockPos target) {
		try {
			return this.isSupportTarget(target) ? target.above() : this.getSupportColumnPos(target.getY()).above();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to compute the support standing spot for target {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks that the standing spot is one cell horizontally from the layer target; takes the feet and target, returns true for an adjacent cell.
	private boolean isHorizontalAdjacentLayerStand(BlockPos standPos, BlockPos target) {
		try {
			return standPos.getY() == target.getY() + 1
					&& Math.abs(standPos.getX() - target.getX()) + Math.abs(standPos.getZ() - target.getZ()) == 1;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check adjacency of standing spot {} and target {} of quarry {}", standPos, target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks the floor under the feet on the layer (including the cell above a mined neighbour); takes the level, feet and target, returns true if the rat can stand.
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
			RatsMod.LOGGER.error("Failed to check standing spot support {} for target {} of quarry {}", standPos, target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks a safe position next to the block being mined; takes the level and foot position.
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
			RatsMod.LOGGER.error("Failed to check mining position {} of quarry {}", standPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks a safe movement cell inside the quarry; takes the level and foot position, returns true if the rat can stand.
	public boolean isSafeQuarryStandPos(Level level, BlockPos standPos) {
		try {
			return this.isSafeMiningStandPos(level, standPos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check safe cell of quarry {} at {}", this.getBlockPos(), standPos, exception);
			throw exception;
		}
	}

	// Checks whether a block is the last in serpentine order on the layer; takes the target position, returns true for the final layer block.
	public boolean isLastSnakeBlockOnLayer(BlockPos target) {
		try {
			BlockPos last = this.getLastSnakeBlockOnLayer(target.getY());
			return last != null && last.equals(target);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the last serpentine block {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the last layer block in serpentine order; takes the layer Y, returns a position or null.
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
			RatsMod.LOGGER.error("Failed to find the last serpentine block of layer {} of quarry {}", layerY, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the standing spot on the corner pillar for mining the last layer block; takes the target, returns the foot position above the pillar platform.
	public BlockPos getColumnLayerMiningStandPos(BlockPos target) {
		try {
			return this.getPrimaryAccessColumnStandPos(target.getY() + 1);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to compute the pillar standing spot for the last block {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the standing spot floor for reservation conflict checks; takes the target, returns the next serpentine cell or the pillar floor for the last block.
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
			RatsMod.LOGGER.error("Failed to compute the standing spot floor for conflict {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the next serpentine block as the rat's floor; takes the target, returns the standing floor or null for the last layer block.
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
			RatsMod.LOGGER.error("Failed to find the next serpentine point for target {} of quarry {}", target, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the top unfinished layer; takes the level, returns Y or minBuildHeight - 1 if there is nothing to mine.
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
			RatsMod.LOGGER.error("Failed to find the active layer of quarry {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks whether unmined blocks remain on the layer, ignoring reservations; takes the level and Y, returns true if the layer is not finished.
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
			RatsMod.LOGGER.error("Failed to check unfinished layer {} of quarry {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Releases stuck reservations on a layer so other rats can finish the remaining blocks; takes the level and Y, returns nothing.
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
				RatsMod.LOGGER.warn("Quarry {} released a stuck reservation of rat {} on layer {} for block {}", this.getBlockPos(), staleRat, y, released);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to release stuck reservations on layer {} of quarry {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finds a remaining layer block for cleanup; takes the level, layer and UUID, returns a position ignoring standing spot conflicts.
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
			RatsMod.LOGGER.error("Failed to find a cleanup block on layer {} of quarry {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finds an unmined block next to the ladder column; takes the level, layer and UUID, returns a position only inside the 5x5.
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
			RatsMod.LOGGER.error("Failed to find a block next to the ladder on layer {} of quarry {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks a block next to the ladder column; takes the level, position and UUID, returns true if it can be finished.
	private boolean isAssignableColumnAdjacentLayerTarget(Level level, BlockPos candidate, @Nullable UUID ratId) {
		try {
			return this.isInsideWorkVolume(candidate)
					&& !this.isSupportColumnPos(candidate)
					&& this.isLayerMineableState(level, candidate, level.getBlockState(candidate))
					&& !this.isReservedByOther(candidate, ratId)
					&& this.getColumnAdjacentMiningStandPos(level, candidate) != null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check block next to ladder {} of quarry {}", candidate, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Forcibly hands out a remaining layer block, releasing another rat's reservation; takes the level, layer and UUID, returns a position or null.
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
					RatsMod.LOGGER.warn("Quarry {} released rat {}'s reservation on layer {} for forced cleanup of {}", this.getBlockPos(), otherRat, y, reserved);
				}
			}
			return this.hasReachableStandForTarget(level, firstUnmined) && !this.isReservedByOther(firstUnmined, ratId) ? firstUnmined : null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to forcibly hand out a cleanup block on layer {} of quarry {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the layer depth relative to the quarry; takes the layer Y, returns a non-negative depth.
	public int getDepthForLayerY(int layerY) {
		return Math.max(0, this.getBlockPos().getY() - 1 - layerY);
	}

	// Returns the layer Y for a depth; takes the depth, returns Y.
	public int getLayerYForDepth(int depth) {
		return this.getBlockPos().getY() - 1 - Math.max(0, depth);
	}

	// Returns the support platform position for a depth; takes the depth, returns the block of the single pillar.
	public BlockPos getAccessPlatformForDepth(int depth) {
		try {
			return this.getSupportColumnPos(this.getLayerYForDepth(depth));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to compute the path position of quarry {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the support position on a layer; takes the layer Y, returns the corner of the single pillar.
	private BlockPos getSupportColumnPos(int layerY) {
		try {
			BlockPos quarry = this.getBlockPos();
			return new BlockPos(quarry.getX() + this.getRadius(), layerY, quarry.getZ() + this.getRadius());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to compute the pillar position of quarry {} on layer {}", this.getBlockPos(), layerY, exception);
			throw exception;
		}
	}

	// Returns the support positions on a layer; takes the layer Y, returns the corner and two inner adjacent columns.
	private BlockPos[] getSupportColumnPositions(int layerY) {
		try {
			BlockPos primary = this.getSupportColumnPos(layerY);
			return new BlockPos[]{primary, primary.west(), primary.north()};
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to compute the pillar positions of quarry {} on layer {}", this.getBlockPos(), layerY, exception);
			throw exception;
		}
	}

	// Checks the horizontal position of the support shaft; takes the position, returns true for any of the three pillars.
	private boolean isSupportColumnXZ(BlockPos pos) {
		try {
			for (BlockPos support : this.getSupportColumnPositions(Math.min(pos.getY(), this.getBlockPos().getY() - 1))) {
				if (pos.getX() == support.getX() && pos.getZ() == support.getZ()) {
					return true;
				}
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check pillar horizontal {} of quarry {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks a pillar position; takes the position, returns true for one of the three supports.
	private boolean isSupportColumnPos(BlockPos pos) {
		try {
			return this.isSupportColumnXZ(pos) && pos.getY() <= this.getBlockPos().getY() - 1;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check pillar position {} of quarry {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks a foot position in the shaft area; takes the position, returns true for the corner and helper columns.
	public boolean isAccessColumnStandPos(BlockPos pos) {
		try {
			return this.isSupportColumnXZ(pos) && pos.getY() <= this.getBlockPos().getY();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the rat position on the pillar of quarry {} at {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Checks the corner climbing column; takes the position, returns true only for the main corner pillar.
	public boolean isPrimaryAccessColumnStandPos(BlockPos pos) {
		try {
			BlockPos primary = this.getSupportColumnPos(Math.min(pos.getY(), this.getBlockPos().getY() - 1));
			return pos.getX() == primary.getX() && pos.getZ() == primary.getZ() && pos.getY() <= this.getBlockPos().getY();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the corner column of quarry {} at {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Checks whether the rat can step down off the shaft; takes the current and target foot Y, returns true within ±1 block when descending.
	public boolean isWithinColumnExitHeight(int currentY, int targetY) {
		try {
			return targetY < currentY && Math.abs(currentY - targetY) <= COLUMN_EXIT_HEIGHT_DELTA;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the shaft exit threshold of quarry {} for Y {} -> {}", this.getBlockPos(), currentY, targetY, exception);
			throw exception;
		}
	}

	// Checks whether a step-by-step climb or descent along the corner column is needed; takes the current and target foot Y, returns true if snapping is needed.
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
			RatsMod.LOGGER.error("Failed to check whether climbing the shaft of quarry {} is needed for Y {} -> {}", this.getBlockPos(), currentY, targetY, exception);
			throw exception;
		}
	}

	// Checks whether the rat is standing on the corner climbing platform; takes the level and foot position, returns true only above the corner pillar.
	public boolean isOnPrimaryAccessColumnSurface(Level level, BlockPos ratPos) {
		try {
			BlockPos floor = ratPos.below();
			BlockPos primary = this.getSupportColumnPos(floor.getY());
			return floor.getX() == primary.getX() && floor.getZ() == primary.getZ() && level.getBlockState(floor).is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check rat support on the corner shaft of quarry {} at {}", this.getBlockPos(), ratPos, exception);
			throw exception;
		}
	}

	// Returns the climbing point on the corner column; takes the target foot height, returns the position of the corner pillar only.
	public BlockPos getPrimaryAccessColumnStandPos(int targetY) {
		try {
			BlockPos primary = this.getSupportColumnPos(Math.min(targetY, this.getBlockPos().getY() - 1));
			return new BlockPos(primary.getX(), targetY, primary.getZ());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to compute the corner column of quarry {} at height {}", this.getBlockPos(), targetY, exception);
			throw exception;
		}
	}

	// Checks a vertical pillar platform; takes the position, returns true for a support.
	public boolean isAccessColumnPlatform(BlockPos pos) {
		try {
			return this.isSupportColumnPos(pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the pillar platform of quarry {} at {}", this.getBlockPos(), pos, exception);
			throw exception;
		}
	}

	// Finds the next fluid to seal; takes the level, layer and rat UUID, returns a water or lava position in the 7x7.
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
			RatsMod.LOGGER.error("Failed to find fluid to seal with quarry {} on layer {}", this.getBlockPos(), y, exception);
			throw exception;
		}
	}

	// Checks a sealing target; takes the level, position and UUID, returns true for reachable water or lava not reserved by others.
	private boolean isAssignableFluidSealTarget(Level level, BlockPos pos, @Nullable UUID ratId) {
		try {
			return this.isFluidSealTarget(level, pos)
					&& !this.isReservedByOther(pos, ratId)
					&& this.findReachableFluidSealStand(level, pos, pos) != null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check fluid {} for sealing with quarry {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Finds the next layer target for the parallel serpentine; takes the level, layer, UUID and rat position, returns a position or null.
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
			RatsMod.LOGGER.error("Failed to find a serpentine target for quarry {} on layer {}", this.getBlockPos(), y, exception);
			throw exception;
		}
	}

	// Counts active mining reservations on the layer; takes the level, layer and current rat, returns the number of other workers.
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
			RatsMod.LOGGER.error("Failed to count workers on layer {} of quarry {}", y, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Picks the best target from the layer pool; takes the level, candidates, UUID and rat position, returns the nearest target with a small UUID-based offset.
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
				RatsMod.LOGGER.info("Quarry {} assigned rat {} layer target {} from standing spot {}", this.getBlockPos(), ratId, best, this.getStandPosForTarget(level, best, ratPos));
			}
			return best;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to pick a parallel target of quarry {} for rat {}", this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Checks for a conflict between a target reservation and a working standing spot; takes the level, candidate and UUID, returns true if the rats would get in each other's way.
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
			RatsMod.LOGGER.error("Failed to check reservation conflict {} of quarry {} for rat {}", candidate, this.getBlockPos(), ratId, exception);
			throw exception;
		}
	}

	// Checks a fluid sealing target; takes the level and position, returns true for water or lava in the 7x7 protective volume.
	public boolean isFluidSealTarget(Level level, BlockPos pos) {
		try {
			BlockState state = level.getBlockState(pos);
			return this.isInsideFluidSealVolume(pos)
					&& !this.isSupportColumnPos(pos)
					&& !state.getFluidState().isEmpty()
					&& !state.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())
					&& level.getBlockEntity(pos) == null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check fluid {} of quarry {}", pos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks a mineable layer block; takes the level, position and state, returns true only for regular mining.
	private boolean isLayerMineableState(Level level, BlockPos pos, BlockState state) {
		if (!this.isInsideWorkVolume(pos) || this.isSupportColumnPos(pos) || this.isPlayerPlacedBlock(pos) || state.isAir() || !state.getFluidState().isEmpty() || state.is(RatsBlockTags.QUARRY_IGNORABLES) || state.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) || level.getBlockEntity(pos) != null) {
			return false;
		}
		float hardness = state.getDestroySpeed(level, pos);
		return hardness >= 0.0F && hardness <= RatConfig.ratStrengthThreshold;
	}

	// Checks another rat's reservation on a target; takes the position and the current rat UUID, returns true if the target is taken by another rat.
	private boolean isReservedByOther(BlockPos pos, @Nullable UUID ratId) {
		return this.reservedTargets.entrySet().stream().anyMatch(entry -> !entry.getKey().equals(ratId) && entry.getValue().equals(pos));
	}

	// Reserves a target; takes the UUID and position, returns the same position.
	private BlockPos reserveTarget(UUID ratId, BlockPos target) {
		this.reservedTargets.put(ratId, target);
		this.reservedAges.put(ratId, 0);
		return target;
	}

	// Checks whether a reservation is still valid; takes the level, position and UUID, returns true if the target is still needed.
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

	// Returns the position next to the quarry for delivering loot; takes the level, returns a point next to the quarry block.
	public BlockPos getDepositApproachPos(Level level) {
		try {
			return this.getQuarryDepositWaitPos(level);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to find a delivery point at quarry {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns the rat foot position for delivering into the quarry; takes the level, returns a point next to the quarry block at its height.
	public BlockPos getQuarryDepositStandPos(Level level) {
		try {
			return this.getQuarryDepositWaitPos(level);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to compute the inner delivery point of quarry {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Returns a stable waiting point for delivery; takes the level, returns the top point of the path at the descent.
	public BlockPos getQuarryDepositWaitPos(Level level) {
		try {
			return this.getPrimaryAccessColumnStandPos(this.getBlockPos().getY());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to compute the delivery waiting point of quarry {}", this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks a valid loot delivery position; takes the level and rat position, returns true in the top dock area of the pillar.
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
			RatsMod.LOGGER.error("Failed to check delivery position {} for quarry {}", ratPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

	// Checks the floor under the delivery point; takes the level and rat foot position, returns true if the rat is not hanging in the air.
	private boolean hasSafeDepositFloor(Level level, BlockPos ratPos) {
		try {
			BlockPos floorPos = ratPos.below();
			BlockState floor = level.getBlockState(floorPos);
			return floor.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) || (!floor.isAir() && !floor.liquid() && floor.isFaceSturdy(level, floorPos, Direction.UP));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check delivery point support {} of quarry {}", ratPos, this.getBlockPos(), exception);
			throw exception;
		}
	}

}
