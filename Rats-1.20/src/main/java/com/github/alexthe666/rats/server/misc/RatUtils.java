package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.data.tags.RatsItemTags;
import com.github.alexthe666.rats.registry.RatVariantRegistry;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.block.RatTubeBlock;
import com.github.alexthe666.rats.server.entity.rat.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@SuppressWarnings("deprecation")
public class RatUtils {

	/** Сколько раз нужно скормить сыр — гарантированное приручение. */
	public static final int WILD_RAT_CHEESE_FEEDINGS_TO_TAME = 5;
	/** Порог доверия для шанса приручения раньше лимита по сыру. */
	public static final int WILD_RAT_TRUST_FOR_RANDOM_TAME = 50;
	/** Шанс приручения при доверии ≥ порога: 1 из N (2 = 50%). */
	public static final int WILD_RAT_TRUST_RANDOM_ROLL = 2;
	/** Прирост доверия за один кусок сыра (база + случайное). */
	public static final int WILD_RAT_TRUST_GAIN_BASE = 20;
	public static final int WILD_RAT_TRUST_GAIN_SPREAD = 15;

	public static final Predicate<LivingEntity> UNTAMED_RAT_SELECTOR = entity -> entity instanceof AbstractRat rat && !rat.isTame();

	public static boolean isRidingOrBeingRiddenBy(Entity us, Entity entity) {
		for (Entity selected : us.getPassengers()) {
			if (selected.equals(entity)) {
				return true;
			}

			if (isRidingOrBeingRiddenBy(selected, entity)) {
				return true;
			}
		}

		return false;
	}

	public static BlockPos getPositionRelativetoWater(TamedRat rat, Level level, double x, double z, RandomSource rng) {
		BlockPos pos;
		BlockPos topY = BlockPos.containing(x, rat.getY(), z);
		BlockPos bottomY = BlockPos.containing(x, rat.getY(), z);
		while (level.getBlockState(topY).is(Blocks.WATER) && topY.getY() < level.getMaxBuildHeight()) {
			topY = topY.above();
		}
		while (level.getBlockState(bottomY).is(Blocks.WATER) && bottomY.getY() > level.getMinBuildHeight()) {
			bottomY = bottomY.below();
		}
		for (int tries = 0; tries < 5; tries++) {
			pos = BlockPos.containing(x, bottomY.getY() + 1 + rng.nextInt(Math.max(1, topY.getY() - bottomY.getY() - 2)), z);
			if (level.getBlockState(pos).is(Blocks.WATER)) {
				return pos;
			}
		}
		return rat.blockPosition();
	}

	public static boolean isRatFood(ItemStack stack) {
		return (stack.has(DataComponents.FOOD) || stack.is(Tags.Items.SEEDS) || stack.is(Items.WHEAT)) && !stack.is(RatsItemRegistry.RAW_RAT.get()) && !stack.is(RatsItemRegistry.COOKED_RAT.get());
	}

	/**
	 * Проверяет, является ли предмет сыром для приручения дикой крысы.
	 * Параметры: {@code stack} — предмет в руке или на земле. Возвращает true для сыра мода и тега {@code c:cheese}.
	 */
	public static boolean isCheeseItem(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		return stack.is(RatsItemRegistry.CHEESE.get())
				|| stack.is(RatsItemRegistry.BLUE_CHEESE.get())
				|| stack.is(RatsItemRegistry.NETHER_CHEESE.get())
				|| stack.is(RatsItemRegistry.STRING_CHEESE.get())
				|| stack.is(RatsItemRegistry.CREATIVE_CHEESE.get())
				|| stack.is(RatsItemTags.CHEESE_ITEMS);
	}

