package com.github.alexthe666.rats.server.items.upgrades;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.compat.farmersdelight.FarmersDelightCompat;
import com.github.alexthe666.rats.registry.RatsRecipeRegistry;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.TickRatUpgrade;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public class ChefRatUpgradeItem extends BaseRatUpgradeItem implements TickRatUpgrade {
	// Создаёт улучшение шефа; принимает свойства предмета, ничего не отдаёт.
	public ChefRatUpgradeItem(Properties properties) {
		super(properties, 2, 2);
	}

	// Сообщает, считается ли предмет в лапах едой; принимает крысу, отдаёт false для собственной логики готовки.
	@Override
	public boolean isRatHoldingFood(TamedRat rat) {
		try {
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить предмет шефа в лапах", rat.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет, нужно ли сдавать предмет; принимает крысу и стек, отдаёт true если шеф не умеет его готовить.
	@Override
	public boolean shouldDepositItem(TamedRat rat, ItemStack stack) {
		try {
			return this.getCookingResultFor(rat, stack).isEmpty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить сдачу предмета шефа {}", rat.getUUID(), stack, exception);
			throw exception;
		}
	}

	// Выполняет тик улучшения шефа; принимает крысу, готовит предмет в лапах только вне рабочих блоков Farmer's Delight.
	@Override
	public void tick(TamedRat rat) {
		try {
			if (this.shouldSkipHeldCookingForFarmersDelightWork(rat)) {
				rat.cookingProgress = 0;
				return;
			}
			if (!rat.getMainHandItem().isEmpty()) {
				this.tryCooking(rat);
				this.createFinishedParticles(rat, ParticleTypes.SMOKE, 3, ParticleTypes.FLAME, 0.125F);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла выполнить тик улучшения шефа", rat.getUUID(), exception);
			throw exception;
		}
	}

	// Готовит предмет в лапах; принимает крысу, заменяет или сдаёт результат при завершении прогресса.
	private void tryCooking(TamedRat rat) {
		try {
			ItemStack heldItem = rat.getMainHandItem();
			ItemStack burntItem = this.getCookingResultFor(rat, heldItem.copyWithCount(1));
			if (burntItem.isEmpty()) {
				rat.cookingProgress = 0;
			} else {
				rat.cookingProgress++;
				if (rat.cookingProgress == 100) {
					heldItem.shrink(1);
					if (heldItem.isEmpty()) {
						rat.setItemInHand(InteractionHand.MAIN_HAND, burntItem);
					} else {
						if (!rat.tryDepositItemInContainers(burntItem) && !rat.level().isClientSide()) {
							rat.spawnAtLocation(burntItem, 0.25F);
						}
					}
					rat.cookingProgress = 0;
				}
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла приготовить предмет в лапах {}", rat.getUUID(), rat.getMainHandItem(), exception);
			throw exception;
		}
	}

	// Возвращает результат готовки в лапах; принимает крысу и стек, отдаёт готовый стек или пустой стек.
	private ItemStack getCookingResultFor(TamedRat rat, ItemStack stack) {
		try {
			if (this.shouldProtectToolFromHeldCooking(stack)) {
				return ItemStack.EMPTY;
			}
			ItemStack specialChefRecipe = rat.getResultForRecipe(RatsRecipeRegistry.CHEF.get(), stack);
			if (!specialChefRecipe.isEmpty()) {
				return specialChefRecipe.copy();
			}

			var optional = rat.level().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), rat.level());
			if (optional.isPresent()) {
				ItemStack itemstack = optional.get().value().getResultItem(rat.level().registryAccess());
				if (!itemstack.isEmpty()) {
					ItemStack itemstack1 = itemstack.copy();
					itemstack1.setCount(stack.getCount() * itemstack.getCount());
					return itemstack1;
				}
			}
			return ItemStack.EMPTY;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла найти результат готовки шефа для {}", rat.getUUID(), stack, exception);
			throw exception;
		}
	}

	// Проверяет, нужно ли остановить старую готовку в лапах ради FD-станции; принимает крысу, отдаёт true для активной FD-разметки.
	private boolean shouldSkipHeldCookingForFarmersDelightWork(TamedRat rat) {
		try {
			if (!RatsMod.FARMERS_DELIGHT_LOADED || rat.getCommand() != RatCommand.HARVEST) {
				return false;
			}
			Optional<GlobalPos> workPos = rat.getCreateWorkPos();
			if (workPos.isEmpty() || !workPos.get().dimension().equals(rat.level().dimension())) {
				return false;
			}
			BlockEntity blockEntity = rat.level().getBlockEntity(workPos.get().pos());
			return blockEntity != null && FarmersDelightCompat.isSupportedWorkBlock(blockEntity);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить FD-разметку шефа", rat.getUUID(), exception);
			throw exception;
		}
	}

	// Защищает инструменты от плавки в лапах; принимает стек, отдаёт true для предметов с прочностью.
	private boolean shouldProtectToolFromHeldCooking(ItemStack stack) {
		try {
			return stack.isDamageableItem();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить защиту инструмента шефа {}", stack, exception);
			throw exception;
		}
	}
}
