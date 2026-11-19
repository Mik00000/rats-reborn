package com.github.alexthe666.rats.server.entity.rat;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.data.tags.RatsItemTags;
import com.github.alexthe666.rats.registry.RatVariantRegistry;
import com.github.alexthe666.rats.registry.RatlantisItemRegistry;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.registry.RatsCapabilityRegistry;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.registry.RatsSoundRegistry;
import com.github.alexthe666.rats.server.block.RatCageBlock;
import com.github.alexthe666.rats.server.block.RatHouseBlock;
import com.github.alexthe666.rats.server.block.RatTubeBlock;
import com.github.alexthe666.rats.server.block.entity.UpgradeCombinerBlockEntity;
import com.github.alexthe666.rats.server.capability.SelectedRat;
import com.github.alexthe666.rats.server.entity.RatMount;
import com.github.alexthe666.rats.server.entity.ai.goal.CreateWorkerGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.FarmersDelightChefGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatDepositGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatFloatGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatFollowOwnerGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatHuntGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatMeleeAttackGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatOwnerHurtByTargetGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatOwnerHurtTargetGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatPatrolGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatPickupGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatReturnToHouseGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatSeekCheeseGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatStayCloseToHomeGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatTargetItemsGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatWanderGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.RatWorkGoal;
import com.github.alexthe666.rats.server.entity.ai.goal.harvest.RatHarvestCropsGoal;
import com.github.alexthe666.rats.server.entity.ai.navigation.control.EtherealRatMoveControl;
import com.github.alexthe666.rats.server.entity.ai.navigation.control.RatCageMoveControl;
import com.github.alexthe666.rats.server.entity.ai.navigation.control.RatFlightMoveControl;
import com.github.alexthe666.rats.server.entity.ai.navigation.control.RatMoveControl;
import com.github.alexthe666.rats.server.entity.ai.navigation.control.RatTubeMoveControl;
import com.github.alexthe666.rats.server.entity.ai.navigation.navigation.EtherealRatNavigation;
import com.github.alexthe666.rats.server.entity.ai.navigation.navigation.RatFlightNavigation;
import com.github.alexthe666.rats.server.entity.ai.navigation.navigation.RatNavigation;
import com.github.alexthe666.rats.server.entity.mount.RatBiplaneMount;
import com.github.alexthe666.rats.server.entity.mount.RatMountBase;
import com.github.alexthe666.rats.server.items.OreRatNuggetItem;
import com.github.alexthe666.rats.server.items.RatSackItem;
import com.github.alexthe666.rats.server.items.RatStaffItem;
import com.github.alexthe666.rats.server.items.upgrades.BaseRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.BucketRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.CombinedRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.EnergyRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.MobFilterUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.MountRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.OreDoublingRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.ChangesAIUpgrade;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.CombinedUpgrade;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.DamageImmunityUpgrade;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.HoldsItemUpgrade;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.PostAttackUpgrade;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.StatBoostingUpgrade;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.TickRatUpgrade;
import com.github.alexthe666.rats.server.message.ManageRatStaffPacket;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.message.SetDancingRatPacket;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import com.github.alexthe666.rats.server.misc.RatPersistenceHelper;
import com.github.alexthe666.rats.server.misc.RatTubeTransit;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import com.github.alexthe666.rats.server.misc.RatUtils;
import com.github.alexthe666.rats.server.misc.RatVariant;
import com.github.alexthe666.rats.server.misc.RatsItemStackData;
import com.github.alexthe666.rats.server.misc.RatsRemovedContent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity.MovementEmission;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

public class TamedRat extends InventoryRat {
   private static final EntityDataAccessor<Boolean> TOGA = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> VISUAL_FLAG = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DYED = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Byte> DYE_COLOR = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Boolean> DANCING = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DANCE_MOVES = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> HELD_RF = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> RESPAWN_COUNTDOWN = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Optional<GlobalPos>> PICKUP_POS = SynchedEntityData.defineId(
      TamedRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS
   );
   private static final EntityDataAccessor<Optional<GlobalPos>> DEPOSIT_POS = SynchedEntityData.defineId(
      TamedRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS
   );
   private static final EntityDataAccessor<Optional<GlobalPos>> CREATE_WORK_POS = SynchedEntityData.defineId(
      TamedRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS
   );
   private static final EntityDataAccessor<Optional<GlobalPos>> BOUND_HOUSE_POS = SynchedEntityData.defineId(
      TamedRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS
   );
   private static final EntityDataAccessor<Boolean> IS_IN_WHEEL = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<String> SPECIAL_DYE = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Integer> MOUNT_RESPAWN_COOLDOWN = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> RAT_GENES_INITIALIZED = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> GENE_SPEED = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> GENE_HEALTH = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> GENE_ARMOR = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> GENE_DAMAGE = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> GENE_REGEN = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<String> LAST_PATH_FAILURE = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Integer> CHEESE_EATEN_TODAY_SYNC = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> LAST_FEED_DAY_SYNC = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
   private static final String RAT_GENES_TAG = "RatGenes";
   private static final float MIN_GENE_MULTIPLIER = 0.5F;
   private static final float MAX_GENE_MULTIPLIER = 2.5F;
   private static final float MIN_RAT_MODEL_SCALE = 0.75F;
   private static final float MAX_RAT_MODEL_SCALE = 1.5F;
   private static final float RAT_GENE_MUTATION_CHANCE = 0.3F;
   private static final float MIN_RAT_GENE_MUTATION_STEP = 0.05F;
   private static final float MAX_RAT_GENE_MUTATION_STEP = 0.25F;
   private static final float RAT_COLOR_MUTATION_CHANCE = 0.1F;
   private static final float RAT_BREEDING_EXCLUSIVE_COLOR_CHANCE = 0.16666667F;
   private static final int RAT_CHEESE_RESERVE_SLOT = 2;
   private static final int MAX_RAT_CHEESE_RESERVE = 16;
   private static final int MIN_RAT_CHEESE_RESERVE_BEFORE_REFILL = 2;
   private static final int RAT_BASE_DAILY_CHEESE = 4;
   private static final int RAT_HUNGER_DAY_TICKS = 24000;
   private static final int RAT_CHEESE_SHARE_INTERVAL = 100;
   private static final double RAT_CHEESE_SHARE_RADIUS = 8.0;
   public Direction depositFacing = Direction.UP;
   public Direction pickupFacing = Direction.UP;
   public Direction createWorkFacing = Direction.UP;
   public boolean crafting = false;
   public boolean climbingTube = false;
   public boolean isClimbingTrees = false;
   public int tubeClimbDirection = 0;
   @Nullable
   private RatTubeTransit.Session tubeTransit;
   public int cookingProgress = 0;
   public int coinCooldown = 0;
   public int breedCooldown = 0;
   public BlockPos jukeboxPos;
   public FluidStack transportingFluid = FluidStack.EMPTY;
   private Goal harvestGoal;
   private Goal pickupGoal;
   private Goal depositGoal;
   private Goal attackGoal;
   private Goal targetGoal;
   protected int navigatorType;
   public int rangedAttackCooldown = 0;
   public int visualCooldown = 0;
   private int poopCooldown = 0;
   public int pickpocketCooldown = 0;
   public int randomEffectCooldown = 0;
   private int cheeseEatenToday = 0;
   private long lastFeedDay = -1L;
   private int cachedHungerEfficiencyTick = -1000;
   private float cachedHungerEfficiency = 0.0F;
   private int updateNavigationCooldown;
   private int lastSmoothMoveTick = -100;
   private double lastSmoothMoveX = Double.NaN;
   private double lastSmoothMoveY = Double.NaN;
   private double lastSmoothMoveZ = Double.NaN;
   @Nullable
   private Entity lastSmoothMoveEntity;
   public boolean isCurrentlyWorking;
   private final Multimap<Holder<Attribute>, AttributeModifier> attributeChanges = HashMultimap.create();
   private static final String SAVED_UPGRADES_TAG = "RatUpgradeSlots";
   private static final int UPGRADE_SLOT_CHEST = 3;
   private static final int UPGRADE_SLOT_LEGS = 4;
   private static final int UPGRADE_SLOT_FEET = 5;
   private boolean pendingUpgradeRefresh;
   private boolean hasAnyUpgradeSlots;
   private boolean hasQuarryUpgradeForNavigation;
   private boolean hasFlightUpgradeCached;
   private boolean hasWorkerUpgradeCached;
   @Nullable
   private ListTag pendingUpgradeTags;
   // True while readAdditionalSaveData runs, so setTame() called by the vanilla loader does not roll fresh genes
   // that loadRatGenes() would overwrite a moment later.
   private boolean loadingSaveData;

   @Override
   public boolean isFood(ItemStack stack) {
      return stack.is(RatsItemTags.CHEESE_ITEMS) || stack.is((Item)RatsItemRegistry.CREATIVE_CHEESE.get());
   }

   public void scheduleUpgradeRefreshAfterLoad() {
      this.pendingUpgradeRefresh = true;
   }

   public TamedRat(EntityType<? extends TamableAnimal> type, Level level) {
      super(type, level);
      this.setPathfindingMalus(PathType.RAIL, -12.0F);
      Arrays.fill(this.armorDropChances, 2.0F);
      Arrays.fill(this.handDropChances, 2.0F);
      this.xpReward = 0;
      this.updateNavigationCooldown = 100;
   }

   @Override
   public float maxUpStep() {
      return 1.0F;
   }

