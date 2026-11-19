package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.data.tags.RatsItemTags;
import com.github.alexthe666.rats.registry.RatVariantRegistry;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.block.RatTubeBlock;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import com.github.alexthe666.rats.server.entity.rat.DiggingRat;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.common.Tags.Items;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class RatUtils {
   public static final int WILD_RAT_CHEESE_FEEDINGS_TO_TAME = 5;
   public static final int WILD_RAT_TRUST_FOR_RANDOM_TAME = 50;
   public static final int WILD_RAT_TRUST_RANDOM_ROLL = 2;
   public static final int WILD_RAT_TRUST_GAIN_BASE = 20;
   public static final int WILD_RAT_TRUST_GAIN_SPREAD = 15;
   public static final Predicate<LivingEntity> UNTAMED_RAT_SELECTOR = entity -> {
      if (entity instanceof AbstractRat rat && !rat.isTame()) {
         return true;
      }

      return false;
   };

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
      BlockPos topY = BlockPos.containing(x, rat.getY(), z);
      BlockPos bottomY = BlockPos.containing(x, rat.getY(), z);

      while (level.getBlockState(topY).is(Blocks.WATER) && topY.getY() < level.getMaxBuildHeight()) {
         topY = topY.above();
      }

      while (level.getBlockState(bottomY).is(Blocks.WATER) && bottomY.getY() > level.getMinBuildHeight()) {
         bottomY = bottomY.below();
      }

      for (int tries = 0; tries < 5; tries++) {
         BlockPos pos = BlockPos.containing(x, (double)(bottomY.getY() + 1 + rng.nextInt(Math.max(1, topY.getY() - bottomY.getY() - 2))), z);
         if (level.getBlockState(pos).is(Blocks.WATER)) {
            return pos;
         }
      }

      return rat.blockPosition();
   }

   public static boolean isRatFood(ItemStack stack) {
      return (stack.has(DataComponents.FOOD) || stack.is(Items.SEEDS) || stack.is(net.minecraft.world.item.Items.WHEAT))
         && !stack.is((Item)RatsItemRegistry.RAW_RAT.get())
         && !stack.is((Item)RatsItemRegistry.COOKED_RAT.get());
   }

   public static boolean isCheeseItem(ItemStack stack) {
      return stack.isEmpty()
         ? false
         : stack.is((Item)RatsItemRegistry.CHEESE.get())
            || stack.is((Item)RatsItemRegistry.BLUE_CHEESE.get())
            || stack.is((Item)RatsItemRegistry.NETHER_CHEESE.get())
            || stack.is((Item)RatsItemRegistry.STRING_CHEESE.get())
            || stack.is((Item)RatsItemRegistry.CREATIVE_CHEESE.get())
            || stack.is(RatsItemTags.CHEESE_ITEMS);
   }

   public static Optional<Player> resolveCheeseFeedingPlayer(ItemEntity itemEntity, Level level) {
      try {
         if (level instanceof ServerLevel server) {
            if (itemEntity.getOwner() instanceof Player player) {
               return Optional.of(player);
            } else {
               UUID throwerId = readItemEntityThrowerUuid(itemEntity);
               if (throwerId != null && server.getEntity(throwerId) instanceof Player player) {
                  return Optional.of(player);
               } else {
                  Player nearestPlayer = level.getNearestPlayer(itemEntity, 16.0);
                  if (nearestPlayer != null) {
                     RatsMod.LOGGER.info("Player for feeding the rat determined as the closest to the cheese: {}", nearestPlayer.getGameProfile().getName());
                     return Optional.of(nearestPlayer);
                  } else {
                     return Optional.empty();
                  }
               }
            }
         } else {
            return Optional.empty();
         }
      } catch (Exception var7) {
         RatsMod.LOGGER.error("Failed to determine the player for a ground item: {}", var7.getMessage(), var7);
         return Optional.empty();
      }
   }

   @Nullable
   private static UUID readItemEntityThrowerUuid(ItemEntity itemEntity) {
      try {
         Field throwerField = ItemEntity.class.getDeclaredField("thrower");
         throwerField.setAccessible(true);
         return (UUID)throwerField.get(itemEntity);
      } catch (ReflectiveOperationException var2) {
         RatsMod.LOGGER.error("ItemEntity thrower field is not accessible: {}", var2.getMessage(), var2);
         return null;
      }
   }

   @Nullable
   public static TamedRat tryProgressWildRatTaming(Rat rat, Player player) {
      try {
         if (!rat.hasPlague() && !rat.level().isClientSide()) {
            rat.setFleePos(rat.blockPosition());
            rat.wildTrust = rat.wildTrust + 20 + rat.getRandom().nextInt(15);
            rat.cheeseFeedings++;
            rat.level().broadcastEntityEvent(rat, (byte)82);
            RatsMod.LOGGER
               .info("Rat taming progress: player={}, cheese={}/{}, trust={}", player.getGameProfile().getName(), rat.cheeseFeedings, 5, rat.wildTrust);
            if ((rat.wildTrust < 50 || rat.getRandom().nextInt(2) != 0) && rat.cheeseFeedings < 5) {
               return null;
            } else {
               TamedRat tamedRat = tameRat(rat, rat.level());
               tamedRat.level().broadcastEntityEvent(rat, (byte)83);
               tamedRat.tame(player);
               tamedRat.setCommand(RatCommand.SIT);
               RatsMod.LOGGER.info("Rat tamed by player {}", player.getGameProfile().getName());
               return tamedRat;
            }
         } else {
            return null;
         }
      } catch (Exception var3) {
         RatsMod.LOGGER.error("Error taming a wild rat by player {}: {}", player.getGameProfile().getName(), var3.getMessage(), var3);
         throw new IllegalStateException("Error taming a wild rat: " + var3.getMessage(), var3);
      }
   }

   public static boolean shouldRaidItem(ItemStack stack) {
      return isRatFood(stack) && !stack.is((Item)RatsItemRegistry.CONTAMINATED_FOOD.get());
   }

   public static boolean doesContainFood(Level level, BlockPos pos, BlockEntity entity) {
      if (entity instanceof Container inventory) {
         for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (isRatFood(inventory.getItem(i))) {
               return true;
            }
         }
      }
      if (level != null && pos != null) {
         IItemHandler handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, null);
         if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
               if (isRatFood(handler.getStackInSlot(i))) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   public static boolean doesContainFood(Container inventory) {
      for (int i = 0; i < inventory.getContainerSize(); i++) {
         if (isRatFood(inventory.getItem(i))) {
            return true;
         }
      }

      return false;
   }

   public static ItemStack extractFoodFromInventory(Level level, BlockPos pos, BlockEntity entity, RandomSource random) {
      if (entity instanceof Container feeder) {
         ItemStack stack = getFoodFromInventory(feeder, random);
         if (!stack.isEmpty()) {
            ItemStack duplicate = stack.copy();
            duplicate.setCount(1);
            stack.shrink(1);
            return duplicate;
         }
      }
      if (level != null && pos != null) {
         IItemHandler handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, null);
         if (handler != null) {
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < handler.getSlots(); i++) {
               ItemStack stack = handler.getStackInSlot(i);
               if (shouldRaidItem(stack)) {
                  slots.add(i);
               }
            }
            if (!slots.isEmpty()) {
               int slot = slots.get(random.nextInt(slots.size()));
               return handler.extractItem(slot, 1, false);
            }
         }
      }
      return ItemStack.EMPTY;
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
      } else {
         return items.size() == 1 ? items.get(0) : items.get(random.nextInt(items.size() - 1));
      }
   }

   public static int getContaminatedSlot(Container inventory, RandomSource random) {
      List<Integer> items = new ArrayList<>();

      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (stack.isEmpty() || stack.is((Item)RatsItemRegistry.CONTAMINATED_FOOD.get())) {
            items.add(i);
         }
      }

      if (items.isEmpty()) {
         return -1;
      } else {
         return items.size() == 1 ? items.get(0) : items.get(random.nextInt(items.size() - 1));
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
      } else {
         return items.size() == 1 ? items.get(0) : items.get(random.nextInt(items.size()));
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
      if (!blockState.isAir() && !blockState.liquid() && level.getBlockEntity(pos) == null) {
         float hardness = blockState.getDestroySpeed(level, pos);
         return hardness >= 0.0F && hardness <= RatConfig.ratStrengthThreshold && EventHooks.onEntityDestroyBlock(rat, pos, level.getBlockState(pos));
      } else {
         return false;
      }
   }

   public static boolean canRatPlaceBlock(Level level, BlockPos pos, DiggingRat rat) {
      return EventHooks.canEntityGrief(level, rat) && !EventHooks.onBlockPlace(rat, BlockSnapshot.create(level.dimension(), level, pos), Direction.UP);
   }

   public static boolean isBlockProtected(Level level, BlockPos pos, DiggingRat rat) {
      if (level.getBlockEntity(pos) instanceof Container) {
         return false;
      }
      return !EventHooks.canEntityGrief(level, rat) || !EventHooks.onEntityDestroyBlock(rat, pos, level.getBlockState(pos));
   }

   public static boolean isOpenRatTube(BlockGetter getter, BlockPos pos) {
      BlockState state = getter.getBlockState(pos);
      if (state.getBlock() instanceof RatTubeBlock) {
         for (int i = 0; i < Direction.values().length; i++) {
            BooleanProperty bool = RatTubeBlock.ALL_OPEN_PROPS[i];
            if ((Boolean)state.getValue(bool)) {
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

      for (BlockPos pos : BlockPos.betweenClosedStream(position.offset(-RADIUS, -RADIUS, -RADIUS), position.offset(RADIUS, RADIUS, RADIUS))
         .map(BlockPos::immutable)
         .toList()) {
         if (canPlantBeBonemealed(level, pos, level.getBlockState(pos))) {
            allBlocks.add(pos);
         }
      }

      if (!allBlocks.isEmpty()) {
         for (BlockPos posx : allBlocks) {
            BlockState block = level.getBlockState(posx);
            Block var8 = block.getBlock();
            if (var8 instanceof BonemealableBlock) {
               BonemealableBlock igrowable = (BonemealableBlock)var8;
               if (igrowable.isValidBonemealTarget(level, posx, block) && level.getRandom().nextInt(3) == 0 && !level.isClientSide()) {
                  level.levelEvent(2005, posx, 0);
                  igrowable.performBonemeal((ServerLevel)level, level.getRandom(), posx, block);
               }
            }
         }
      }
   }

   private static boolean canPlantBeBonemealed(Level level, BlockPos pos, BlockState BlockState) {
      if (BlockState.getBlock() instanceof BonemealableBlock igrowable
         && !(BlockState.getBlock() instanceof TallGrassBlock)
         && !(BlockState.getBlock() instanceof GrassBlock)
         && igrowable.isValidBonemealTarget(level, pos, BlockState)
         && !level.isClientSide()) {
         return igrowable.isBonemealSuccess(level, level.getRandom(), pos, BlockState);
      }

      return false;
   }

   public static void accelerateTick(Level level, BlockPos pos, int randomTickInt, int beTickInt) {
      if (!level.isClientSide()) {
         BlockState blockState = level.getBlockState(pos);
         Block block = blockState.getBlock();
         if (blockState.isRandomlyTicking() && randomTickInt <= 0 && level instanceof ServerLevel serverLevel) {
            blockState.randomTick(serverLevel, pos, level.getRandom());
         }

         if (beTickInt <= 0) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity != null) {
               BlockEntityTicker ticker = blockState.getTicker(level, entity.getType());
               if (ticker != null) {
                  ticker.tick(level, pos, blockState, entity);
               }
            }
         }
      }
   }

   public static TamedRat tameRat(Rat rat, Level level) {
      TamedRat newRat = new TamedRat((EntityType<? extends TamableAnimal>)RatsEntityRegistry.TAMED_RAT.get(), level);
      CompoundTag tag = new CompoundTag();
      rat.addAdditionalSaveData(tag);
      newRat.setToga(rat.hasToga());
      newRat.moveTo(rat.blockPosition(), rat.getYRot(), rat.getXRot());
      if (rat.getLeashHolder() != null) {
         newRat.setLeashedTo(rat.getLeashHolder(), true);
         rat.setLeashedTo(null, true);
      }

      EventHooks.finalizeMobSpawn(newRat, (ServerLevelAccessor)level, level.getCurrentDifficultyAt(rat.blockPosition()), MobSpawnType.EVENT, null);
      newRat.readAdditionalSaveData(tag);
      newRat.setColorVariant(rat.getColorVariant());

      for (EquipmentSlot slot : EquipmentSlot.values()) {
         newRat.setItemSlot(slot, rat.getItemBySlot(slot));
      }

      level.addFreshEntity(newRat);
      newRat.level().broadcastEntityEvent(rat, (byte)83);
      newRat.setYRot(rat.getYRot());
      rat.discard();
      return newRat;
   }

   public static RatVariant convertOldRatVariant(int variant) {
      return switch (variant) {
         case 1 -> (RatVariant)RatVariantRegistry.BLACK.get();
         case 2 -> (RatVariant)RatVariantRegistry.BROWN.get();
         case 3 -> (RatVariant)RatVariantRegistry.GREEN.get();
         case 4 -> (RatVariant)RatVariantRegistry.ALBINO.get();
         case 5 -> (RatVariant)RatVariantRegistry.HOODED.get();
         case 6 -> (RatVariant)RatVariantRegistry.BROWN_HOODED.get();
         case 7 -> (RatVariant)RatVariantRegistry.GRAY_HOODED.get();
         case 8 -> (RatVariant)RatVariantRegistry.SIAMESE.get();
         case 9 -> (RatVariant)RatVariantRegistry.WHITE.get();
         case 10 -> (RatVariant)RatVariantRegistry.YELLOW_HOODED.get();
         case 11, 12 -> (RatVariant)RatVariantRegistry.BROWN_UNDERCOAT.get();
         default -> (RatVariant)RatVariantRegistry.BLUE.get();
      };
   }
}
