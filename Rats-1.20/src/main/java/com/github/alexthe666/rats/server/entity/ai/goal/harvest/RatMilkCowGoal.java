package com.github.alexthe666.rats.server.entity.ai.goal.harvest;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.message.UpdateRatFluidPacket;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class RatMilkCowGoal extends BaseRatHarvestGoal {
	private final TamedRat rat;
	private final Predicate<LivingEntity> COW_PREDICATE = entity -> entity != null && RatUtils.isCow(entity) && !entity.isBaby();

	public RatMilkCowGoal(TamedRat rat) {
		super(rat);
		this.rat = rat;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (!super.canUse() || !this.checkTheBasics(false, false)) {
			return false;
		}
		if (!this.rat.transportingFluid.isEmpty() && this.rat.transportingFluid.getAmount() >= this.rat.getMBTransferRate()) {
			return false;
		}
		this.resetTarget();
		if (this.getTargetEntity() == null && !this.rat.level().isClientSide() && this.rat.tickCount % 200 == 0) {
			RatsMod.LOGGER.warn("Rat {} found no adult animal to milk within radius {}", this.rat.getUUID(), this.rat.getRadius());
		}
		return this.getTargetEntity() != null;
	}

	// Checks whether milking continues; no parameters, returns true while there is a target and room for milk.
	@Override
	public boolean canContinueToUse() {
		return this.getTargetEntity() != null && (this.rat.transportingFluid.isEmpty() || this.rat.transportingFluid.getAmount() < this.rat.getMBTransferRate());
	}

	// Approaches the animal and collects milk; no parameters, returns nothing.
	@Override
	public void tick() {
		try {
			if (this.getTargetEntity() != null && this.getTargetEntity().isAlive() && (this.rat.transportingFluid.isEmpty() || this.rat.transportingFluid.getAmount() < this.rat.getMBTransferRate())) {
				this.rat.moveSmoothlyTo(this.getTargetEntity(), this.rat.getHungerAdjustedWorkSpeed(1.25D));
				if (this.rat.distanceToSqr(this.getTargetEntity()) < this.rat.getRatHarvestDistance(0.0D)) {
					FluidStack milkFluid = this.createMilkStack();
					if (!this.rat.transportingFluid.isEmpty() && !this.rat.transportingFluid.getFluid().isSame(milkFluid.getFluid())) {
						RatsMod.LOGGER.warn("Rat {} cannot milk {}: already carrying {}", this.rat.getUUID(), this.getTargetEntity().getType().getDescriptionId(), this.rat.transportingFluid.getHoverName().getString());
						this.stop();
						return;
					}
					int freeAmount = this.rat.getMBTransferRate() - (this.rat.transportingFluid.isEmpty() ? 0 : this.rat.transportingFluid.getAmount());
					if (freeAmount <= 0) {
						this.stop();
						return;
					}
					milkFluid.setAmount(Math.min(milkFluid.getAmount(), freeAmount));
					if (this.rat.transportingFluid.isEmpty()) {
						this.rat.transportingFluid = milkFluid.copy();
					} else {
						this.rat.transportingFluid.setAmount(this.rat.transportingFluid.getAmount() + milkFluid.getAmount());
					}
					if (!this.rat.level().isClientSide()) {
						RatsNetworkHandler.CHANNEL.sendToTrackingEntityAndSelf(this.rat, new UpdateRatFluidPacket(this.rat.getId(), this.rat.transportingFluid));
					}
					this.rat.playSound(SoundEvents.COW_MILK, 1, 1);
					this.rat.gameEvent(GameEvent.ENTITY_INTERACT);
					this.stop();
				}
			} else {
				this.stop();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to milk animal {}", this.rat.getUUID(), this.getTargetEntity(), exception);
			throw exception;
		}
	}

	// Creates the milk stack for the milker; no parameters, returns a bucket of NeoForge milk.
	private FluidStack createMilkStack() {
		try {
			FluidBucketWrapper milkWrapper = new FluidBucketWrapper(new ItemStack(Items.MILK_BUCKET));
			FluidStack milkFluid = milkWrapper.getFluid().copy();
			if (milkFluid.isEmpty()) {
				milkFluid = new FluidStack(NeoForgeMod.MILK.get(), 1000);
			}
			if (milkFluid.isEmpty()) {
				throw new IllegalStateException("NeoForge did not return fluid milk for a milk bucket");
			}
			return milkFluid;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to create fluid milk for the milker rat {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	// Finds the nearest adult animal to milk; no parameters, updates the target.
	private void resetTarget() {
		try {
			int radius = this.rat.getRadius();
			AABB bb = new AABB(-radius, -radius, -radius, radius, radius, radius).move(this.rat.getSearchCenter());
			List<LivingEntity> list = this.rat.level().getEntitiesOfClass(LivingEntity.class, bb, COW_PREDICATE);
			LivingEntity closestCow = null;
			for (LivingEntity base : list) {
				if (closestCow == null || base.distanceToSqr(this.rat) < closestCow.distanceToSqr(this.rat)) {
					closestCow = base;
				}
			}
			if (closestCow != null) {
				this.setTargetEntity(closestCow);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to find an animal to milk", this.rat.getUUID(), exception);
			throw exception;
		}
	}
}
