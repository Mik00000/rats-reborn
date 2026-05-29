package com.github.alexthe666.rats.server.entity.ai.goal.harvest;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RatBreedAnimalsGoal extends BaseRatHarvestGoal {
	private final TamedRat rat;
	private final List<Animal> targets = new ArrayList<>();
	private int targetIndex;

	public RatBreedAnimalsGoal(TamedRat rat) {
		super(rat);
		this.rat = rat;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (!super.canUse() || !this.checkTheBasics(false, false)) {
			return false;
		}
		this.resetTargets();
		return !this.targets.isEmpty();
	}

	@Override
	public void tick() {
		try {
			if (this.targets.isEmpty()) {
				this.stop();
				return;
			}
			if (this.rat.getMainHandItem().isEmpty() && !this.tryTakeFoodFromPickupChest()) {
				this.stop();
				return;
			}
			Animal target = this.findNextFeedTarget(this.rat.getMainHandItem());
			if (target == null) {
				this.stop();
				return;
			}
			this.rat.moveSmoothlyTo(target, this.rat.getHungerAdjustedWorkSpeed(1.25D));
			if (this.rat.distanceToSqr(target) < this.rat.getRatHarvestDistance(1.0D)) {
				ItemStack food = this.rat.getMainHandItem();
				if (!food.isEmpty() && target.isFood(food)) {
					target.setInLove(null);
					food.shrink(1);
					this.rat.gameEvent(GameEvent.ENTITY_INTERACT);
					this.targetIndex++;
					if (this.rat.getMainHandItem().isEmpty()) {
						this.tryTakeFoodFromPickupChest();
					}
				}
			}
		} catch (Exception e) {
			RatsMod.LOGGER.error("RatBreedAnimalsGoal.tick: {}", e.getMessage(), e);
			this.stop();
		}
	}

	@Override
	public void stop() {
		super.stop();
		this.targets.clear();
		this.targetIndex = 0;
	}

	// Ищет ближайшее ещё не накормленное животное под текущую еду; принимает стек еды, отдаёт животное или null.
	private Animal findNextFeedTarget(ItemStack food) {
		try {
			if (food.isEmpty()) {
				return null;
			}
			for (int i = this.targetIndex; i < this.targets.size(); i++) {
				Animal animal = this.targets.get(i);
				if (animal.isAlive() && !animal.isBaby() && animal.getAge() == 0 && !animal.isInLove() && animal.isFood(food)) {
					this.targetIndex = i;
					return animal;
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти животное для кормления у крысы {}", this.rat.getUUID(), exception);
			throw exception;
		}
	}

	private boolean tryTakeFoodFromPickupChest() {
		return this.tryTakeFoodFromPickupChest(1);
	}

	private boolean tryTakeFoodFromPickupChest(int amount) {
		Optional<BlockPos> pickup = this.rat.getPickupPos().map(GlobalPos::pos);
		if (pickup.isEmpty() || amount <= 0) {
			return false;
		}
		BlockEntity be = this.rat.level().getBlockEntity(pickup.get());
		if (!(be instanceof Container container)) {
			return false;
		}
		ItemStack hand = this.rat.getMainHandItem();
		int needed = amount - (hand.isEmpty() ? 0 : hand.getCount());
		if (needed <= 0) {
			return true;
		}
		for (int i = 0; i < container.getContainerSize() && needed > 0; i++) {
			ItemStack stack = container.getItem(i);
			if (!stack.isEmpty() && this.isBreedingFood(stack)) {
				int take = Math.min(needed, stack.getCount());
				ItemStack taken = stack.split(take);
				if (hand.isEmpty()) {
					this.rat.setItemInHand(InteractionHand.MAIN_HAND, taken);
				} else if (ItemStack.isSameItemSameComponents(hand, taken)) {
					hand.grow(taken.getCount());
				} else {
					this.rat.spawnAtLocation(taken, 0.0F);
					continue;
				}
				needed -= take;
			}
		}
		return !this.rat.getMainHandItem().isEmpty();
	}

	private boolean isBreedingFood(ItemStack stack) {
		int radius = this.rat.getRadius();
		AABB bb = new AABB(-radius, -radius, -radius, radius, radius, radius).move(this.rat.getSearchCenter());
		List<Animal> animals = this.rat.level().getEntitiesOfClass(Animal.class, bb, a -> !(a instanceof AbstractRat) && !a.isBaby() && a.getAge() == 0 && !a.isInLove());
		for (Animal animal : animals) {
			if (animal.isFood(stack)) {
				return true;
			}
		}
		return false;
	}

	private void resetTargets() {
		this.targets.clear();
		this.targetIndex = 0;
		this.tryTakeFoodFromPickupChest(64);
		int radius = this.rat.getRadius();
		AABB bb = new AABB(-radius, -radius, -radius, radius, radius, radius).move(this.rat.getSearchCenter());
		List<Animal> list = this.rat.level().getEntitiesOfClass(Animal.class, bb, entity ->
				!(entity instanceof AbstractRat) && !entity.isBaby() && entity.getAge() == 0 && !entity.isInLove());
		Map<net.minecraft.world.entity.EntityType<?>, List<Animal>> byType = list.stream()
				.collect(Collectors.groupingBy(Animal::getType));
		for (List<Animal> group : byType.values()) {
			group.sort(Comparator.comparingDouble(this.rat::distanceToSqr));
			this.targets.addAll(group);
		}
		this.targets.sort(Comparator.comparingDouble(this.rat::distanceToSqr));
	}
}
