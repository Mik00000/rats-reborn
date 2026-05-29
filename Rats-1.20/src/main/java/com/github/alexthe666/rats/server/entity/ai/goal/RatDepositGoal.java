package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.block.entity.RatQuarryBlockEntity;
import com.github.alexthe666.rats.server.block.entity.AutoCurdlerBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.message.UpdateCurdlerFluidPacket;
import com.github.alexthe666.rats.server.message.UpdateRatFluidPacket;
import com.github.alexthe666.rats.server.misc.RatInventoryAccess;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.material.Fluids;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class RatDepositGoal extends Goal implements RatWorkGoal {
	private static Field createItemDrainInternalTankField;
	private static Class<?> createInternalTankClass;
	private static Method createTankAllowInsertionMethod;
	private static Method createTankForbidInsertionMethod;
	private static Method createTankGetPrimaryHandlerMethod;
	private static Class<?> createNotifyUpdateClass;
	private static Method createNotifyUpdateMethod;
	private final TamedRat rat;
	private final DepositType type;
	private BlockPos targetBlock = null;
	private int nextStartTick;
	private int nextPlatterNearbyCheckTick;
	private boolean cachedPlatterNearbyItems;
	private ItemStack cachedPlatterStack = ItemStack.EMPTY;

	public RatDepositGoal(TamedRat entity, DepositType type) {
		super();
		this.rat = entity;
		this.type = type;
		this.nextStartTick = entity.getRandom().nextInt(10);
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (this.nextStartTick > 0) {
			this.nextStartTick--;
			return false;
		}
		this.nextStartTick = this.adjustedTickDelay(5 + this.rat.getRandom().nextInt(6));
		if (!this.rat.canMove()) return false;
		if (!this.rat.canWorkBySchedule()) return false;
		if (!this.rat.canWorkFromHunger()) return false;
		if (!this.rat.getCommand().workCommand) return false;
		if (this.rat.getTarget() != null) return false;
		if (this.rat.getDepositPos().isEmpty() || !this.rat.getDepositPos().get().dimension().equals(this.rat.level().dimension()) || RatUtils.isBlockProtected(this.rat.level(), this.rat.getDepositPos().get().pos(), this.rat))
			return false;

		BlockPos depositPos = this.rat.getDepositPos().get().pos();
		BlockEntity te = this.rat.level().getBlockEntity(depositPos);
		if (te == null && !(this.type == DepositType.FLUID && this.canDepositFluidInEmptyCauldron(depositPos))) return false;

		if (this.type == DepositType.INVENTORY) {
			if (!this.rat.shouldDepositItem(this.rat.getMainHandItem())) return false;
			if (this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
				return false;
			}
			if (RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_PLATTER.get())) {
				if (this.rat.getMainHandItem().getCount() < 64 && this.hasItemsOfTypeAroundCached(this.rat.getMainHandItem(), 10))
					return false;
			}
			if (!RatInventoryAccess.canInsert(this.rat.level(), te.getBlockPos(), this.rat.depositFacing, this.rat.getMainHandItem(), this.rat, "сдача предметов")) {
				return false;
			}

		} else if (this.type == DepositType.ENERGY) {
			if (this.rat.getRFTransferRate() <= 0 || this.rat.getHeldRF() <= 0) {
				return false;
			}
			if (this.rat.level().getCapability(Capabilities.EnergyStorage.BLOCK, te.getBlockPos(), this.rat.depositFacing) == null) {
				return false;
			}
		} else if (this.type == DepositType.FLUID) {
			if (this.rat.transportingFluid.isEmpty() || this.rat.transportingFluid.getAmount() == 0) {
				return false;
			}
			if (te != null && this.resolveFluidHandler(te.getBlockPos(), this.rat.depositFacing) == null && !this.canDepositFluidInEmptyCauldron(depositPos)) {
				return false;
			}
		}
		this.resetTarget();
		return targetBlock != null;
	}

	// Ищет рядом предметы того же вида; принимает образец предмета, отдаёт достижимые ближайшие сущности.
	private List<ItemEntity> getItemsOfTypeAround(ItemStack stack) {
		try {
			List<ItemEntity> items = this.rat.level().getEntitiesOfClass(ItemEntity.class, this.rat.getBoundingBox().inflate(this.rat.getRadius()), item -> ItemStack.isSameItemSameComponents(stack, item.getItem()));
			if (items.isEmpty()) {
				return List.of();
			}
			items.sort(Comparator.comparingDouble(this.rat::distanceToSqr));
			List<ItemEntity> reachable = new ArrayList<>();
			int checkedPaths = 0;
			for (ItemEntity item : items) {
				if (checkedPaths++ >= 8) {
					break;
				}
				Path path = this.rat.getNavigation().createPath(item, 1);
				if (path != null && path.getNodeCount() > 0) {
					reachable.add(item);
				}
			}
			return reachable;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить ближайшие предметы перед сдачей", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	private void resetTarget() {
		this.targetBlock = this.rat.getDepositPos().get().pos();
	}

	@Override
	public boolean canContinueToUse() {
		if (RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_PLATTER.get())) {
			if (this.rat.getMainHandItem().getCount() < 64 && !this.hasItemsOfTypeAroundCached(this.rat.getMainHandItem(), 20))
				return false;
		}
		if (this.type == DepositType.FLUID) {
			return this.targetBlock != null && !this.rat.transportingFluid.isEmpty();
		}
		if (this.type == DepositType.ENERGY) {
			return this.targetBlock != null && this.rat.getHeldRF() > 0;
		}
		return this.targetBlock != null && !this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() && this.rat.shouldDepositItem(this.rat.getMainHandItem());
	}

	// Проверяет ближайшие предметы для Platter с коротким кешем; принимает стек и задержку, отдаёт true если рядом есть достижимые предметы.
	private boolean hasItemsOfTypeAroundCached(ItemStack stack, int delayTicks) {
		try {
			if (ItemStack.isSameItemSameComponents(stack, this.cachedPlatterStack) && this.rat.tickCount < this.nextPlatterNearbyCheckTick) {
				return this.cachedPlatterNearbyItems;
			}
			this.cachedPlatterStack = stack.copyWithCount(1);
			this.cachedPlatterNearbyItems = !this.getItemsOfTypeAround(stack).isEmpty();
			this.nextPlatterNearbyCheckTick = this.rat.tickCount + Math.max(1, delayTicks);
			return this.cachedPlatterNearbyItems;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить кеш ближайших предметов перед сдачей", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	@Override
	public void start() {
		this.rat.isCurrentlyWorking = true;
	}

	@Override
	public void stop() {
		this.rat.isCurrentlyWorking = false;
	}

	private Vec3 getMovePos() {
		BlockEntity entity = this.rat.level().getBlockEntity(this.targetBlock);
		if (entity instanceof RatQuarryBlockEntity quarry) {
			return Vec3.atBottomCenterOf(quarry.getQuarryDepositWaitPos(this.rat.level()));
		}
		return Vec3.atBottomCenterOf(this.targetBlock.relative(this.rat.depositFacing));
	}

	@Override
	public void tick() {
		BlockEntity te = this.rat.level().getBlockEntity(this.targetBlock);
		if (this.targetBlock != null && (te != null || this.canDepositFluidInEmptyCauldron(this.targetBlock))) {
			if (te instanceof RatQuarryBlockEntity quarry) {
				if (!this.isAtQuarryDepositStand(quarry)) {
					if (!this.moveQuarryGridTo(quarry, quarry.getQuarryDepositWaitPos(this.rat.level()))) {
						RatsMod.LOGGER.warn("Крыса {} не нашла безопасный путь к сдаче карьера {} с позиции {}", this.rat.getUUID(), quarry.getBlockPos(), this.rat.blockPosition());
						this.rat.getNavigation().stop();
					}
					return;
				}
				this.finishQuarryDeposit(te);
				return;
			}
			Vec3 movePos = this.getMovePos();
			this.rat.moveSmoothlyTo(movePos.x(), movePos.y(), movePos.z(), this.rat.getHungerAdjustedWorkSpeed(1.25D));
			double distance = Math.sqrt(this.rat.distanceToSqr(movePos.x(), movePos.y(), movePos.z()));
			if (distance < 4.5D * this.rat.getRatDistanceModifier() && distance > 2.5D * this.rat.getRatDistanceModifier() && te instanceof Container container) {
				this.toggleChest(container, true);
			}
			if (distance <= 2.0D * this.rat.getRatDistanceModifier()) {
				if (te instanceof Container container) {
					this.toggleChest(container, false);
				}

				this.executeTask(te);
				this.targetBlock = null;
				this.stop();
			}
		}
	}

	// Завершает сдачу в карьер; принимает блок карьера, переносит предметы и очищает состояние цели.
	private void finishQuarryDeposit(BlockEntity te) {
		try {
			this.rat.getNavigation().stop();
			this.rat.setDeltaMovement(Vec3.ZERO);
			this.executeTask(te);
			this.targetBlock = null;
			this.stop();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла завершить сдачу в карьер {}", this.rat.getUUID(), te.getBlockPos(), exception);
			throw exception;
		}
	}

	// Перемещает крысу по угловой колонне карьера; принимает карьер и цель, отдаёт true если шаг выполнен.
	private boolean moveAlongQuarryColumn(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			BlockPos climbTarget = quarry.getPrimaryAccessColumnStandPos(target.getY());
			if (!quarry.isPrimaryAccessColumnStandPos(climbTarget)) {
				return false;
			}
			int currentY = this.rat.blockPosition().getY();
			if (currentY == climbTarget.getY() || !quarry.shouldUseColumnVerticalAssist(currentY, climbTarget.getY())) {
				return false;
			}
			if (!quarry.isOnPrimaryAccessColumnSurface(this.rat.level(), this.rat.blockPosition())) {
				BlockPos approach = quarry.getPrimaryAccessColumnStandPos(currentY);
				return this.moveQuarryGridHorizontally(quarry, approach);
			}
			BlockPos currentColumn = quarry.getPrimaryAccessColumnStandPos(currentY);
			double targetX = currentColumn.getX() + 0.5D;
			double targetZ = currentColumn.getZ() + 0.5D;
			if (Math.abs(this.rat.getX() - targetX) > 0.85D || Math.abs(this.rat.getZ() - targetZ) > 0.85D) {
				return false;
			}
			int nextY = currentY + Integer.compare(climbTarget.getY(), currentY);
			BlockPos nextStep = quarry.getPrimaryAccessColumnStandPos(nextY);
			if (!quarry.isPrimaryAccessColumnStandPos(nextStep)) {
				return false;
			}
			this.rat.getNavigation().stop();
			this.animateQuarryStep(this.rat.blockPosition(), nextStep);
			this.rat.setDeltaMovement(Vec3.ZERO);
			this.rat.setPos(targetX, nextY, targetZ);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла подняться по столбу карьера {}", this.rat.getUUID(), quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Ведёт крысу с добычей по безопасной сетке карьера; принимает карьер и точку сдачи, отдаёт true если шаг выполнен.
	private boolean moveQuarryGridTo(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			BlockPos current = this.rat.blockPosition();
			if (current.equals(target)) {
				return false;
			}
			this.rat.getNavigation().stop();
			this.rat.setDeltaMovement(Vec3.ZERO);
			if (current.getY() != target.getY()) {
				BlockPos currentColumn = quarry.getPrimaryAccessColumnStandPos(current.getY());
				if (!current.equals(currentColumn)) {
					return this.moveQuarryGridHorizontally(quarry, currentColumn);
				}
				int nextY = current.getY() + Integer.compare(target.getY(), current.getY());
				BlockPos nextStep = quarry.getPrimaryAccessColumnStandPos(nextY);
				this.animateQuarryStep(current, nextStep);
				this.rat.setPos(nextStep.getX() + 0.5D, nextStep.getY(), nextStep.getZ() + 0.5D);
				return true;
			}
			return this.moveQuarryGridHorizontally(quarry, target);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла пройти к сдаче карьера {}", this.rat.getUUID(), quarry.getBlockPos(), exception);
			return false;
		}
	}

	// Двигает крысу по горизонтали строго по клеткам; принимает карьер и цель ног, отдаёт true если шаг выполнен.
	private boolean moveQuarryGridHorizontally(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			BlockPos current = this.rat.blockPosition();
			if (current.equals(target)) {
				return false;
			}
			BlockPos next = quarry.findSafeHorizontalGridStep(this.rat.level(), current, target);
			if (next == null) {
				RatsMod.LOGGER.warn("Крыса {} не нашла безопасный горизонтальный шаг к сдаче из {} к {}", this.rat.getUUID(), current, target);
				return false;
			}
			this.rat.getNavigation().stop();
			this.animateQuarryStep(current, next);
			this.rat.setDeltaMovement(Vec3.ZERO);
			this.rat.setPos(next.getX() + 0.5D, next.getY(), next.getZ() + 0.5D);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла пройти по клеткам к сдаче {}", this.rat.getUUID(), target, exception);
			return false;
		}
	}

	// Разворачивает крысу и запускает шаговую анимацию; принимает текущую и следующую клетку, ничего не возвращает.
	private void animateQuarryStep(BlockPos current, BlockPos next) {
		try {
			double dx = next.getX() - current.getX();
			double dz = next.getZ() - current.getZ();
			if (dx == 0.0D && dz == 0.0D) {
				return;
			}
			float yaw = (float) (Math.atan2(dz, dx) * (180F / (float) Math.PI)) - 90.0F;
			this.rat.setYRot(yaw);
			this.rat.yBodyRot = yaw;
			this.rat.yHeadRot = yaw;
			this.rat.yRotO = yaw;
			this.rat.getLookControl().setLookAt(next.getX() + 0.5D, next.getY() + 0.5D, next.getZ() + 0.5D);
			this.rat.walkAnimation.update(1.0F, 0.4F);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось обновить анимацию шага крысы {} из {} в {}", this.rat.getUUID(), current, next, exception);
			throw exception;
		}
	}

	// Проверяет точку сдачи для карьера; принимает блок карьера, отдаёт true только на верхней дорожке нужной высоты.
	private boolean isAtQuarryDepositStand(RatQuarryBlockEntity quarry) {
		try {
			return quarry.isValidQuarryDepositStand(this.rat.level(), this.rat.blockPosition());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить точку сдачи карьера для крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	public void toggleChest(Container te, boolean open) {
		if (te instanceof ChestBlockEntity chest) {
			if (open) {
				this.rat.level().blockEvent(this.targetBlock, chest.getBlockState().getBlock(), 1, 1);
			} else {
				this.rat.level().blockEvent(this.targetBlock, chest.getBlockState().getBlock(), 1, 0);
			}
			this.rat.level().gameEvent(this.rat, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, chest.getBlockPos());
		}
	}

	private void executeTask(BlockEntity entity) {
		if (this.type == DepositType.INVENTORY) {
			ItemStack duplicate = this.rat.getItemInHand(InteractionHand.MAIN_HAND).copy();
			ItemStack shrunkenStack = RatInventoryAccess.insert(this.rat.level(), entity.getBlockPos(), this.rat.depositFacing, duplicate, this.rat, "сдача предметов");
			if (shrunkenStack.getCount() != duplicate.getCount()) {
				if (shrunkenStack.isEmpty()) {
					this.rat.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				} else {
					this.rat.setItemInHand(InteractionHand.MAIN_HAND, shrunkenStack);
				}
			}
		} else if (this.type == DepositType.ENERGY) {
			IEnergyStorage storage = this.rat.level().getCapability(Capabilities.EnergyStorage.BLOCK, entity.getBlockPos(), this.rat.depositFacing);
			if (storage != null) {
				int howMuchWeHave = this.rat.getHeldRF();
				int inputtedEnergy = 0;
				try {
					if (storage.canReceive() && storage.receiveEnergy(howMuchWeHave, true) > 0) {
						inputtedEnergy = storage.receiveEnergy(howMuchWeHave, false);
					}
				} catch (RuntimeException exception) {
					RatsMod.LOGGER.error("Крыса {} не смогла положить энергию в блок {}", this.rat.getUUID(), entity.getBlockPos(), exception);
					throw exception;
				}
				if (inputtedEnergy > 0) {
					this.rat.setHeldRF(Math.max(0, this.rat.getHeldRF() - inputtedEnergy));
				}
			}
		} else if (this.type == DepositType.FLUID) {
			FluidStack copiedFluid = this.rat.transportingFluid.copy();
			IFluidHandler fluidHandler = entity == null ? null : this.resolveFluidHandler(entity.getBlockPos(), this.rat.depositFacing);
			if (fluidHandler == null && entity == null) {
				this.fillEmptyCauldronWithFluid(this.targetBlock);
				return;
			}
			if (fluidHandler != null) {
				if (!this.rat.transportingFluid.isEmpty()) {
					int minusAmount = 0;
					try {
						if (fluidHandler.getTanks() > 0) {
							FluidStack firstTank = fluidHandler.getFluidInTank(0);
							if (fluidHandler.getTanks() > 1) {
								for (int i = 0; i < fluidHandler.getTanks(); i++) {
									FluidStack otherTank = fluidHandler.getFluidInTank(i);
									if (copiedFluid != null && copiedFluid.isFluidEqual(otherTank)) {
										firstTank = otherTank;
									}
								}
							}
							if (firstTank.isEmpty() || (copiedFluid == null || copiedFluid.isFluidEqual(firstTank))) {
								if (fluidHandler.fill(copiedFluid, IFluidHandler.FluidAction.SIMULATE) != 0) {
									minusAmount = fluidHandler.fill(copiedFluid, IFluidHandler.FluidAction.EXECUTE);
								}
							}
							if (minusAmount == 0 && entity != null) {
								minusAmount = this.fillCreateItemDrain(entity, copiedFluid);
							}
							if (minusAmount == 0) {
								this.nextStartTick = this.adjustedTickDelay(40 + this.rat.getRandom().nextInt(40));
							}
						}
					} catch (RuntimeException exception) {
						RatsMod.LOGGER.error("Крыса {} не смогла положить жидкость в блок {}", this.rat.getUUID(), entity.getBlockPos(), exception);
						throw exception;
					}

					if (minusAmount > 0) {
						int total = copiedFluid.getAmount() - minusAmount;
						if (total <= 0) {
							this.rat.transportingFluid = FluidStack.EMPTY;
						} else {
							this.rat.transportingFluid.setAmount(total);
						}
						if (!this.rat.level().isClientSide()) {
							RatsNetworkHandler.CHANNEL.sendToTrackingEntityAndSelf(this.rat, new UpdateRatFluidPacket(this.rat.getId(), this.rat.transportingFluid));
							if (this.rat.level().getBlockEntity(this.targetBlock) instanceof AutoCurdlerBlockEntity curdler) {
								curdler.setChanged();
								RatsNetworkHandler.CHANNEL.send(RatsNetworkHandler.allTarget(), new UpdateCurdlerFluidPacket(this.targetBlock.asLong(), curdler.getTank().getFluid()));
							}
						}
						SoundEvent sound = this.rat.transportingFluid.isEmpty() ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL;
						this.rat.playSound(sound, 1, 1);
					}
				}
			}
		}
	}

	// Заливает жидкость в осушитель предметов Create; принимает блок-сущность и жидкость, отдаёт принятый объём.
	private int fillCreateItemDrain(BlockEntity entity, FluidStack fluid) {
		try {
			if (entity == null || fluid == null || fluid.isEmpty() || !"com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity".equals(entity.getClass().getName())) {
				return 0;
			}
			Field internalTankField = this.getCreateItemDrainInternalTankField(entity.getClass());
			Object internalTank = internalTankField.get(entity);
			if (internalTank == null) {
				RatsMod.LOGGER.debug("Крыса {} не смогла залить жидкость в Create Item Drain {}: внутренний бак не найден", this.rat.getUUID(), entity.getBlockPos());
				return 0;
			}
			this.ensureCreateTankMethods(internalTank.getClass());
			createTankAllowInsertionMethod.invoke(internalTank);
			try {
				Object primaryHandler = createTankGetPrimaryHandlerMethod.invoke(internalTank);
				if (!(primaryHandler instanceof IFluidHandler handler)) {
					RatsMod.LOGGER.debug("Крыса {} не смогла залить жидкость в Create Item Drain {}: основной бак не является IFluidHandler", this.rat.getUUID(), entity.getBlockPos());
					return 0;
				}
				int simulated = handler.fill(fluid.copy(), IFluidHandler.FluidAction.SIMULATE);
				if (simulated <= 0) {
					RatsMod.LOGGER.debug("Крыса {} не смогла залить {} в Create Item Drain {}: бак отказался принять жидкость", this.rat.getUUID(), fluid.getHoverName().getString(), entity.getBlockPos());
					return 0;
				}
				FluidStack toInsert = fluid.copy();
				toInsert.setAmount(Math.min(toInsert.getAmount(), simulated));
				int inserted = handler.fill(toInsert, IFluidHandler.FluidAction.EXECUTE);
				if (inserted > 0) {
					entity.setChanged();
					this.notifyCreateBlockEntity(entity);
					RatsMod.LOGGER.debug("Крыса {} залила {} мБ {} в Create Item Drain {}", this.rat.getUUID(), inserted, fluid.getHoverName().getString(), entity.getBlockPos());
				}
				return inserted;
			} finally {
				createTankForbidInsertionMethod.invoke(internalTank);
			}
		} catch (ReflectiveOperationException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла обратиться к API Create Item Drain в {}", this.rat.getUUID(), entity == null ? null : entity.getBlockPos(), exception);
			throw new IllegalStateException("Ошибка совместимости с Create Item Drain: " + exception.getMessage(), exception);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла залить жидкость в Create Item Drain {}", this.rat.getUUID(), entity == null ? null : entity.getBlockPos(), exception);
			throw exception;
		}
	}

	// Возвращает кешированное поле внутреннего бака Create Item Drain; принимает класс блока, отдаёт поле internalTank.
	private Field getCreateItemDrainInternalTankField(Class<?> entityClass) throws NoSuchFieldException {
		try {
			if (createItemDrainInternalTankField != null && createItemDrainInternalTankField.getDeclaringClass().isAssignableFrom(entityClass)) {
				return createItemDrainInternalTankField;
			}
			Field field = entityClass.getDeclaredField("internalTank");
			field.setAccessible(true);
			createItemDrainInternalTankField = field;
			return field;
		} catch (NoSuchFieldException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла получить поле бака Create Item Drain", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Подготавливает кеш методов внутреннего бака Create; принимает класс бака, ничего не отдаёт.
	private void ensureCreateTankMethods(Class<?> tankClass) throws NoSuchMethodException {
		try {
			if (tankClass.equals(createInternalTankClass)
					&& createTankAllowInsertionMethod != null
					&& createTankForbidInsertionMethod != null
					&& createTankGetPrimaryHandlerMethod != null) {
				return;
			}
			createInternalTankClass = tankClass;
			createTankAllowInsertionMethod = tankClass.getMethod("allowInsertion");
			createTankForbidInsertionMethod = tankClass.getMethod("forbidInsertion");
			createTankGetPrimaryHandlerMethod = tankClass.getMethod("getPrimaryHandler");
			createTankAllowInsertionMethod.setAccessible(true);
			createTankForbidInsertionMethod.setAccessible(true);
			createTankGetPrimaryHandlerMethod.setAccessible(true);
		} catch (NoSuchMethodException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла подготовить методы бака Create Item Drain", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Просит блок-сущность Create отправить обновление клиенту; принимает блок-сущность, ничего не отдаёт.
	private void notifyCreateBlockEntity(BlockEntity entity) {
		try {
			Method notifyUpdate = this.getCreateNotifyUpdateMethod(entity.getClass());
			notifyUpdate.invoke(entity);
		} catch (NoSuchMethodException ignored) {
			entity.setChanged();
		} catch (ReflectiveOperationException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла обновить Create Item Drain {}", this.rat.getUUID(), entity.getBlockPos(), exception);
			throw new IllegalStateException("Ошибка обновления Create Item Drain: " + exception.getMessage(), exception);
		}
	}

	// Возвращает кешированный метод обновления Create block entity; принимает класс блока, отдаёт notifyUpdate.
	private Method getCreateNotifyUpdateMethod(Class<?> entityClass) throws NoSuchMethodException {
		try {
			if (entityClass.equals(createNotifyUpdateClass) && createNotifyUpdateMethod != null) {
				return createNotifyUpdateMethod;
			}
			Method method = entityClass.getMethod("notifyUpdate");
			method.setAccessible(true);
			createNotifyUpdateClass = entityClass;
			createNotifyUpdateMethod = method;
			return method;
		} catch (NoSuchMethodException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла получить метод обновления Create block entity", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Ищет жидкостный обработчик блока; принимает позицию и предпочитаемую сторону, отдаёт найденный IFluidHandler или null.
	private IFluidHandler resolveFluidHandler(BlockPos pos, Direction preferredSide) {
		try {
			IFluidHandler handler = this.rat.level().getCapability(Capabilities.FluidHandler.BLOCK, pos, preferredSide);
			if (handler != null) {
				return handler;
			}
			for (Direction direction : Direction.values()) {
				if (direction == preferredSide) {
					continue;
				}
				handler = this.rat.level().getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
				if (handler != null) {
					RatsMod.LOGGER.debug("Крыса {} использует сторону {} для сдачи жидкости в {}, потому что сторона {} не дала обработчик", this.rat.getUUID(), direction, pos, preferredSide);
					return handler;
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла найти жидкостный обработчик для сдачи в {}", this.rat.getUUID(), pos, exception);
			throw exception;
		}
	}

	// Проверяет, можно ли вылить жидкость крысы в пустой котёл; принимает позицию, отдаёт true для молока или воды объёмом ведро.
	private boolean canDepositFluidInEmptyCauldron(BlockPos pos) {
		try {
			return this.type == DepositType.FLUID
					&& pos != null
					&& this.rat.level().getBlockState(pos).is(Blocks.CAULDRON)
					&& !this.rat.transportingFluid.isEmpty()
					&& this.rat.transportingFluid.getAmount() >= FluidType.BUCKET_VOLUME
					&& (this.rat.transportingFluid.getFluid().isSame(NeoForgeMod.MILK.get()) || this.rat.transportingFluid.getFluid().isSame(Fluids.WATER));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить котёл для жидкости {}", this.rat.getUUID(), pos, exception);
			throw exception;
		}
	}

	// Выливает ведро жидкости в пустой котёл; принимает позицию котла, обновляет блок и переносимую жидкость крысы.
	private void fillEmptyCauldronWithFluid(BlockPos pos) {
		try {
			if (!this.canDepositFluidInEmptyCauldron(pos)) {
				RatsMod.LOGGER.debug("Крыса {} не может вылить жидкость {} в котёл {}", this.rat.getUUID(), this.rat.transportingFluid.getHoverName().getString(), pos);
				return;
			}
			if (this.rat.transportingFluid.getFluid().isSame(NeoForgeMod.MILK.get())) {
				this.rat.level().setBlockAndUpdate(pos, RatsBlockRegistry.MILK_CAULDRON.get().defaultBlockState());
			} else if (this.rat.transportingFluid.getFluid().isSame(Fluids.WATER)) {
				this.rat.level().setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
			} else {
				throw new IllegalStateException("Пустой котёл не поддерживает жидкость " + this.rat.transportingFluid.getHoverName().getString());
			}
			int remaining = this.rat.transportingFluid.getAmount() - FluidType.BUCKET_VOLUME;
			if (remaining <= 0) {
				this.rat.transportingFluid = FluidStack.EMPTY;
			} else {
				this.rat.transportingFluid.setAmount(remaining);
			}
			if (!this.rat.level().isClientSide()) {
				RatsNetworkHandler.CHANNEL.sendToTrackingEntityAndSelf(this.rat, new UpdateRatFluidPacket(this.rat.getId(), this.rat.transportingFluid));
			}
			this.rat.playSound(SoundEvents.BUCKET_EMPTY, 1, 1);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла вылить жидкость в котёл {}", this.rat.getUUID(), pos, exception);
			throw exception;
		}
	}

	@Override
	public TaskType getRatTaskType() {
		return TaskType.DEPOSIT;
	}

	public enum DepositType {
		INVENTORY,
		FLUID,
		ENERGY
	}
}
