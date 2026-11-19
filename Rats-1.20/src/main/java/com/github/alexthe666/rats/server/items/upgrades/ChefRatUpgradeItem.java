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
	// Creates the chef upgrade; takes item properties, returns nothing.
	public ChefRatUpgradeItem(Properties properties) {
		super(properties, 2, 2);
	}

	// Reports whether the item in the paws counts as food; takes the rat, returns false for the custom cooking logic.
	@Override
	public boolean isRatHoldingFood(TamedRat rat) {
		try {
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to check the chef item in its paws", rat.getUUID(), exception);
			throw exception;
		}
	}

	// Checks whether the item should be delivered; takes the rat and stack, returns true if the chef cannot cook it.
	@Override
	public boolean shouldDepositItem(TamedRat rat, ItemStack stack) {
		try {
			return this.getCookingResultFor(rat, stack).isEmpty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Rat {} failed to check delivering chef item {}", rat.getUUID(), stack, exception);
			throw exception;
		}
	}

	// Runs the chef upgrade tick; takes the rat, cooks the item in its paws only away from Farmer's Delight work blocks.
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
			RatsMod.LOGGER.error("Rat {} failed to run the chef upgrade tick", rat.getUUID(), exception);
			throw exception;
		}
	}

	// Cooks the item in the paws; takes the rat, replaces or delivers the result when progress completes.
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
			RatsMod.LOGGER.error("Rat {} failed to cook the item in its paws {}", rat.getUUID(), rat.getMainHandItem(), exception);
			throw exception;
		}
	}

	// Returns the in-paw cooking result; takes the rat and stack, returns the cooked stack or an empty stack.
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
			RatsMod.LOGGER.error("Rat {} failed to find the chef cooking result for {}", rat.getUUID(), stack, exception);
			throw exception;
		}
	}

	// Checks whether old in-paw cooking should stop in favour of an FD station; takes the rat, returns true for an active FD setup.
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
			RatsMod.LOGGER.error("Rat {} failed to check the chef FD setup", rat.getUUID(), exception);
			throw exception;
		}
	}

	// Protects tools from in-paw smelting; takes the stack, returns true for items with durability.
	private boolean shouldProtectToolFromHeldCooking(ItemStack stack) {
		try {
			return stack.isDamageableItem();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check chef tool protection {}", stack, exception);
			throw exception;
		}
	}
}