	/**
	 * Определяет игрока, бросившего предмет на землю (для прогресса приручения).
	 * Параметры: {@code itemEntity} — сущность предмета, {@code level} — уровень.
	 * Возвращает игрока или пусто, если источник не найден.
	 */
	public static Optional<Player> resolveCheeseFeedingPlayer(ItemEntity itemEntity, Level level) {
		try {
			if (!(level instanceof ServerLevel server)) {
				return Optional.empty();
			}
			Entity owner = itemEntity.getOwner();
			if (owner instanceof Player player) {
				return Optional.of(player);
			}
			UUID throwerId = readItemEntityThrowerUuid(itemEntity);
			if (throwerId != null) {
				Entity thrower = server.getEntity(throwerId);
				if (thrower instanceof Player player) {
					return Optional.of(player);
				}
			}
			Player nearestPlayer = level.getNearestPlayer(itemEntity, 16.0D);
			if (nearestPlayer != null) {
				RatsMod.LOGGER.info("Игрок для кормления крысы определён по ближайшему к сыру: {}", nearestPlayer.getGameProfile().getName());
				return Optional.of(nearestPlayer);
			}
			return Optional.empty();
		} catch (Exception e) {
			RatsMod.LOGGER.error("Не удалось определить игрока для предмета на земле: {}", e.getMessage(), e);
			return Optional.empty();
		}
	}

	/**
	 * Читает UUID бросившего из {@link ItemEntity} (поле thrower в Mojmap).
	 * Параметры: {@code itemEntity} — предмет. Возвращает UUID или null.
	 */
	@Nullable
	private static UUID readItemEntityThrowerUuid(ItemEntity itemEntity) {
		try {
			Field throwerField = ItemEntity.class.getDeclaredField("thrower");
			throwerField.setAccessible(true);
			return (UUID) throwerField.get(itemEntity);
		} catch (ReflectiveOperationException e) {
			RatsMod.LOGGER.error("Поле thrower у ItemEntity недоступно: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Увеличивает доверие дикой крысы от сыра; при пороге заменяет сущность на приручённую.
	 * Параметры: {@code rat} — дикая крыса, {@code player} — кормящий игрок.
	 * Возвращает приручённую крысу, если приручение произошло, иначе null.
	 */
	@Nullable
	public static TamedRat tryProgressWildRatTaming(Rat rat, Player player) {
		try {
			if (rat.hasPlague() || rat.level().isClientSide()) {
				return null;
			}
			rat.setFleePos(rat.blockPosition());
			rat.wildTrust += WILD_RAT_TRUST_GAIN_BASE + rat.getRandom().nextInt(WILD_RAT_TRUST_GAIN_SPREAD);
			rat.cheeseFeedings++;
			rat.level().broadcastEntityEvent(rat, (byte) 82);
			RatsMod.LOGGER.info(
					"Прогресс приручения крысы: игрок={}, сыр={}/{}, доверие={}",
					player.getGameProfile().getName(),
					rat.cheeseFeedings,
					WILD_RAT_CHEESE_FEEDINGS_TO_TAME,
					rat.wildTrust);
			if ((rat.wildTrust >= WILD_RAT_TRUST_FOR_RANDOM_TAME && rat.getRandom().nextInt(WILD_RAT_TRUST_RANDOM_ROLL) == 0)
					|| rat.cheeseFeedings >= WILD_RAT_CHEESE_FEEDINGS_TO_TAME) {
				TamedRat tamedRat = tameRat(rat, rat.level());
				tamedRat.level().broadcastEntityEvent(rat, (byte) 83);
				tamedRat.tame(player);
				tamedRat.setCommand(RatCommand.SIT);
				RatsMod.LOGGER.info("Крыса приручена игроком {}", player.getGameProfile().getName());
				return tamedRat;
			}
			return null;
		} catch (Exception e) {
			RatsMod.LOGGER.error("Ошибка приручения дикой крысы игроком {}: {}", player.getGameProfile().getName(), e.getMessage(), e);
			throw new IllegalStateException("Ошибка приручения дикой крысы: " + e.getMessage(), e);
		}
	}

	public static boolean shouldRaidItem(ItemStack stack) {
		return isRatFood(stack) && !stack.is(RatsItemRegistry.CONTAMINATED_FOOD.get());
	}

	public static boolean doesContainFood(Container inventory) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (isRatFood(inventory.getItem(i))) {
				return true;
			}
		}
		return false;
	}

	public static ItemStack getFoodFromInventory(Container inventory, RandomSource random) {
		List<ItemStack> items = new ArrayList<>();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (shouldRaidItem(stack)) {
				items.add(stack);
			}
		}
		if (items.isEmpty()) {
			return ItemStack.EMPTY;
		} else if (items.size() == 1) {
			return items.get(0);
		} else {
			return items.get(random.nextInt(items.size() - 1));
		}
	}

