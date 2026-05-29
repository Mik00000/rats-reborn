package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.EnumSet;

public class RatPickupGoal extends Goal implements RatWorkGoal {
	private final TamedRat rat;
	private final PickupType type;
	private BlockPos targetBlock = null;
	private int nextStartTick;

	public RatPickupGoal(TamedRat rat, PickupType type) {
		this.rat = rat;
		this.type = type;
		this.nextStartTick = rat.getRandom().nextInt(10);
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
		if (!this.canPickUp()) return false;
		if (this.rat.getTarget() != null) return false;
		if (this.rat.getPickupPos().isEmpty() || !this.rat.getPickupPos().get().dimension().equals(this.rat.level().dimension()) || RatUtils.isBlockProtected(this.rat.level(), this.rat.getPickupPos().get().pos(), this.rat))
			return false;

		if (this.type == PickupType.INVENTORY) {
			if (!this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
				return false;
			}
			BlockEntity pickupEntity = this.rat.level().getBlockEntity(this.rat.getPickupPos().get().pos());
			if (pickupEntity == null) {
				return false;
			}
			if (!RatInventoryAccess.canExtract(this.rat.level(), pickupEntity.getBlockPos(), this.rat.pickupFacing, this.rat, "забор предметов")) {
				return false;
			}
		} else if (this.type == PickupType.ENERGY) {
			if (this.rat.getHeldRF() >= this.rat.getRFTransferRate()) {
				return false;
			}
		} else if (this.type == PickupType.FLUID) {
			if (!this.rat.transportingFluid.isEmpty() && this.rat.transportingFluid.getAmount() >= this.rat.getMBTransferRate()) {
				return false;
			}
			if (RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_MILKER.get()) && !this.rat.transportingFluid.isEmpty()) {
				return false;
			}
		}
		this.resetTarget();
		return this.targetBlock != null;
	}

	private boolean canPickUp() {
		if (this.type == PickupType.FLUID) {
			return this.rat.getCommand().workCommand;
		}
		return this.rat.getCommand() == RatCommand.TRANSPORT || (this.rat.getCommand() == RatCommand.HARVEST && (RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_FARMER.get()) || RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_PLACER.get()) || RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_BREEDER.get())));
	}

	private void resetTarget() {
		this.targetBlock = this.rat.getPickupPos().get().pos();
	}

	@Override
	public boolean canContinueToUse() {
		return this.targetBlock != null && this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty();
	}

	@Override
	public void start() {
		this.rat.isCurrentlyWorking = true;
	}

	@Override
	public void stop() {
		this.rat.isCurrentlyWorking = false;
	}

	@Override
	public void tick() {
		BlockEntity te = this.rat.level().getBlockEntity(this.targetBlock);
		if (this.targetBlock != null && (te != null || this.canDrainSourceFluid(this.targetBlock))) {
			this.rat.moveSmoothlyTo(this.targetBlock.getX() + 0.5D, this.targetBlock.getY() + 0.5D, this.targetBlock.getZ() + 0.5D, this.rat.getHungerAdjustedWorkSpeed(1.25D));
			double distance = Math.sqrt(this.rat.getRatDistanceSq(this.targetBlock.getX() + 0.5D, this.targetBlock.getY() + 0.5D, this.targetBlock.getZ() + 0.5D));
			if (distance < 3.5D * this.rat.getRatDistanceModifier() && distance > 2.5D * this.rat.getRatDistanceModifier() && te instanceof Container container) {
				this.toggleChest(container, true);
			}
			if (distance <= 2.5D * this.rat.getRatDistanceModifier()) {
				if (te instanceof Container container) {
					this.toggleChest(container, false);
				}

				this.executeTask(te);
				this.targetBlock = null;
				this.stop();
			}
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
		if (this.type == PickupType.INVENTORY) {
			int extractSize = RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_PLATTER.get()) ? 64 : 1;
			ItemStack stack = RatInventoryAccess.extract(this.rat.level(), entity.getBlockPos(), this.rat.pickupFacing, this.rat, extractSize, "забор предметов");
			if (!stack.isEmpty()) {
				ItemStack duplicate = stack.copy();
				if (!this.rat.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() && !this.rat.level().isClientSide()) {
					this.rat.spawnAtLocation(this.rat.getItemInHand(InteractionHand.MAIN_HAND), 0.0F);
				}
				this.rat.setItemInHand(InteractionHand.MAIN_HAND, duplicate);
			}
		} else if (this.type == PickupType.ENERGY) {
			IEnergyStorage storage = this.rat.level().getCapability(Capabilities.EnergyStorage.BLOCK, entity.getBlockPos(), this.rat.pickupFacing);
			if (storage != null) {
				int howMuchWeWant = this.rat.getRFTransferRate() - this.rat.getHeldRF();
				int recievedEnergy = 0;
				try {
					howMuchWeWant = Math.min(storage.getEnergyStored(), howMuchWeWant);
					if (storage.extractEnergy(howMuchWeWant, true) > 0) {
						recievedEnergy = storage.extractEnergy(howMuchWeWant, false);
					}
				} catch (RuntimeException exception) {
					RatsMod.LOGGER.error("Крыса {} не смогла забрать энергию из блока {}", this.rat.getUUID(), entity.getBlockPos(), exception);
					throw exception;
				}
				if (recievedEnergy > 0) {
					this.rat.setHeldRF(this.rat.getHeldRF() + recievedEnergy);
				}
			}
		} else if (this.type == PickupType.FLUID) {
			IFluidHandler fluidHandler = entity == null ? null : this.resolveFluidHandler(entity.getBlockPos(), this.rat.pickupFacing);
			if (fluidHandler == null && entity == null) {
				this.drainSourceFluidBlock(this.targetBlock);
				return;
			}
			if (fluidHandler != null) {
				int currentAmount = 0;
				if (!this.rat.transportingFluid.isEmpty()) {
					currentAmount = this.rat.transportingFluid.getAmount();
				}
				int howMuchWeWant = this.rat.getMBTransferRate() - currentAmount;

				FluidStack drainedStack = null;
				try {
					if (fluidHandler.getTanks() > 0) {
						FluidStack firstTank = fluidHandler.getFluidInTank(0);
						if (fluidHandler.getTanks() > 1) {
							for (int i = 0; i < fluidHandler.getTanks(); i++) {
								FluidStack otherTank = fluidHandler.getFluidInTank(i);
								if (!this.rat.transportingFluid.isEmpty() && this.rat.transportingFluid.isFluidEqual(otherTank)) {
									firstTank = otherTank;
								}
							}
						}
						if (!firstTank.isEmpty() && (this.rat.transportingFluid.isEmpty() || this.rat.transportingFluid.isFluidEqual(firstTank))) {
							howMuchWeWant = Math.min(firstTank.getAmount(), howMuchWeWant);

							fluidHandler.drain(howMuchWeWant, IFluidHandler.FluidAction.SIMULATE);
							drainedStack = fluidHandler.drain(howMuchWeWant, IFluidHandler.FluidAction.EXECUTE);
						}
					}
				} catch (RuntimeException exception) {
					RatsMod.LOGGER.error("Крыса {} не смогла забрать жидкость из блока {}", this.rat.getUUID(), entity.getBlockPos(), exception);
					throw exception;
				}
				if (drainedStack != null) {
					if (this.rat.transportingFluid.isEmpty()) {
						this.rat.transportingFluid = drainedStack.copy();
					} else {
						this.rat.transportingFluid.setAmount(this.rat.transportingFluid.getAmount() + Math.max(drainedStack.getAmount(), 0));
					}
					if (!this.rat.level().isClientSide()) {
						RatsNetworkHandler.CHANNEL.sendToTrackingEntityAndSelf(this.rat, new UpdateRatFluidPacket(this.rat.getId(), this.rat.transportingFluid));
					}
					SoundEvent sound = SoundEvents.BUCKET_FILL;
					this.rat.playSound(sound, 1, 1);
				}
			}
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
					RatsMod.LOGGER.debug("Крыса {} использует сторону {} для забора жидкости из {}, потому что сторона {} не дала обработчик", this.rat.getUUID(), direction, pos, preferredSide);
					return handler;
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла найти жидкостный обработчик для забора из {}", this.rat.getUUID(), pos, exception);
			throw exception;
		}
	}

	// Проверяет, можно ли забрать жидкость из блока-источника; принимает позицию, отдаёт true только для чистого source-блока.
	private boolean canDrainSourceFluid(BlockPos pos) {
		try {
			if (this.type != PickupType.FLUID || pos == null) {
				return false;
			}
			BlockState state = this.rat.level().getBlockState(pos);
			FluidState fluidState = state.getFluidState();
			return fluidState.isSource() && !fluidState.isEmpty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить источник жидкости {}", this.rat.getUUID(), pos, exception);
			throw exception;
		}
	}

	// Забирает жидкость из блока-источника; принимает позицию источника, обновляет переносимую жидкость крысы.
	private void drainSourceFluidBlock(BlockPos pos) {
		try {
			if (!this.canDrainSourceFluid(pos)) {
				RatsMod.LOGGER.warn("Крыса {} не нашла жидкость-источник в {}", this.rat.getUUID(), pos);
				return;
			}
			int currentAmount = this.rat.transportingFluid.isEmpty() ? 0 : this.rat.transportingFluid.getAmount();
			int wantedAmount = this.rat.getMBTransferRate() - currentAmount;
			if (wantedAmount <= 0) {
				return;
			}
			FluidState fluidState = this.rat.level().getFluidState(pos);
			FluidStack drainedStack = new FluidStack(fluidState.getType(), Math.min(FluidType.BUCKET_VOLUME, wantedAmount));
			if (!this.rat.transportingFluid.isEmpty() && !this.rat.transportingFluid.isFluidEqual(drainedStack)) {
				RatsMod.LOGGER.warn("Крыса {} не может смешать {} с {}", this.rat.getUUID(), drainedStack.getHoverName().getString(), this.rat.transportingFluid.getHoverName().getString());
				return;
			}
			this.rat.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			if (this.rat.transportingFluid.isEmpty()) {
				this.rat.transportingFluid = drainedStack.copy();
			} else {
				this.rat.transportingFluid.setAmount(this.rat.transportingFluid.getAmount() + drainedStack.getAmount());
			}
			if (!this.rat.level().isClientSide()) {
				RatsNetworkHandler.CHANNEL.sendToTrackingEntityAndSelf(this.rat, new UpdateRatFluidPacket(this.rat.getId(), this.rat.transportingFluid));
			}
			this.rat.playSound(SoundEvents.BUCKET_FILL, 1, 1);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла забрать жидкость из источника {}", this.rat.getUUID(), pos, exception);
			throw exception;
		}
	}

	@Override
	public TaskType getRatTaskType() {
		return TaskType.PICKUP;
	}

	public enum PickupType {
		INVENTORY,
		FLUID,
		ENERGY
	}
}
