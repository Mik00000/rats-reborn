package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.data.tags.RatsBlockTags;
import com.github.alexthe666.rats.compat.create.CreateCompat;
import com.github.alexthe666.rats.server.block.entity.RatCheeseFeederBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class RatSeekCheeseGoal extends Goal {
	private static final int MAX_PATH_CHECKS_PER_SEARCH = 16;
	private final TamedRat rat;
	private BlockPos targetBlock;
	private int nextStartTick;

	// Создаёт цель поиска сыра; принимает приручённую крысу и готовит движение к кормушкам или контейнерам.
	public RatSeekCheeseGoal(TamedRat rat) {
		try {
			this.rat = rat;
			this.nextStartTick = 40 + rat.getRandom().nextInt(120);
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось создать цель поиска сыра для крысы {}", rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет запуск поиска сыра; ничего не принимает, отдаёт true если крысе нужно поесть или пополнить запас.
	@Override
	public boolean canUse() {
		try {
			if (this.nextStartTick > 0) {
				this.nextStartTick--;
				return false;
			}
			this.nextStartTick = reducedTickDelay(120 + this.rat.getRandom().nextInt(80));
			if ((!this.rat.canMove() && !CreateCompat.isMountedOnCreateSeat(this.rat)) || this.rat.isInCage() || this.rat.getTarget() != null) {
				return false;
			}
			if (RatDaySchedule.shouldSleep(this.rat.level())) {
				return false;
			}
			if (this.rat.consumeCheeseFromReserve("личный запас") && !this.rat.needsCheeseNow() && !this.rat.shouldRefillCheeseReserve()) {
				return false;
			}
			if (!this.rat.needsCheeseNow() && !this.rat.shouldRefillCheeseReserve()) {
				return false;
			}
			this.targetBlock = this.findTargetBlock();
			if (this.targetBlock == null) {
				this.nextStartTick = reducedTickDelay(240 + this.rat.getRandom().nextInt(160));
				this.rat.setLastPathFailure("не найден путь к источнику сыра в радиусе " + RatConfig.pathfindFeederDistance);
				return false;
			}
			this.rat.clearLastPathFailure();
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось запустить поиск сыра крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет продолжение поиска сыра; ничего не принимает, отдаёт true пока цель есть и крысе нужна еда или запас.
	@Override
	public boolean canContinueToUse() {
		try {
			return this.targetBlock != null && (this.rat.canMove() || CreateCompat.isMountedOnCreateSeat(this.rat)) && (this.rat.needsCheeseNow() || this.rat.shouldRefillCheeseReserve());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось продолжить поиск сыра крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Останавливает поиск сыра; ничего не принимает и не отдаёт.
	@Override
	public void stop() {
		try {
			this.targetBlock = null;
			this.rat.getNavigation().stop();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось остановить поиск сыра крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Двигает крысу к найденному источнику сыра и кормит или пополняет запас при достижении.
	@Override
	public void tick() {
		try {
			if (this.targetBlock == null) {
				return;
			}
			if (CreateCompat.isMountedOnCreateSeat(this.rat)) {
				this.rat.stopRiding();
				this.rat.setOrderedToSit(false);
				RatsMod.LOGGER.debug("????? {} ?????? ? Create-???????, ????? ????? ?? ???? {}", this.rat.getUUID(), this.targetBlock);
			}
			Vec3 target = Vec3.atCenterOf(this.targetBlock);
			this.rat.moveSmoothlyTo(target.x(), target.y(), target.z(), 1.25D);
			if (this.rat.distanceToSqr(target) <= this.rat.getRatHarvestDistance(0.0D)) {
				if (!this.collectCheeseFromTarget()) {
					RatsMod.LOGGER.warn("Крыса {} дошла до источника сыра {}, но не смогла получить сыр", this.rat.getUUID(), this.targetBlock);
				}
				this.stop();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выполнить поиск сыра крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Ищет ближайший источник сыра; ничего не принимает, отдаёт позицию кормушки или контейнера.
	private BlockPos findTargetBlock() {
		try {
			BlockPos feeder = this.findFeederTarget();
			if (feeder != null) {
				return feeder;
			}
			if (this.rat.needsCheeseNow() && this.rat.getCheeseEatenToday() <= 0 && EventHooks.canEntityGrief(this.rat.level(), this.rat)) {
				return this.findRaidTarget();
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти источник сыра для крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Ищет кормушку с сыром; ничего не принимает, отдаёт позицию или null.
	private BlockPos findFeederTarget() {
		try {
			BlockPos center = this.rat.getSearchCenter();
			int radius = RatConfig.pathfindFeederDistance;
			List<CheeseSearchCandidate> candidates = new ArrayList<>();
			for (BlockEntity entity : this.collectNearbyBlockEntities(center, radius)) {
				BlockPos pos = entity.getBlockPos();
				if (!pos.closerThan(center, radius + 1.0D) || !(entity instanceof RatCheeseFeederBlockEntity feeder) || !this.isValidFeeder(feeder)) {
					continue;
				}
				candidates.add(new CheeseSearchCandidate(pos, this.rat.distanceToSqr(Vec3.atCenterOf(pos))));
			}
			return this.firstReachableCandidate(candidates);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти кормушку с сыром для крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Ищет контейнер с сыром для голодного грабежа; ничего не принимает, отдаёт позицию или null.
	private BlockPos findRaidTarget() {
		try {
			BlockPos center = this.rat.getSearchCenter();
			int radius = RatConfig.pathfindFeederDistance;
			List<CheeseSearchCandidate> candidates = new ArrayList<>();
			for (BlockEntity entity : this.collectNearbyBlockEntities(center, radius)) {
				BlockPos pos = entity.getBlockPos();
				if (!pos.closerThan(center, radius + 1.0D) || !this.isValidRaidContainer(entity)) {
					continue;
				}
				candidates.add(new CheeseSearchCandidate(pos, this.rat.distanceToSqr(Vec3.atCenterOf(pos))));
			}
			return this.firstReachableCandidate(candidates);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти контейнер с сыром для крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Выбирает ближайшего достижимого кандидата; принимает список найденных целей, отдаёт позицию или null.
	private BlockPos firstReachableCandidate(List<CheeseSearchCandidate> candidates) {
		try {
			candidates.sort(Comparator.comparingDouble(CheeseSearchCandidate::distance));
			int checkedPaths = 0;
			for (CheeseSearchCandidate candidate : candidates) {
				if (checkedPaths++ >= MAX_PATH_CHECKS_PER_SEARCH) {
					return null;
				}
				if (this.canReach(candidate.pos())) {
					return candidate.pos();
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выбрать достижимый источник сыра для крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Собирает block entity в загруженных чанках вокруг центра; принимает центр и радиус, отдаёт iterable без скана каждого блока.
	private Iterable<BlockEntity> collectNearbyBlockEntities(BlockPos center, int radius) {
		try {
			java.util.List<BlockEntity> entities = new java.util.ArrayList<>();
			int minChunkX = (center.getX() - radius) >> 4;
			int maxChunkX = (center.getX() + radius) >> 4;
			int minChunkZ = (center.getZ() - radius) >> 4;
			int maxChunkZ = (center.getZ() + radius) >> 4;
			for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
				for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
					if (!this.rat.level().hasChunk(chunkX, chunkZ)) {
						continue;
					}
					LevelChunk chunk = this.rat.level().getChunk(chunkX, chunkZ);
					entities.addAll(chunk.getBlockEntities().values());
				}
			}
			return entities;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось собрать block entity вокруг {} для крысы {}", center, this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет путь к источнику сыра; принимает позицию, отдаёт true если путь построен.
	private boolean canReach(BlockPos pos) {
		try {
			Path path = this.rat.getNavigation().createPath(pos, 1);
			boolean reachable = path != null && path.getNodeCount() > 0;
			if (!reachable) {
				this.rat.setLastPathFailure("нет пути к сыру " + pos);
			}
			return reachable;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить путь крысы {} к сыру {}", this.rat.getUUID(), pos, exception);
			throw exception;
		}
	}

	// Проверяет кормушку как источник сыра; принимает мир и позицию, отдаёт true если кормушка доступна.
	private boolean isValidFeeder(LevelReader reader, BlockPos pos) {
		try {
			if (RatUtils.isBlockProtected(this.rat.level(), pos, this.rat)) {
				return false;
			}
			BlockEntity entity = reader.getBlockEntity(pos);
			return entity instanceof RatCheeseFeederBlockEntity feeder && this.isValidFeeder(feeder);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить кормушку {} для крысы {}", pos, this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет уже найденную кормушку; принимает block entity, отдаёт true если в ней есть сыр и блок не защищён.
	private boolean isValidFeeder(RatCheeseFeederBlockEntity feeder) {
		try {
			return !RatUtils.isBlockProtected(this.rat.level(), feeder.getBlockPos(), this.rat) && feeder.hasCheese();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить найденную кормушку {} для крысы {}", feeder.getBlockPos(), this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет контейнер для голодного грабежа; принимает мир и позицию, отдаёт true если внутри есть сыр.
	private boolean isValidRaidContainer(LevelReader reader, BlockPos pos) {
		try {
			if (reader.getBlockState(pos).is(RatsBlockTags.UNRAIDABLE_CONTAINERS)) {
				return false;
			}
			if (!(reader.getBlockState(pos).getBlock() instanceof EntityBlock)) {
				return false;
			}
			if (RatUtils.isBlockProtected(this.rat.level(), pos, this.rat)) {
				return false;
			}
			BlockEntity entity = reader.getBlockEntity(pos);
			if (entity instanceof RandomizableContainerBlockEntity container && container.saveWithFullMetadata(this.rat.level().registryAccess()).contains(RandomizableContainerBlockEntity.LOOT_TABLE_TAG)) {
				return false;
			}
			return entity instanceof Container inventory && this.containerHasCheese(inventory);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить контейнер {} для голодной крысы {}", pos, this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет уже найденный контейнер для голодного грабежа; принимает block entity, отдаёт true если внутри есть сыр.
	private boolean isValidRaidContainer(BlockEntity entity) {
		try {
			BlockPos pos = entity.getBlockPos();
			if (this.rat.level().getBlockState(pos).is(RatsBlockTags.UNRAIDABLE_CONTAINERS)) {
				return false;
			}
			if (RatUtils.isBlockProtected(this.rat.level(), pos, this.rat)) {
				return false;
			}
			if (entity instanceof RandomizableContainerBlockEntity container && container.saveWithFullMetadata(this.rat.level().registryAccess()).contains(RandomizableContainerBlockEntity.LOOT_TABLE_TAG)) {
				return false;
			}
			return entity instanceof Container inventory && this.containerHasCheese(inventory);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить найденный контейнер {} для голодной крысы {}", entity.getBlockPos(), this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет наличие сыра в контейнере; принимает контейнер, отдаёт true если есть допустимый сыр.
	private boolean containerHasCheese(Container inventory) {
		try {
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				if (TamedRat.isCheeseReserveItem(inventory.getItem(slot))) {
					return true;
				}
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить сыр в контейнере для крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Получает сыр из текущей цели; ничего не принимает, отдаёт true если кормление или пополнение прошло.
	private boolean collectCheeseFromTarget() {
		try {
			if (this.targetBlock == null) {
				return false;
			}
			BlockEntity entity = this.rat.level().getBlockEntity(this.targetBlock);
			if (entity instanceof RatCheeseFeederBlockEntity feeder) {
				return this.collectFromFeeder(feeder);
			}
			if (entity instanceof Container inventory) {
				return this.eatFromContainer(inventory);
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить сыр крысы {} из цели {}", this.rat.getUUID(), this.targetBlock, exception);
			throw exception;
		}
	}

	// Кормит крысу и пополняет запас из кормушки; принимает block entity кормушки, отдаёт true если сыр съеден или переложен.
	private boolean collectFromFeeder(RatCheeseFeederBlockEntity feeder) {
		try {
			boolean changed = false;
			if (this.rat.needsCheeseNow()) {
				ItemStack cheese = feeder.takeCheeseForRat(1);
				if (!cheese.isEmpty()) {
					boolean eaten = this.rat.eatCheeseFromStack(cheese, "кормушка");
					if (!eaten) {
						feeder.addCheeseFrom(cheese);
						throw new IllegalStateException("Крыса получила сыр из кормушки, но не съела его");
					}
					changed = true;
				}
			}
			while (this.rat.hasCheeseReserveSpace() && feeder.hasCheese()) {
				ItemStack cheese = feeder.takeCheeseForRat(this.rat.getMaxCheeseReserve());
				if (cheese.isEmpty()) {
					break;
				}
				int inserted = this.rat.addCheeseToReserve(cheese);
				if (!cheese.isEmpty()) {
					feeder.addCheeseFrom(cheese);
				}
				if (inserted <= 0) {
					break;
				}
				changed = true;
			}
			return changed;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить сыр крысе {} из кормушки {}", this.rat.getUUID(), feeder.getBlockPos(), exception);
			throw exception;
		}
	}

	// Кормит крысу из контейнера; принимает контейнер, отдаёт true если сыр съеден.
	private boolean eatFromContainer(Container inventory) {
		try {
			if (this.targetBlock != null) {
				this.toggleChest(inventory, false);
			}
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (TamedRat.isCheeseReserveItem(stack) && this.rat.eatCheeseFromStack(stack, "голодный грабёж")) {
					if (stack.isEmpty()) {
						inventory.setItem(slot, ItemStack.EMPTY);
					} else {
						inventory.setChanged();
					}
					return true;
				}
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось украсть сыр для крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Открывает или закрывает сундук при грабежe; принимает контейнер и признак открытия, ничего не отдаёт.
	private void toggleChest(Container inventory, boolean open) {
		try {
			if (this.targetBlock != null && inventory instanceof ChestBlockEntity chest) {
				this.rat.level().blockEvent(this.targetBlock, chest.getBlockState().getBlock(), 1, open ? 1 : 0);
				this.rat.level().playSound(null, this.targetBlock, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось переключить сундук при поиске сыра крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Хранит найденный источник сыра и расстояние до него; принимает позицию и квадрат расстояния, отдаёт данные для сортировки.
	private record CheeseSearchCandidate(BlockPos pos, double distance) {
	}
}