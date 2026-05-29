package com.github.alexthe666.rats.server.entity.ai.goal.harvest;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.data.tags.RatsBlockTags;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.server.block.entity.RatQuarryBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class RatQuarryGoal extends BaseRatHarvestGoal {
	private enum QuarryMode {
		MINE_LAYER,
		MOVE_TO_LAYER,
		RETURN_TO_DEPOSIT
	}

	private final TamedRat rat;
	private QuarryMode mode = QuarryMode.MOVE_TO_LAYER;
	private int breakingTime;
	private int previousBreakProgress;
	private int pathStuckTicks;
	private BlockState prevMiningState = null;

	public RatQuarryGoal(TamedRat rat) {
		super(rat);
		this.rat = rat;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (!super.canUse() || this.rat.getDepositPos().isEmpty() || !this.rat.getDepositPos().get().dimension().equals(this.rat.level().dimension()) || !this.rat.level().getBlockState(this.rat.getDepositPos().get().pos()).is(RatsBlockRegistry.RAT_QUARRY.get()) || !this.checkTheBasics(false, false)) {
			return false;
		}

		this.resetTarget();
		return this.getTargetBlock() != null;
	}

	@Override
	public boolean isReadyToFire() {
		if (this.nextStartTick > 0) {
			this.nextStartTick--;
			return false;
		}
		this.nextStartTick = this.adjustedTickDelay(RatConfig.ratHarvestDelay / 2);
		return true;
	}

	// Выбирает цель: возврат к карьеру, подход к столбу или добыча забронированного блока.
	private void resetTarget() {
		try {
			this.setTargetBlock(null);
			GlobalPos quarryPos = this.rat.getDepositPos().orElse(null);
			if (quarryPos == null || !(this.rat.level().getBlockEntity(quarryPos.pos()) instanceof RatQuarryBlockEntity quarry)) {
				return;
			}
			if (!this.rat.getMainHandItem().isEmpty()) {
				this.mode = QuarryMode.RETURN_TO_DEPOSIT;
				this.setTargetBlock(quarry.getQuarryDepositWaitPos(this.rat.level()));
				return;
			}
			BlockPos miningTarget = quarry.claimNextTarget(this.rat.level(), this.rat.getUUID(), this.rat.blockPosition());
			if (miningTarget != null) {
				if (this.canMineFromCurrentPosition(quarry, miningTarget)) {
					this.mode = QuarryMode.MINE_LAYER;
				} else if (!this.isAtStandPos(quarry.getStandPosForTarget(this.rat.level(), miningTarget, this.rat.blockPosition()))) {
					this.mode = QuarryMode.MOVE_TO_LAYER;
				} else {
					this.mode = QuarryMode.MINE_LAYER;
				}
				this.setTargetBlock(miningTarget);
			}
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Не удалось выбрать цель карьера для крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет точную рабочую позицию; принимает позицию ног, отдаёт true при совпадении блока крысы.
	private boolean isAtStandPos(BlockPos standPos) {
		try {
			return this.rat.blockPosition().equals(standPos);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Не удалось проверить позицию крысы {} у карьера", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет, может ли блок дать предмет крысе; принимает уровень и позицию, отдаёт true если предмет подходит.
	private boolean doesListContainBlock(Level level, BlockPos pos) {
		try {
			BlockState state = level.getBlockState(pos);
			ItemStack getStack = state.getBlock().getCloneItemStack(level, pos, state);
			return this.rat.canRatPickupItem(getStack);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Не удалось проверить добычу блока {} для крысы {}", pos, this.rat.getUUID(), exception);
			throw exception;
		}
	}

	private boolean canRatHoldTargetedBlock(Level level, BlockPos pos) {
		if (this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
			return true;
		}
		BlockState state = level.getBlockState(pos);
		LootParams.Builder loot = new LootParams.Builder((ServerLevel) level).withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withParameter(LootContextParams.ORIGIN, this.rat.position()).withLuck(1.0F);
		List<ItemStack> drops = state.getDrops(loot);
		return drops.stream().map(ItemStack::getItem).toList().contains(this.rat.getItemInHand(InteractionHand.MAIN_HAND).getItem());
	}

	@Override
	public boolean canContinueToUse() {
		if (this.getTargetBlock() == null) {
			return false;
		}
		if (this.mode == QuarryMode.RETURN_TO_DEPOSIT) {
			return this.checkTheBasics(false, false);
		}
		if (!this.checkTheBasics(false, true)) {
			return false;
		}
		if (this.mode == QuarryMode.MOVE_TO_LAYER || this.mode == QuarryMode.RETURN_TO_DEPOSIT) {
			return true;
		}
		BlockState state = this.rat.level().getBlockState(this.getTargetBlock());
		RatQuarryBlockEntity quarry = this.getQuarry();
		if (quarry != null && quarry.isSupportTarget(this.getTargetBlock())) {
			return true;
		}
		return quarry != null && this.canMineBlock(quarry, this.getTargetBlock()) && !state.isAir();
	}

	public void stop() {
		RatQuarryBlockEntity quarry = this.getQuarry();
		BlockPos target = this.getTargetBlock();
		if (quarry != null && target != null && this.mode != QuarryMode.RETURN_TO_DEPOSIT) {
			quarry.releaseTarget(this.rat.getUUID());
		}
		super.stop();
		this.rat.crafting = false;
		this.mode = QuarryMode.MOVE_TO_LAYER;
		this.pathStuckTicks = 0;
	}

	// Сбрасывает недостижимую цель и выбирает новую на том же слое; принимает карьер, ничего не возвращает.
	private void retargetAfterBlockedWork(RatQuarryBlockEntity quarry) {
		try {
			BlockPos oldTarget = this.getTargetBlock();
			quarry.abandonTarget(this.rat.level(), this.rat.getUUID(), oldTarget);
			this.breakingTime = 0;
			this.previousBreakProgress = -1;
			this.prevMiningState = null;
			this.pathStuckTicks = 0;
			this.setTargetBlock(null);
			this.resetTarget();
			if (this.getTargetBlock() == null) {
				this.stop();
			}
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Крыса {} не смогла переназначить цель карьера {}", this.rat.getUUID(), quarry.getBlockPos(), exception);
			this.stop();
			throw exception;
		}
	}

	@Override
	public void tick() {
		if (this.getTargetBlock() == null) {
			return;
		}
		if (this.mode == QuarryMode.MOVE_TO_LAYER || this.mode == QuarryMode.RETURN_TO_DEPOSIT) {
			this.tickPathMovement();
			return;
		}
		this.tickMining();
	}

	// Движение по лестнице поддержки вверх или вниз.
	private void tickPathMovement() {
		try {
			BlockPos target = this.getTargetBlock();
			RatQuarryBlockEntity quarry = this.getQuarry();
			if (this.mode == QuarryMode.RETURN_TO_DEPOSIT) {
				if (quarry != null && this.moveQuarryGridTo(quarry, target)) {
					return;
				}
				if (quarry == null || !quarry.isValidQuarryDepositStand(this.rat.level(), this.rat.blockPosition())) {
					return;
				}
				this.stop();
				return;
			}
			if (quarry == null) {
				this.stop();
				return;
			}
			BlockPos standPos = quarry.getStandPosForTarget(this.rat.level(), target, this.rat.blockPosition());
			if (this.isStandingOnTarget(target) && this.moveOffTargetBlock(quarry, target, standPos)) {
				return;
			}
			if (this.canMineFromCurrentPosition(quarry, target)) {
				this.mode = QuarryMode.MINE_LAYER;
				this.rat.getNavigation().stop();
				return;
			}
			if (this.moveQuarryGridTo(quarry, standPos)) {
				this.pathStuckTicks = 0;
				return;
			}
			if (this.isAtStandPos(standPos) || this.canMineFromCurrentPosition(quarry, target)) {
				this.pathStuckTicks = 0;
				this.mode = QuarryMode.MINE_LAYER;
				return;
			}
			this.pathStuckTicks++;
			if (this.pathStuckTicks >= 40) {
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Крыса {} зависла у цели {} карьера {}, переназначение", this.rat.getUUID(), target, quarry.getBlockPos());
				this.retargetAfterBlockedWork(quarry);
			}
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Ошибка движения по лестнице карьера {}: {}", this.rat.getUUID(), exception.getMessage(), exception);
			this.stop();
		}
	}

	// Перемещает крысу по угловой колонне карьера; принимает карьер и цель, отдаёт true если шаг уже выполнен.
	private boolean moveAlongAccessColumn(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			if (!quarry.isPrimaryAccessColumnStandPos(target)) {
				return false;
			}
			int currentY = this.rat.blockPosition().getY();
			if (currentY == target.getY() || !quarry.shouldUseColumnVerticalAssist(currentY, target.getY())) {
				return false;
			}
			if (!quarry.isOnPrimaryAccessColumnSurface(this.rat.level(), this.rat.blockPosition())) {
				return false;
			}
			BlockPos currentColumn = quarry.getPrimaryAccessColumnStandPos(currentY);
			double targetX = currentColumn.getX() + 0.5D;
			double targetZ = currentColumn.getZ() + 0.5D;
			if (Math.abs(this.rat.getX() - targetX) > 0.85D || Math.abs(this.rat.getZ() - targetZ) > 0.85D) {
				return false;
			}
			int nextY = currentY + Integer.compare(target.getY(), currentY);
			BlockPos nextStep = quarry.getPrimaryAccessColumnStandPos(nextY);
			if (!quarry.isPrimaryAccessColumnStandPos(nextStep)) {
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Карьер {} не смог провести крысу {} по столбу через {}", quarry.getBlockPos(), this.rat.getUUID(), nextStep);
				return false;
			}
			if (!quarry.hasAccessColumnPlatformBelow(this.rat.level(), nextStep)) {
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Карьер {} не провёл крысу {} по столбу на {} без платформы", quarry.getBlockPos(), this.rat.getUUID(), nextStep);
				return false;
			}
			this.rat.getNavigation().stop();
			this.animateQuarryStep(this.rat.blockPosition(), nextStep);
			this.rat.setDeltaMovement(Vec3.ZERO);
			this.rat.setPos(targetX, nextY, targetZ);
			return true;
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Ошибка движения крысы {} по столбу карьера {}", this.rat.getUUID(), quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Безопасно выводит крысу на уровень рабочей стойки через угловую колонну; принимает карьер и стойку, отдаёт true если шаг выполнен.
	private boolean moveAlongAccessColumnToStandLevel(RatQuarryBlockEntity quarry, BlockPos standPos) {
		try {
			int currentY = this.rat.blockPosition().getY();
			if (!quarry.shouldUseColumnVerticalAssist(currentY, standPos.getY())) {
				return false;
			}
			BlockPos columnTarget = quarry.getPrimaryAccessColumnStandPos(standPos.getY());
			if (this.moveAlongAccessColumn(quarry, columnTarget)) {
				return true;
			}
			BlockPos approach = quarry.getPrimaryAccessColumnStandPos(currentY);
			if (!quarry.isOnPrimaryAccessColumnSurface(this.rat.level(), this.rat.blockPosition())) {
				return this.moveQuarryGridHorizontally(quarry, approach);
			}
			return false;
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Крыса {} не смогла безопасно пройти по шахте карьера {}", this.rat.getUUID(), quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Ведёт крысу внутри карьера по безопасной сетке; принимает карьер и точку ног, отдаёт true если шаг движения выполнен.
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
				if (!quarry.hasAccessColumnPlatformBelow(this.rat.level(), nextStep)) {
					com.github.alexthe666.rats.RatsMod.LOGGER.warn("Крыса {} не может подняться по шахте карьера {} на {} без платформы под ногами", this.rat.getUUID(), quarry.getBlockPos(), nextStep);
					return false;
				}
				this.animateQuarryStep(current, nextStep);
				this.rat.setPos(nextStep.getX() + 0.5D, nextStep.getY(), nextStep.getZ() + 0.5D);
				return true;
			}
			return this.moveQuarryGridHorizontally(quarry, target);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Крыса {} не смогла пройти по сетке карьера {} к {}", this.rat.getUUID(), quarry.getBlockPos(), target, exception);
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
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Крыса {} не нашла безопасный горизонтальный шаг в карьере {} из {} к {}", this.rat.getUUID(), quarry.getBlockPos(), current, target);
				return false;
			}
			this.rat.getNavigation().stop();
			this.animateQuarryStep(current, next);
			this.rat.setDeltaMovement(Vec3.ZERO);
			this.rat.setPos(next.getX() + 0.5D, next.getY(), next.getZ() + 0.5D);
			return true;
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Крыса {} не смогла пройти по клеткам к {} в карьере {}", this.rat.getUUID(), target, quarry.getBlockPos(), exception);
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
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Не удалось обновить анимацию шага крысы {} из {} в {}", this.rat.getUUID(), current, next, exception);
			throw exception;
		}
	}

	// Копает текущий блок на слое.
	private void tickMining() {
		try {
			BlockPos target = this.getTargetBlock();
			RatQuarryBlockEntity quarry = this.getQuarry();
			if (target == null || quarry == null) {
				this.stop();
				return;
			}
			BlockPos standPos = quarry.getStandPosForTarget(this.rat.level(), target, this.rat.blockPosition());
			if (this.isStandingOnTarget(target) && this.moveOffTargetBlock(quarry, target, standPos)) {
				this.mode = QuarryMode.MOVE_TO_LAYER;
				return;
			}
			if (!this.isAtStandPos(standPos) && !this.canMineFromCurrentPosition(quarry, target)) {
				this.mode = QuarryMode.MOVE_TO_LAYER;
				return;
			}
			if (this.rat.isInLava() && this.rat.level().getBlockState(this.rat.blockPosition().above()).isAir()) {
				this.rat.level().setBlockAndUpdate(this.rat.blockPosition().above(), Blocks.WATER.defaultBlockState());
				this.rat.heal(15.0F);
				this.rat.getJumpControl().jump();
			}
			BlockState block = this.rat.level().getBlockState(target);
			if (quarry.isSupportTarget(target) && (block.isAir() || block.canBeReplaced() || block.liquid())) {
				this.finishSupportTarget(quarry, target);
				return;
			}
			if (quarry.isFluidSealTarget(this.rat.level(), target)) {
				this.finishFluidSealTarget(quarry, target);
				return;
			}
			if (!this.canMineBlock(quarry, target) || !this.canReachTargetFromCurrentPosition(quarry, target)) {
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Крыса {} не может добыть {} в карьере {}, переназначение", this.rat.getUUID(), target, quarry.getBlockPos());
				this.retargetAfterBlockedWork(quarry);
				return;
			}
			this.mineTargetBlock(quarry, target, block);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Ошибка добычи карьером для крысы {}", this.rat.getUUID(), exception);
			this.stop();
			throw exception;
		}
	}

	// Проверяет, стоит ли крыса на добываемом блоке; принимает цель, отдаёт true если цель в ногах или под ногами.
	private boolean isStandingOnTarget(BlockPos target) {
		try {
			BlockPos current = this.rat.blockPosition();
			return target.equals(current) || target.equals(current.below());
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Не удалось проверить положение крысы {} над целью {}", this.rat.getUUID(), target, exception);
			throw exception;
		}
	}

	// Отводит крысу с блока цели; принимает карьер, цель и безопасную стойку, отдаёт true если движение начато или выполнено.
	private boolean moveOffTargetBlock(RatQuarryBlockEntity quarry, BlockPos target, BlockPos standPos) {
		try {
			if (quarry.isSupportTarget(target)) {
				BlockPos current = this.rat.blockPosition();
				if (target.equals(current) || target.equals(current.below())) {
					quarry.snapRatOntoSupportPlatform(this.rat.level(), this.rat, target);
					return true;
				}
				return false;
			}
			double targetX = standPos.getX() + 0.5D;
			double targetZ = standPos.getZ() + 0.5D;
			double distance = this.rat.distanceToSqr(targetX, standPos.getY(), targetZ);
			if (distance <= 5.0D && this.rat.level().getBlockState(standPos).isAir() && this.rat.level().getBlockState(standPos.above()).isAir()) {
				this.rat.getNavigation().stop();
				this.rat.setDeltaMovement(Vec3.ZERO);
				this.rat.setPos(targetX, standPos.getY(), targetZ);
			} else {
				return this.moveQuarryGridHorizontally(quarry, standPos);
			}
			return true;
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Крыса {} не смогла отойти с цели {} карьера {}", this.rat.getUUID(), target, quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Добывает текущий блок цели; принимает карьер, цель и состояние, ничего не возвращает до завершения добычи.
	private void mineTargetBlock(RatQuarryBlockEntity quarry, BlockPos target, BlockState block) {
		try {
			SoundType soundType = block.getBlock().getSoundType(block, this.rat.level(), target, null);
			this.rat.getNavigation().stop();
			this.rat.level().broadcastEntityEvent(this.rat, (byte) 85);
			this.rat.crafting = true;
			if (block != this.prevMiningState) {
				this.rat.level().broadcastEntityEvent(this.rat, (byte) 86);
				this.rat.crafting = false;
			}
			this.breakingTime++;
			int hardness = Math.max(1, (int) (block.getDestroySpeed(this.rat.level(), target) * 10));
			int progress = (int) ((float) this.breakingTime / hardness * 10.0F);
			if (this.breakingTime % 5 == 0) {
				this.rat.playSound(soundType.getHitSound(), soundType.getVolume() + 1, soundType.getPitch());
			}
			if (progress != this.previousBreakProgress) {
				this.rat.level().destroyBlockProgress(this.rat.getId(), target, progress);
				this.previousBreakProgress = progress;
			}
			if (this.breakingTime >= hardness) {
				this.finishMinedTarget(quarry, target, block, soundType);
			}
			this.prevMiningState = block;
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Не удалось добыть цель {} карьером {}", target, quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Завершает добычу блока; принимает карьер, цель, состояние и звук, переносит дроп и обновляет цель.
	private void finishMinedTarget(RatQuarryBlockEntity quarry, BlockPos target, BlockState state, SoundType soundType) {
		try {
			this.rat.level().broadcastEntityEvent(this.rat, (byte) 86);
			this.rat.playSound(soundType.getBreakSound(), soundType.getVolume(), soundType.getPitch());
			this.rat.playSound(SoundEvents.ITEM_PICKUP, 1, 1F);
			this.breakingTime = 0;
			this.previousBreakProgress = -1;
			if (!state.is(Blocks.SPONGE) && !state.is(Blocks.WET_SPONGE)) {
				this.holdItemHarvestedIfPossible(this.rat, Block.getDrops(state, (ServerLevel) this.rat.level(), target, null, this.rat, this.rat.getMainHandItem()));
			}
			this.rat.level().destroyBlock(target, false);
			if (quarry.isSupportTarget(target)) {
				quarry.placeSupport(this.rat.level(), target);
				quarry.snapRatOntoSupportPlatform(this.rat.level(), this.rat, target);
			}
			quarry.sealExposedFluidsAfterMining(this.rat.level(), target);
			quarry.completeTarget(this.rat.getUUID(), target);
			this.stop();
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Не удалось завершить добычу цели {} карьером {}", target, quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Завершает герметизацию жидкости; принимает карьер и цель, заменяет воду или лаву булыжником без добычи предмета.
	private void finishFluidSealTarget(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			if (quarry.sealFluidBlock(this.rat.level(), target)) {
				quarry.completeTarget(this.rat.getUUID(), target);
				this.stop();
			} else {
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Крыса {} не смогла закупорить жидкость {} карьером {}", this.rat.getUUID(), target, quarry.getBlockPos());
				this.retargetAfterBlockedWork(quarry);
			}
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Не удалось завершить герметизацию жидкости {} карьером {}", target, quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Завершает пустую цель поддержки; принимает карьер и цель, ставит платформу без добычи.
	private void finishSupportTarget(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			if (quarry.placeSupport(this.rat.level(), target)) {
				quarry.snapRatOntoSupportPlatform(this.rat.level(), this.rat, target);
				quarry.completeTarget(this.rat.getUUID(), target);
				this.stop();
			} else {
				com.github.alexthe666.rats.RatsMod.LOGGER.warn("Крыса {} не смогла поставить платформу {} карьером {}", this.rat.getUUID(), target, quarry.getBlockPos());
				this.retargetAfterBlockedWork(quarry);
			}
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Не удалось завершить поддержку {} карьером {}", target, quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	private RatQuarryBlockEntity getQuarry() {
		GlobalPos quarryPos = this.rat.getDepositPos().orElse(null);
		if (quarryPos == null || !quarryPos.dimension().equals(this.rat.level().dimension())) {
			return null;
		}
		return this.rat.level().getBlockEntity(quarryPos.pos()) instanceof RatQuarryBlockEntity quarry ? quarry : null;
	}

	// Проверяет возможность добычи цели; принимает карьер и позицию, отдаёт true для обычного блока или блока поддержки.
	private boolean canMineBlock(RatQuarryBlockEntity quarry, BlockPos rayPos) {
		BlockPos feet = this.rat.blockPosition();
		if (!quarry.isSupportTarget(rayPos) && !quarry.isInsideWorkVolume(rayPos)) {
			return false;
		}
		if (!quarry.isSupportTarget(rayPos) && (rayPos.equals(feet) || rayPos.equals(feet.below()))) {
			return false;
		}
		if (quarry.isQuarryLadderProtected(this.rat.level(), rayPos) && !quarry.isSupportTarget(rayPos)) {
			return false;
		}
		BlockState state = this.rat.level().getBlockState(rayPos);
		if (state.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) || state.liquid() || this.rat.level().getBlockEntity(rayPos) != null) {
			return false;
		}
		return !state.is(RatsBlockTags.QUARRY_IGNORABLES) && this.doesListContainBlock(this.rat.level(), rayPos) && RatUtils.canRatBreakBlock(this.rat.level(), rayPos, this.rat);
	}

	// Проверяет, можно ли копать с текущего места крысы; принимает карьер и цель, отдаёт true при безопасной близкой позиции.
	private boolean canMineFromCurrentPosition(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			BlockPos current = this.rat.blockPosition();
			if (quarry.isSupportTarget(target)) {
				BlockPos placementStand = quarry.getSupportPlacementStandPos(this.rat.level(), target, current);
				if (placementStand != null && quarry.canPlaceSupportFromStand(current, target)) {
					return true;
				}
				return target.equals(current.below()) || this.canReachSupportTargetFromCurrentPosition(target, current);
			}
			if (quarry.isFluidSealTarget(this.rat.level(), target)) {
				return quarry.canReachFluidSealBlockFromStand(current, target);
			}
			if (quarry.canReachLayerBlockFromStand(current, target)) {
				return true;
			}
			BlockState feet = this.rat.level().getBlockState(current);
			BlockState head = this.rat.level().getBlockState(current.above());
			if ((!feet.isAir() && !feet.canBeReplaced() && !feet.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) || feet.liquid() || (!head.isAir() && !head.canBeReplaced()) || head.liquid()) {
				return false;
			}
			if (target.equals(current) || target.equals(current.below())) {
				return false;
			}
			return this.canReachTargetFromCurrentPosition(quarry, target);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Не удалось проверить текущую позицию крысы {} для цели {}", this.rat.getUUID(), target, exception);
			throw exception;
		}
	}

	private boolean canReachTargetFromCurrentPosition(RatQuarryBlockEntity quarry, BlockPos target) {
		try {
			if (quarry.isSupportTarget(target)) {
				BlockPos current = this.rat.blockPosition();
				if (quarry.canPlaceSupportFromStand(current, target)) {
					return true;
				}
				return target.equals(current.below()) || this.canReachSupportTargetFromCurrentPosition(target, current);
			}
			BlockPos current = this.rat.blockPosition();
			if (quarry.isFluidSealTarget(this.rat.level(), target)) {
				return quarry.canReachFluidSealBlockFromStand(current, target);
			}
			if (quarry.canReachLayerBlockFromStand(current, target)) {
				return true;
			}
			int dx = Math.abs(target.getX() - current.getX());
			int dy = target.getY() - current.getY();
			int dz = Math.abs(target.getZ() - current.getZ());
			return dx <= 2 && dz <= 2 && dy >= -2 && dy <= 1 && this.rat.getRatDistanceCenterSq(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) < this.rat.getRatHarvestDistance(3.0D);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Не удалось проверить досягаемость цели {} карьером {}", target, quarry.getBlockPos(), exception);
			throw exception;
		}
	}

	// Проверяет досягаемость блока поддержки; принимает цель и текущую позицию ног, отдаёт true для соседнего блока.
	private boolean canReachSupportTargetFromCurrentPosition(BlockPos target, BlockPos current) {
		try {
			int dx = Math.abs(target.getX() - current.getX());
			int dy = target.getY() - current.getY();
			int dz = Math.abs(target.getZ() - current.getZ());
			return dx <= 1 && dz <= 1 && dy >= -1 && dy <= 0 && !target.equals(current) && this.rat.getRatDistanceCenterSq(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) < this.rat.getRatHarvestDistance(2.0D);
		} catch (RuntimeException exception) {
			com.github.alexthe666.rats.RatsMod.LOGGER.error("Не удалось проверить досягаемость поддержки {} для крысы {}", target, this.rat.getUUID(), exception);
			throw exception;
		}
	}

}