	public static int getContaminatedSlot(Container inventory, RandomSource random) {
		List<Integer> items = new ArrayList<>();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty() || stack.is(RatsItemRegistry.CONTAMINATED_FOOD.get())) {
				items.add(i);
			}
		}
		if (items.isEmpty()) {
			return -1;
		} else if (items.size() == 1) {
			return items.get(0);
		} else {
			return items.get(random.nextInt(items.size() - 1));
		}
	}

	public static int getItemSlotFromItemHandler(TamedRat rat, IItemHandler handler, RandomSource random) {
		List<Integer> items = new ArrayList<>();
		for (int i = 0; i < handler.getSlots(); i++) {
			ItemStack stack = handler.extractItem(i, handler.getSlotLimit(i), true);
			if (rat.canRatPickupItem(stack)) {
				items.add(i);
			}
		}
		if (items.isEmpty()) {
			return -1;
		} else if (items.size() == 1) {
			return items.get(0);
		} else {
			return items.get(random.nextInt(items.size()));
		}
	}

	public static boolean isPredator(Entity entity) {
		return entity instanceof Ocelot || entity instanceof Cat || entity instanceof Fox;
	}

	public static RatCommand wrapCommand(int newCommand) {
		int length = RatCommand.values().length;
		if (newCommand >= length) {
			newCommand = 0;
		}
		if (newCommand < 0) {
			newCommand = length - 1;
		}
		return RatCommand.values()[newCommand];
	}

	public static BlockPos findLowestWater(BlockPos pos, PathfinderMob rat) {
		if (rat.level().getBlockState(pos).is(Blocks.WATER)) {
			return pos;
		} else {
			BlockPos blockpos;
			do {
				blockpos = pos.below();
			} while (blockpos.getY() > rat.level().getMinBuildHeight() && rat.level().getBlockState(blockpos).is(Blocks.WATER));
			return blockpos;
		}
	}

	public static boolean canRatBreakBlock(Level level, BlockPos pos, DiggingRat rat) {
		BlockState blockState = level.getBlockState(pos);
		if (blockState.isAir() || blockState.liquid() || level.getBlockEntity(pos) != null) {
			return false;
		}
		float hardness = blockState.getDestroySpeed(level, pos);
		return hardness >= 0.0F && hardness <= RatConfig.ratStrengthThreshold && EventHooks.onEntityDestroyBlock(rat, pos, level.getBlockState(pos));
	}

	public static boolean canRatPlaceBlock(Level level, BlockPos pos, DiggingRat rat) {
		return EventHooks.canEntityGrief(level, rat) && !EventHooks.onBlockPlace(rat, BlockSnapshot.create(level.dimension(), level, pos), Direction.UP);
	}

	public static boolean isBlockProtected(Level level, BlockPos pos, DiggingRat rat) {
		return !EventHooks.canEntityGrief(level, rat) || !EventHooks.onEntityDestroyBlock(rat, pos, level.getBlockState(pos));
	}

	public static boolean isOpenRatTube(BlockGetter getter, BlockPos pos) {
		BlockState state = getter.getBlockState(pos);
		if (state.getBlock() instanceof RatTubeBlock) {
			for (int i = 0; i < Direction.values().length; i++) {
				BooleanProperty bool = RatTubeBlock.ALL_OPEN_PROPS[i];
				if (state.getValue(bool)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isCow(Entity entity) {
		String s = entity.getType().getDescriptionId();
		return entity instanceof Cow || s.contains("cow");
	}

	public static void polinateAround(Level level, BlockPos position) {
		int RADIUS = 10;
		List<BlockPos> allBlocks = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosedStream(position.offset(-RADIUS, -RADIUS, -RADIUS), position.offset(RADIUS, RADIUS, RADIUS)).map(BlockPos::immutable).toList()) {
			if (canPlantBeBonemealed(level, pos, level.getBlockState(pos))) {
				allBlocks.add(pos);
			}
		}
		if (!allBlocks.isEmpty()) {
			for (BlockPos pos : allBlocks) {
				BlockState block = level.getBlockState(pos);
				if (block.getBlock() instanceof BonemealableBlock igrowable) {
					if (igrowable.isValidBonemealTarget(level, pos, block) && level.getRandom().nextInt(3) == 0) {
						if (!level.isClientSide()) {
							level.levelEvent(2005, pos, 0);
							igrowable.performBonemeal((ServerLevel) level, level.getRandom(), pos, block);
						}
					}
				}
			}
		}
	}

	private static boolean canPlantBeBonemealed(Level level, BlockPos pos, BlockState BlockState) {
		if (BlockState.getBlock() instanceof BonemealableBlock igrowable && !(BlockState.getBlock() instanceof TallGrassBlock) && !(BlockState.getBlock() instanceof GrassBlock)) {
			if (igrowable.isValidBonemealTarget(level, pos, BlockState)) {
				if (!level.isClientSide()) {
					//  igrowable.grow(level, level.rand, target, BlockState);
					return igrowable.isBonemealSuccess(level, level.getRandom(), pos, BlockState);
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	// Ускоряет случайный тик блока и тик блок-сущности (улучшения времени).
	public static void accelerateTick(Level level, BlockPos pos, int randomTickInt, int beTickInt) {
		if (level.isClientSide()) {
			return;
		}
		BlockState blockState = level.getBlockState(pos);
		Block block = blockState.getBlock();
		if (blockState.isRandomlyTicking() && randomTickInt <= 0 && level instanceof ServerLevel serverLevel) {
			blockState.randomTick(serverLevel, pos, level.getRandom());
		}
		if (beTickInt <= 0) {
			BlockEntity entity = level.getBlockEntity(pos);
			if (entity != null) {
				BlockEntityTicker<BlockEntity> ticker = blockState.getTicker(level, (BlockEntityType<BlockEntity>) entity.getType());
				if (ticker != null) {
					ticker.tick(level, pos, blockState, entity);
				}
			}
		}
	}

	public static TamedRat tameRat(Rat rat, Level level) {
		TamedRat newRat = new TamedRat(RatsEntityRegistry.TAMED_RAT.get(), level);
		CompoundTag tag = new CompoundTag();
		rat.addAdditionalSaveData(tag);
		newRat.setToga(rat.hasToga());
		newRat.moveTo(rat.blockPosition(), rat.getYRot(), rat.getXRot());
		if (rat.getLeashHolder() != null) {
			newRat.setLeashedTo(rat.getLeashHolder(), true);
			rat.setLeashedTo(null, true);
		}
		EventHooks.finalizeMobSpawn(newRat, (ServerLevelAccessor) level, level.getCurrentDifficultyAt(rat.blockPosition()), MobSpawnType.EVENT, null);
		newRat.readAdditionalSaveData(tag);
		newRat.setColorVariant(rat.getColorVariant());
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			newRat.setItemSlot(slot, rat.getItemBySlot(slot));
		}
		level.addFreshEntity(newRat);
		newRat.level().broadcastEntityEvent(rat, (byte) 83);
		newRat.setYRot(rat.getYRot());
		rat.discard();
		return newRat;
	}

	//helper method to convert the old rat variant integer system to the new registry
	public static RatVariant convertOldRatVariant(int variant) {
		return switch (variant) {
			case 1 -> RatVariantRegistry.BLACK.get();
			case 2 -> RatVariantRegistry.BROWN.get();
			case 3 -> RatVariantRegistry.GREEN.get();
			case 4 -> RatVariantRegistry.ALBINO.get();
			case 5 -> RatVariantRegistry.HOODED.get();
			case 6 -> RatVariantRegistry.BROWN_HOODED.get();
			case 7 -> RatVariantRegistry.GRAY_HOODED.get();
			case 8 -> RatVariantRegistry.SIAMESE.get();
			case 9 -> RatVariantRegistry.WHITE.get();
			case 10 -> RatVariantRegistry.YELLOW_HOODED.get();
			case 11, 12 -> RatVariantRegistry.BROWN_UNDERCOAT.get();
			default -> RatVariantRegistry.BLUE.get();
		};
	}
}
