package com.github.alexthe666.rats.server.items.upgrades;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.TickRatUpgrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class EnchanterRatUpgradeItem extends BaseRatUpgradeItem implements TickRatUpgrade {
	private static final int ENCHANTING_PROGRESS_TIME = 250;

	public EnchanterRatUpgradeItem(Properties properties, int rarity, int textLength) {
		super(properties, rarity, textLength);
	}

	// Запрещает крысе есть предметы, которые держит колдун; принимает крысу, отдаёт false.
	@Override
	public boolean isRatHoldingFood(TamedRat rat) {
		return false;
	}

	// Решает, можно ли сдавать предмет в склад; принимает крысу и стек, отдаёт false для предметов, которые колдун должен обработать.
	@Override
	public boolean shouldDepositItem(TamedRat rat, ItemStack stack) {
		return !this.canProcessStack(rat, stack, this == RatsItemRegistry.RAT_UPGRADE_DISENCHANTER.get());
	}

	// Выполняет тик улучшения колдуна; принимает крысу, ничего не отдаёт.
	@Override
	public void tick(TamedRat rat) {
		try {
			if (!rat.getMainHandItem().isEmpty()) {
				this.tryEnchanting(rat, this == RatsItemRegistry.RAT_UPGRADE_DISENCHANTER.get());
				this.createFinishedParticles(rat, ParticleTypes.ENCHANT, 2, ParticleTypes.SMOKE, 0.0F);
			} else if (rat.cookingProgress > 0) {
				rat.cookingProgress = 0;
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла выполнить зачарование: {}", rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Пытается обработать предмет в лапе; принимает крысу и режим разочарования, ничего не отдаёт.
	private void tryEnchanting(TamedRat rat, boolean disenchant) {
		BlockPos enchantingTable = this.findNearbyEnchantingTable(rat);
		if (enchantingTable == null) {
			if (!rat.level().isClientSide() && rat.tickCount % 200 == 0) {
				RatsMod.LOGGER.debug("Крыса {} не может {} предмет: рядом нет стола зачарования", rat.getUUID(), disenchant ? "разочаровать" : "зачаровать");
			}
			rat.cookingProgress = 0;
			return;
		}
		ItemStack heldItem = rat.getMainHandItem();
		ItemStack burntItem = ItemStack.EMPTY;
		if (heldItem.getItem() == Items.BOOK && !disenchant) {
			burntItem = heldItem.copyWithCount(1);
		}
		if (heldItem.getItem() == Items.ENCHANTED_BOOK && disenchant) {
			burntItem = new ItemStack(Items.BOOK);
		}
		if (heldItem.isEnchantable() && !disenchant && !heldItem.isEnchanted()) {
			burntItem = heldItem.copyWithCount(1);
		}
		if (disenchant && heldItem.isEnchanted()) {
			burntItem = heldItem.copyWithCount(1);
			burntItem.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		}
		if (burntItem.isEmpty()) {
			if (!rat.level().isClientSide() && rat.tickCount % 200 == 0) {
				RatsMod.LOGGER.debug("Крыса {} не может {} предмет {}: предмет в лапе не подходит", rat.getUUID(), disenchant ? "разочаровать" : "зачаровать", heldItem.getHoverName().getString());
			}
			rat.cookingProgress = 0;
		} else {
			rat.cookingProgress++;
			if (rat.cookingProgress >= ENCHANTING_PROGRESS_TIME) {
				if (!disenchant) {
					float power = this.getEnchantingPower(rat, enchantingTable);
					int enchantingLevel = this.getEnchantingLevel(rat, power);
					var enchantments = rat.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
					burntItem = EnchantmentHelper.enchantItem(rat.getRandom(), burntItem, enchantingLevel,
							enchantments.listElements().map(holder -> (Holder<Enchantment>) holder).filter(holder -> holder.is(EnchantmentTags.IN_ENCHANTING_TABLE)));
					if (!rat.level().isClientSide()) {
						RatsMod.LOGGER.info("Крыса {} зачаровала предмет у стола {}: сила полок={}, уровень зачарования={}", rat.getUUID(), enchantingTable, power, enchantingLevel);
					}
				}
				if (heldItem.getCount() == 1) {
					heldItem.shrink(1);
					rat.setItemInHand(InteractionHand.MAIN_HAND, burntItem);
				} else if (this.depositResult(rat, burntItem)) {
					heldItem.shrink(1);
				} else if (!rat.level().isClientSide()) {
					RatsMod.LOGGER.warn("Крыса {} не смогла положить результат {}: нет доступного контейнера склада", rat.getUUID(), burntItem.getHoverName().getString());
				}
				rat.cookingProgress = 0;
			}
		}
	}

	// Проверяет, подходит ли стек для работы колдуна; принимает крысу, стек и режим, отдаёт true/false.
	private boolean canProcessStack(TamedRat rat, ItemStack stack, boolean disenchant) {
		try {
			if (stack.isEmpty()) {
				return false;
			}
			if (disenchant) {
				return stack.getItem() == Items.ENCHANTED_BOOK || stack.isEnchanted();
			}
			return stack.getItem() == Items.BOOK || stack.isEnchantable() && !stack.isEnchanted();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить предмет {} для работы колдуна", rat.getUUID(), stack, exception);
			throw exception;
		}
	}

	// Ищет стол зачарования для работы крысы; принимает крысу, отдаёт позицию ближайшего стола или null.
	private BlockPos findNearbyEnchantingTable(TamedRat rat) {
		try {
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			BlockPos best = null;
			double bestDistance = Double.MAX_VALUE;
			for (int x = -2; x <= 2; x++) {
				for (int y = -1; y <= 1; y++) {
					for (int z = -2; z <= 2; z++) {
						cursor.set(rat.blockPosition().getX() + x, rat.blockPosition().getY() + y, rat.blockPosition().getZ() + z);
						if (rat.level().getBlockState(cursor).getBlock() instanceof EnchantingTableBlock) {
							double distance = rat.distanceToSqr(cursor.getX() + 0.5D, cursor.getY() + 0.5D, cursor.getZ() + 0.5D);
							if (distance < bestDistance) {
								bestDistance = distance;
								best = cursor.immutable();
							}
						}
					}
				}
			}
			return best;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось найти стол зачарования для крысы {}", rat.getUUID(), exception);
			throw exception;
		}
	}

	// Считает силу книжных полок вокруг стола; принимает крысу и позицию стола, отдаёт суммарную силу.
	private float getEnchantingPower(TamedRat rat, BlockPos enchantingTable) {
		try {
			float power = 0;
			for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
				BlockPos shelfPos = enchantingTable.offset(offset);
				if (EnchantingTableBlock.isValidBookShelf(rat.level(), enchantingTable, shelfPos)) {
					power += rat.level().getBlockState(shelfPos).getEnchantPowerBonus(rat.level(), shelfPos);
				}
			}
			return power;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось посчитать силу зачарования для крысы {} у стола {}", rat.getUUID(), enchantingTable, exception);
			throw exception;
		}
	}

	// Переводит силу книжных полок в уровень зачарования; принимает крысу и силу полок, отдаёт уровень 1-30.
	private int getEnchantingLevel(TamedRat rat, float power) {
		try {
			int level = 1 + Math.round(power * 2.0F) + rat.getRandom().nextInt(3);
			return Math.max(1, Math.min(30, level));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось посчитать уровень зачарования для крысы {} при силе полок {}", rat.getUUID(), power, exception);
			throw exception;
		}
	}

	// Кладёт результат обработки в склад крысы; принимает крысу и предмет, отдаёт true если предмет полностью принят.
	private boolean depositResult(TamedRat rat, ItemStack result) {
		try {
			if (rat.getDepositPos().isEmpty() || !rat.getDepositPos().get().dimension().equals(rat.level().dimension())) {
				return rat.tryDepositItemInContainers(result);
			}
			BlockEntity entity = rat.level().getBlockEntity(rat.getDepositPos().get().pos());
			if (entity == null) {
				RatsMod.LOGGER.warn("Крыса {} не нашла блок склада для результата зачарования в {}", rat.getUUID(), rat.getDepositPos().get().pos());
				return false;
			}
			IItemHandler handler = rat.level().getCapability(Capabilities.ItemHandler.BLOCK, entity.getBlockPos(), rat.depositFacing);
			if (handler == null) {
				for (Direction direction : Direction.values()) {
					handler = rat.level().getCapability(Capabilities.ItemHandler.BLOCK, entity.getBlockPos(), direction);
					if (handler != null) {
						break;
					}
				}
			}
			if (handler == null) {
				RatsMod.LOGGER.warn("Крыса {} не нашла доступный item handler у склада {}", rat.getUUID(), entity.getBlockPos());
				return false;
			}
			return ItemHandlerHelper.insertItem(handler, result.copy(), true).isEmpty() && ItemHandlerHelper.insertItem(handler, result.copy(), false).isEmpty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла положить результат зачарования {}", rat.getUUID(), result.getHoverName().getString(), exception);
			throw exception;
		}
	}
}
