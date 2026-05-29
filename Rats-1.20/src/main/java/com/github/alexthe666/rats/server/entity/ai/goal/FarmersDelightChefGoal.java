package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.compat.farmersdelight.FarmersDelightCompat;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;

public class FarmersDelightChefGoal extends Goal {
	private static final double WORK_REACH_EXPANSION = 1.0D;
	private static final double SEAT_WORK_REACH = 8.0D;
	private final TamedRat rat;
	private BlockPos workPos;
	private BlockPos pickupPos;
	private BlockPos depositPos;
	private int nextStartTick;

	// Создаёт цель шефа Farmer's Delight; принимает приручённую крысу, сохраняет её и настраивает флаги движения.
	public FarmersDelightChefGoal(TamedRat rat) {
		try {
			this.rat = rat;
			this.nextStartTick = rat.getRandom().nextInt(30);
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось создать цель шефа Farmer's Delight для крысы {}", rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет возможность запуска цели; параметров нет, отдаёт true если крыса готова работать у блока FD.
	@Override
	public boolean canUse() {
		try {
			if (this.nextStartTick > 0) {
				this.nextStartTick--;
				return false;
			}
			this.nextStartTick = this.adjustedTickDelay(20 + this.rat.getRandom().nextInt(20));
			if (!RatsMod.FARMERS_DELIGHT_LOADED || !this.canRatWorkAsChef()) {
				return false;
			}
			Optional<GlobalPos> work = this.rat.getCreateWorkPos();
			Optional<GlobalPos> pickup = this.rat.getPickupPos();
			Optional<GlobalPos> deposit = this.rat.getDepositPos();
			if (work.isEmpty() || pickup.isEmpty() || deposit.isEmpty()) {
				return false;
			}
			if (!work.get().dimension().equals(this.rat.level().dimension())
					|| !pickup.get().dimension().equals(this.rat.level().dimension())
					|| !deposit.get().dimension().equals(this.rat.level().dimension())) {
				return false;
			}
			this.workPos = work.get().pos();
			this.pickupPos = pickup.get().pos();
			this.depositPos = deposit.get().pos();
			if (RatUtils.isBlockProtected(this.rat.level(), this.workPos, this.rat)
					|| RatUtils.isBlockProtected(this.rat.level(), this.pickupPos, this.rat)
					|| RatUtils.isBlockProtected(this.rat.level(), this.depositPos, this.rat)) {
				return false;
			}
			BlockEntity blockEntity = this.rat.level().getBlockEntity(this.workPos);
			return blockEntity != null && FarmersDelightCompat.isSupportedWorkBlock(blockEntity);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить запуск цели шефа Farmer's Delight", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет продолжение цели; параметров нет, отдаёт true пока сохранён рабочий блок и крыса может работать.
	@Override
	public boolean canContinueToUse() {
		try {
			if (this.workPos == null || !this.canRatWorkAsChef()) {
				return false;
			}
			BlockEntity blockEntity = this.rat.level().getBlockEntity(this.workPos);
			return blockEntity != null && FarmersDelightCompat.isSupportedWorkBlock(blockEntity);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить продолжение цели шефа Farmer's Delight", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Запускает цель; параметров нет, отмечает крысу работающей.
	@Override
	public void start() {
		try {
			this.rat.isCurrentlyWorking = true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла начать цель шефа Farmer's Delight", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Останавливает цель; параметров нет, очищает состояние и ставит короткую паузу перед новым циклом.
	@Override
	public void stop() {
		try {
			this.rat.isCurrentlyWorking = false;
			this.workPos = null;
			this.pickupPos = null;
			this.depositPos = null;
			this.nextStartTick = 10;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла остановить цель шефа Farmer's Delight", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Выполняет шаг цели; параметров нет, ведёт крысу к точке применения или работает с сиденья.
	@Override
	public void tick() {
		try {
			if (this.workPos == null || this.pickupPos == null || this.depositPos == null) {
				this.stop();
				return;
			}
			if (this.canWorkFromCurrentSeat()) {
				this.performChefCycle();
				return;
			}
			if (this.rat.isPassenger()) {
				this.rat.stopRiding();
			}
			Vec3 movePos = Vec3.atBottomCenterOf(this.workPos.relative(this.rat.createWorkFacing));
			this.rat.moveSmoothlyTo(movePos.x(), movePos.y(), movePos.z(), this.rat.getHungerAdjustedWorkSpeed(1.25D));
			double distance = this.rat.distanceToSqr(movePos.x(), movePos.y(), movePos.z());
			double reach = this.rat.getRatHarvestDistance(WORK_REACH_EXPANSION);
			if (distance <= reach * reach) {
				this.performChefCycle();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла выполнить шаг цели шефа Farmer's Delight", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет базовые условия работы шефа; параметров нет, отдаёт true если команда, улучшение и состояние подходят.
	private boolean canRatWorkAsChef() {
		try {
			return this.rat.canMove()
					&& this.rat.canWorkBySchedule()
					&& this.rat.canWorkFromHunger()
					&& !this.rat.isInCage()
					&& this.rat.getTarget() == null
					&& this.rat.getCommand() == RatCommand.HARVEST
					&& RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_CHEF.get());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить условия работы шефа Farmer's Delight", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Выполняет рабочий цикл у блока; параметров нет, вызывает compat-слой и завершает цель после попытки.
	private void performChefCycle() {
		try {
			boolean worked = FarmersDelightCompat.tryRunChefCycle(this.rat, this.workPos, this.pickupPos, this.depositPos);
			if (!worked) {
				RatsMod.LOGGER.debug("Крыса {} не выполнила действие Farmer's Delight у {}", this.rat.getUUID(), this.workPos);
			}
			this.stop();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла выполнить рабочий цикл Farmer's Delight у {}", this.rat.getUUID(), this.workPos, exception);
			throw exception;
		}
	}

	// Проверяет работу с сиденья; параметров нет, отдаёт true если крыса сидит на сиденье и FD-блок в радиусе.
	private boolean canWorkFromCurrentSeat() {
		try {
			return this.isMountedOnSeat()
					&& this.workPos != null
					&& this.rat.distanceToSqr(Vec3.atCenterOf(this.workPos)) <= SEAT_WORK_REACH * SEAT_WORK_REACH;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить работу с сиденья Farmer's Delight", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет, сидит ли крыса на сущности сиденья; параметров нет, отдаёт true по имени класса сущности сиденья.
	private boolean isMountedOnSeat() {
		try {
			Entity vehicle = this.rat.getVehicle();
			if (vehicle == null) {
				return false;
			}
			String name = vehicle.getClass().getName().toLowerCase();
			return name.contains("seat");
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить сиденье Farmer's Delight", this.rat.getUUID(), exception);
			throw exception;
		}
	}
}