   public static Builder createAttributes() {
      try {
         return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 8.0)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.FLYING_SPEED, 0.35)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.ARMOR, 1.0)
            .add(Attributes.FOLLOW_RANGE, 12.0);
      } catch (RuntimeException var1) {
         RatsMod.LOGGER.error("Failed to create the base attributes of a tamed rat", var1);
         throw var1;
      }
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor accessor, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
      try {
         data = super.finalizeSpawn(accessor, difficulty, type, data);
         this.initializeRandomRatGenes("spawn");
         return data;
      } catch (RuntimeException var6) {
         RatsMod.LOGGER.error("Failed to create genes when rat {} spawned", this.getUUID(), var6);
         throw var6;
      }
   }

   @Override
   protected void registerGoals() {
      this.harvestGoal = new RatHarvestCropsGoal(this);
      this.pickupGoal = new RatPickupGoal(this, RatPickupGoal.PickupType.INVENTORY);
      this.depositGoal = new RatDepositGoal(this, RatDepositGoal.DepositType.INVENTORY);
      this.attackGoal = new RatMeleeAttackGoal(this, 1.3, true);
      this.targetGoal = new RatHuntGoal(this, true, new ArrayList<>());
      this.goalSelector.addGoal(0, new RatFloatGoal(this));
      this.goalSelector.addGoal(1, this.attackGoal);
      this.goalSelector.addGoal(2, new RatFollowOwnerGoal(this, 1.25, 10.0F, 3.0F));
      this.goalSelector.addGoal(2, new RatReturnToHouseGoal(this, 1.15));
      this.goalSelector.addGoal(2, new RatStayCloseToHomeGoal(this, 1.1));
      this.goalSelector.addGoal(3, new RatSeekCheeseGoal(this));
      this.goalSelector.addGoal(4, new CreateWorkerGoal(this));
      this.goalSelector.addGoal(4, new FarmersDelightChefGoal(this));
      this.goalSelector.addGoal(4, this.harvestGoal);
      this.goalSelector.addGoal(5, this.depositGoal);
      this.goalSelector.addGoal(6, this.pickupGoal);
      this.goalSelector.addGoal(6, new SitWhenOrderedToGoal(this));
      this.goalSelector.addGoal(7, new RatWanderGoal(this, 1.25));
      this.goalSelector.addGoal(7, new RatPatrolGoal(this));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, LivingEntity.class, 6.0F) {
         public boolean canUse() {
            return TamedRat.this.sleepProgress <= 0.0F && super.canUse();
         }
      });
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this) {
         public boolean canUse() {
            return TamedRat.this.sleepProgress <= 0.0F && super.canUse();
         }
      });
      this.targetSelector.addGoal(0, new RatTargetItemsGoal(this));
      this.targetSelector.addGoal(1, this.targetGoal);
      this.targetSelector.addGoal(2, new RatOwnerHurtByTargetGoal(this));
      this.targetSelector.addGoal(3, new RatOwnerHurtTargetGoal(this));
   }

   protected void setupDynamicAI() {
      AtomicReference<Goal> newHarvest = new AtomicReference<>(new RatHarvestCropsGoal(this));
      AtomicReference<Goal> newDeposit = new AtomicReference<>(new RatDepositGoal(this, RatDepositGoal.DepositType.INVENTORY));
      AtomicReference<Goal> newPickup = new AtomicReference<>(new RatPickupGoal(this, RatPickupGoal.PickupType.INVENTORY));
      AtomicReference<Goal> newAttack = new AtomicReference<>(new RatMeleeAttackGoal(this, 1.45, true));
      RatUpgradeUtils.forEachUpgrade(
         this, item -> item instanceof ChangesAIUpgrade, (stack, slot) -> ((ChangesAIUpgrade)stack.getItem()).addNewWorkGoals(this).forEach(goal -> {
               if (goal instanceof RatWorkGoal workGoal) {
                  switch (workGoal.getRatTaskType()) {
                     case ATTACK:
                        newAttack.set(goal);
                        break;
                     case DEPOSIT:
                        newDeposit.set(goal);
                        break;
                     case PICKUP:
                        newPickup.set(goal);
                        break;
                     case HARVEST:
                        newHarvest.set(goal);
                  }
               } else {
                  throw new IllegalArgumentException("Rat Goals must implement the interface RatWorkGoal! Goal" + goal.getClass().getName() + "doesnt do this!");
               }
            })
      );
      if (!this.level().isClientSide()) {
         this.goalSelector.removeGoal(this.harvestGoal);
         this.goalSelector.removeGoal(this.depositGoal);
         this.goalSelector.removeGoal(this.pickupGoal);
         this.goalSelector.removeGoal(this.attackGoal);
         this.attackGoal = newAttack.get();
         this.depositGoal = newDeposit.get();
         this.pickupGoal = newPickup.get();
         this.harvestGoal = newHarvest.get();
         this.goalSelector.addGoal(1, this.attackGoal);
         this.goalSelector.addGoal(4, this.harvestGoal);
         this.goalSelector.addGoal(5, this.depositGoal);
         this.goalSelector.addGoal(6, this.pickupGoal);
         this.targetSelector.removeGoal(this.targetGoal);
         ItemStack filterUpgrade = RatUpgradeUtils.getUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_MOB_FILTER.get());
         if (!filterUpgrade.isEmpty()) {
            this.targetGoal = new RatHuntGoal(this, MobFilterUpgradeItem.isWhitelist(filterUpgrade), MobFilterUpgradeItem.getSelectedMobs(filterUpgrade));
         } else {
            this.targetGoal = new RatHuntGoal(this, true, new ArrayList<>());
         }

         this.targetSelector.addGoal(1, this.targetGoal);
         if (this.hasFlightUpgrade()) {
            this.switchNavigator(1);
         } else if (RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_AQUATIC.get())) {
            this.switchNavigator(3);
         } else if (RatUpgradeUtils.hasUpgrade(this, (Item)RatlantisItemRegistry.RAT_UPGRADE_ETHEREAL.get())) {
            this.switchNavigator(4);
         } else {
            this.switchNavigator(this.isInCage() ? 5 : (this.isInTube() ? 2 : 0));
         }
      }
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(TOGA, false);
      builder.define(DANCING, false);
      builder.define(DANCE_MOVES, 0);
      builder.define(HELD_RF, 0);
      builder.define(RESPAWN_COUNTDOWN, 0);
      builder.define(VISUAL_FLAG, false);
      builder.define(DYED, false);
      builder.define(FLYING, false);
      builder.define(DYE_COLOR, (byte)0);
      builder.define(DEPOSIT_POS, Optional.empty());
      builder.define(PICKUP_POS, Optional.empty());
      builder.define(CREATE_WORK_POS, Optional.empty());
      builder.define(BOUND_HOUSE_POS, Optional.empty());
      builder.define(IS_IN_WHEEL, false);
      builder.define(SPECIAL_DYE, "rainbow");
      builder.define(MOUNT_RESPAWN_COOLDOWN, 20);
      builder.define(RAT_GENES_INITIALIZED, false);
      builder.define(GENE_SPEED, 1.0F);
      builder.define(GENE_HEALTH, 1.0F);
      builder.define(GENE_ARMOR, 1.0F);
      builder.define(GENE_DAMAGE, 1.0F);
      builder.define(GENE_REGEN, 1.0F);
      builder.define(LAST_PATH_FAILURE, "");
      builder.define(CHEESE_EATEN_TODAY_SYNC, 0);
      builder.define(LAST_FEED_DAY_SYNC, -1);
   }

   public void switchNavigator(int type) {
      if (this.navigatorType == type && this.navigation != null && this.moveControl != null) {
         return;
      }
      if (type == 0) {
         this.moveControl = new RatMoveControl(this);
         this.navigation = new RatNavigation(this, this.level());
         this.navigatorType = 0;
      } else if (type == 1) {
         this.moveControl = new RatFlightMoveControl(this, 1.0F);
         this.navigation = new RatFlightNavigation(this, this.level());
         this.navigatorType = 1;
      } else if (type == 2) {
         this.moveControl = new RatTubeMoveControl(this);
         this.navigation = new RatNavigation(this, this.level());
         this.navigatorType = 2;
      } else if (type == 3) {
         this.moveControl = new SmoothSwimmingMoveControl(this, 360, 360, 10.0F, 1.0F, true);
         this.navigation = new AmphibiousPathNavigation(this, this.level());
         this.navigatorType = 3;
      } else if (type == 4) {
         this.moveControl = new EtherealRatMoveControl(this);
         this.navigation = new EtherealRatNavigation(this, this.level());
         this.navigatorType = 4;
      } else if (type == 5) {
         this.moveControl = new RatCageMoveControl(this);
         this.navigation = new RatNavigation(this, this.level());
         this.navigatorType = 5;
      }
   }

   @Override
   public boolean isHoldingFood() {
      return !this.getCommand().allowsEating && !RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_ORE_DOUBLING.get())
         ? false
         : !this.getMainHandItem().isEmpty()
            && RatUpgradeUtils.forEachUpgradeBool(this, stack -> stack.isRatHoldingFood(this), RatUtils.isRatFood(this.getMainHandItem()));
   }

   @Override
   public boolean isVisuallySitting() {
      return super.isVisuallySitting() || this.isDancing();
   }

   @Override
   public boolean isHoldingItemInHands() {
      return (
            this.isHoldingFood()
               || !this.getMainHandItem().isEmpty() && this.cookingProgress > 0
               || this.holdsItemInHandUpgrade()
               || this.getMBTransferRate() > 0
         )
         && this.sleepProgress <= 0.0F;
   }

   public boolean doHurtTarget(Entity entity) {
      try {
         if (this.getVehicle() instanceof LivingEntity living && this.isRidingSpecialMount()) {
            return living.doHurtTarget(entity);
         }

         float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
         boolean flag = entity.hurt(this.damageSources().mobAttack(this), damage);
         if (flag) {
            this.getMainHandItem().hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
            if (entity instanceof LivingEntity) {
               RatUpgradeUtils.forEachUpgrade(
                  this, item -> item instanceof PostAttackUpgrade, (stack, slot) -> ((PostAttackUpgrade)stack.getItem()).afterHit(this, (LivingEntity)entity)
               );
            }
         }

         return flag;
      } catch (RuntimeException var4) {
         RatsMod.LOGGER.error("Rat {} failed to hit target {}", this.getUUID(), entity.getUUID(), var4);
         throw var4;
      }
   }

   public boolean isInCage() {
      return this.level().getBlockState(this.blockPosition()).getBlock() instanceof RatCageBlock;
   }

   public boolean isInTube() {
      return this.level().getBlockState(this.blockPosition()).getBlock() instanceof RatTubeBlock;
   }

   public boolean onClimbable() {
      if (this.isClimbingTrees) {
         return true;
      }
      return this.isInTube() ? this.climbingTube : this.isClimbingBoundHouse() || super.onClimbable();
   }

   private boolean isClimbingBoundHouse() {
      try {
         Optional<GlobalPos> house = this.getBoundHousePos();
         if (!house.isEmpty() && house.get().dimension().equals(this.level().dimension())) {
            BlockPos housePos = house.get().pos();
            if (!(this.level().getBlockState(housePos).getBlock() instanceof RatHouseBlock)) {
               return false;
            } else {
               double dx = Math.abs(this.getX() - ((double)housePos.getX() + 0.5));
               double dz = Math.abs(this.getZ() - ((double)housePos.getZ() + 0.5));
               boolean nearSide = dx <= 1.15 && dz <= 1.15 && (dx > 0.55 || dz > 0.55);
               boolean nearHeight = this.getY() >= (double)housePos.getY() - 0.25 && this.getY() <= (double)housePos.getY() + 1.8;
               return nearSide && nearHeight;
            }
         } else {
            return false;
         }
      } catch (RuntimeException var9) {
         RatsMod.LOGGER.error("Failed to check rat {} climbing the house", this.getUUID(), var9);
         throw var9;
      }
   }

   public void setTubeClimbDirection(int direction) {
      this.tubeClimbDirection = direction;
   }

   public int getTubeClimbDirection() {
      return this.tubeClimbDirection;
   }

   @Nullable
   public RatTubeTransit.Session getTubeTransit() {
      return this.tubeTransit;
   }

   public void setTubeTransit(@Nullable RatTubeTransit.Session session) {
      this.tubeTransit = session;
   }

   public void clearTubeTransit() {
      this.tubeTransit = null;
   }

   public boolean isTubeTransitActive() {
      return this.tubeTransit != null && this.tubeTransit.isActive();
   }

   public void onTubeSegmentTick(BlockPos pos, BlockState state) {
      try {
         if (this.isInTube()) {
            this.fallDistance = 0.0F;
         }
      } catch (RuntimeException var4) {
         RatsMod.LOGGER.error("Tick error of rat {} in tube {}: {}", this.getUUID(), pos, var4.getMessage(), var4);
         throw var4;
      }
   }

   public boolean canCollideWith(Entity entity) {
      if (this.isInTube() && entity instanceof TamedRat other && other.isInTube()) {
         return false;
      }

      return super.canCollideWith(entity);
   }

   public int getArmorValue() {
      return super.getArmorValue() * RatConfig.ratArmorMultiplier;
   }

   @Override
   public boolean removeWhenFarAway(double dist) {
      return false;
   }

   protected MovementEmission getMovementEmission() {
      return RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_SCULKED.get()) ? MovementEmission.SOUNDS : MovementEmission.ALL;
   }

   public boolean canDrownInFluidType(FluidType type) {
      return type == NeoForgeMod.WATER_TYPE.value()
         && (
            !RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_AQUATIC.get())
               || !RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_UNDERWATER.get())
         );
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      this.saveUpgradeSlots(tag);
      RatPersistenceHelper.logSave(this, tag);
      tag.putInt("NavCooldown", this.updateNavigationCooldown);
      tag.putInt("CookingProgress", this.cookingProgress);
      tag.putInt("BreedCooldown", this.breedCooldown);
      tag.putInt("CoinCooldown", this.coinCooldown);
      tag.putInt("PickpocketCooldown", this.pickpocketCooldown);
      tag.putInt("CheeseEatenToday", this.cheeseEatenToday);
      tag.putLong("LastFeedDay", this.lastFeedDay);
      tag.putInt("MountCooldown", this.getMountCooldown());
      tag.putInt("TransportingRF", this.getHeldRF());
      tag.putInt("RespawnCountdown", this.getRespawnCountdown());
      tag.putInt("Command", this.getCommandInteger());
      tag.putBoolean("VisualFlag", this.getVisualFlag());
      tag.putBoolean("Dancing", this.isDancing());
      tag.putBoolean("Toga", this.hasToga());
      tag.putBoolean("Dyed", this.isDyed());
      tag.putByte("DyeColor", (byte)this.getDyeColor());
      tag.putString("SpecialDye", this.getSpecialDye());
      this.saveRatGenes(tag);
      this.getDepositPos()
         .flatMap(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error))
         .ifPresent(tag1 -> tag.put("DepositPos", tag1));
      this.getPickupPos()
         .flatMap(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error))
         .ifPresent(tag1 -> tag.put("PickupPos", tag1));
      this.getCreateWorkPos()
         .flatMap(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error))
         .ifPresent(tag1 -> tag.put("CreateWorkPos", tag1));
      this.getBoundHousePos()
         .flatMap(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error))
         .ifPresent(tag1 -> tag.put("BoundHousePos", tag1));
      tag.putInt("DepositFacing", this.depositFacing.ordinal());
      tag.putInt("PickupFacing", this.pickupFacing.ordinal());
      tag.putInt("CreateWorkFacing", this.createWorkFacing.ordinal());
      tag.putInt("RandomEffectCooldown", this.randomEffectCooldown);
      if (!this.transportingFluid.isEmpty()) {
         CompoundTag fluidTag = new CompoundTag();
         this.transportingFluid.save(this.registryAccess(), fluidTag);
         tag.put("TransportingFluid", fluidTag);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      this.loadingSaveData = true;
      try {
         super.readAdditionalSaveData(tag);
      } finally {
         this.loadingSaveData = false;
      }
      if (tag.contains("RatUpgradeSlots", 9)) {
         this.pendingUpgradeTags = tag.getList("RatUpgradeSlots", 10).copy();
      }

      this.updateNavigationCooldown = tag.getInt("NavCooldown");
      this.cookingProgress = tag.getInt("CookingProgress");
      this.breedCooldown = tag.getInt("BreedCooldown");
      this.coinCooldown = tag.getInt("CoinCooldown");
      this.pickpocketCooldown = tag.getInt("PickpocketCooldown");
      this.cheeseEatenToday = tag.getInt("CheeseEatenToday");
      this.lastFeedDay = tag.contains("LastFeedDay") ? tag.getLong("LastFeedDay") : -1L;
      this.syncRatHungerData();
      this.setMountCooldown(tag.getInt("MountCooldown"));
      this.randomEffectCooldown = tag.getInt("RandomEffectCooldown");
      this.setHeldRF(tag.getInt("TransportingRF"));
      this.setRespawnCountdown(tag.getInt("RespawnCountdown"));
      this.setCommandInteger(tag.getInt("Command"));
      this.setDancing(tag.getBoolean("Dancing"));
      this.setVisualFlag(tag.getBoolean("VisualFlag"));
      this.setToga(tag.getBoolean("Toga"));
      this.setDyed(tag.getBoolean("Dyed"));
      this.setDyeColor(tag.getByte("DyeColor"));
      this.setSpecialDye(tag.getString("SpecialDye"));
      this.loadRatGenes(tag);
      if (tag.contains("DepositPos")) {
         this.setDepositPos((GlobalPos)GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("DepositPos")).resultOrPartial(RatsMod.LOGGER::error).orElse(null));
      }

      if (tag.contains("PickupPos")) {
         this.setPickupPos((GlobalPos)GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("PickupPos")).resultOrPartial(RatsMod.LOGGER::error).orElse(null));
      }

      if (tag.contains("CreateWorkPos")) {
         this.setCreateWorkPos((GlobalPos)GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("CreateWorkPos")).resultOrPartial(RatsMod.LOGGER::error).orElse(null));
      }

      if (tag.contains("BoundHousePos")) {
         this.setBoundHousePos((GlobalPos)GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("BoundHousePos")).resultOrPartial(RatsMod.LOGGER::error).orElse(null));
      }

      if (tag.contains("DepositFacing")) {
         this.depositFacing = this.readSavedDirection(tag, "DepositFacing", Direction.UP);
      }

      if (tag.contains("PickupFacing")) {
         this.pickupFacing = this.readSavedDirection(tag, "PickupFacing", Direction.UP);
      }

      if (tag.contains("CreateWorkFacing")) {
         this.createWorkFacing = this.readSavedDirection(tag, "CreateWorkFacing", Direction.UP);
      }

      if (tag.contains("TransportingFluid")) {
         CompoundTag fluidTag = tag.getCompound("TransportingFluid");
         if (!fluidTag.isEmpty()) {
            this.transportingFluid = FluidStack.parse(this.registryAccess(), fluidTag).orElse(FluidStack.EMPTY);
         }
      }

      this.pendingUpgradeRefresh = true;
      this.tryReloadPendingSaveData();
   }

   public boolean hasPendingUpgradeTags() {
      return this.pendingUpgradeTags != null;
   }

   public int getPendingUpgradeCount() {
      return this.pendingUpgradeTags == null ? 0 : this.pendingUpgradeTags.size();
   }

   public boolean hasAnyUpgradeSlotFilled() {
      if (this.getInventory() == null) {
         return false;
      }
      for (int slot = UPGRADE_SLOT_CHEST; slot <= UPGRADE_SLOT_FEET; slot++) {
         if (!this.getInventory().getItem(slot).isEmpty()) {
            return true;
         }
      }
      return false;
   }

   public void tryReloadPendingUpgradeTags() {
      if (!this.level().isClientSide()) {
         this.loadPendingUpgradeTags();
      }
   }

   private void loadPendingUpgradeTags() {
      try {
         if (this.pendingUpgradeTags != null) {
            CompoundTag wrapper = new CompoundTag();
            wrapper.put("RatUpgradeSlots", this.pendingUpgradeTags);
            this.loadUpgradeSlots(wrapper);
            this.pendingUpgradeTags = null;
         }
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to load pending upgrades of rat {} on joining the world", this.getUUID(), var2);
         throw var2;
      }
   }

   private void saveUpgradeSlots(CompoundTag tag) {
      try {
         ListTag upgrades = new ListTag();
         int[] upgradeSlots = new int[]{3, 4, 5};
         if (this.getInventory() != null) {
            for (int slot : upgradeSlots) {
               ItemStack stack = this.getInventory().getItem(slot);
               if (!stack.isEmpty()) {
                  CompoundTag stackTag = RatsItemStackData.save(this.registryAccess(), stack);
                  stackTag.putInt("InventorySlot", slot);
                  upgrades.add(stackTag);
               }
            }
         }

         tag.put("RatUpgradeSlots", upgrades);
      } catch (RuntimeException var10) {
         RatsMod.LOGGER.error("Failed to save upgrades of rat {}", this.getUUID(), var10);
         throw var10;
      }
   }

   private void loadUpgradeSlots(CompoundTag tag) {
      try {
         if (tag.contains("RatUpgradeSlots", 9) && this.getInventory() != null) {
            if (this.level() != null) {
               RegistryAccess provider = this.level().registryAccess();
               ListTag upgrades = tag.getList("RatUpgradeSlots", 10);

               for (int i = 0; i < upgrades.size(); i++) {
                  CompoundTag stackTag = upgrades.getCompound(i);
                  int slot = stackTag.contains("InventorySlot")
                     ? stackTag.getInt("InventorySlot")
                     : this.mapLegacyUpgradeSlot(stackTag.getInt("EquipmentSlot"));
                  if (slot >= 3 && slot <= 5) {
                     ItemStack stack = ItemStack.parse(provider, stackTag).orElse(ItemStack.EMPTY);
                     if (stack.isEmpty()) {
                        stack = RatsItemStackData.parse(provider, stackTag);
                     }

                     if (!stack.isEmpty()) {
                        this.getInventory().setItem(slot, stack);
                     }
                  }
               }
            }
         }
      } catch (RuntimeException var8) {
         RatsMod.LOGGER.error("Failed to restore upgrades of rat {}", this.getUUID(), var8);
         throw var8;
      }
   }

   private int mapLegacyUpgradeSlot(int equipmentSlotOrdinal) {
      EquipmentSlot slot = EquipmentSlot.values()[Mth.clamp(equipmentSlotOrdinal, 0, EquipmentSlot.values().length - 1)];

      return switch (slot) {
         case CHEST -> 3;
         case LEGS -> 4;
         case FEET -> 5;
         default -> -1;
      };
   }

   private Direction readSavedDirection(CompoundTag tag, String key, Direction fallback) {
      try {
         int ordinal = tag.getInt(key);
         Direction[] directions = Direction.values();
         if (ordinal >= 0 && ordinal < directions.length) {
            return directions[ordinal];
         } else {
            RatsMod.LOGGER.error("Rat {} got a broken direction {}={} in NBT, applying {}", this.getUUID(), key, ordinal, fallback);
            return fallback;
         }
      } catch (RuntimeException var6) {
         RatsMod.LOGGER.error("Failed to read direction {} of rat {}", key, this.getUUID(), var6);
         throw var6;
      }
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!this.level().isClientSide()) {
         this.ensureRatGenesInitialized("tick");
         this.tickRatHunger();
      }

      if (!this.level().isClientSide() && this.isTubeTransitActive()) {
         RatTubeTransit.tick(this);
      }

      if (this.pendingUpgradeRefresh && !this.level().isClientSide()) {
         this.pendingUpgradeRefresh = false;
         this.onUpgradeChanged();
      }

      if (this.isBaby() && this.getCommand() != RatCommand.SIT) {
         this.setCommand(RatCommand.SIT);
      }

      if (this.breedCooldown > 0) {
         this.breedCooldown--;
      }

      if (this.isOrderedToSit() && this.getTarget() != null) {
         this.setTarget(null);
      }

      if (this.getTarget() != null && this.getMountCooldown() > 0) {
         this.setTarget(null);
      }

      if (this.getRespawnCountdown() > 0) {
         if (this.level().isClientSide() && this.tickCount % 5 == 0) {
            double d0 = this.position().x();
            double d1 = this.position().y() + 0.25;
            double d2 = this.position().z();
            double d3 = ((double)this.getRandom().nextFloat() - 0.5) * 0.15;
            double d4 = ((double)this.getRandom().nextFloat() - 0.5) * 0.15;
            double d5 = ((double)this.getRandom().nextFloat() - 0.5) * 0.15;
            this.level().addParticle(ParticleTypes.END_ROD, d0, d1, d2, d3, d4, d5);
         }

         this.setRespawnCountdown(this.getRespawnCountdown() - 1);
      }

      this.setNoAi(this.getRespawnCountdown() > 0);
      if (!this.level().isClientSide() && this.getMountEntityType() != null && !this.isPassenger() && this.getMountCooldown() == 0) {
         Entity entity = this.getMountEntityType().create(this.level());
         entity.copyPosition(this);
         if (entity instanceof Mob mob && this.level() instanceof ServerLevelAccessor accessor) {
            EventHooks.finalizeMobSpawn(mob, accessor, this.level().getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
         }

         this.level().addFreshEntity(entity);
         this.level().broadcastEntityEvent(this, (byte)127);
         this.startRiding(entity, true);
      }

      if (this.hasAnyUpgrades()) {
         RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof TickRatUpgrade, (stack, slot) -> ((TickRatUpgrade)stack.getItem()).tick(this));
      }

      if (!this.level().isClientSide() && this.isTame()) {
         if (this.tickCount % 40 == 0 && this.getCommand() == RatCommand.HARVEST && com.github.alexthe666.rats.RatsMod.LOGGER.isDebugEnabled()) {
            String activeGoals = this.goalSelector.getAvailableGoals().stream().filter(net.minecraft.world.entity.ai.goal.WrappedGoal::isRunning).map(g -> g.getGoal().getClass().getSimpleName()).collect(java.util.stream.Collectors.joining(", "));
            com.github.alexthe666.rats.RatsMod.LOGGER.debug("[RAT_STATE] Rat {} pos={} | cmd={} | hand={} | sit={} | depositPos={} | activeGoals=[{}]",
               this.getUUID(), this.blockPosition(), this.getCommand(), this.getMainHandItem(), this.isOrderedToSit(),
               this.getDepositPos().map(p -> p.pos().toShortString()).orElse("none"), activeGoals);
         }
         if (this.getY() < (double)(this.level().getMinBuildHeight() + 2)) {
            this.noPhysics = false;
            this.setDeltaMovement(Vec3.ZERO);
            if (this.getDepositPos().isPresent()) {
               BlockPos dep = this.getDepositPos().get().pos();
               this.teleportTo((double)dep.getX() + 0.5, (double)dep.getY() + 1.0, (double)dep.getZ() + 0.5);
            } else if (this.getOwner() != null) {
               this.teleportTo(this.getOwner().getX(), this.getOwner().getY(), this.getOwner().getZ());
            }
         }
      }

      if (RatConfig.upgradeRegenRate > 0 && this.hasAnyUpgrades()) {
         RatUpgradeUtils.forEachUpgrade(
            this,
            item -> item instanceof StatBoostingUpgrade,
            (stack, slot) -> {
               if (((StatBoostingUpgrade)stack.getItem()).regeneratesHealth()
                  && this.getHealth() < this.getMaxHealth()
                  && this.tickCount % RatConfig.upgradeRegenRate == 0) {
                  float hungerEfficiency = this.getRatHungerEfficiency();
                  if (hungerEfficiency > 0.0F) {
                     this.heal(this.getRatRegenGene() * hungerEfficiency);
                  }
               }
            }
         );
      }

      if (!this.isTubeTransitActive()) {
         if (this.isInTube() && !this.isInCage() && this.navigatorType != 2) {
            this.switchNavigator(2);
         } else if (this.updateNavigationCooldown-- == 0) {
            this.updateNavigationCooldown = 60;
            int savedNav = this.navigatorType;
            if (this.isInCage()) {
               this.switchNavigator(5);
            } else if (this.isInTube()) {
               this.switchNavigator(2);
            } else if (this.hasFlightUpgrade()) {
               this.switchNavigator(1);
            } else if (savedNav == 2) {
               this.switchNavigator(0);
            } else {
               this.switchNavigator(savedNav);
            }
         }
      }

      this.setNoGravity(this.isFlying());
      if (this.isFlying()) {
         if (this.isOrderedToSit() || this.verticalCollisionBelow || this.onGround()) {
            this.setFlying(false);
         }

         if (Math.abs(this.getDeltaMovement().x()) < 0.01 && Math.abs(this.getDeltaMovement().z()) < 0.01 && Math.abs(this.getDeltaMovement().y()) > 0.0) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.1, 1.0));
         }
      }

      if (this.isInWheel() && !this.level().getBlockState(this.blockPosition()).is((Block)RatsBlockRegistry.RAT_CAGE_WHEEL.get())) {
         this.setInWheel(false);
      }

      if (this.getMountCooldown() > 0) {
         this.setMountCooldown(this.getMountCooldown() - 1);
      }

      if (this.rangedAttackCooldown > 0) {
         this.rangedAttackCooldown--;
      }

      if (this.visualCooldown > 0) {
         this.visualCooldown--;
      }

      if (this.poopCooldown > 0) {
         this.poopCooldown--;
      }

      if (this.isDancing() && this.getAnimation() != this.getDanceAnimation()) {
         this.setAnimation(this.getDanceAnimation());
      }

      if (this.isDancing()
         && (
            this.jukeboxPos == null
               || this.jukeboxPos.distToCenterSqr(this.getX(), this.getY(), this.getZ()) > 256.0
               || !this.level().getBlockState(this.jukeboxPos).is(Blocks.JUKEBOX)
         )) {
         this.setDancing(false);
      }

      this.handleRatQuarryPlatformLift();
   }

   private void handleRatQuarryPlatformLift() {
      try {
         if (!this.level().isClientSide()) {
            boolean quarryRatGoingDown = RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_QUARRY.get()) && this.getMainHandItem().isEmpty();
            if (!quarryRatGoingDown) {
               BlockPos current = this.blockPosition();
               BlockState currentState = this.level().getBlockState(current);
               BlockState aboveState = this.level().getBlockState(current.above());
               if (!currentState.is((Block)RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())
                  || !aboveState.isAir() && !aboveState.is((Block)RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())) {
                  BlockState aboveTwoState = this.level().getBlockState(current.above(2));
                  if (this.jumping
                     && aboveState.is((Block)RatsBlockRegistry.RAT_QUARRY_PLATFORM.get())
                     && (aboveTwoState.isAir() || aboveTwoState.is((Block)RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()))) {
                     this.setPos(this.getX(), this.getY() + 1.0, this.getZ());
                     this.getNavigation().stop();
                  }
               } else {
                  this.setPos(this.getX(), this.getY() + 1.0, this.getZ());
                  this.getNavigation().stop();
               }
            }
         }
      } catch (RuntimeException var6) {
         RatsMod.LOGGER.error("Failed to handle rat {} climbing through the quarry platform", this.getUUID(), var6);
         throw var6;
      }
   }

   public boolean fireImmune() {
      return this.getRespawnCountdown() > 0 || super.fireImmune();
   }

   public boolean isPushedByFluid(FluidType type) {
      return !RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_QUARRY.get())
         && !RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_AQUATIC.get())
         && super.isPushedByFluid(type);
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!this.isBaby() && !this.isInvulnerableTo(source) && (!source.is(DamageTypes.IN_WALL) || !this.isPassenger())) {
         Entity entity = source.getEntity();
         if (entity != null && !(entity instanceof Player) && !(entity instanceof AbstractArrow)) {
            amount = (amount + 1.0F) / 2.0F;
         }

         boolean flag = super.hurt(source, amount);
         if (flag && this.getVehicle() != null && this.isRidingSpecialMount()) {
            this.getVehicle().hurt(source, amount);
            this.invulnerableTime = 20;
            return false;
         } else {
            return flag;
         }
      } else {
         return false;
      }
   }

   public ItemStack getPickedResult(HitResult target) {
      return new ItemStack((ItemLike)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("rats", "rat_spawn_egg")));
   }

   public void setFlying(boolean flying) {
      this.getEntityData().set(FLYING, flying);
   }

   public boolean isFlying() {
      return (Boolean)this.getEntityData().get(FLYING);
   }

   public void setToga(boolean toga) {
      this.getEntityData().set(TOGA, toga);
   }

   public boolean hasToga() {
      return (Boolean)this.getEntityData().get(TOGA);
   }

   public boolean getVisualFlag() {
      return (Boolean)this.getEntityData().get(VISUAL_FLAG);
   }

   public void setVisualFlag(boolean flag) {
      this.getEntityData().set(VISUAL_FLAG, flag);
   }

   public boolean isDancing() {
      return (Boolean)this.getEntityData().get(DANCING);
   }

   public void setDancing(boolean dancing) {
      this.getEntityData().set(DANCING, dancing);
   }

   public int getDanceMoves() {
      return (Integer)this.getEntityData().get(DANCE_MOVES);
   }

   public void setDanceMoves(int moves) {
      this.getEntityData().set(DANCE_MOVES, moves);
   }

   public int getHeldRF() {
      return (Integer)this.getEntityData().get(HELD_RF);
   }

   public void setHeldRF(int rf) {
      this.getEntityData().set(HELD_RF, rf);
   }

   public int getRespawnCountdown() {
      return (Integer)this.getEntityData().get(RESPAWN_COUNTDOWN);
   }

   public void setRespawnCountdown(int respawn) {
      this.getEntityData().set(RESPAWN_COUNTDOWN, respawn);
   }

   public boolean isInWheel() {
      return (Boolean)this.getEntityData().get(IS_IN_WHEEL);
   }

   public void setInWheel(boolean wheel) {
      this.getEntityData().set(IS_IN_WHEEL, wheel);
   }

   public boolean isDyed() {
      return (Boolean)this.getEntityData().get(DYED);
   }

   public void setDyed(boolean dyed) {
      this.getEntityData().set(DYED, dyed);
   }

   public int getDyeColor() {
      return (Byte)this.getEntityData().get(DYE_COLOR);
   }

   public void setDyeColor(int color) {
      this.getEntityData().set(DYE_COLOR, (byte)color);
   }

   public String getSpecialDye() {
      return (String)this.getEntityData().get(SPECIAL_DYE);
   }

   public void setSpecialDye(String keyword) {
      this.getEntityData().set(SPECIAL_DYE, keyword);
   }

   public Optional<GlobalPos> getPickupPos() {
      return (Optional<GlobalPos>)this.getEntityData().get(PICKUP_POS);
   }

   public void setPickupPos(@Nullable GlobalPos pos) {
      this.getEntityData().set(PICKUP_POS, Optional.ofNullable(pos));
   }

   public Optional<GlobalPos> getDepositPos() {
      return (Optional<GlobalPos>)this.getEntityData().get(DEPOSIT_POS);
   }

   public void setDepositPos(@Nullable GlobalPos pos) {
      this.getEntityData().set(DEPOSIT_POS, Optional.ofNullable(pos));
   }

   public boolean canWorkBySchedule() {
      try {
         return RatDaySchedule.canWork(this.level());
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to check the work schedule of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   public void setLastPathFailure(String reason) {
      try {
         this.getEntityData().set(LAST_PATH_FAILURE, reason == null ? "" : reason);
         if (reason != null && !reason.isBlank()) {
            RatsMod.LOGGER.debug("Rat {} did not build a path: {}", this.getUUID(), reason);
         }
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to record the path failure of rat {}", this.getUUID(), var3);
         throw var3;
      }
   }

   public String getLastPathFailure() {
      try {
         return (String)this.getEntityData().get(LAST_PATH_FAILURE);
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the path failure of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   public void clearLastPathFailure() {
      this.setLastPathFailure("");
   }

   public Optional<GlobalPos> getCreateWorkPos() {
      try {
         return (Optional<GlobalPos>)this.getEntityData().get(CREATE_WORK_POS);
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the Create use point for rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   public void setCreateWorkPos(@Nullable GlobalPos pos) {
      try {
         this.getEntityData().set(CREATE_WORK_POS, Optional.ofNullable(pos));
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to set the Create use point for rat {}", this.getUUID(), var3);
         throw var3;
      }
   }

   public Optional<GlobalPos> getBoundHousePos() {
      try {
         return (Optional<GlobalPos>)this.getEntityData().get(BOUND_HOUSE_POS);
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the bound house of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   public void setBoundHousePos(@Nullable GlobalPos pos) {
      try {
         this.getEntityData().set(BOUND_HOUSE_POS, Optional.ofNullable(pos));
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to set the bound house of rat {}", this.getUUID(), var3);
         throw var3;
      }
   }

   public int getMountCooldown() {
      return (Integer)this.getEntityData().get(MOUNT_RESPAWN_COOLDOWN);
   }

   public void setMountCooldown(int cooldown) {
      this.getEntityData().set(MOUNT_RESPAWN_COOLDOWN, cooldown);
   }

   @Override
   public void onItemEaten() {
      ItemStack handCopy = this.getMainHandItem().copy();
      if (RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_ORE_DOUBLING.get())
         && OreDoublingRatUpgradeItem.isProcessable(this.level(), handCopy)) {
         ItemStack attemptedSmelt = handCopy.copy();
         Optional<RecipeHolder<SmeltingRecipe>> recipeHolder = this.level()
            .getRecipeManager()
            .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(attemptedSmelt), this.level());
         if (recipeHolder.isPresent() && !((SmeltingRecipe)recipeHolder.get().value()).getResultItem(this.level().registryAccess()).isEmpty()) {
            attemptedSmelt = ((SmeltingRecipe)recipeHolder.get().value()).getResultItem(this.level().registryAccess()).copy();
         }

         if (!attemptedSmelt.is(handCopy.getItem())) {
            ItemStack nugget = OreRatNuggetItem.saveResourceToNugget(attemptedSmelt, this.level().registryAccess()).copyWithCount(2);
            if (RatConfig.ratFartNoises) {
               this.playSound(
                  (SoundEvent)RatsSoundRegistry.RAT_POOP.get(), 0.5F + this.getRandom().nextFloat() * 0.5F, 1.0F + this.getRandom().nextFloat() * 0.5F
               );
            }

            if (!this.level().isClientSide()) {
               this.spawnAtLocation(nugget, 0.0F);
            }

            this.getMainHandItem().shrink(1);
         }
      } else if (this.getRandom().nextFloat() <= 0.05F) {
         ItemStack nuggetx = new ItemStack((ItemLike)RatsItemRegistry.RAT_NUGGET.get());
         if (RatConfig.ratFartNoises) {
            this.playSound((SoundEvent)RatsSoundRegistry.RAT_POOP.get(), 0.5F + this.getRandom().nextFloat() * 0.5F, 1.0F + this.getRandom().nextFloat() * 0.5F);
         }

         if (!this.level().isClientSide()) {
            this.spawnAtLocation(nuggetx, 0.0F);
         }
      } else {
         this.getMainHandItem().shrink(1);
      }
   }

   public void createBabiesFrom(TamedRat mother, TamedRat father) {
      TamedRat baby = new TamedRat((EntityType<? extends TamableAnimal>)RatsEntityRegistry.TAMED_RAT.get(), this.level());
      baby.setMale(this.getRandom().nextBoolean());
      baby.setColorVariant(this.chooseBabyColorVariant(mother, father));
      baby.setPos(mother.getX() - 0.5 + (double)mother.getRandom().nextFloat(), mother.getY(), mother.getZ() - 0.5 + (double)mother.getRandom().nextFloat());
      baby.setAge(-2400);
      baby.setCommand(RatCommand.SIT);
      if (mother.isTame()) {
         baby.setTame(true, true);
         baby.setOwnerUUID(mother.getOwnerUUID());
      } else if (father.isTame()) {
         baby.setTame(true, true);
         baby.setOwnerUUID(father.getOwnerUUID());
      }

      baby.inheritRatGenesFromParents(mother, father);
      this.level().addFreshEntity(baby);
      this.tryInheritBabyUpgrade(father, mother, baby);
   }

   private RatVariant chooseBabyColorVariant(TamedRat mother, TamedRat father) {
      try {
         RatVariant motherVariant = mother.getColorVariant();
         RatVariant fatherVariant = father.getColorVariant();
         boolean hasExclusiveParent = motherVariant.isBreedingExclusive() || fatherVariant.isBreedingExclusive();
         if (hasExclusiveParent && this.getRandom().nextFloat() <= 0.16666667F) {
            RatVariant variant = this.getRandomDifferentColorVariant(true, motherVariant, fatherVariant);
            RatsMod.LOGGER.info("Rat got a rare offspring coat mutation: {}", RatVariant.getVariantId(variant));
            return variant;
         } else if (this.getRandom().nextFloat() <= 0.1F) {
            RatVariant variant = this.getRandomDifferentColorVariant(false, motherVariant, fatherVariant);
            RatsMod.LOGGER.info("Rat got a regular offspring coat mutation: {}", RatVariant.getVariantId(variant));
            return variant;
         } else {
            return this.getRandom().nextBoolean() ? fatherVariant : motherVariant;
         }
      } catch (RuntimeException var7) {
         RatsMod.LOGGER.error("Failed to pick the baby coat from parents {} and {}", mother.getUUID(), father.getUUID(), var7);
         throw var7;
      }
   }

   private RatVariant getRandomDifferentColorVariant(boolean breedingExclusive, RatVariant motherVariant, RatVariant fatherVariant) {
      try {
         List<RatVariant> variants = new ArrayList<>(RatVariantRegistry.RAT_VARIANT_REGISTRY.stream().toList());
         variants.removeIf(variant -> variant.isBreedingExclusive() != breedingExclusive || variant == motherVariant || variant == fatherVariant);
         if (variants.isEmpty()) {
            throw new IllegalStateException("No coat variants available for mutation, breedingExclusive=" + breedingExclusive);
         } else {
            return variants.get(this.getRandom().nextInt(variants.size()));
         }
      } catch (RuntimeException var5) {
         RatsMod.LOGGER.error("Failed to pick a random mutation coat for rat {}", this.getUUID(), var5);
         throw var5;
      }
   }

   public float getRatModelScaleFromGenes() {
      try {
         return !this.getEntityData().get(RAT_GENES_INITIALIZED) ? 1.0F : this.calculateRatModelScale((Float)this.getEntityData().get(GENE_HEALTH));
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the model size from the genes of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   private float getRatRegenGene() {
      try {
         return this.getRatGene(GENE_REGEN);
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the regeneration gene of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   public float getRatSpeedGeneForDisplay() {
      try {
         return this.getRatGene(GENE_SPEED);
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the speed gene of rat {} for the GUI", this.getUUID(), var2);
         throw var2;
      }
   }

   public float getRatHealthGeneForDisplay() {
      try {
         return this.getRatGene(GENE_HEALTH);
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the health gene of rat {} for the GUI", this.getUUID(), var2);
         throw var2;
      }
   }

   public float getRatArmorGeneForDisplay() {
      try {
         return this.getRatGene(GENE_ARMOR);
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the armor gene of rat {} for the GUI", this.getUUID(), var2);
         throw var2;
      }
   }

   public float getRatDamageGeneForDisplay() {
      try {
         return this.getRatGene(GENE_DAMAGE);
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the damage gene of rat {} for the GUI", this.getUUID(), var2);
         throw var2;
      }
   }

   public float getRatRegenGeneForDisplay() {
      try {
         return this.getRatGene(GENE_REGEN);
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the regeneration gene of rat {} for the GUI", this.getUUID(), var2);
         throw var2;
      }
   }

   public void setRatGivenName(String name) {
      try {
         String trimmedName = name == null ? "" : name.trim();
         if (trimmedName.isBlank()) {
            this.setCustomName(null);
            this.setCustomNameVisible(false);
         } else if (trimmedName.length() > 48) {
            throw new IllegalArgumentException("Rat name is longer than 48 characters: " + trimmedName.length());
         } else {
            this.setCustomName(Component.literal(trimmedName));
            this.setCustomNameVisible(true);
            RatsMod.LOGGER.info("Rat {} was named '{}'", this.getUUID(), trimmedName);
         }
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to name rat {}: {}", this.getUUID(), name, var3);
         throw var3;
      }
   }

   public static boolean isCheeseReserveItem(ItemStack stack) {
      try {
         return !stack.isEmpty() && (stack.is(RatsItemTags.CHEESE_ITEMS) || stack.is((Item)RatsItemRegistry.CREATIVE_CHEESE.get()));
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to check the cheese stack of rat {}", stack, var2);
         throw var2;
      }
   }

   private void tickRatHunger() {
      try {
         if (Math.floorMod(this.tickCount + this.getId(), 20) == 0) {
            this.resetRatHungerDayIfNeeded();
            if (this.needsCheeseNow()) {
               this.consumeCheeseFromReserve("personal stock");
            }

            if (Math.floorMod(this.tickCount + this.getId(), 100) == 0) {
               this.shareCheeseWithNearbyRats();
            }
         }
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to process hunger of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   private long getRatHungerDay() {
      try {
         return this.level().getGameTime() / 24000L;
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the hunger day of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   private void resetRatHungerDayIfNeeded() {
      try {
         long currentDay = this.getRatHungerDay();
         if (this.level().isClientSide()) {
            this.cheeseEatenToday = (Integer)this.getEntityData().get(CHEESE_EATEN_TODAY_SYNC);
            this.lastFeedDay = (long)((Integer)this.getEntityData().get(LAST_FEED_DAY_SYNC)).intValue();
         } else {
            if (this.lastFeedDay != currentDay) {
               this.cheeseEatenToday = 0;
               this.lastFeedDay = currentDay;
               this.cachedHungerEfficiencyTick = this.tickCount;
               this.cachedHungerEfficiency = 0.0F;
               this.syncRatHungerData();
               RatsMod.LOGGER.debug("Rat {} started a new hunger day {}", this.getUUID(), currentDay);
            }
         }
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to reset the hunger day of rat {}", this.getUUID(), var3);
         throw var3;
      }
   }

   private void syncRatHungerData() {
      try {
         this.getEntityData().set(CHEESE_EATEN_TODAY_SYNC, this.cheeseEatenToday);
         this.getEntityData().set(LAST_FEED_DAY_SYNC, Mth.clamp((int)this.lastFeedDay, -1, Integer.MAX_VALUE));
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to sync hunger of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   public int getDailyCheeseRequirement() {
      try {
         return 4;
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the cheese quota of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   public int getCheeseEatenToday() {
      try {
         this.resetRatHungerDayIfNeeded();
         return this.cheeseEatenToday;
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the cheese counter of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   public float getRatHungerEfficiency() {
      try {
         if (!this.level().isClientSide() && this.tickCount - this.cachedHungerEfficiencyTick <= 20) {
            return this.cachedHungerEfficiency;
         } else {
            this.resetRatHungerDayIfNeeded();
            int requirement = this.getDailyCheeseRequirement();
            if (requirement <= 0) {
               throw new IllegalStateException("The daily cheese quota must be positive");
            } else {
               float efficiency = Mth.clamp((float)this.cheeseEatenToday / (float)requirement, 0.0F, 1.0F);
               if (!this.level().isClientSide()) {
                  this.cachedHungerEfficiencyTick = this.tickCount;
                  this.cachedHungerEfficiency = efficiency;
               }

               return efficiency;
            }
         }
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to get the satiety of rat {}", this.getUUID(), var3);
         throw var3;
      }
   }

   public boolean canWorkFromHunger() {
      try {
         return this.getRatHungerEfficiency() > 0.0F;
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to check satiety-based work of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   public double getHungerAdjustedWorkSpeed(double baseSpeed) {
      try {
         if (baseSpeed <= 0.0) {
            throw new IllegalArgumentException("The base speed of a work goal must be positive: " + baseSpeed);
         } else {
            float efficiency = this.getRatHungerEfficiency();
            return efficiency <= 0.0F ? 0.0 : baseSpeed * (double)efficiency;
         }
      } catch (RuntimeException var4) {
         RatsMod.LOGGER.error("Failed to compute the work speed of rat {}", this.getUUID(), var4);
         throw var4;
      }
   }

   public boolean moveSmoothlyTo(double x, double y, double z, double speed) {
      try {
         if (this.shouldUseDirectNavigation()) {
            return this.getNavigation().moveTo(x, y, z, speed);
         } else if (this.canReuseSmoothPath(x, y, z)) {
            return true;
         } else {
            this.rememberSmoothMoveTarget(x, y, z, null);
            return this.getNavigation().moveTo(x, y, z, speed);
         }
      } catch (RuntimeException var10) {
         RatsMod.LOGGER.error("Rat {} failed to move smoothly to {}, {}, {}", this.getUUID(), x, y, z, var10);
         throw var10;
      }
   }

   public boolean moveSmoothlyTo(Entity target, double speed) {
      try {
         if (this.shouldUseDirectNavigation()) {
            return this.getNavigation().moveTo(target, speed);
         } else if (this.lastSmoothMoveEntity == target && !this.getNavigation().isDone() && this.tickCount - this.lastSmoothMoveTick <= 8) {
            return true;
         } else {
            this.rememberSmoothMoveTarget(target.getX(), target.getY(), target.getZ(), target);
            return this.getNavigation().moveTo(target, speed);
         }
      } catch (RuntimeException var5) {
         RatsMod.LOGGER.error("Rat {} failed to move smoothly to entity {}", this.getUUID(), target.getUUID(), var5);
         throw var5;
      }
   }

   public boolean moveSmoothlyTo(Path path, double speed) {
      try {
         if (this.shouldUseDirectNavigation()) {
            return this.getNavigation().moveTo(path, speed);
         } else if (!this.getNavigation().isDone() && this.tickCount - this.lastSmoothMoveTick <= 8) {
            return true;
         } else {
            this.lastSmoothMoveTick = this.tickCount;
            this.lastSmoothMoveEntity = null;
            return this.getNavigation().moveTo(path, speed);
         }
      } catch (RuntimeException var5) {
         RatsMod.LOGGER.error("Rat {} failed to move smoothly along a ready path", this.getUUID(), var5);
         throw var5;
      }
   }

   private boolean shouldUseDirectNavigation() {
      try {
         return this.hasQuarryUpgradeForNavigation || this.navigatorType == 2 || this.navigatorType == 5;
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Rat {} failed to check the smooth navigation mode", this.getUUID(), var2);
         throw var2;
      }
   }

   private boolean canReuseSmoothPath(double x, double y, double z) {
      try {
         if (!this.getNavigation().isDone() && this.tickCount - this.lastSmoothMoveTick <= 8) {
            double dx = this.lastSmoothMoveX - x;
            double dy = this.lastSmoothMoveY - y;
            double dz = this.lastSmoothMoveZ - z;
            return dx * dx + dy * dy + dz * dz <= 1.0;
         } else {
            return false;
         }
      } catch (RuntimeException var13) {
         RatsMod.LOGGER.error("Rat {} failed to check path reuse", this.getUUID(), var13);
         throw var13;
      }
   }

   private void rememberSmoothMoveTarget(double x, double y, double z, @Nullable Entity target) {
      try {
         this.lastSmoothMoveTick = this.tickCount;
         this.lastSmoothMoveX = x;
         this.lastSmoothMoveY = y;
         this.lastSmoothMoveZ = z;
         this.lastSmoothMoveEntity = target;
      } catch (RuntimeException var9) {
         RatsMod.LOGGER.error("Rat {} failed to remember the smooth movement target", this.getUUID(), var9);
         throw var9;
      }
   }

   public boolean needsCheeseNow() {
      try {
         this.resetRatHungerDayIfNeeded();
         int requirement = this.getDailyCheeseRequirement();
         if (requirement <= 0) {
            throw new IllegalStateException("The daily cheese quota must be positive");
         } else {
            long dayProgress = this.level().getGameTime() % 24000L;
            int interval = Math.max(1, 24000 / requirement);
            int requiredByNow = Mth.clamp((int)(dayProgress / (long)interval) + 1, 0, requirement);
            return this.cheeseEatenToday < requiredByNow;
         }
      } catch (RuntimeException var6) {
         RatsMod.LOGGER.error("Failed to check the cheese need of rat {}", this.getUUID(), var6);
         throw var6;
      }
   }

   public boolean consumeCheeseFromReserve(String sourceName) {
      try {
         if (this.getInventory() == null) {
            throw new IllegalStateException("The rat has no inventory for a personal cheese stock");
         } else {
            ItemStack reserve = this.getInventory().getItem(2);
            if (reserve.isEmpty()) {
               return false;
            } else if (!isCheeseReserveItem(reserve)) {
               return false;
            } else {
               boolean consumed = this.eatCheeseFromStack(reserve, sourceName);
               if (consumed) {
                  if (reserve.isEmpty()) {
                     this.getInventory().setItem(2, ItemStack.EMPTY);
                  } else {
                     this.getInventory().setChanged();
                  }
               }

               return consumed;
            }
         }
      } catch (RuntimeException var4) {
         RatsMod.LOGGER.error("Failed to eat cheese from the personal stock of rat {}", this.getUUID(), var4);
         throw var4;
      }
   }

   public int getMaxCheeseReserve() {
      try {
         return 16;
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to get the personal cheese stock maximum of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   public int getCheeseReserveCount() {
      try {
         if (this.getInventory() == null) {
            throw new IllegalStateException("The rat has no inventory to count cheese");
         } else {
            ItemStack reserve = this.getInventory().getItem(2);
            return isCheeseReserveItem(reserve) ? reserve.getCount() : 0;
         }
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to count the personal cheese of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   public int getCheeseReserveFreeSpace(ItemStack stack) {
      try {
         if (!isCheeseReserveItem(stack)) {
            throw new IllegalArgumentException("The stock space check received something that is not cheese: " + stack);
         } else if (this.getInventory() == null) {
            throw new IllegalStateException("The rat has no inventory for a personal cheese stock");
         } else {
            ItemStack reserve = this.getInventory().getItem(2);
            if (reserve.isEmpty()) {
               return this.getMaxCheeseReserve();
            } else {
               return isCheeseReserveItem(reserve) && ItemStack.isSameItemSameComponents(reserve, stack)
                  ? Math.max(0, this.getMaxCheeseReserve() - reserve.getCount())
                  : 0;
            }
         }
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to check cheese space in the stock of rat {}", this.getUUID(), var3);
         throw var3;
      }
   }

   public boolean hasCheeseReserveSpace() {
      try {
         if (this.getInventory() == null) {
            throw new IllegalStateException("The rat has no inventory to check the cheese stock");
         } else {
            ItemStack reserve = this.getInventory().getItem(2);
            return reserve.isEmpty() ? true : isCheeseReserveItem(reserve) && reserve.getCount() < this.getMaxCheeseReserve();
         }
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to check free cheese stock space of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   public boolean shouldRefillCheeseReserve() {
      try {
         return this.hasCheeseReserveSpace() && this.getCheeseReserveCount() <= 2;
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to check the cheese refill threshold of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   public int addCheeseToReserve(ItemStack source) {
      try {
         if (!isCheeseReserveItem(source)) {
            throw new IllegalArgumentException("Only cheese can be put into the rat's personal stock: " + source);
         } else if (this.getInventory() == null) {
            throw new IllegalStateException("The rat has no inventory to refill cheese");
         } else {
            int space = this.getCheeseReserveFreeSpace(source);
            if (space <= 0) {
               return 0;
            } else {
               int moved = Math.min(space, source.getCount());
               ItemStack reserve = this.getInventory().getItem(2);
               if (reserve.isEmpty()) {
                  ItemStack inserted = source.split(moved);
                  inserted.setCount(Math.min(inserted.getCount(), this.getMaxCheeseReserve()));
                  this.getInventory().setItem(2, inserted);
               } else {
                  reserve.grow(moved);
                  source.shrink(moved);
                  this.getInventory().setChanged();
               }

               return moved;
            }
         }
      } catch (RuntimeException var6) {
         RatsMod.LOGGER.error("Failed to refill the personal cheese stock of rat {}", this.getUUID(), var6);
         throw var6;
      }
   }

   public ItemStack takeCheeseFromReserve(int amount) {
      try {
         if (amount <= 0) {
            throw new IllegalArgumentException("The amount of cheese to take must be positive: " + amount);
         } else if (this.getInventory() == null) {
            throw new IllegalStateException("The rat has no inventory to hand out cheese");
         } else {
            ItemStack reserve = this.getInventory().getItem(2);
            if (!isCheeseReserveItem(reserve)) {
               return ItemStack.EMPTY;
            } else {
               ItemStack removed = reserve.split(Math.min(amount, reserve.getCount()));
               if (reserve.isEmpty()) {
                  this.getInventory().setItem(2, ItemStack.EMPTY);
               } else {
                  this.getInventory().setChanged();
               }

               return removed;
            }
         }
      } catch (RuntimeException var4) {
         RatsMod.LOGGER.error("Failed to take cheese from the personal stock of rat {}", this.getUUID(), var4);
         throw var4;
      }
   }

   private void shareCheeseWithNearbyRats() {
      try {
         if (this.getOwnerUUID() != null && this.getCheeseReserveCount() > 1) {
            TamedRat receiver = this.level()
               .getEntitiesOfClass(
                  TamedRat.class,
                  this.getBoundingBox().inflate(8.0),
                  other -> other != this
                        && other.isAlive()
                        && Objects.equals(other.getOwnerUUID(), this.getOwnerUUID())
                        && other.getCheeseReserveCount() + 1 < this.getCheeseReserveCount()
               )
               .stream()
               .min((first, second) -> Integer.compare(first.getCheeseReserveCount(), second.getCheeseReserveCount()))
               .orElse(null);
            if (receiver != null) {
               ItemStack moved = this.takeCheeseFromReserve(1);
               if (!moved.isEmpty()) {
                  int accepted = receiver.addCheeseToReserve(moved);
                  if (accepted <= 0) {
                     this.addCheeseToReserve(moved);
                  } else {
                     if (!moved.isEmpty()) {
                        this.addCheeseToReserve(moved);
                     }

                     RatsMod.LOGGER
                        .debug(
                           "Rat {} shared cheese with rat {}: donor={}, recipient={}",
                           this.getUUID(),
                           receiver.getUUID(),
                           this.getCheeseReserveCount(),
                           receiver.getCheeseReserveCount()
                        );
                  }
               }
            }
         }
      } catch (RuntimeException var4) {
         RatsMod.LOGGER.error("Failed to share cheese for rat {}", this.getUUID(), var4);
         throw var4;
      }
   }

   public boolean eatCheeseFromStack(ItemStack stack, String sourceName) {
      try {
         if (stack.isEmpty()) {
            return false;
         } else if (!isCheeseReserveItem(stack)) {
            throw new IllegalArgumentException("A rat can only eat cheese, got stack: " + stack);
         } else if (!this.needsCheeseNow()) {
            return false;
         } else {
            stack.shrink(1);
            this.recordRatCheeseMeal(sourceName);
            return true;
         }
      } catch (RuntimeException var4) {
         RatsMod.LOGGER.error("Failed to feed rat {} from source {}", this.getUUID(), sourceName, var4);
         throw var4;
      }
   }

   private void recordRatCheeseMeal(String sourceName) {
      try {
         int requirement = this.getDailyCheeseRequirement();
         this.cheeseEatenToday = Mth.clamp(this.cheeseEatenToday + 1, 0, requirement);
         this.lastFeedDay = this.getRatHungerDay();
         this.cachedHungerEfficiencyTick = -1000;
         this.syncRatHungerData();
         this.playSound(SoundEvents.GENERIC_EAT, 0.6F, 0.9F + this.getRandom().nextFloat() * 0.2F);
         RatsMod.LOGGER.debug("Rat {} ate cheese from source '{}': {}/{}", this.getUUID(), sourceName, this.cheeseEatenToday, requirement);
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to record feeding of rat {}", this.getUUID(), var3);
         throw var3;
      }
   }

   private void ensureRatGenesInitialized(String reason) {
      try {
         if (!(Boolean)this.getEntityData().get(RAT_GENES_INITIALIZED)) {
            this.initializeRandomRatGenes(reason);
         }
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to check genes of rat {} for reason {}", this.getUUID(), reason, var3);
         throw var3;
      }
   }

   private void initializeRandomRatGenes(String reason) {
      try {
         RandomSource random = this.getRandom();
         this.setRatGenes(
            0.5F + random.nextFloat() * 2.0F,
            0.5F + random.nextFloat() * 2.0F,
            0.5F + random.nextFloat() * 2.0F,
            0.5F + random.nextFloat() * 2.0F,
            0.5F + random.nextFloat() * 2.0F
         );
         this.onUpgradeChanged();
         RatsMod.LOGGER
            .debug(
               "Rat {} got random genes on {}: speed={}, health={}, armor={}, damage={}, regeneration={}",
               this.getUUID(),
               reason,
               this.getRatGene(GENE_SPEED),
               this.getRatGene(GENE_HEALTH),
               this.getRatGene(GENE_ARMOR),
               this.getRatGene(GENE_DAMAGE),
               this.getRatGene(GENE_REGEN)
            );
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to create random genes for rat {} on {}", this.getUUID(), reason, var3);
         throw var3;
      }
   }

   private void inheritRatGenesFromParents(TamedRat mother, TamedRat father) {
      try {
         mother.ensureRatGenesInitialized("mother_breeding");
         father.ensureRatGenesInitialized("father_breeding");
         this.setRatGenes(
            this.mixParentGene(mother.getRatGene(GENE_SPEED), father.getRatGene(GENE_SPEED), "speed"),
            this.mixParentGene(mother.getRatGene(GENE_HEALTH), father.getRatGene(GENE_HEALTH), "health"),
            this.mixParentGene(mother.getRatGene(GENE_ARMOR), father.getRatGene(GENE_ARMOR), "armor"),
            this.mixParentGene(mother.getRatGene(GENE_DAMAGE), father.getRatGene(GENE_DAMAGE), "damage"),
            this.mixParentGene(mother.getRatGene(GENE_REGEN), father.getRatGene(GENE_REGEN), "regen")
         );
         this.onUpgradeChanged();
         RatsMod.LOGGER
            .info(
               "Rat {} inherited genes: speed={}, health={}, armor={}, damage={}, regeneration={}",
               this.getUUID(),
               this.getRatGene(GENE_SPEED),
               this.getRatGene(GENE_HEALTH),
               this.getRatGene(GENE_ARMOR),
               this.getRatGene(GENE_DAMAGE),
               this.getRatGene(GENE_REGEN)
            );
      } catch (RuntimeException var4) {
         RatsMod.LOGGER.error("Failed to inherit genes of rat {} from parents {} and {}", this.getUUID(), mother.getUUID(), father.getUUID(), var4);
         throw var4;
      }
   }

   private float mixParentGene(float firstParentGene, float secondParentGene, String geneName) {
      try {
         float min = Math.min(firstParentGene, secondParentGene);
         float max = Math.max(firstParentGene, secondParentGene);
         float mixed = min + this.getRandom().nextFloat() * (max - min);
         if (this.getRandom().nextFloat() <= 0.3F) {
            mixed = this.mutateRatGene(mixed, min, max, geneName);
         }

         return this.clampRatGene(mixed);
      } catch (RuntimeException var7) {
         RatsMod.LOGGER.error("Failed to mix gene {} for rat {}", geneName, this.getUUID(), var7);
         throw var7;
      }
   }

   private float mutateRatGene(float mixed, float parentMin, float parentMax, String geneName) {
      try {
         float mutation = 0.05F + this.getRandom().nextFloat() * 0.2F;
         boolean canMutateDown = parentMin > 0.5F;
         boolean canMutateUp = parentMax < 2.5F;
         if (!canMutateDown && !canMutateUp) {
            return this.clampRatGene(mixed);
         } else {
            boolean mutateUp = canMutateUp && (!canMutateDown || this.getRandom().nextBoolean());
            float mutated = mutateUp ? parentMax + mutation : parentMin - mutation;
            RatsMod.LOGGER.debug("Gene {} mutation in rat {}: {} -> {}", geneName, this.getUUID(), mixed, mutated);
            return this.clampRatGene(mutated);
         }
      } catch (RuntimeException var10) {
         RatsMod.LOGGER.error("Failed to mutate gene {} in rat {}", geneName, this.getUUID(), var10);
         throw var10;
      }
   }

   private void setRatGenes(float speed, float health, float armor, float damage, float regen) {
      try {
         this.getEntityData().set(GENE_SPEED, this.clampRatGene(speed));
         this.getEntityData().set(GENE_HEALTH, this.clampRatGene(health));
         this.getEntityData().set(GENE_ARMOR, this.clampRatGene(armor));
         this.getEntityData().set(GENE_DAMAGE, this.clampRatGene(damage));
         this.getEntityData().set(GENE_REGEN, this.clampRatGene(regen));
         this.getEntityData().set(RAT_GENES_INITIALIZED, true);
         this.refreshDimensions();
      } catch (RuntimeException var7) {
         RatsMod.LOGGER.error("Failed to write genes of rat {}", this.getUUID(), var7);
         throw var7;
      }
   }

   private void saveRatGenes(CompoundTag tag) {
      try {
         if ((Boolean)this.getEntityData().get(RAT_GENES_INITIALIZED)) {
            CompoundTag genes = new CompoundTag();
            genes.putFloat("Speed", this.getRatGene(GENE_SPEED));
            genes.putFloat("Health", this.getRatGene(GENE_HEALTH));
            genes.putFloat("Armor", this.getRatGene(GENE_ARMOR));
            genes.putFloat("Damage", this.getRatGene(GENE_DAMAGE));
            genes.putFloat("Regen", this.getRatGene(GENE_REGEN));
            tag.put("RatGenes", genes);
         }
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to save genes of rat {}", this.getUUID(), var3);
         throw var3;
      }
   }

   private void loadRatGenes(CompoundTag tag) {
      try {
         if (!tag.contains("RatGenes", 10)) {
            this.getEntityData().set(RAT_GENES_INITIALIZED, false);
         } else {
            CompoundTag genes = tag.getCompound("RatGenes");
            this.setRatGenes(genes.getFloat("Speed"), genes.getFloat("Health"), genes.getFloat("Armor"), genes.getFloat("Damage"), genes.getFloat("Regen"));
         }
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to load genes of rat {}", this.getUUID(), var3);
         throw var3;
      }
   }

   private void applyRatGeneAttributeModifiers() {
      try {
         if ((Boolean)this.getEntityData().get(RAT_GENES_INITIALIZED)) {
            this.addRatGeneAttributeModifier(Attributes.MOVEMENT_SPEED, "gene_speed", this.getRatGene(GENE_SPEED));
            this.addRatGeneAttributeModifier(Attributes.FLYING_SPEED, "gene_flying_speed", this.getRatGene(GENE_SPEED));
            this.addRatGeneAttributeModifier(Attributes.MAX_HEALTH, "gene_health", this.getRatGene(GENE_HEALTH));
            this.addRatGeneAttributeModifier(Attributes.ARMOR, "gene_armor", this.getRatGene(GENE_ARMOR));
            this.addRatGeneAttributeModifier(Attributes.ATTACK_DAMAGE, "gene_damage", this.getRatGene(GENE_DAMAGE));
         }
      } catch (RuntimeException var2) {
         RatsMod.LOGGER.error("Failed to apply genes to the attributes of rat {}", this.getUUID(), var2);
         throw var2;
      }
   }

   private void addRatGeneAttributeModifier(Holder<Attribute> attribute, String modifierName, float multiplier) {
      try {
         AttributeInstance attributeInstance = this.getAttribute(attribute);
         if (attributeInstance == null) {
            throw new IllegalStateException("The rat has no attribute " + modifierName);
         } else {
            AttributeModifier modifier = new AttributeModifier(
               ResourceLocation.fromNamespaceAndPath("rats", modifierName), (double)multiplier - 1.0, Operation.ADD_MULTIPLIED_BASE
            );
            attributeInstance.addTransientModifier(modifier);
            this.attributeChanges.put(attribute, modifier);
         }
      } catch (RuntimeException var6) {
         RatsMod.LOGGER.error("Failed to add genetic modifier {} to rat {}", modifierName, this.getUUID(), var6);
         throw var6;
      }
   }

   private float getRatGene(EntityDataAccessor<Float> gene) {
      try {
         return !this.getEntityData().get(RAT_GENES_INITIALIZED) ? 1.0F : this.clampRatGene((Float)this.getEntityData().get(gene));
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to read a gene of rat {}", this.getUUID(), var3);
         throw var3;
      }
   }

   private float clampRatGene(float value) {
      try {
         return Mth.clamp(value, 0.5F, 2.5F);
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to clamp gene of rat {} with value {}", this.getUUID(), value, var3);
         throw var3;
      }
   }

   private float calculateRatModelScale(float healthGene) {
      try {
         float normalized = (this.clampRatGene(healthGene) - 0.5F) / 2.0F;
         return 0.75F + normalized * 0.75F;
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to compute model size of rat {} from health gene {}", this.getUUID(), healthGene, var3);
         throw var3;
      }
   }

   private void tryInheritBabyUpgrade(TamedRat father, TamedRat mother, TamedRat baby) {
      try {
         if (this.getRandom().nextFloat() > 0.35F) {
            return;
         }

         List<ItemStack> pool = new ArrayList<>();
         this.collectParentUpgradesForGenetics(father, pool);
         this.collectParentUpgradesForGenetics(mother, pool);
         pool.removeIf(
            stack -> stack.isEmpty()
                  || RatsRemovedContent.isRemoved(stack.getItem())
                  || !(stack.getItem() instanceof BaseRatUpgradeItem)
                  || stack.getItem() instanceof CombinedRatUpgradeItem
         );
         if (pool.size() < 2) {
            return;
         }

         RegistryAccess registries = baby.level().registryAccess();

         for (int attempt = 0; attempt < 12; attempt++) {
            ItemStack first = pool.get(this.getRandom().nextInt(pool.size())).copy();
            ItemStack second = pool.get(this.getRandom().nextInt(pool.size())).copy();
            if (!ItemStack.isSameItemSameComponents(first, second)) {
               ItemStack combined = new ItemStack((ItemLike)RatsItemRegistry.RAT_UPGRADE_COMBINED.get());
               if (CombinedRatUpgradeItem.canCombineWithUpgrade(combined, first)) {
                  combined = UpgradeCombinerBlockEntity.mergeUpgradeIntoCombined(combined, first, registries);
                  if (CombinedRatUpgradeItem.canCombineWithUpgrade(combined, second)) {
                     combined = UpgradeCombinerBlockEntity.mergeUpgradeIntoCombined(combined, second, registries);

                     for (EquipmentSlot slot : RatUpgradeUtils.UPGRADE_SLOTS) {
                        if (baby.getItemBySlot(slot).isEmpty()) {
                           baby.setItemSlot(slot, combined);
                           baby.onUpgradeChanged();
                           return;
                        }
                     }

                     return;
                  }
               }
            }
         }
      } catch (Exception var14) {
         RatsMod.LOGGER.error("tryInheritBabyUpgrade: {}", var14.getMessage(), var14);
      }
   }

   private void collectParentUpgradesForGenetics(TamedRat parent, List<ItemStack> pool) {
      for (EquipmentSlot slot : RatUpgradeUtils.UPGRADE_SLOTS) {
         ItemStack equipped = parent.getItemBySlot(slot);
         if (!equipped.isEmpty()) {
            if (equipped.getItem() instanceof CombinedUpgrade combined) {
               CompoundTag tag = RatsItemStackData.getTag(equipped);
               if (tag != null && tag.contains("Items", 9)) {
                  NonNullList<ItemStack> contents = NonNullList.withSize(combined.getUpgradeSlots(), ItemStack.EMPTY);
                  ContainerHelper.loadAllItems(tag, contents, parent.level().registryAccess());

                  for (ItemStack inner : contents) {
                     if (!inner.isEmpty()) {
                        pool.add(inner);
                     }
                  }
               }
            } else if (equipped.getItem() instanceof BaseRatUpgradeItem) {
               pool.add(equipped);
            }
         }
      }
   }

   public boolean isPickable() {
      return !(this.getVehicle() instanceof Player);
   }

   public ItemStack getResultForRecipe(RecipeType<? extends SingleItemRecipe> recipe, ItemStack stack) {
      Optional<? extends RecipeHolder<? extends SingleItemRecipe>> optional = this.level()
         .getRecipeManager()
         .getRecipeFor(recipe, new SingleRecipeInput(stack), this.level());
      if (optional.isPresent()) {
         ItemStack itemstack = ((SingleItemRecipe)optional.get().value()).getResultItem(this.level().registryAccess());
         if (!itemstack.isEmpty()) {
            ItemStack itemstack1 = itemstack.copy();
            itemstack1.setCount(stack.getCount() * itemstack.getCount());
            return itemstack1;
         }
      }

      return ItemStack.EMPTY;
   }

   public boolean tryDepositItemInContainers(ItemStack burntItem) {
      if (this.level().getBlockEntity(this.blockPosition()) != null) {
         BlockEntity te = this.level().getBlockEntity(this.blockPosition());
         if (te != null) {
            IItemHandler handler = (IItemHandler)this.level().getCapability(ItemHandler.BLOCK, te.getBlockPos(), Direction.UP);
            if (handler != null && ItemHandlerHelper.insertItem(handler, burntItem, true).isEmpty()) {
               ItemHandlerHelper.insertItem(handler, burntItem, false);
               return true;
            }
         }
      }

      return false;
   }

   protected void doPush(Entity entity) {
      if (!this.crafting) {
         entity.push(this);
      }
   }

   public Animation getDanceAnimation() {
      return this.getDanceMoves() == 0 ? ANIMATION_DANCE : NO_ANIMATION;
   }

   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      if (this.hasToga()) {
         this.spawnAtLocation(new ItemStack((ItemLike)RatlantisItemRegistry.RAT_TOGA.get()), 0.0F);
      }

      super.dropCustomDeathLoot(level, source, recentlyHit);
   }

   public void spawnAngelCopy() {
      TamedRat copy = (TamedRat)((EntityType)RatsEntityRegistry.TAMED_RAT.get()).create(this.level());
      CompoundTag tag = new CompoundTag();
      this.addAdditionalSaveData(tag);
      tag.putShort("HurtTime", (short)0);
      tag.putInt("HurtByTimestamp", 0);
      tag.putShort("DeathTime", (short)0);
      copy.readAdditionalSaveData(tag);
      copy.setHealth(copy.getMaxHealth());
      copy.copyPosition(this);
      copy.setRespawnCountdown(1200);
      if (copy.isVisuallySitting()) {
         copy.sitProgress = 20.0F;
      }

      if (this.hasCustomName()) {
         copy.setCustomName(this.getCustomName());
      }

      copy.clearFire();
      this.level().addFreshEntity(copy);
   }

   public boolean canBeAffected(MobEffectInstance instance) {
      return instance.getEffect() != MobEffects.POISON
            || !RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_POISON.get())
               && !RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_DAMAGE_PROTECTION.get())
         ? super.canBeAffected(instance)
         : false;
   }

   @Override
   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      ItemStack itemstack = player.getItemInHand(hand);
      if (this.getRespawnCountdown() > 0 || itemstack.is((Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("rats", "rat_spawn_egg")))) {
         return InteractionResult.PASS;
      } else if (RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_CARRAT.get()) && player.getFoodData().needsFood()) {
         player.getFoodData().eat(1, 0.1F);
         player.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);

         for (int i = 0; i < 8; i++) {
            double d0 = this.getRandom().nextGaussian() * 0.02;
            double d1 = this.getRandom().nextGaussian() * 0.02;
            double d2 = this.getRandom().nextGaussian() * 0.02;
            this.level()
               .addParticle(
                  new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CARROT)),
                  this.getX() + (double)(this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
                  this.getY() + (double)(this.getRandom().nextFloat() * this.getBbHeight() * 2.0F) - (double)this.getBbHeight(),
                  this.getZ() + (double)(this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
                  d0,
                  d1,
                  d2
               );
         }

         if (RatConfig.carratDamagePerBite > 0.0) {
            this.hurt(this.damageSources().playerAttack(player), (float)RatConfig.carratDamagePerBite);
         }

         return InteractionResult.SUCCESS;
      } else {
         if (this.isBaby()) {
            if (itemstack.is(RatsItemTags.CHEESE_ITEMS)) {
               if (!player.getAbilities().instabuild) {
                  itemstack.shrink(1);
               }

               this.ageUp(getSpeedUpSecondsWhenFeeding(-this.getAge()), true);
               return InteractionResult.sidedSuccess(this.level().isClientSide());
            }

            if (itemstack.is((Item)RatsItemRegistry.CREATIVE_CHEESE.get())) {
               this.ageUp(-(this.getAge() / 20), true);
               return InteractionResult.sidedSuccess(this.level().isClientSide());
            }
         }

         if (!this.isBaby() && this.isOwnedBy(player)) {
            if (player.isSecondaryUseActive() && !this.isPassenger()) {
               if (player.getPassengers().size() < 3) {
                  this.startRiding(player, true);
                  player.displayClientMessage(Component.translatable("entity.rats.rat.dismount"), true);
               }

               return InteractionResult.sidedSuccess(this.level().isClientSide());
            }

            if (!itemstack.is((Item)RatsItemRegistry.RAT_PAPERS.get())) {
               if (itemstack.is((Item)RatlantisItemRegistry.RAT_TOGA.get())) {
                  if (!this.hasToga()) {
                     if (!player.isCreative()) {
                        itemstack.shrink(1);
                     }
                  } else if (!this.level().isClientSide()) {
                     this.spawnAtLocation(new ItemStack((ItemLike)RatlantisItemRegistry.RAT_TOGA.get()), 0.0F);
                  }

                  this.setToga(!this.hasToga());
                  this.playSound((SoundEvent)SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.5F);
                  return InteractionResult.SUCCESS;
               }

               if (itemstack.is(((Block)RatsBlockRegistry.DYE_SPONGE.get()).asItem()) && this.isDyed()) {
                  this.setDyed(false);
                  this.setDyeColor(0);
                  this.setSpecialDye("");

                  for (int i = 0; i < 8; i++) {
                     double d0 = this.getRandom().nextGaussian() * 0.02;
                     double d1 = this.getRandom().nextGaussian() * 0.02;
                     double d2 = this.getRandom().nextGaussian() * 0.02;
                     this.level()
                        .addParticle(
                           new ItemParticleOption(ParticleTypes.ITEM, new ItemStack((ItemLike)RatsBlockRegistry.DYE_SPONGE.get())),
                           this.getX() + (double)(this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
                           this.getY() + (double)(this.getRandom().nextFloat() * this.getBbHeight() * 2.0F) - (double)this.getBbHeight(),
                           this.getZ() + (double)(this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
                           d0,
                           d1,
                           d2
                        );
                  }

                  this.playSound((SoundEvent)RatsSoundRegistry.DYE_SPONGE_USED.get(), this.getSoundVolume(), this.getVoicePitch());
                  return InteractionResult.SUCCESS;
               }

               if (itemstack.is((Item)RatsItemRegistry.RATBOW_ESSENCE.get()) && this.applySpecialDyeIfPossible(itemstack)) {
                  return InteractionResult.SUCCESS;
               }

               if (this.applyNormalDyeIfPossible(itemstack)) {
                  return InteractionResult.SUCCESS;
               }

               if (itemstack.is((Item)RatsItemRegistry.RAT_SACK.get())) {
                  if (RatSackItem.getRatsInSack(itemstack) >= RatConfig.ratSackCapacity) {
                     player.displayClientMessage(Component.translatable("item.rats.rat_sack.too_full").withStyle(ChatFormatting.RED), true);
                     return InteractionResult.PASS;
                  }

                  RatSackItem.packRatIntoSack(itemstack, this, RatSackItem.getRatsInSack(itemstack) + 1);
                  this.playSound((SoundEvent)SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
                  this.discard();
                  player.swing(hand);
                  return InteractionResult.SUCCESS;
               }

               if (itemstack.getItem() instanceof RatStaffItem) {
                  SelectedRat selectedRat = (SelectedRat)player.getCapability(RatsCapabilityRegistry.SELECTED_RAT);
                  if (selectedRat != null) {
                     selectedRat.setSelectedRat(this);
                  }

                  player.swing(hand);
                  if (!this.level().isClientSide() && player instanceof ServerPlayer sp) {
                     RatsNetworkHandler.CHANNEL
                        .send(
                           RatsNetworkHandler.playerTarget(() -> sp),
                           new ManageRatStaffPacket(this.getId(), BlockPos.ZERO, Direction.NORTH.ordinal(), false, false)
                        );
                  }

                  player.displayClientMessage(Component.translatable("item.rats.staff.bind", new Object[]{this.getName()}), true);
                  return InteractionResult.SUCCESS;
               }

               if (itemstack.is(Items.ARROW)) {
                  itemstack.shrink(1);
                  ItemStack arrow = new ItemStack((ItemLike)RatsItemRegistry.RAT_ARROW.get());
                  CompoundTag tag = new CompoundTag();
                  CompoundTag ratTag = new CompoundTag();
                  this.addAdditionalSaveData(ratTag);
                  if (this.hasCustomName()) {
                     ratTag.putString("CustomName", Serializer.toJson(this.getCustomName(), this.registryAccess()));
                  }

                  tag.put("Rat", ratTag);
                  arrow.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                  if (itemstack.isEmpty()) {
                     player.setItemInHand(hand, arrow);
                  } else if (!player.getInventory().add(arrow)) {
                     player.drop(arrow, false);
                  }

                  this.playSound((SoundEvent)RatsSoundRegistry.RAT_HURT.get(), 1.0F, 1.0F);
                  player.swing(hand);
                  this.discard();
                  return InteractionResult.SUCCESS;
               }

               return super.mobInteract(player, hand);
            }

            InteractionResult result = itemstack.interactLivingEntity(player, this, hand);
            if (result.consumesAction()) {
               return result;
            }
         }

         return InteractionResult.PASS;
      }
   }

   public boolean applyNormalDyeIfPossible(ItemStack stack) {
      if (stack.getItem() instanceof DyeItem item && (!this.isDyed() || this.getDyeColor() != item.getDyeColor().getId())) {
         if (!this.isDyed()) {
            this.setDyed(true);
         }

         this.setDyeColor(item.getDyeColor().getId());

         for (int i = 0; i < 8; i++) {
            double d0 = this.getRandom().nextGaussian() * 0.02;
            double d1 = this.getRandom().nextGaussian() * 0.02;
            double d2 = this.getRandom().nextGaussian() * 0.02;
            this.level()
               .addParticle(
                  new ItemParticleOption(ParticleTypes.ITEM, stack),
                  this.getX() + (double)(this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
                  this.getY() + (double)(this.getRandom().nextFloat() * this.getBbHeight() * 2.0F) - (double)this.getBbHeight(),
                  this.getZ() + (double)(this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
                  d0,
                  d1,
                  d2
               );
         }

         this.playSound((SoundEvent)RatsSoundRegistry.ESSENCE_APPLIED.get(), this.getSoundVolume(), this.getVoicePitch());
         stack.shrink(1);
         return true;
      }

      return false;
   }

   public boolean applySpecialDyeIfPossible(ItemStack stack) {
      String name = stack.getHoverName().getString();
      if (this.getSpecialDye().equals(name) && this.isDyed()) {
         return false;
      } else {
         if (!this.isDyed()) {
            this.setDyed(true);
         }

         this.setDyeColor(100);
         this.setSpecialDye(name);

         for (int i = 0; i < 8; i++) {
            double d0 = this.getRandom().nextGaussian() * 0.02;
            double d1 = this.getRandom().nextGaussian() * 0.02;
            double d2 = this.getRandom().nextGaussian() * 0.02;
            this.level()
               .addParticle(
                  new ItemParticleOption(ParticleTypes.ITEM, new ItemStack((ItemLike)RatsItemRegistry.RATBOW_ESSENCE.get())),
                  this.getX() + (double)(this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
                  this.getY() + (double)(this.getRandom().nextFloat() * this.getBbHeight() * 2.0F) - (double)this.getBbHeight(),
                  this.getZ() + (double)(this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
                  d0,
                  d1,
                  d2
               );
         }

         this.playSound((SoundEvent)RatsSoundRegistry.ESSENCE_APPLIED.get(), this.getSoundVolume(), this.getVoicePitch());
         stack.shrink(1);
         return true;
      }
   }

   public void setTame(boolean tamed) {
      try {
         this.setTame(tamed, true);
      } catch (RuntimeException var3) {
         RatsMod.LOGGER.error("Failed to change taming of rat {} to {}", this.getUUID(), tamed, var3);
         throw var3;
      }
   }

   public void setTame(boolean tamed, boolean applyTamingSideEffects) {
      try {
         if (tamed) {
            Arrays.fill(this.armorDropChances, 1.1F);
            Arrays.fill(this.handDropChances, 1.1F);
         }

         super.setTame(tamed, applyTamingSideEffects);
         if (tamed && !this.level().isClientSide() && !this.loadingSaveData) {
            this.ensureRatGenesInitialized("taming");
         }
      } catch (RuntimeException var4) {
         RatsMod.LOGGER.error("Failed to change taming of rat {} to {}", this.getUUID(), tamed, var4);
         throw var4;
      }
   }

   public int getAmbientSoundInterval() {
      return RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_CHRISTMAS.get()) ? 1000 : 200;
   }

   public boolean causeFallDamage(float dist, float mult, DamageSource source) {
      try {
         if (source.is(DamageTypes.FALL) && dist < 5.0F) {
            this.resetFallDistance();
            RatsMod.LOGGER.debug("Rat {} safely fell from height {} at {}", this.getUUID(), dist, this.blockPosition());
            return false;
         } else {
            return !this.isInvulnerableTo(source) && !this.isPassenger() ? super.causeFallDamage(dist, mult, source) : false;
         }
      } catch (RuntimeException var5) {
         RatsMod.LOGGER.error("Failed to handle the fall of rat {} from height {}", this.getUUID(), dist, var5);
         throw var5;
      }
   }

   protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
      if (!this.hasFlightUpgrade()) {
         super.checkFallDamage(y, onGround, state, pos);
      }
   }

   public void rideTick() {
      super.rideTick();
      if (this.getVehicle() instanceof Player player) {
         this.updateRiding(player);
      }
   }

   public void updateRiding(Player riding) {
      int i = riding.getPassengers().indexOf(this);
      float radius = (i == 0 ? 0.0F : 0.4F) + (float)(riding.isFallFlying() ? 1 : 0);
      float angle = (float) (Math.PI / 180.0) * riding.yBodyRot + (i == 2 ? -92.5F : (i == 1 ? 92.5F : 0.0F));
      double extraX = (double)(radius * Mth.sin((float)(Math.PI + (double)angle)));
      double extraZ = (double)(radius * Mth.cos(angle));
      double extraY = riding.isCrouching() ? 1.2 : (riding.isFallFlying() ? 0.25 : 1.4);
      this.setYRot(riding.yHeadRot);
      this.yHeadRot = riding.yHeadRot;
      this.yRotO = riding.yHeadRot;
      this.setPos(riding.getX() + extraX, riding.getY() + extraY, riding.getZ() + extraZ);
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == 85) {
         this.crafting = true;
      } else if (id == 86) {
         this.crafting = false;
      } else if (id == 127) {
         for (int k = 0; k < 20; k++) {
            double d2 = this.getRandom().nextGaussian() * 0.02;
            double d0 = this.getRandom().nextGaussian() * 0.02;
            double d1 = this.getRandom().nextGaussian() * 0.02;
            this.level()
               .addParticle(
                  ParticleTypes.POOF,
                  this.getX() + (double)(this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
                  this.getY() + (double)(this.getRandom().nextFloat() * this.getBbHeight()),
                  this.getZ() + (double)(this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
                  d2,
                  d0,
                  d1
               );
         }
      } else {
         super.handleEntityEvent(id);
      }
   }

   @Override
   protected SoundEvent getAmbientSound() {
      if (RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_CHRISTMAS.get())) {
         return (SoundEvent)RatsSoundRegistry.RAT_SANTA.get();
      } else {
         if (RatsMod.ICEANDFIRE_LOADED && RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_DRAGON.get())) {
            SoundEvent possibleDragonSound = (SoundEvent)BuiltInRegistries.SOUND_EVENT
               .get(ResourceLocation.fromNamespaceAndPath("iceandfire", "firedragon_child_idle"));
            if (possibleDragonSound != null) {
               return possibleDragonSound;
            }
         }

         return super.getAmbientSound();
      }
   }

   @Override
   protected SoundEvent getDeathSound() {
      if (RatsMod.ICEANDFIRE_LOADED && RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_DRAGON.get())) {
         SoundEvent possibleDragonSound = (SoundEvent)BuiltInRegistries.SOUND_EVENT
            .get(ResourceLocation.fromNamespaceAndPath("iceandfire", "firedragon_child_death"));
         if (possibleDragonSound != null) {
            return possibleDragonSound;
         }
      }

      return super.getDeathSound();
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      if (RatsMod.ICEANDFIRE_LOADED && RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_DRAGON.get())) {
         SoundEvent possibleDragonSound = (SoundEvent)BuiltInRegistries.SOUND_EVENT
            .get(ResourceLocation.fromNamespaceAndPath("iceandfire", "firedragon_child_hurt"));
         if (possibleDragonSound != null) {
            return possibleDragonSound;
         }
      }

      return super.getHurtSound(source);
   }

   public boolean onHearFlute(Player player, RatCommand ratCommand) {
      if (this.isOwnedBy(player) && !this.isBaby() && !RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_NO_FLUTE.get())) {
         this.setCommand(ratCommand);
         return true;
      } else {
         return false;
      }
   }

   public boolean canRatPickupItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         boolean hasBlacklist = RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get());
         boolean hasWhitelist = RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_WHITELIST.get());
         if (!hasBlacklist && !hasWhitelist) {
            return true;
         } else {
            ItemStack filterUpgrade = hasBlacklist
               ? RatUpgradeUtils.getUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get())
               : RatUpgradeUtils.getUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_WHITELIST.get());
            CompoundTag tag = ((CustomData)filterUpgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
            if (!tag.contains("Items", 9)) {
               return hasBlacklist;
            } else {
               ListTag list = tag.getList("Items", 10);
               if (list.isEmpty()) {
                  return hasBlacklist;
               } else {
                  String ourItemID = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(stack.getItem())).toString();

                  for (int i = 0; i < list.size(); i++) {
                     String itemID = list.getCompound(i).getString("id");
                     if (ourItemID.equals(itemID)) {
                        return hasWhitelist;
                     }
                  }

                  return hasBlacklist;
               }
            }
         }
      }
   }

   public boolean isPushable() {
      return !this.isBaby() && !this.isInTube() && super.isPushable();
   }

   public boolean isCurrentlyGlowing() {
      return this.level().isClientSide() && com.github.alexthe666.rats.client.RatsClientHooks.isRatSelectedOnStaff(this) ? true : super.isCurrentlyGlowing();
   }

   public void attemptTeleport(double x, double y, double z) {
      double d0 = this.getX();
      double d1 = this.getY();
      double d2 = this.getZ();
      this.setPos(x, y, z);
      this.level().broadcastEntityEvent(this, (byte)84);
      boolean flag = false;
      BlockPos blockpos = this.blockPosition();
      if (this.level().isLoaded(blockpos)) {
         boolean flag1 = false;

         while (!flag1 && blockpos.getY() > 0) {
            BlockPos blockpos1 = blockpos.below();
            BlockState state = this.level().getBlockState(blockpos1);
            if (state.blocksMotion()) {
               flag1 = true;
            } else {
               this.setPos(this.getX(), this.getY() - 1.0, this.getZ());
               blockpos = blockpos1;
            }
         }

         if (flag1) {
            this.teleportTo(this.getX(), this.getY(), this.getZ());
            if (this.level().noCollision(this) && !this.level().containsAnyLiquid(this.getBoundingBox())) {
               flag = true;
            }
         }
      }

      if (!flag) {
         this.teleportTo(d0, d1, d2);
      } else {
         this.playSound((SoundEvent)RatsSoundRegistry.RAT_TELEPORT.get(), 1.0F, 1.0F);
      }
   }

   public boolean isDirectPathBetweenPoints(Vec3 target) {
      BlockHitResult result = this.level()
         .clip(new ClipContext(this.position(), target, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, this));
      BlockPos pos = result.getBlockPos();
      BlockPos sidePos = result.getBlockPos().relative(result.getDirection());
      return this.level().isEmptyBlock(pos) && this.level().isEmptyBlock(sidePos) ? result.getType() == Type.MISS : true;
   }

   public boolean canBeSeenAsEnemy() {
      return !RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_UNDEAD.get()) && super.canBeSeenAsEnemy();
   }

   public boolean isNoAi() {
      return super.isNoAi() || this.getRespawnCountdown() > 0;
   }

   public boolean holdsItemInHandUpgrade() {
      boolean bool = RatUpgradeUtils.forEachUpgradeBool(this, stack -> {
         if (stack instanceof HoldsItemUpgrade upgrade && !upgrade.isFakeHandRender()) {
            return true;
         }

         return false;
      }, false);
      return !this.isInWheel() && bool;
   }

   @Override
   public boolean shouldPlayIdleAnimations() {
      boolean bool = RatUpgradeUtils.forEachUpgradeBool(this, stack -> stack.playIdleAnimation(this), true);
      return super.shouldPlayIdleAnimations() && !this.isInTube() && !this.isInWheel() && this.cookingProgress <= 0 && bool;
   }

   public boolean hasAnyUpgrades() {
      return this.hasAnyUpgradeSlots;
   }

   @Override
   public boolean canDigThroughBlocks() {
      return false;
   }

   public void onUpgradeChanged() {
      this.rebuildUpgradeCache();
      this.setupDynamicAI();
      Entity vehicle = this.getVehicle();
      if (!this.level().isClientSide() && vehicle instanceof RatMount mount && !RatUpgradeUtils.hasUpgrade(this, mount.getUpgradeItem())) {
         this.level().broadcastEntityEvent(this, (byte)127);
         this.stopRiding();
         vehicle.discard();
      }

      if (!this.attributeChanges.isEmpty()) {
         this.getAttributes().removeAttributeModifiers(this.attributeChanges);
         this.attributeChanges.clear();
      }

      this.applyRatGeneAttributeModifiers();
      RatUpgradeUtils.forEachUpgrade(
         this,
         item -> item instanceof StatBoostingUpgrade,
         (stack, slot) -> ((StatBoostingUpgrade)stack.getItem())
               .getAttributeBoosts()
               .forEach((attribute, aDouble) -> this.tryIncreaseStat(stack.getHoverName().getString(), (Holder<Attribute>)attribute, aDouble))
      );
      if (this.getHeldRF() > this.getRFTransferRate()) {
         this.setHeldRF(this.getRFTransferRate());
      }

      this.heal(this.getMaxHealth());
      this.setFlying(this.isFlying() && this.hasFlightUpgradeCached);
      this.setNoGravity(RatUpgradeUtils.hasUpgrade(this, (Item)RatlantisItemRegistry.RAT_UPGRADE_ETHEREAL.get()) || this.isFlying());
   }

   private void rebuildUpgradeCache() {
      try {
         boolean any = false;

         for (EquipmentSlot slot : RatUpgradeUtils.UPGRADE_SLOTS) {
            if (!this.getItemBySlot(slot).isEmpty()) {
               any = true;
               break;
            }
         }

         this.hasAnyUpgradeSlots = any;
         this.hasQuarryUpgradeForNavigation = any && RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_QUARRY.get());
         this.hasWorkerUpgradeCached = any && RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_WORKER.get());
         this.hasFlightUpgradeCached = any && RatUpgradeUtils.forEachUpgradeBool(this, stack -> stack.canFly(this), false);
      } catch (RuntimeException var6) {
         RatsMod.LOGGER.error("Failed to recompute the upgrade cache of rat {}", this.getUUID(), var6);
         throw var6;
      }
   }

   public void tryIncreaseStat(String itemName, Holder<Attribute> stat, double value) {
      try {
         String statPath = ((ResourceKey)stat.unwrapKey().orElseThrow()).location().getPath();
         String safeName = itemName.replaceAll("[^a-z0-9_./-]", "_").toLowerCase(Locale.ROOT);
         ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath("rats", safeName + "_" + statPath);
         AttributeInstance attributeInstance = this.getAttribute(stat);
         if (attributeInstance == null) {
            RatsMod.LOGGER.error("tryIncreaseStat: rat {} has no attribute {}", this.getUUID(), statPath);
            return;
         }

         AttributeModifier modifier = new AttributeModifier(modifierId, value, Operation.ADD_VALUE);
         attributeInstance.addTransientModifier(modifier);
         this.attributeChanges.put(stat, modifier);
      } catch (Exception var10) {
         RatsMod.LOGGER.error("tryIncreaseStat: failure for upgrade {}: {}", itemName, var10.getMessage(), var10);
      }
   }

   public boolean isInvulnerableTo(DamageSource source) {
      if (this.getRespawnCountdown() > 0) {
         return true;
      } else {
         AtomicBoolean upgradePrevented = new AtomicBoolean(false);
         RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof DamageImmunityUpgrade, (stack, slot) -> {
            if (((DamageImmunityUpgrade)stack.getItem()).isImmuneToDamageSource(this, source)) {
               upgradePrevented.set(true);
            }
         });
         if (RatUpgradeUtils.hasUpgrade(this, (Item)RatsItemRegistry.RAT_UPGRADE_CREATIVE.get())) {
            if (source.getEntity() == null) {
               return true;
            } else {
               if (source.getEntity() instanceof LivingEntity living && !this.isOwnedBy(living)) {
                  return true;
               }

               return false;
            }
         } else {
            return upgradePrevented.get() || super.isInvulnerableTo(source);
         }
      }
   }

   public boolean isInvulnerable() {
      return super.isInvulnerable() || this.getRespawnCountdown() > 0;
   }

   public void setRecordPlayingNearby(BlockPos pos, boolean partying) {
      int moves = this.getRandom().nextInt(4);
      if (!this.isDancing() && partying) {
         this.setDanceMoves(moves);
      }

      this.setDancing(partying);
      this.jukeboxPos = pos;
      if (this.level().isClientSide()) {
         RatsNetworkHandler.CHANNEL.sendToServer(new SetDancingRatPacket(this.getId(), partying, pos.asLong(), moves));
      }
   }

   public boolean shouldDepositItem(ItemStack item) {
      return RatUpgradeUtils.forEachUpgradeBool(this, stack -> stack.shouldDepositItem(this, item), true);
   }

   public boolean shouldCollectItem(ItemStack item) {
      return RatUpgradeUtils.forEachUpgradeBool(this, stack -> stack.shouldCollectItem(this, item), true);
   }

   public int getRFTransferRate() {
      AtomicInteger energy = new AtomicInteger();
      RatUpgradeUtils.forEachUpgrade(
         this, item -> item instanceof EnergyRatUpgradeItem, (stack, slot) -> energy.set(((EnergyRatUpgradeItem)stack.getItem()).getRFTransferRate())
      );
      return energy.get();
   }

   public int getMBTransferRate() {
      AtomicInteger fluid = new AtomicInteger();
      RatUpgradeUtils.forEachUpgrade(
         this, item -> item instanceof BucketRatUpgradeItem, (stack, slot) -> fluid.set(((BucketRatUpgradeItem)stack.getItem()).getMbTransferRate())
      );
      return fluid.get();
   }

   public boolean isAlliedTo(Entity entity) {
      LivingEntity livingentity = this.getOwner();
      if (entity == livingentity) {
         return true;
      } else if (!(entity instanceof RatMountBase mount)) {
         if (livingentity != null && entity instanceof TamableAnimal animal) {
            return animal.isOwnedBy(livingentity);
         } else {
            return livingentity != null ? livingentity.isAlliedTo(entity) : super.isAlliedTo(entity);
         }
      } else {
         return mount.getRat() != null && this.isAlliedTo(mount.getRat());
      }
   }

   @Nullable
   public EntityType<?> getMountEntityType() {
      AtomicReference<EntityType<?>> type = new AtomicReference<>(null);
      RatUpgradeUtils.forEachUpgrade(
         this, item -> item instanceof MountRatUpgradeItem, (stack, slot) -> type.set(((MountRatUpgradeItem)stack.getItem()).getEntityType())
      );
      return type.get();
   }

   public double getRatDistanceCenterSq(double x, double y, double z) {
      double d0 = this.getX() - x - 0.5;
      double d1 = this.getY() - y - 0.5;
      double d2 = this.getZ() - z - 0.5;
      if (this.getVehicle() != null && this.getMountEntityType() != null && this.getVehicle().getType() == this.getMountEntityType()) {
         d0 = this.getVehicle().getX() - x - 0.5;
         d1 = this.getVehicle().getY() - y - 0.5;
         d2 = this.getVehicle().getZ() - z - 0.5;
      }

      return d0 * d0 + d1 * d1 + d2 * d2;
   }

   public double getRatDistanceSq(double x, double y, double z) {
      double d0 = this.getX() - x;
      double d1 = this.getY() - y;
      double d2 = this.getZ() - z;
      if (this.getVehicle() != null && this.getMountEntityType() != null && this.getVehicle().getType() == this.getMountEntityType()) {
         d0 = this.getVehicle().getX() - x;
         d1 = this.getVehicle().getY() - y;
         d2 = this.getVehicle().getZ() - z;
      }

      return d0 * d0 + d1 * d1 + d2 * d2;
   }

   public double distanceToSqr(Vec3 vec) {
      double d0 = this.getX() - vec.x();
      double d1 = this.getY() - vec.y();
      double d2 = this.getZ() - vec.z();
      if (this.getVehicle() != null && this.getMountEntityType() != null && this.getVehicle().getType() == this.getMountEntityType()) {
         d0 = this.getVehicle().getX() - vec.x();
         d1 = this.getVehicle().getY() - vec.y();
         d2 = this.getVehicle().getZ() - vec.z();
      }

      return d0 * d0 + d1 * d1 + d2 * d2;
   }

   public boolean isRidingSpecialMount() {
      boolean ret = false;
      if (this.getVehicle() != null && this.getMountEntityType() != null) {
         ret = this.getVehicle().getType().equals(this.getMountEntityType());
      }

      return ret;
   }

   public boolean canAttack(LivingEntity target) {
      return !this.isRidingSpecialMount() || !RatUtils.isRidingOrBeingRiddenBy(this, target);
   }

   public double getRatHarvestDistance(double expansion) {
      return (3.5 + expansion) * this.getRatDistanceModifier();
   }

   public double getRatDistanceModifier() {
      if (this.isRidingSpecialMount()) {
         Entity entity = this.getVehicle();
         if (entity != null) {
            if (entity instanceof RatBiplaneMount) {
               return 3.95;
            }

            return 1.5;
         }
      }

      return 1.0;
   }

   public boolean hasFlightUpgrade() {
      return this.hasFlightUpgradeCached;
   }

   public boolean hasWorkerUpgrade() {
      return this.hasWorkerUpgradeCached;
   }
}
