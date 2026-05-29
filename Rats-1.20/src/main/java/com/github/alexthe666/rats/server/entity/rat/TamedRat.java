package com.github.alexthe666.rats.server.entity.rat;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.events.ForgeClientEvents;
import com.github.alexthe666.rats.data.tags.RatsItemTags;
import com.github.alexthe666.rats.registry.*;
import com.github.alexthe666.rats.server.block.RatCageBlock;
import com.github.alexthe666.rats.server.block.RatHouseBlock;
import com.github.alexthe666.rats.server.block.RatTubeBlock;
import com.github.alexthe666.rats.server.block.entity.UpgradeCombinerBlockEntity;
import com.github.alexthe666.rats.server.misc.RatTubeTransit;
import com.github.alexthe666.rats.server.misc.RatsItemStackData;
import com.github.alexthe666.rats.server.entity.RatMount;
import com.github.alexthe666.rats.server.entity.ai.goal.*;
import com.github.alexthe666.rats.server.entity.ai.goal.harvest.RatHarvestCropsGoal;
import com.github.alexthe666.rats.server.entity.ai.navigation.control.*;
import com.github.alexthe666.rats.server.entity.ai.navigation.navigation.EtherealRatNavigation;
import com.github.alexthe666.rats.server.entity.ai.navigation.navigation.RatFlightNavigation;
import com.github.alexthe666.rats.server.entity.ai.navigation.navigation.RatNavigation;
import com.github.alexthe666.rats.server.entity.mount.RatBiplaneMount;
import com.github.alexthe666.rats.server.entity.mount.RatMountBase;
import com.github.alexthe666.rats.server.capability.SelectedRat;
import com.github.alexthe666.rats.server.items.OreRatNuggetItem;
import com.github.alexthe666.rats.server.items.RatSackItem;
import com.github.alexthe666.rats.server.items.RatStaffItem;
import com.github.alexthe666.rats.server.items.upgrades.*;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.*;
import com.github.alexthe666.rats.server.message.ManageRatStaffPacket;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import com.github.alexthe666.rats.server.misc.RatPersistenceHelper;
import com.github.alexthe666.rats.server.message.SetDancingRatPacket;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import com.github.alexthe666.rats.server.misc.RatsRemovedContent;
import com.github.alexthe666.rats.server.misc.RatUtils;
import com.github.alexthe666.rats.server.misc.RatVariant;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TamedRat extends InventoryRat {

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(RatsItemTags.CHEESE_ITEMS) || stack.is(RatsItemRegistry.CREATIVE_CHEESE.get());
	}

	private static final EntityDataAccessor<Boolean> TOGA = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> VISUAL_FLAG = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DYED = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Byte> DYE_COLOR = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Boolean> DANCING = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DANCE_MOVES = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> HELD_RF = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> RESPAWN_COUNTDOWN = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Optional<GlobalPos>> PICKUP_POS = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS);
	private static final EntityDataAccessor<Optional<GlobalPos>> DEPOSIT_POS = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS);
	private static final EntityDataAccessor<Optional<GlobalPos>> CREATE_WORK_POS = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS);
	private static final EntityDataAccessor<Optional<GlobalPos>> BOUND_HOUSE_POS = SynchedEntityData.defineId(TamedRat.class, EntityDataSerializers.OPTIONAL_GLOBAL_POS);
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
	private static final float RAT_BREEDING_EXCLUSIVE_COLOR_CHANCE = 1.0F / 6.0F;
	private static final int RAT_CHEESE_RESERVE_SLOT = 2;
	private static final int MAX_RAT_CHEESE_RESERVE = 16;
	private static final int MIN_RAT_CHEESE_RESERVE_BEFORE_REFILL = 2;
	private static final int RAT_BASE_DAILY_CHEESE = 4;
	private static final int RAT_HUNGER_DAY_TICKS = 24000;
	private static final int RAT_CHEESE_SHARE_INTERVAL = 100;
	private static final double RAT_CHEESE_SHARE_RADIUS = 8.0D;

	public Direction depositFacing = Direction.UP;
	public Direction pickupFacing = Direction.UP;
	public Direction createWorkFacing = Direction.UP;
	public boolean crafting = false;
	public boolean climbingTube = false;
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
	/*
	   0 = tamed navigator
	   1 = flight navigator
	   2 = tube navigator
	   3 = aquatic navigator
	   4 = ethereal navigator
	   5 = cage navigator
	 */
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

	// Планирует пересчёт улучшений после загрузки сущности в мир (только с сервера).
	public void scheduleUpgradeRefreshAfterLoad() {
		this.pendingUpgradeRefresh = true;
	}

	public TamedRat(EntityType<? extends TamableAnimal> type, Level level) {
		super(type, level);
		// Дешёвый проход по RAIL: сегменты трубы (OPEN в ванили уводит узел вниз под трубу).
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

	// Создаёт базовые атрибуты приручённой крысы; параметров нет, отдаёт строитель атрибутов для регистрации сущности.
	public static AttributeSupplier.Builder createAttributes() {
		try {
			return Mob.createMobAttributes()
					.add(Attributes.MAX_HEALTH, 8.0D)
					.add(Attributes.MOVEMENT_SPEED, 0.35D)
					.add(Attributes.FLYING_SPEED, 0.35D)
					.add(Attributes.ATTACK_DAMAGE, 2.0D)
					.add(Attributes.ARMOR, 1.0D)
					.add(Attributes.FOLLOW_RANGE, 12.0D);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось создать базовые атрибуты приручённой крысы", exception);
			throw exception;
		}
	}

	// Создаёт начальные гены при природном появлении приручённой крысы; принимает мир, сложность, тип появления и старые данные спавна, отдаёт данные спавна.
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor accessor, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
		try {
			data = super.finalizeSpawn(accessor, difficulty, type, data);
			this.initializeRandomRatGenes("spawn");
			return data;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось создать гены при появлении крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	@Override
	protected void registerGoals() {
		this.harvestGoal = new RatHarvestCropsGoal(this);
		this.pickupGoal = new RatPickupGoal(this, RatPickupGoal.PickupType.INVENTORY);
		this.depositGoal = new RatDepositGoal(this, RatDepositGoal.DepositType.INVENTORY);
		this.attackGoal = new RatMeleeAttackGoal(this, 1.3D, true);
		this.targetGoal = new RatHuntGoal(this, true, new ArrayList<>());
		this.goalSelector.addGoal(0, new RatFloatGoal(this));
		this.goalSelector.addGoal(1, this.attackGoal);
		this.goalSelector.addGoal(2, new RatFollowOwnerGoal(this, 1.25D, 10.0F, 3.0F));
		this.goalSelector.addGoal(2, new RatReturnToHouseGoal(this, 1.15D));
		this.goalSelector.addGoal(2, new RatStayCloseToHomeGoal(this, 1.1D));
		this.goalSelector.addGoal(3, new RatSeekCheeseGoal(this));
		this.goalSelector.addGoal(4, new CreateWorkerGoal(this));
		this.goalSelector.addGoal(4, new FarmersDelightChefGoal(this));
		this.goalSelector.addGoal(4, this.harvestGoal);
		this.goalSelector.addGoal(5, this.depositGoal);
		this.goalSelector.addGoal(6, this.pickupGoal);
		this.goalSelector.addGoal(6, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(7, new RatWanderGoal(this, 1.25D));
		this.goalSelector.addGoal(7, new RatPatrolGoal(this));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, LivingEntity.class, 6.0F) {
			@Override
			public boolean canUse() {
				return TamedRat.this.sleepProgress <= 0.0F && super.canUse();
			}
		});
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this) {
			@Override
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
		AtomicReference<Goal> newAttack = new AtomicReference<>(new RatMeleeAttackGoal(this, 1.45D, true));

		RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof ChangesAIUpgrade, (stack, slot) ->
				((ChangesAIUpgrade) stack.getItem()).addNewWorkGoals(this).forEach(goal -> {
					if (!(goal instanceof RatWorkGoal workGoal)) {
						throw new IllegalArgumentException("Rat Goals must implement the interface RatWorkGoal! Goal" + goal.getClass().getName() + "doesnt do this!");
					}
					switch (workGoal.getRatTaskType()) {
						case ATTACK -> newAttack.set(goal);
						case DEPOSIT -> newDeposit.set(goal);
						case PICKUP -> newPickup.set(goal);
						case HARVEST -> newHarvest.set(goal);
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
			ItemStack filterUpgrade = RatUpgradeUtils.getUpgrade(this, RatsItemRegistry.RAT_UPGRADE_MOB_FILTER.get());

			if (!filterUpgrade.isEmpty()) {
				this.targetGoal = new RatHuntGoal(this, MobFilterUpgradeItem.isWhitelist(filterUpgrade), MobFilterUpgradeItem.getSelectedMobs(filterUpgrade));
			} else {
				this.targetGoal = new RatHuntGoal(this, true, new ArrayList<>());
			}

			this.targetSelector.addGoal(1, this.targetGoal);

			if (this.hasFlightUpgrade()) {
				this.switchNavigator(1);
			} else if (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_AQUATIC.get())) {
				this.switchNavigator(3);
			} else if (RatUpgradeUtils.hasUpgrade(this, RatlantisItemRegistry.RAT_UPGRADE_ETHEREAL.get())) {
				this.switchNavigator(4);
			} else {
				this.switchNavigator(this.isInCage() ? 5 : this.isInTube() ? 2 : 0);
			}
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TOGA, false);
		builder.define(DANCING, false);
		builder.define(DANCE_MOVES, 0);
		builder.define(HELD_RF, 0);
		builder.define(RESPAWN_COUNTDOWN, 0);
		builder.define(VISUAL_FLAG, false);
		builder.define(DYED, false);
		builder.define(FLYING, false);
		builder.define(DYE_COLOR, (byte) 0);
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
		if (type == 0) { //tamed
			this.moveControl = new RatMoveControl(this);
			this.navigation = new RatNavigation(this, this.level());
			this.navigatorType = 0;
		} else if (type == 1) { //flying
			this.moveControl = new RatFlightMoveControl(this, 1.0F);
			this.navigation = new RatFlightNavigation(this, this.level());
			this.navigatorType = 1;
		} else if (type == 2) { //tube
			this.moveControl = new RatTubeMoveControl(this);
			this.navigation = new RatNavigation(this, this.level());
			this.navigatorType = 2;
		} else if (type == 3) { //aquatic
			this.moveControl = new SmoothSwimmingMoveControl(this, 360, 360, 10.0F, 1.0F, true);
			this.navigation = new AmphibiousPathNavigation(this, this.level());
			this.navigatorType = 3;
		} else if (type == 4) { //ethereal
			this.moveControl = new EtherealRatMoveControl(this);
			this.navigation = new EtherealRatNavigation(this, this.level());
			this.navigatorType = 4;
		} else if (type == 5) { //cage
			this.moveControl = new RatCageMoveControl(this);
			this.navigation = new RatNavigation(this, this.level());
			this.navigatorType = 5;
		}
	}

	@Override
	public boolean isHoldingFood() {
		if (!this.getCommand().allowsEating && !RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_ORE_DOUBLING.get()))
			return false;
		return !this.getMainHandItem().isEmpty() && RatUpgradeUtils.forEachUpgradeBool(this, (stack) -> stack.isRatHoldingFood(this), RatUtils.isRatFood(this.getMainHandItem()));
	}

	@Override
	public boolean isVisuallySitting() {
		return super.isVisuallySitting() || this.isDancing();
	}

	@Override
	public boolean isHoldingItemInHands() {
		return (this.isHoldingFood() || (!this.getMainHandItem().isEmpty() && this.cookingProgress > 0) || this.holdsItemInHandUpgrade() || this.getMBTransferRate() > 0) && this.sleepProgress <= 0.0F;
	}

	@Override
	public boolean doHurtTarget(Entity entity) {
		try {
			if (this.getVehicle() instanceof LivingEntity living && this.isRidingSpecialMount()) {
				return living.doHurtTarget(entity);
			}
			float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
			boolean flag = entity.hurt(this.damageSources().mobAttack(this), damage);
			if (flag) {
				this.getMainHandItem().hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
				if (entity instanceof LivingEntity) {
					RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof PostAttackUpgrade, (stack, slot) -> ((PostAttackUpgrade) stack.getItem()).afterHit(this, (LivingEntity) entity));
				}
			}
			return flag;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла ударить цель {}", this.getUUID(), entity.getUUID(), exception);
			throw exception;
		}
	}

	public boolean isInCage() {
		return this.level().getBlockState(this.blockPosition()).getBlock() instanceof RatCageBlock;
	}

	public boolean isInTube() {
		return this.level().getBlockState(this.blockPosition()).getBlock() instanceof RatTubeBlock;
	}

	@Override
	public boolean onClimbable() {
		return this.isInTube() ? this.climbingTube : this.isClimbingBoundHouse() || super.onClimbable();
	}

	// Проверяет лазание по бокам привязанного домика; параметров нет, отдаёт true если крыса у стенки своего дома.
	private boolean isClimbingBoundHouse() {
		try {
			Optional<GlobalPos> house = this.getBoundHousePos();
			if (house.isEmpty() || !house.get().dimension().equals(this.level().dimension())) {
				return false;
			}
			BlockPos housePos = house.get().pos();
			if (!(this.level().getBlockState(housePos).getBlock() instanceof RatHouseBlock)) {
				return false;
			}
			double dx = Math.abs(this.getX() - (housePos.getX() + 0.5D));
			double dz = Math.abs(this.getZ() - (housePos.getZ() + 0.5D));
			boolean nearSide = dx <= 1.15D && dz <= 1.15D && (dx > 0.55D || dz > 0.55D);
			boolean nearHeight = this.getY() >= housePos.getY() - 0.25D && this.getY() <= housePos.getY() + 1.8D;
			return nearSide && nearHeight;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить лазание крысы {} по домику", this.getUUID(), exception);
			throw exception;
		}
	}

	// Запоминает направление вертикального движения в трубе; принимает -1, 0 или 1, ничего не возвращает.
	public void setTubeClimbDirection(int direction) {
		this.tubeClimbDirection = direction;
	}

	// Возвращает направление вертикального движения в трубе: -1 вниз, 0 горизонтально, 1 вверх.
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

	// Сбрасывает активный транзит по трубе; ничего не принимает.
	public void clearTubeTransit() {
		this.tubeTransit = null;
	}

	public boolean isTubeTransitActive() {
		return this.tubeTransit != null && this.tubeTransit.isActive();
	}

	// Тик сегмента трубы под крысой; принимает позицию и состояние блока, ничего не возвращает.
	public void onTubeSegmentTick(BlockPos pos, BlockState state) {
		try {
			if (!this.isInTube()) {
				return;
			}
			this.fallDistance = 0.0F;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка тика крысы {} в трубе {}: {}", this.getUUID(), pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	@Override
	public boolean canCollideWith(Entity entity) {
		if (this.isInTube() && entity instanceof TamedRat other && other.isInTube()) {
			return false;
		}
		return super.canCollideWith(entity);
	}

	@Override
	public int getArmorValue() {
		return super.getArmorValue() * RatConfig.ratArmorMultiplier;
	}

	@Override
	public boolean removeWhenFarAway(double dist) {
		return false;
	}

	@Override
	protected MovementEmission getMovementEmission() {
		return RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_SCULKED.get()) ? MovementEmission.SOUNDS : MovementEmission.ALL;
	}

	@Override
	public boolean canDrownInFluidType(FluidType type) {
		return type == NeoForgeMod.WATER_TYPE.value() && (!RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_AQUATIC.get()) || !RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_UNDERWATER.get()));
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
		tag.putByte("DyeColor", (byte) this.getDyeColor());
		tag.putString("SpecialDye", this.getSpecialDye());
		this.saveRatGenes(tag);
		this.getDepositPos().flatMap(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error)).ifPresent(tag1 -> tag.put("DepositPos", tag1));
		this.getPickupPos().flatMap(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error)).ifPresent(tag1 -> tag.put("PickupPos", tag1));
		this.getCreateWorkPos().flatMap(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error)).ifPresent(tag1 -> tag.put("CreateWorkPos", tag1));
		this.getBoundHousePos().flatMap(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(RatsMod.LOGGER::error)).ifPresent(tag1 -> tag.put("BoundHousePos", tag1));
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
		super.readAdditionalSaveData(tag);
		if (tag.contains(SAVED_UPGRADES_TAG, Tag.TAG_LIST)) {
			this.pendingUpgradeTags = tag.getList(SAVED_UPGRADES_TAG, Tag.TAG_COMPOUND).copy();
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
		this.setDyeColor((tag.getByte("DyeColor")));
		this.setSpecialDye(tag.getString("SpecialDye"));
		this.loadRatGenes(tag);
		if (tag.contains("DepositPos")) {
			this.setDepositPos(GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("DepositPos")).resultOrPartial(RatsMod.LOGGER::error).orElse(null));
		}
		if (tag.contains("PickupPos")) {
			this.setPickupPos(GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("PickupPos")).resultOrPartial(RatsMod.LOGGER::error).orElse(null));
		}
		if (tag.contains("CreateWorkPos")) {
			this.setCreateWorkPos(GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("CreateWorkPos")).resultOrPartial(RatsMod.LOGGER::error).orElse(null));
		}
		if (tag.contains("BoundHousePos")) {
			this.setBoundHousePos(GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("BoundHousePos")).resultOrPartial(RatsMod.LOGGER::error).orElse(null));
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

	// Возвращает true, если ещё не применён отложенный список улучшений.
	public boolean hasPendingUpgradeTags() {
		return this.pendingUpgradeTags != null;
	}

	// Перезагружает RatUpgradeSlots, если мир доступен.
	public void tryReloadPendingUpgradeTags() {
		if (!this.level().isClientSide()) {
			this.loadPendingUpgradeTags();
		}
	}

	// Загружает резервную копию улучшений после появления крысы в мире.
	private void loadPendingUpgradeTags() {
		try {
			if (this.pendingUpgradeTags == null) {
				return;
			}
			CompoundTag wrapper = new CompoundTag();
			wrapper.put(SAVED_UPGRADES_TAG, this.pendingUpgradeTags);
			this.loadUpgradeSlots(wrapper);
			this.pendingUpgradeTags = null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось загрузить отложенные улучшения крысы {} при входе в мир", this.getUUID(), exception);
			throw exception;
		}
	}

	// Сохраняет слоты улучшений отдельным устойчивым списком; принимает NBT сущности, ничего не возвращает.
	private void saveUpgradeSlots(CompoundTag tag) {
		try {
			ListTag upgrades = new ListTag();
			int[] upgradeSlots = new int[]{UPGRADE_SLOT_CHEST, UPGRADE_SLOT_LEGS, UPGRADE_SLOT_FEET};
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
			tag.put(SAVED_UPGRADES_TAG, upgrades);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось сохранить улучшения крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Восстанавливает слоты улучшений из отдельного списка; принимает NBT сущности, ничего не возвращает.
	private void loadUpgradeSlots(CompoundTag tag) {
		try {
			if (!tag.contains(SAVED_UPGRADES_TAG, Tag.TAG_LIST) || this.getInventory() == null) {
				return;
			}
			if (this.level() == null) {
				return;
			}
			var provider = this.level().registryAccess();
			ListTag upgrades = tag.getList(SAVED_UPGRADES_TAG, Tag.TAG_COMPOUND);
			for (int i = 0; i < upgrades.size(); i++) {
				CompoundTag stackTag = upgrades.getCompound(i);
				int slot = stackTag.contains("InventorySlot") ? stackTag.getInt("InventorySlot") : this.mapLegacyUpgradeSlot(stackTag.getInt("EquipmentSlot"));
				if (slot < 3 || slot > 5) {
					continue;
				}
				ItemStack stack = ItemStack.parse(provider, stackTag).orElse(ItemStack.EMPTY);
				if (stack.isEmpty()) {
					stack = RatsItemStackData.parse(provider, stackTag);
				}
				if (!stack.isEmpty()) {
					this.getInventory().setItem(slot, stack);
				}
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось восстановить улучшения крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Переводит старый EquipmentSlot в индекс слота инвентаря крысы.
	private int mapLegacyUpgradeSlot(int equipmentSlotOrdinal) {
		EquipmentSlot slot = EquipmentSlot.values()[Mth.clamp(equipmentSlotOrdinal, 0, EquipmentSlot.values().length - 1)];
		return switch (slot) {
			case CHEST -> UPGRADE_SLOT_CHEST;
			case LEGS -> UPGRADE_SLOT_LEGS;
			case FEET -> UPGRADE_SLOT_FEET;
			default -> -1;
		};
	}

	// Читает сохранённое направление из NBT; принимает тег, имя поля и запасное направление, отдаёт валидный Direction.
	private Direction readSavedDirection(CompoundTag tag, String key, Direction fallback) {
		try {
			int ordinal = tag.getInt(key);
			Direction[] directions = Direction.values();
			if (ordinal < 0 || ordinal >= directions.length) {
				RatsMod.LOGGER.error("Крыса {} получила битое направление {}={} в NBT, применяю {}", this.getUUID(), key, ordinal, fallback);
				return fallback;
			}
			return directions[ordinal];
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось прочитать направление {} крысы {}", key, this.getUUID(), exception);
			throw exception;
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
				double d1 = this.position().y() + 0.25D;
				double d2 = this.position().z();
				double d3 = ((double) this.getRandom().nextFloat() - 0.5D) * 0.15D;
				double d4 = ((double) this.getRandom().nextFloat() - 0.5D) * 0.15D;
				double d5 = ((double) this.getRandom().nextFloat() - 0.5D) * 0.15D;
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

			this.level().broadcastEntityEvent(this, (byte) 127);
			this.startRiding(entity, true);
		}

		if (this.hasAnyUpgrades()) {
			RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof TickRatUpgrade, (stack, slot) -> ((TickRatUpgrade) stack.getItem()).tick(this));
		}

		if (RatConfig.upgradeRegenRate > 0 && this.hasAnyUpgrades()) {
			RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof StatBoostingUpgrade, (stack, slot) -> {
				if (((StatBoostingUpgrade) stack.getItem()).regeneratesHealth() && this.getHealth() < this.getMaxHealth() && this.tickCount % RatConfig.upgradeRegenRate == 0) {
					float hungerEfficiency = this.getRatHungerEfficiency();
					if (hungerEfficiency > 0.0F) {
						this.heal(this.getRatRegenGene() * hungerEfficiency);
					}
				}
			});
		}

		if (this.isTubeTransitActive()) {
			// Транзит сам решает, когда обычная навигация должна смениться движением внутри трубы.
		} else if (this.isInTube() && !this.isInCage() && this.navigatorType != 2) {
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

		this.setNoGravity(this.isFlying());
		if (this.isFlying()) {
			if (this.isOrderedToSit() || this.verticalCollisionBelow || this.onGround()) {
				this.setFlying(false);
			}
			if (Math.abs(this.getDeltaMovement().x()) < 0.01D && Math.abs(this.getDeltaMovement().z()) < 0.01D) {
				if (Math.abs(this.getDeltaMovement().y()) > 0.0D) {
					this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.1D, 1.0D));
				}
			}
		}

		if (this.isInWheel() && !this.level().getBlockState(this.blockPosition()).is(RatsBlockRegistry.RAT_CAGE_WHEEL.get())) {
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
		if (this.isDancing() && (this.jukeboxPos == null || this.jukeboxPos.distToCenterSqr(this.getX(), this.getY(), this.getZ()) > 256.0D || !this.level().getBlockState(this.jukeboxPos).is(Blocks.JUKEBOX))) {
			this.setDancing(false);
		}
		this.handleRatQuarryPlatformLift();
	}

	// Поднимает крысу через карьерные платформы; не мешает пустой карьерной крысе спускаться по столбу.
	private void handleRatQuarryPlatformLift() {
		try {
			if (this.level().isClientSide()) {
				return;
			}
			boolean quarryRatGoingDown = RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_QUARRY.get()) && this.getMainHandItem().isEmpty();
			if (quarryRatGoingDown) {
				return;
			}
			BlockPos current = this.blockPosition();
			BlockState currentState = this.level().getBlockState(current);
			BlockState aboveState = this.level().getBlockState(current.above());
			if (currentState.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) && (aboveState.isAir() || aboveState.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()))) {
				this.setPos(this.getX(), this.getY() + 1, this.getZ());
				this.getNavigation().stop();
				return;
			}
			BlockState aboveTwoState = this.level().getBlockState(current.above(2));
			if (this.jumping && aboveState.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()) && (aboveTwoState.isAir() || aboveTwoState.is(RatsBlockRegistry.RAT_QUARRY_PLATFORM.get()))) {
				this.setPos(this.getX(), this.getY() + 1, this.getZ());
				this.getNavigation().stop();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось обработать подъём крысы {} через платформу карьера", this.getUUID(), exception);
			throw exception;
		}
	}

	@Override
	public boolean fireImmune() {
		return this.getRespawnCountdown() > 0 || super.fireImmune();
	}

	@Override
	public boolean isPushedByFluid(FluidType type) {
		return !RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_QUARRY.get()) && !RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_AQUATIC.get()) && super.isPushedByFluid(type);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (this.isBaby() || this.isInvulnerableTo(source) || (source.is(DamageTypes.IN_WALL) && this.isPassenger())) {
			return false;
		} else {
			Entity entity = source.getEntity();

			if (entity != null && !(entity instanceof Player) && !(entity instanceof AbstractArrow)) {
				amount = (amount + 1.0F) / 2.0F;
			}

			boolean flag = super.hurt(source, amount);

			if (flag && this.getVehicle() != null && this.isRidingSpecialMount()) {
				this.getVehicle().hurt(source, amount);
				this.invulnerableTime = 20;
				return false;
			}

			return flag;
		}
	}

	@Override
	public ItemStack getPickedResult(HitResult target) {
		return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "rat_spawn_egg")));
	}

	public void setFlying(boolean flying) {
		this.getEntityData().set(FLYING, flying);
	}

	public boolean isFlying() {
		return this.getEntityData().get(FLYING);
	}

	public void setToga(boolean toga) {
		this.getEntityData().set(TOGA, toga);
	}

	public boolean hasToga() {
		return this.getEntityData().get(TOGA);
	}

	public boolean getVisualFlag() {
		return this.getEntityData().get(VISUAL_FLAG);
	}

	public void setVisualFlag(boolean flag) {
		this.getEntityData().set(VISUAL_FLAG, flag);
	}

	public boolean isDancing() {
		return this.getEntityData().get(DANCING);
	}

	public void setDancing(boolean dancing) {
		this.getEntityData().set(DANCING, dancing);
	}

	public int getDanceMoves() {
		return this.getEntityData().get(DANCE_MOVES);
	}

	public void setDanceMoves(int moves) {
		this.getEntityData().set(DANCE_MOVES, moves);
	}

	public int getHeldRF() {
		return this.getEntityData().get(HELD_RF);
	}

	public void setHeldRF(int rf) {
		this.getEntityData().set(HELD_RF, rf);
	}

	public int getRespawnCountdown() {
		return this.getEntityData().get(RESPAWN_COUNTDOWN);
	}

	public void setRespawnCountdown(int respawn) {
		this.getEntityData().set(RESPAWN_COUNTDOWN, respawn);
	}

	public boolean isInWheel() {
		return this.getEntityData().get(IS_IN_WHEEL);
	}

	public void setInWheel(boolean wheel) {
		this.getEntityData().set(IS_IN_WHEEL, wheel);
	}

	public boolean isDyed() {
		return this.getEntityData().get(DYED);
	}

	public void setDyed(boolean dyed) {
		this.getEntityData().set(DYED, dyed);
	}

	public int getDyeColor() {
		return this.getEntityData().get(DYE_COLOR);
	}

	public void setDyeColor(int color) {
		this.getEntityData().set(DYE_COLOR, (byte) (color));
	}

	public String getSpecialDye() {
		return this.getEntityData().get(SPECIAL_DYE);
	}

	public void setSpecialDye(String keyword) {
		this.getEntityData().set(SPECIAL_DYE, keyword);
	}


	public Optional<GlobalPos> getPickupPos() {
		return this.getEntityData().get(PICKUP_POS);
	}

	public void setPickupPos(@Nullable GlobalPos pos) {
		this.getEntityData().set(PICKUP_POS, Optional.ofNullable(pos));
	}

	public Optional<GlobalPos> getDepositPos() {
		return this.getEntityData().get(DEPOSIT_POS);
	}

	public void setDepositPos(@Nullable GlobalPos pos) {
		this.getEntityData().set(DEPOSIT_POS, Optional.ofNullable(pos));
	}



	// Проверяет, разрешена ли рабочая активность по распорядку; параметров нет, отдаёт true вне окна сна.
	public boolean canWorkBySchedule() {
		try {
			return RatDaySchedule.canWork(this.level());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить распорядок работы крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Запоминает последнюю причину сбоя пути; принимает текст причины, ничего не отдаёт.
	public void setLastPathFailure(String reason) {
		try {
			this.getEntityData().set(LAST_PATH_FAILURE, reason == null ? "" : reason);
			if (reason != null && !reason.isBlank()) {
				RatsMod.LOGGER.debug("Крыса {} не построила путь: {}", this.getUUID(), reason);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось записать сбой пути крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает последнюю причину сбоя пути; параметров нет, отдаёт строку или пустую строку.
	public String getLastPathFailure() {
		try {
			return this.getEntityData().get(LAST_PATH_FAILURE);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить сбой пути крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Очищает последнюю причину сбоя пути; параметров нет, ничего не отдаёт.
	public void clearLastPathFailure() {
		this.setLastPathFailure("");
	}

	// Возвращает точку применения Create; параметров нет, отдаёт сохранённую глобальную позицию, если она отмечена.
	public Optional<GlobalPos> getCreateWorkPos() {
		try {
			return this.getEntityData().get(CREATE_WORK_POS);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить точку применения Create для крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Задаёт точку применения Create; принимает глобальную позицию или null, ничего не отдаёт.
	public void setCreateWorkPos(@Nullable GlobalPos pos) {
		try {
			this.getEntityData().set(CREATE_WORK_POS, Optional.ofNullable(pos));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось задать точку применения Create для крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает привязанный домик крысы; параметров нет, отдаёт глобальную позицию домика, если она есть.
	public Optional<GlobalPos> getBoundHousePos() {
		try {
			return this.getEntityData().get(BOUND_HOUSE_POS);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить привязанный домик крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Задаёт привязанный домик крысы; принимает глобальную позицию или null, ничего не отдаёт.
	public void setBoundHousePos(@Nullable GlobalPos pos) {
		try {
			this.getEntityData().set(BOUND_HOUSE_POS, Optional.ofNullable(pos));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось задать привязанный домик крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	public int getMountCooldown() {
		return this.getEntityData().get(MOUNT_RESPAWN_COOLDOWN);
	}

	public void setMountCooldown(int cooldown) {
		this.getEntityData().set(MOUNT_RESPAWN_COOLDOWN, cooldown);
	}

	@Override
	public void onItemEaten() {
		ItemStack handCopy = this.getMainHandItem().copy();
		if (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_ORE_DOUBLING.get()) && OreDoublingRatUpgradeItem.isProcessable(this.level(), handCopy)) {
			ItemStack attemptedSmelt = handCopy.copy();
			var recipeHolder = this.level().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(attemptedSmelt), this.level());
			if (recipeHolder.isPresent() && !recipeHolder.get().value().getResultItem(this.level().registryAccess()).isEmpty()) {
				attemptedSmelt = recipeHolder.get().value().getResultItem(this.level().registryAccess()).copy();
			}

			if (!attemptedSmelt.is(handCopy.getItem())) {
				ItemStack nugget = OreRatNuggetItem.saveResourceToNugget(attemptedSmelt, this.level().registryAccess()).copyWithCount(2);
				if (RatConfig.ratFartNoises) {
					this.playSound(RatsSoundRegistry.RAT_POOP.get(), 0.5F + this.getRandom().nextFloat() * 0.5F, 1.0F + this.getRandom().nextFloat() * 0.5F);
				}
				if (!this.level().isClientSide()) {
					this.spawnAtLocation(nugget, 0.0F);
				}
				this.getMainHandItem().shrink(1);
			}
		} else if (this.getRandom().nextFloat() <= 0.05F) {
			ItemStack nugget = new ItemStack(RatsItemRegistry.RAT_NUGGET.get());
			if (RatConfig.ratFartNoises) {
				this.playSound(RatsSoundRegistry.RAT_POOP.get(), 0.5F + this.getRandom().nextFloat() * 0.5F, 1.0F + this.getRandom().nextFloat() * 0.5F);
			}
			if (!this.level().isClientSide()) {
				this.spawnAtLocation(nugget, 0.0F);
			}
		} else {
			this.getMainHandItem().shrink(1);
		}
	}

	public void createBabiesFrom(TamedRat mother, TamedRat father) {
		// Создаёт детёныша от двух родителей; принимает мать и отца, добавляет нового детёныша в мир.
		TamedRat baby = new TamedRat(RatsEntityRegistry.TAMED_RAT.get(), this.level());
		baby.setMale(this.getRandom().nextBoolean());
		baby.setColorVariant(this.chooseBabyColorVariant(mother, father));
		baby.setPos(mother.getX() - 0.5F + mother.getRandom().nextFloat(), mother.getY(), mother.getZ() - 0.5F + mother.getRandom().nextFloat());
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

	// Выбирает окрас детёныша; принимает родителей, отдаёт родительский окрас или редкую мутацию.
	private RatVariant chooseBabyColorVariant(TamedRat mother, TamedRat father) {
		try {
			RatVariant motherVariant = mother.getColorVariant();
			RatVariant fatherVariant = father.getColorVariant();
			boolean hasExclusiveParent = motherVariant.isBreedingExclusive() || fatherVariant.isBreedingExclusive();
			if (hasExclusiveParent && this.getRandom().nextFloat() <= RAT_BREEDING_EXCLUSIVE_COLOR_CHANCE) {
				RatVariant variant = this.getRandomDifferentColorVariant(true, motherVariant, fatherVariant);
				RatsMod.LOGGER.info("Крыса получила редкую мутацию окраса потомства: {}", RatVariant.getVariantId(variant));
				return variant;
			}
			if (this.getRandom().nextFloat() <= RAT_COLOR_MUTATION_CHANCE) {
				RatVariant variant = this.getRandomDifferentColorVariant(false, motherVariant, fatherVariant);
				RatsMod.LOGGER.info("Крыса получила мутацию обычного окраса потомства: {}", RatVariant.getVariantId(variant));
				return variant;
			}
			return this.getRandom().nextBoolean() ? fatherVariant : motherVariant;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выбрать окрас детёныша от родителей {} и {}", mother.getUUID(), father.getUUID(), exception);
			throw exception;
		}
	}

	// Выбирает случайный окрас, отличающийся от родителей; принимает тип окраса и родительские варианты, отдаёт найденный вариант.
	private RatVariant getRandomDifferentColorVariant(boolean breedingExclusive, RatVariant motherVariant, RatVariant fatherVariant) {
		try {
			List<RatVariant> variants = new ArrayList<>(RatVariantRegistry.RAT_VARIANT_REGISTRY.stream().toList());
			variants.removeIf(variant -> variant.isBreedingExclusive() != breedingExclusive || variant == motherVariant || variant == fatherVariant);
			if (variants.isEmpty()) {
				throw new IllegalStateException("Нет доступных вариантов окраса для мутации, breedingExclusive=" + breedingExclusive);
			}
			return variants.get(this.getRandom().nextInt(variants.size()));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выбрать случайный окрас мутации для крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает множитель размера модели из генетики здоровья; ничего не принимает, отдаёт 1 для крыс без инициализированных генов.
	public float getRatModelScaleFromGenes() {
		try {
			if (!this.getEntityData().get(RAT_GENES_INITIALIZED)) {
				return 1.0F;
			}
			return this.calculateRatModelScale(this.getEntityData().get(GENE_HEALTH));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить размер модели по генам крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает генетический множитель регенерации; ничего не принимает, отдаёт множитель лечения.
	private float getRatRegenGene() {
		try {
			return this.getRatGene(GENE_REGEN);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить ген регенерации крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает генетический множитель скорости; параметров нет, отдаёт множитель 50-250%.
	public float getRatSpeedGeneForDisplay() {
		try {
			return this.getRatGene(GENE_SPEED);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить ген скорости крысы {} для GUI", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает генетический множитель здоровья; параметров нет, отдаёт множитель 50-250%.
	public float getRatHealthGeneForDisplay() {
		try {
			return this.getRatGene(GENE_HEALTH);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить ген здоровья крысы {} для GUI", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает генетический множитель брони; параметров нет, отдаёт множитель 50-250%.
	public float getRatArmorGeneForDisplay() {
		try {
			return this.getRatGene(GENE_ARMOR);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить ген брони крысы {} для GUI", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает генетический множитель урона; параметров нет, отдаёт множитель 50-250%.
	public float getRatDamageGeneForDisplay() {
		try {
			return this.getRatGene(GENE_DAMAGE);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить ген урона крысы {} для GUI", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает генетический множитель регенерации; параметров нет, отдаёт множитель 50-250%.
	public float getRatRegenGeneForDisplay() {
		try {
			return this.getRatGene(GENE_REGEN);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить ген регенерации крысы {} для GUI", this.getUUID(), exception);
			throw exception;
		}
	}

	// Назначает видимое имя крысы; принимает строку имени, ничего не отдаёт.
	public void setRatGivenName(String name) {
		try {
			String trimmedName = name == null ? "" : name.trim();
			if (trimmedName.isBlank()) {
				this.setCustomName(null);
				this.setCustomNameVisible(false);
				return;
			}
			if (trimmedName.length() > 48) {
				throw new IllegalArgumentException("Имя крысы длиннее 48 символов: " + trimmedName.length());
			}
			this.setCustomName(Component.literal(trimmedName));
			this.setCustomNameVisible(true);
			RatsMod.LOGGER.info("Крысе {} назначено имя '{}'", this.getUUID(), trimmedName);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось назначить имя крысе {}: {}", this.getUUID(), name, exception);
			throw exception;
		}
	}

	// Проверяет, является ли стек сыром для личного запаса крысы; принимает стек, отдаёт true для допустимого сыра.
	public static boolean isCheeseReserveItem(ItemStack stack) {
		try {
			return !stack.isEmpty() && (stack.is(RatsItemTags.CHEESE_ITEMS) || stack.is(RatsItemRegistry.CREATIVE_CHEESE.get()));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить сырный стек крысы {}", stack, exception);
			throw exception;
		}
	}

	// Тикает голод крысы; ничего не принимает и не отдаёт.
	private void tickRatHunger() {
		try {
			if (Math.floorMod(this.tickCount + this.getId(), 20) != 0) {
				return;
			}
			this.resetRatHungerDayIfNeeded();
			if (this.needsCheeseNow()) {
				this.consumeCheeseFromReserve("личный запас");
			}
			if (Math.floorMod(this.tickCount + this.getId(), RAT_CHEESE_SHARE_INTERVAL) == 0) {
				this.shareCheeseWithNearbyRats();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось обработать голод крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает дневной номер голода; ничего не принимает, отдаёт номер суток по игровому времени.
	private long getRatHungerDay() {
		try {
			return this.level().getGameTime() / RAT_HUNGER_DAY_TICKS;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить день голода крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Сбрасывает суточный счётчик сыра при смене дня; ничего не принимает и не отдаёт.
	private void resetRatHungerDayIfNeeded() {
		try {
			long currentDay = this.getRatHungerDay();
			if (this.level().isClientSide()) {
				this.cheeseEatenToday = this.getEntityData().get(CHEESE_EATEN_TODAY_SYNC);
				this.lastFeedDay = this.getEntityData().get(LAST_FEED_DAY_SYNC);
				return;
			}
			if (this.lastFeedDay != currentDay) {
				this.cheeseEatenToday = 0;
				this.lastFeedDay = currentDay;
				this.cachedHungerEfficiencyTick = this.tickCount;
				this.cachedHungerEfficiency = 0.0F;
				this.syncRatHungerData();
				RatsMod.LOGGER.debug("Крыса {} начала новый день голода {}", this.getUUID(), currentDay);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось сбросить день голода крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Синхронизирует суточный голод крысы с клиентом; параметров нет, ничего не отдаёт.
	private void syncRatHungerData() {
		try {
			this.getEntityData().set(CHEESE_EATEN_TODAY_SYNC, this.cheeseEatenToday);
			this.getEntityData().set(LAST_FEED_DAY_SYNC, Mth.clamp((int) this.lastFeedDay, -1, Integer.MAX_VALUE));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось синхронизировать голод крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает суточную норму сыра; ничего не принимает, отдаёт количество кусков сыра за сутки.
	public int getDailyCheeseRequirement() {
		try {
			return RAT_BASE_DAILY_CHEESE;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить норму сыра крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает текущий счётчик съеденного сыра; ничего не принимает, отдаёт количество за текущие сутки.
	public int getCheeseEatenToday() {
		try {
			this.resetRatHungerDayIfNeeded();
			return this.cheeseEatenToday;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить счётчик сыра крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает долю сытости для работы и регенерации; ничего не принимает, отдаёт число от 0 до 1.
	public float getRatHungerEfficiency() {
		try {
			if (!this.level().isClientSide() && this.tickCount - this.cachedHungerEfficiencyTick <= 20) {
				return this.cachedHungerEfficiency;
			}
			this.resetRatHungerDayIfNeeded();
			int requirement = this.getDailyCheeseRequirement();
			if (requirement <= 0) {
				throw new IllegalStateException("Суточная норма сыра должна быть положительной");
			}
			float efficiency = Mth.clamp((float) this.cheeseEatenToday / (float) requirement, 0.0F, 1.0F);
			if (!this.level().isClientSide()) {
				this.cachedHungerEfficiencyTick = this.tickCount;
				this.cachedHungerEfficiency = efficiency;
			}
			return efficiency;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить сытость крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет, может ли крыса выполнять рабочие задачи; ничего не принимает, отдаёт true если съеден хотя бы один сыр за сутки.
	public boolean canWorkFromHunger() {
		try {
			return this.getRatHungerEfficiency() > 0.0F;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить работу по сытости крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Масштабирует скорость рабочей цели по сытости; принимает базовую скорость, отдаёт итоговую скорость.
	public double getHungerAdjustedWorkSpeed(double baseSpeed) {
		try {
			if (baseSpeed <= 0.0D) {
				throw new IllegalArgumentException("Базовая скорость рабочей цели должна быть положительной: " + baseSpeed);
			}
			float efficiency = this.getRatHungerEfficiency();
			if (efficiency <= 0.0F) {
				return 0.0D;
			}
			return baseSpeed * efficiency;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось рассчитать скорость работы крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Ведёт крысу к координатам без пересоздания одинакового пути каждый тик; принимает координаты и скорость, отдаёт true если путь поставлен или уже продолжается.
	public boolean moveSmoothlyTo(double x, double y, double z, double speed) {
		try {
			if (this.shouldUseDirectNavigation()) {
				return this.getNavigation().moveTo(x, y, z, speed);
			}
			if (this.canReuseSmoothPath(x, y, z)) {
				return true;
			}
			this.rememberSmoothMoveTarget(x, y, z, null);
			return this.getNavigation().moveTo(x, y, z, speed);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла плавно пойти к {}, {}, {}", this.getUUID(), x, y, z, exception);
			throw exception;
		}
	}

	// Ведёт крысу к сущности без частого пересчёта пути; принимает цель и скорость, отдаёт true если путь поставлен или уже продолжается.
	public boolean moveSmoothlyTo(Entity target, double speed) {
		try {
			if (this.shouldUseDirectNavigation()) {
				return this.getNavigation().moveTo(target, speed);
			}
			if (this.lastSmoothMoveEntity == target && !this.getNavigation().isDone() && this.tickCount - this.lastSmoothMoveTick <= 8) {
				return true;
			}
			this.rememberSmoothMoveTarget(target.getX(), target.getY(), target.getZ(), target);
			return this.getNavigation().moveTo(target, speed);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла плавно пойти к сущности {}", this.getUUID(), target.getUUID(), exception);
			throw exception;
		}
	}

	// Ведёт крысу по готовому пути без частой повторной установки; принимает путь и скорость, отдаёт true если путь поставлен или уже продолжается.
	public boolean moveSmoothlyTo(Path path, double speed) {
		try {
			if (this.shouldUseDirectNavigation()) {
				return this.getNavigation().moveTo(path, speed);
			}
			if (!this.getNavigation().isDone() && this.tickCount - this.lastSmoothMoveTick <= 8) {
				return true;
			}
			this.lastSmoothMoveTick = this.tickCount;
			this.lastSmoothMoveEntity = null;
			return this.getNavigation().moveTo(path, speed);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла плавно пойти по готовому пути", this.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет, надо ли оставить старую прямую навигацию; параметров нет, отдаёт true для карьерных крыс и особых навигаторов.
	private boolean shouldUseDirectNavigation() {
		try {
			return this.hasQuarryUpgradeForNavigation || this.navigatorType == 2 || this.navigatorType == 5;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить режим плавной навигации", this.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет возможность продолжить текущий путь; принимает координаты цели, отдаёт true если цель почти не изменилась.
	private boolean canReuseSmoothPath(double x, double y, double z) {
		try {
			if (this.getNavigation().isDone() || this.tickCount - this.lastSmoothMoveTick > 8) {
				return false;
			}
			double dx = this.lastSmoothMoveX - x;
			double dy = this.lastSmoothMoveY - y;
			double dz = this.lastSmoothMoveZ - z;
			return dx * dx + dy * dy + dz * dz <= 1.0D;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла проверить повторное использование пути", this.getUUID(), exception);
			throw exception;
		}
	}

	// Запоминает цель плавного движения; принимает координаты и сущность или null, ничего не отдаёт.
	private void rememberSmoothMoveTarget(double x, double y, double z, @Nullable Entity target) {
		try {
			this.lastSmoothMoveTick = this.tickCount;
			this.lastSmoothMoveX = x;
			this.lastSmoothMoveY = y;
			this.lastSmoothMoveZ = z;
			this.lastSmoothMoveEntity = target;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Крыса {} не смогла запомнить цель плавного движения", this.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет, должна ли крыса съесть сыр прямо сейчас; ничего не принимает, отдаёт true если очередная порция уже нужна.
	public boolean needsCheeseNow() {
		try {
			this.resetRatHungerDayIfNeeded();
			int requirement = this.getDailyCheeseRequirement();
			if (requirement <= 0) {
				throw new IllegalStateException("Суточная норма сыра должна быть положительной");
			}
			long dayProgress = this.level().getGameTime() % RAT_HUNGER_DAY_TICKS;
			int interval = Math.max(1, RAT_HUNGER_DAY_TICKS / requirement);
			int requiredByNow = Mth.clamp((int) (dayProgress / interval) + 1, 0, requirement);
			return this.cheeseEatenToday < requiredByNow;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить потребность в сыре крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Съедает один сыр из личного слота крысы; принимает описание источника, отдаёт true если сыр съеден.
	public boolean consumeCheeseFromReserve(String sourceName) {
		try {
			if (this.getInventory() == null) {
				throw new IllegalStateException("У крысы нет инвентаря для личного запаса сыра");
			}
			ItemStack reserve = this.getInventory().getItem(RAT_CHEESE_RESERVE_SLOT);
			if (reserve.isEmpty()) {
				return false;
			}
			if (!isCheeseReserveItem(reserve)) {
				return false;
			}
			boolean consumed = this.eatCheeseFromStack(reserve, sourceName);
			if (consumed) {
				if (reserve.isEmpty()) {
					this.getInventory().setItem(RAT_CHEESE_RESERVE_SLOT, ItemStack.EMPTY);
				} else {
					this.getInventory().setChanged();
				}
			}
			return consumed;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось съесть сыр из личного запаса крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает максимум личного запаса сыра; ничего не принимает, отдаёт число предметов.
	public int getMaxCheeseReserve() {
		try {
			return MAX_RAT_CHEESE_RESERVE;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить максимум личного запаса сыра крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает количество сыра в личном запасе; ничего не принимает, отдаёт число предметов.
	public int getCheeseReserveCount() {
		try {
			if (this.getInventory() == null) {
				throw new IllegalStateException("У крысы нет инвентаря для подсчёта сыра");
			}
			ItemStack reserve = this.getInventory().getItem(RAT_CHEESE_RESERVE_SLOT);
			return isCheeseReserveItem(reserve) ? reserve.getCount() : 0;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось посчитать личный сыр крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Возвращает свободное место в личном запасе под указанный сыр; принимает стек сыра, отдаёт количество мест.
	public int getCheeseReserveFreeSpace(ItemStack stack) {
		try {
			if (!isCheeseReserveItem(stack)) {
				throw new IllegalArgumentException("Проверка места в запасе получила не сыр: " + stack);
			}
			if (this.getInventory() == null) {
				throw new IllegalStateException("У крысы нет инвентаря для личного запаса сыра");
			}
			ItemStack reserve = this.getInventory().getItem(RAT_CHEESE_RESERVE_SLOT);
			if (reserve.isEmpty()) {
				return this.getMaxCheeseReserve();
			}
			if (!isCheeseReserveItem(reserve) || !ItemStack.isSameItemSameComponents(reserve, stack)) {
				return 0;
			}
			return Math.max(0, this.getMaxCheeseReserve() - reserve.getCount());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить место под сыр в запасе крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет свободное место в личном запасе под любой сыр; ничего не принимает, отдаёт true если запас не полон.
	public boolean hasCheeseReserveSpace() {
		try {
			if (this.getInventory() == null) {
				throw new IllegalStateException("У крысы нет инвентаря для проверки запаса сыра");
			}
			ItemStack reserve = this.getInventory().getItem(RAT_CHEESE_RESERVE_SLOT);
			if (reserve.isEmpty()) {
				return true;
			}
			return isCheeseReserveItem(reserve) && reserve.getCount() < this.getMaxCheeseReserve();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить свободное место запаса сыра крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет необходимость пополнить личный запас сыра; ничего не принимает, отдаёт true если запас снизился до рабочего минимума.
	public boolean shouldRefillCheeseReserve() {
		try {
			return this.hasCheeseReserveSpace() && this.getCheeseReserveCount() <= MIN_RAT_CHEESE_RESERVE_BEFORE_REFILL;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить порог пополнения сыра крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Перекладывает сыр в личный запас; принимает стек-источник, отдаёт количество переложенных предметов.
	public int addCheeseToReserve(ItemStack source) {
		try {
			if (!isCheeseReserveItem(source)) {
				throw new IllegalArgumentException("В личный запас крысы можно положить только сыр: " + source);
			}
			if (this.getInventory() == null) {
				throw new IllegalStateException("У крысы нет инвентаря для пополнения сыра");
			}
			int space = this.getCheeseReserveFreeSpace(source);
			if (space <= 0) {
				return 0;
			}
			int moved = Math.min(space, source.getCount());
			ItemStack reserve = this.getInventory().getItem(RAT_CHEESE_RESERVE_SLOT);
			if (reserve.isEmpty()) {
				ItemStack inserted = source.split(moved);
				inserted.setCount(Math.min(inserted.getCount(), this.getMaxCheeseReserve()));
				this.getInventory().setItem(RAT_CHEESE_RESERVE_SLOT, inserted);
			} else {
				reserve.grow(moved);
				source.shrink(moved);
				this.getInventory().setChanged();
			}
			return moved;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось пополнить личный запас сыра крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Забирает сыр из личного запаса; принимает количество, отдаёт стек сыра или пустой стек.
	public ItemStack takeCheeseFromReserve(int amount) {
		try {
			if (amount <= 0) {
				throw new IllegalArgumentException("Количество забираемого сыра должно быть положительным: " + amount);
			}
			if (this.getInventory() == null) {
				throw new IllegalStateException("У крысы нет инвентаря для выдачи сыра");
			}
			ItemStack reserve = this.getInventory().getItem(RAT_CHEESE_RESERVE_SLOT);
			if (!isCheeseReserveItem(reserve)) {
				return ItemStack.EMPTY;
			}
			ItemStack removed = reserve.split(Math.min(amount, reserve.getCount()));
			if (reserve.isEmpty()) {
				this.getInventory().setItem(RAT_CHEESE_RESERVE_SLOT, ItemStack.EMPTY);
			} else {
				this.getInventory().setChanged();
			}
			return removed;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось забрать сыр из личного запаса крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Делится сыром с ближайшей крысой того же владельца; ничего не принимает и не отдаёт.
	private void shareCheeseWithNearbyRats() {
		try {
			if (this.getOwnerUUID() == null || this.getCheeseReserveCount() <= 1) {
				return;
			}
			TamedRat receiver = this.level().getEntitiesOfClass(TamedRat.class, this.getBoundingBox().inflate(RAT_CHEESE_SHARE_RADIUS), other ->
					other != this && other.isAlive() && Objects.equals(other.getOwnerUUID(), this.getOwnerUUID()) && other.getCheeseReserveCount() + 1 < this.getCheeseReserveCount()
			).stream().min((first, second) -> Integer.compare(first.getCheeseReserveCount(), second.getCheeseReserveCount())).orElse(null);
			if (receiver == null) {
				return;
			}
			ItemStack moved = this.takeCheeseFromReserve(1);
			if (moved.isEmpty()) {
				return;
			}
			int accepted = receiver.addCheeseToReserve(moved);
			if (accepted <= 0) {
				this.addCheeseToReserve(moved);
				return;
			}
			if (!moved.isEmpty()) {
				this.addCheeseToReserve(moved);
			}
			RatsMod.LOGGER.debug("Крыса {} поделилась сыром с крысой {}: донор={}, получатель={}", this.getUUID(), receiver.getUUID(), this.getCheeseReserveCount(), receiver.getCheeseReserveCount());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось поделиться сыром у крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Съедает один сыр из переданного стека; принимает стек и источник, отдаёт true если сыр был съеден.
	public boolean eatCheeseFromStack(ItemStack stack, String sourceName) {
		try {
			if (stack.isEmpty()) {
				return false;
			}
			if (!isCheeseReserveItem(stack)) {
				throw new IllegalArgumentException("Крыса может есть только сыр, получен стек: " + stack);
			}
			if (!this.needsCheeseNow()) {
				return false;
			}
			stack.shrink(1);
			this.recordRatCheeseMeal(sourceName);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось накормить крысу {} из источника {}", this.getUUID(), sourceName, exception);
			throw exception;
		}
	}

	// Записывает факт кормления сыром; принимает источник кормления, ничего не отдаёт.
	private void recordRatCheeseMeal(String sourceName) {
		try {
			int requirement = this.getDailyCheeseRequirement();
			this.cheeseEatenToday = Mth.clamp(this.cheeseEatenToday + 1, 0, requirement);
			this.lastFeedDay = this.getRatHungerDay();
			this.cachedHungerEfficiencyTick = -1000;
			this.syncRatHungerData();
			this.playSound(SoundEvents.GENERIC_EAT, 0.6F, 0.9F + this.getRandom().nextFloat() * 0.2F);
			RatsMod.LOGGER.debug("Крыса {} съела сыр из источника '{}': {}/{}", this.getUUID(), sourceName, this.cheeseEatenToday, requirement);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось записать кормление крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Проверяет наличие генов и создаёт их для старых или новых крыс; принимает причину вызова, ничего не отдаёт.
	private void ensureRatGenesInitialized(String reason) {
		try {
			if (this.getEntityData().get(RAT_GENES_INITIALIZED)) {
				return;
			}
			this.initializeRandomRatGenes(reason);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить гены крысы {} по причине {}", this.getUUID(), reason, exception);
			throw exception;
		}
	}

	// Создаёт случайные гены 50-250%; принимает причину создания, ничего не отдаёт.
	private void initializeRandomRatGenes(String reason) {
		try {
			RandomSource random = this.getRandom();
			this.setRatGenes(
					MIN_GENE_MULTIPLIER + random.nextFloat() * (MAX_GENE_MULTIPLIER - MIN_GENE_MULTIPLIER),
					MIN_GENE_MULTIPLIER + random.nextFloat() * (MAX_GENE_MULTIPLIER - MIN_GENE_MULTIPLIER),
					MIN_GENE_MULTIPLIER + random.nextFloat() * (MAX_GENE_MULTIPLIER - MIN_GENE_MULTIPLIER),
					MIN_GENE_MULTIPLIER + random.nextFloat() * (MAX_GENE_MULTIPLIER - MIN_GENE_MULTIPLIER),
					MIN_GENE_MULTIPLIER + random.nextFloat() * (MAX_GENE_MULTIPLIER - MIN_GENE_MULTIPLIER)
			);
			this.onUpgradeChanged();
			RatsMod.LOGGER.info("Крыса {} получила случайные гены при {}: скорость={}, здоровье={}, броня={}, урон={}, регенерация={}",
					this.getUUID(), reason, this.getRatGene(GENE_SPEED), this.getRatGene(GENE_HEALTH), this.getRatGene(GENE_ARMOR), this.getRatGene(GENE_DAMAGE), this.getRatGene(GENE_REGEN));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось создать случайные гены крысы {} при {}", this.getUUID(), reason, exception);
			throw exception;
		}
	}

	// Наследует гены от родителей; принимает мать и отца, ничего не отдаёт.
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
			RatsMod.LOGGER.info("Крыса {} унаследовала гены: скорость={}, здоровье={}, броня={}, урон={}, регенерация={}",
					this.getUUID(), this.getRatGene(GENE_SPEED), this.getRatGene(GENE_HEALTH), this.getRatGene(GENE_ARMOR), this.getRatGene(GENE_DAMAGE), this.getRatGene(GENE_REGEN));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось унаследовать гены крысы {} от родителей {} и {}", this.getUUID(), mother.getUUID(), father.getUUID(), exception);
			throw exception;
		}
	}

	// Смешивает один ген родителей; принимает два значения и имя гена, отдаёт итоговый множитель.
	private float mixParentGene(float firstParentGene, float secondParentGene, String geneName) {
		try {
			float min = Math.min(firstParentGene, secondParentGene);
			float max = Math.max(firstParentGene, secondParentGene);
			float mixed = min + this.getRandom().nextFloat() * (max - min);
			if (this.getRandom().nextFloat() <= RAT_GENE_MUTATION_CHANCE) {
				mixed = this.mutateRatGene(mixed, min, max, geneName);
			}
			return this.clampRatGene(mixed);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось смешать ген {} для крысы {}", geneName, this.getUUID(), exception);
			throw exception;
		}
	}

	// Мутирует ген за пределы родительского диапазона; принимает значение, границы родителей и имя гена, отдаёт новый множитель.
	private float mutateRatGene(float mixed, float parentMin, float parentMax, String geneName) {
		try {
			float mutation = MIN_RAT_GENE_MUTATION_STEP + this.getRandom().nextFloat() * (MAX_RAT_GENE_MUTATION_STEP - MIN_RAT_GENE_MUTATION_STEP);
			boolean canMutateDown = parentMin > MIN_GENE_MULTIPLIER;
			boolean canMutateUp = parentMax < MAX_GENE_MULTIPLIER;
			if (!canMutateDown && !canMutateUp) {
				return this.clampRatGene(mixed);
			}
			boolean mutateUp = canMutateUp && (!canMutateDown || this.getRandom().nextBoolean());
			float mutated = mutateUp ? parentMax + mutation : parentMin - mutation;
			RatsMod.LOGGER.debug("Мутация гена {} у крысы {}: {} -> {}", geneName, this.getUUID(), mixed, mutated);
			return this.clampRatGene(mutated);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось мутировать ген {} у крысы {}", geneName, this.getUUID(), exception);
			throw exception;
		}
	}

	// Записывает набор генов в синхронизируемые поля; принимает пять множителей, ничего не отдаёт.
	private void setRatGenes(float speed, float health, float armor, float damage, float regen) {
		try {
			this.getEntityData().set(GENE_SPEED, this.clampRatGene(speed));
			this.getEntityData().set(GENE_HEALTH, this.clampRatGene(health));
			this.getEntityData().set(GENE_ARMOR, this.clampRatGene(armor));
			this.getEntityData().set(GENE_DAMAGE, this.clampRatGene(damage));
			this.getEntityData().set(GENE_REGEN, this.clampRatGene(regen));
			this.getEntityData().set(RAT_GENES_INITIALIZED, true);
			this.refreshDimensions();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось записать гены крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Сохраняет гены крысы в NBT; принимает тег сущности, ничего не отдаёт.
	private void saveRatGenes(CompoundTag tag) {
		try {
			if (!this.getEntityData().get(RAT_GENES_INITIALIZED)) {
				return;
			}
			CompoundTag genes = new CompoundTag();
			genes.putFloat("Speed", this.getRatGene(GENE_SPEED));
			genes.putFloat("Health", this.getRatGene(GENE_HEALTH));
			genes.putFloat("Armor", this.getRatGene(GENE_ARMOR));
			genes.putFloat("Damage", this.getRatGene(GENE_DAMAGE));
			genes.putFloat("Regen", this.getRatGene(GENE_REGEN));
			tag.put(RAT_GENES_TAG, genes);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось сохранить гены крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Загружает гены крысы из NBT; принимает тег сущности, ничего не отдаёт.
	private void loadRatGenes(CompoundTag tag) {
		try {
			if (!tag.contains(RAT_GENES_TAG, Tag.TAG_COMPOUND)) {
				this.getEntityData().set(RAT_GENES_INITIALIZED, false);
				return;
			}
			CompoundTag genes = tag.getCompound(RAT_GENES_TAG);
			this.setRatGenes(
					genes.getFloat("Speed"),
					genes.getFloat("Health"),
					genes.getFloat("Armor"),
					genes.getFloat("Damage"),
					genes.getFloat("Regen")
			);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось загрузить гены крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Добавляет генетические модификаторы атрибутов; ничего не принимает и не отдаёт.
	private void applyRatGeneAttributeModifiers() {
		try {
			if (!this.getEntityData().get(RAT_GENES_INITIALIZED)) {
				return;
			}
			this.addRatGeneAttributeModifier(Attributes.MOVEMENT_SPEED, "gene_speed", this.getRatGene(GENE_SPEED));
			this.addRatGeneAttributeModifier(Attributes.FLYING_SPEED, "gene_flying_speed", this.getRatGene(GENE_SPEED));
			this.addRatGeneAttributeModifier(Attributes.MAX_HEALTH, "gene_health", this.getRatGene(GENE_HEALTH));
			this.addRatGeneAttributeModifier(Attributes.ARMOR, "gene_armor", this.getRatGene(GENE_ARMOR));
			this.addRatGeneAttributeModifier(Attributes.ATTACK_DAMAGE, "gene_damage", this.getRatGene(GENE_DAMAGE));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось применить гены к атрибутам крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Добавляет один генетический модификатор атрибута; принимает атрибут, имя и множитель, ничего не отдаёт.
	private void addRatGeneAttributeModifier(Holder<Attribute> attribute, String modifierName, float multiplier) {
		try {
			var attributeInstance = this.getAttribute(attribute);
			if (attributeInstance == null) {
				throw new IllegalStateException("У крысы нет атрибута " + modifierName);
			}
			AttributeModifier modifier = new AttributeModifier(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, modifierName), multiplier - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
			attributeInstance.addTransientModifier(modifier);
			this.attributeChanges.put(attribute, modifier);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось добавить генетический модификатор {} крысе {}", modifierName, this.getUUID(), exception);
			throw exception;
		}
	}

	// Читает один ген; принимает поле синхронизации, отдаёт множитель 50-250%.
	private float getRatGene(EntityDataAccessor<Float> gene) {
		try {
			if (!this.getEntityData().get(RAT_GENES_INITIALIZED)) {
				return 1.0F;
			}
			return this.clampRatGene(this.getEntityData().get(gene));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось прочитать ген крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Ограничивает ген допустимым диапазоном; принимает множитель, отдаёт значение 50-250%.
	private float clampRatGene(float value) {
		try {
			return Mth.clamp(value, MIN_GENE_MULTIPLIER, MAX_GENE_MULTIPLIER);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось ограничить ген крысы {} со значением {}", this.getUUID(), value, exception);
			throw exception;
		}
	}

	// Считает размер модели по гену здоровья; принимает множитель здоровья, отдаёт множитель размера модели.
	private float calculateRatModelScale(float healthGene) {
		try {
			float normalized = (this.clampRatGene(healthGene) - MIN_GENE_MULTIPLIER) / (MAX_GENE_MULTIPLIER - MIN_GENE_MULTIPLIER);
			return MIN_RAT_MODEL_SCALE + normalized * (MAX_RAT_MODEL_SCALE - MIN_RAT_MODEL_SCALE);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось посчитать размер модели крысы {} по гену здоровья {}", this.getUUID(), healthGene, exception);
			throw exception;
		}
	}

	// Наследование combined-улучшения от родителей (два случайных улучшения из пула, как на комбайнере).
	private void tryInheritBabyUpgrade(TamedRat father, TamedRat mother, TamedRat baby) {
		try {
			if (this.getRandom().nextFloat() > 0.35F) {
				return;
			}
			List<ItemStack> pool = new ArrayList<>();
			this.collectParentUpgradesForGenetics(father, pool);
			this.collectParentUpgradesForGenetics(mother, pool);
			pool.removeIf(stack -> stack.isEmpty() || RatsRemovedContent.isRemoved(stack.getItem()) || !(stack.getItem() instanceof BaseRatUpgradeItem) || stack.getItem() instanceof CombinedRatUpgradeItem);
			if (pool.size() < 2) {
				return;
			}
			var registries = baby.level().registryAccess();
			for (int attempt = 0; attempt < 12; attempt++) {
				ItemStack first = pool.get(this.getRandom().nextInt(pool.size())).copy();
				ItemStack second = pool.get(this.getRandom().nextInt(pool.size())).copy();
				if (ItemStack.isSameItemSameComponents(first, second)) {
					continue;
				}
				ItemStack combined = new ItemStack(RatsItemRegistry.RAT_UPGRADE_COMBINED.get());
				if (!CombinedRatUpgradeItem.canCombineWithUpgrade(combined, first)) {
					continue;
				}
				combined = UpgradeCombinerBlockEntity.mergeUpgradeIntoCombined(combined, first, registries);
				if (!CombinedRatUpgradeItem.canCombineWithUpgrade(combined, second)) {
					continue;
				}
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
		} catch (Exception e) {
			RatsMod.LOGGER.error("tryInheritBabyUpgrade: {}", e.getMessage(), e);
		}
	}

	private void collectParentUpgradesForGenetics(TamedRat parent, List<ItemStack> pool) {
		for (EquipmentSlot slot : RatUpgradeUtils.UPGRADE_SLOTS) {
			ItemStack equipped = parent.getItemBySlot(slot);
			if (equipped.isEmpty()) {
				continue;
			}
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

	@Override
	public boolean isPickable() {
		return !(this.getVehicle() instanceof Player);
	}

	public ItemStack getResultForRecipe(RecipeType<? extends SingleItemRecipe> recipe, ItemStack stack) {
		var optional = this.level().getRecipeManager().getRecipeFor(recipe, new SingleRecipeInput(stack), this.level());
		if (optional.isPresent()) {
			ItemStack itemstack = optional.get().value().getResultItem(this.level().registryAccess());
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
				IItemHandler handler = this.level().getCapability(Capabilities.ItemHandler.BLOCK, te.getBlockPos(), Direction.UP);
				if (handler != null) {
					if (ItemHandlerHelper.insertItem(handler, burntItem, true).isEmpty()) {
						ItemHandlerHelper.insertItem(handler, burntItem, false);
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
		if (!this.crafting) {
			entity.push(this);
		}
	}

	public Animation getDanceAnimation() {
		if (this.getDanceMoves() == 0) {
			return ANIMATION_DANCE;
		}
		return NO_ANIMATION;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		if (this.hasToga()) {
			this.spawnAtLocation(new ItemStack(RatlantisItemRegistry.RAT_TOGA.get()), 0.0F);
		}
		super.dropCustomDeathLoot(level, source, recentlyHit);
	}

	public void spawnAngelCopy() {
		TamedRat copy = RatsEntityRegistry.TAMED_RAT.get().create(this.level());
		CompoundTag tag = new CompoundTag();
		this.addAdditionalSaveData(tag);
		tag.putShort("HurtTime", (short) 0);
		tag.putInt("HurtByTimestamp", 0);
		tag.putShort("DeathTime", (short) 0);
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

	@Override
	public boolean canBeAffected(MobEffectInstance instance) {
		if (instance.getEffect() == MobEffects.POISON && (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_POISON.get()) || RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_DAMAGE_PROTECTION.get()))) {
			return false;
		}
		return super.canBeAffected(instance);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack itemstack = player.getItemInHand(hand);
		if (this.getRespawnCountdown() > 0 || itemstack.is(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "rat_spawn_egg")))) {
			return InteractionResult.PASS;
		}
		if (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_CARRAT.get())) {
			if (player.getFoodData().needsFood()) {
				player.getFoodData().eat(1, 0.1F);
				player.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
				for (int i = 0; i < 8; i++) {
					double d0 = this.getRandom().nextGaussian() * 0.02D;
					double d1 = this.getRandom().nextGaussian() * 0.02D;
					double d2 = this.getRandom().nextGaussian() * 0.02D;
					this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CARROT)), this.getX() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), this.getY() + (double) (this.getRandom().nextFloat() * this.getBbHeight() * 2.0F) - (double) this.getBbHeight(), this.getZ() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), d0, d1, d2);
				}
				if (RatConfig.carratDamagePerBite > 0.0D) {
					this.hurt(this.damageSources().playerAttack(player), (float) RatConfig.carratDamagePerBite);
				}
				return InteractionResult.SUCCESS;
			}
		}
		if (this.isBaby()) {
			if (itemstack.is(RatsItemTags.CHEESE_ITEMS)) {
				if (!player.getAbilities().instabuild) {
					itemstack.shrink(1);
				}
				this.ageUp(getSpeedUpSecondsWhenFeeding(-this.getAge()), true);
				return InteractionResult.sidedSuccess(this.level().isClientSide());
			}
			if (itemstack.is(RatsItemRegistry.CREATIVE_CHEESE.get())) {
				this.ageUp(-(this.getAge() / 20), true);
				return InteractionResult.sidedSuccess(this.level().isClientSide());
			}
		}
		if (!this.isBaby() && this.isOwnedBy(player)) {
			if (player.isSecondaryUseActive() && !this.isPassenger()) {
				if (player.getPassengers().size() < 3) {
					this.startRiding(player, true);
					player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_DISMOUNT), true);
				}
				return InteractionResult.sidedSuccess(this.level().isClientSide());
			}
			if (itemstack.is(RatsItemRegistry.RAT_PAPERS.get())) {
				InteractionResult result = itemstack.interactLivingEntity(player, this, hand);
				if (result.consumesAction()) {
					return result;
				}
			} else if (itemstack.is(RatlantisItemRegistry.RAT_TOGA.get())) {
				if (!this.hasToga()) {
					if (!player.isCreative()) {
						itemstack.shrink(1);
					}
				} else {
					if (!this.level().isClientSide()) {
						this.spawnAtLocation(new ItemStack(RatlantisItemRegistry.RAT_TOGA.get()), 0.0F);
					}
				}
				this.setToga(!this.hasToga());
				this.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1F, 1.5F);
				return InteractionResult.SUCCESS;
			} else if (itemstack.is(RatsBlockRegistry.DYE_SPONGE.get().asItem()) && this.isDyed()) {
				this.setDyed(false);
				this.setDyeColor(0);
				this.setSpecialDye("");
				for (int i = 0; i < 8; i++) {
					double d0 = this.getRandom().nextGaussian() * 0.02D;
					double d1 = this.getRandom().nextGaussian() * 0.02D;
					double d2 = this.getRandom().nextGaussian() * 0.02D;
					this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(RatsBlockRegistry.DYE_SPONGE.get())), this.getX() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), this.getY() + (double) (this.getRandom().nextFloat() * this.getBbHeight() * 2.0F) - (double) this.getBbHeight(), this.getZ() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), d0, d1, d2);
				}
				this.playSound(RatsSoundRegistry.DYE_SPONGE_USED.get(), this.getSoundVolume(), this.getVoicePitch());
				return InteractionResult.SUCCESS;
			} else if (itemstack.is(RatsItemRegistry.RATBOW_ESSENCE.get()) && this.applySpecialDyeIfPossible(itemstack)) {
				return InteractionResult.SUCCESS;
			} else if (this.applyNormalDyeIfPossible(itemstack)) {
				return InteractionResult.SUCCESS;
			} else if (itemstack.is(RatsItemRegistry.RAT_SACK.get())) {
				if (RatSackItem.getRatsInSack(itemstack) >= RatConfig.ratSackCapacity) {
					player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_SACK_TOO_FULL).withStyle(ChatFormatting.RED), true);
					return InteractionResult.PASS;
				} else {
					RatSackItem.packRatIntoSack(itemstack, this, RatSackItem.getRatsInSack(itemstack) + 1);
					this.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1, 1);
					this.discard();
					player.swing(hand);
					return InteractionResult.SUCCESS;
				}
			} else if (itemstack.getItem() instanceof RatStaffItem) {
				SelectedRat selectedRat = player.getCapability(RatsCapabilityRegistry.SELECTED_RAT);
				if (selectedRat != null) {
					selectedRat.setSelectedRat(this);
				}
				player.swing(hand);
				if (!this.level().isClientSide() && player instanceof ServerPlayer sp) {
					RatsNetworkHandler.CHANNEL.send(RatsNetworkHandler.playerTarget(() -> sp), new ManageRatStaffPacket(this.getId(), BlockPos.ZERO, Direction.NORTH.ordinal(), false, false));
				}
				player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_STAFF_BIND, this.getName()), true);
				return InteractionResult.SUCCESS;
			} else if (itemstack.is(Items.ARROW)) {
				itemstack.shrink(1);
				ItemStack arrow = new ItemStack(RatsItemRegistry.RAT_ARROW.get());
				CompoundTag tag = new CompoundTag();
				CompoundTag ratTag = new CompoundTag();
				this.addAdditionalSaveData(ratTag);
				if (this.hasCustomName()) {
					ratTag.putString("CustomName", Component.Serializer.toJson(this.getCustomName(), this.registryAccess()));
				}
				tag.put("Rat", ratTag);
				arrow.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
				if (itemstack.isEmpty()) {
					player.setItemInHand(hand, arrow);
				} else if (!player.getInventory().add(arrow)) {
					player.drop(arrow, false);
				}
				this.playSound(RatsSoundRegistry.RAT_HURT.get(), 1, 1);
				player.swing(hand);
				this.discard();
				return InteractionResult.SUCCESS;
			} else {
				return super.mobInteract(player, hand);
			}
		}
		return InteractionResult.PASS;
	}

	public boolean applyNormalDyeIfPossible(ItemStack stack) {
		if (stack.getItem() instanceof DyeItem item && (!this.isDyed() || this.getDyeColor() != item.getDyeColor().getId())) {
			if (!this.isDyed()) {
				this.setDyed(true);
			}
			this.setDyeColor(item.getDyeColor().getId());
			for (int i = 0; i < 8; i++) {
				double d0 = this.getRandom().nextGaussian() * 0.02D;
				double d1 = this.getRandom().nextGaussian() * 0.02D;
				double d2 = this.getRandom().nextGaussian() * 0.02D;
				this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), this.getX() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), this.getY() + (double) (this.getRandom().nextFloat() * this.getBbHeight() * 2.0F) - (double) this.getBbHeight(), this.getZ() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), d0, d1, d2);
			}
			this.playSound(RatsSoundRegistry.ESSENCE_APPLIED.get(), this.getSoundVolume(), this.getVoicePitch());
			stack.shrink(1);
			return true;
		}
		return false;
	}

	public boolean applySpecialDyeIfPossible(ItemStack stack) {
		String name = stack.getHoverName().getString();
		if (!this.getSpecialDye().equals(name) || !this.isDyed()) {
			if (!this.isDyed()) {
				this.setDyed(true);
			}
			this.setDyeColor(100);
			this.setSpecialDye(name);
			for (int i = 0; i < 8; i++) {
				double d0 = this.getRandom().nextGaussian() * 0.02D;
				double d1 = this.getRandom().nextGaussian() * 0.02D;
				double d2 = this.getRandom().nextGaussian() * 0.02D;
				this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(RatsItemRegistry.RATBOW_ESSENCE.get())), this.getX() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), this.getY() + (double) (this.getRandom().nextFloat() * this.getBbHeight() * 2.0F) - (double) this.getBbHeight(), this.getZ() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), d0, d1, d2);
			}
			this.playSound(RatsSoundRegistry.ESSENCE_APPLIED.get(), this.getSoundVolume(), this.getVoicePitch());
			stack.shrink(1);
			return true;
		}
		return false;
	}

	// Меняет приручение крысы старым вызовом; принимает флаг приручения, ничего не отдаёт.
	public void setTame(boolean tamed) {
		try {
			this.setTame(tamed, true);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось изменить приручение крысы {} на {}", this.getUUID(), tamed, exception);
			throw exception;
		}
	}

	// Меняет приручение крысы и создаёт гены при первом приручении; принимает флаг и режим частиц, ничего не отдаёт.
	@Override
	public void setTame(boolean tamed, boolean applyTamingSideEffects) {
		try {
			if (tamed) {
				Arrays.fill(this.armorDropChances, 1.1F);
				Arrays.fill(this.handDropChances, 1.1F);
			}
			super.setTame(tamed, applyTamingSideEffects);
			if (tamed && !this.level().isClientSide()) {
				this.ensureRatGenesInitialized("taming");
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось изменить приручение крысы {} на {}", this.getUUID(), tamed, exception);
			throw exception;
		}
	}

	@Override
	public int getAmbientSoundInterval() {
		return RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_CHRISTMAS.get()) ? 1000 : 200;
	}

	@Override
	public boolean causeFallDamage(float dist, float mult, DamageSource source) {
		try {
			if (source.is(DamageTypes.FALL) && dist < 5.0F) {
				this.resetFallDistance();
				RatsMod.LOGGER.debug("Крыса {} безопасно упала с высоты {} в {}", this.getUUID(), dist, this.blockPosition());
				return false;
			}
			if (!this.isInvulnerableTo(source) && !this.isPassenger()) {
				return super.causeFallDamage(dist, mult, source);
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось обработать падение крысы {} с высоты {}", this.getUUID(), dist, exception);
			throw exception;
		}
	}

	@Override
	protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
		if (!this.hasFlightUpgrade()) {
			super.checkFallDamage(y, onGround, state, pos);
		}
	}

	@Override
	public void rideTick() {
		super.rideTick();
		if (this.getVehicle() instanceof Player player) {
			this.updateRiding(player);
		}
	}

	public void updateRiding(Player riding) {
		int i = riding.getPassengers().indexOf(this);
		float radius = (i == 0 ? 0F : 0.4F) + (riding.isFallFlying() ? 1 : 0);
		float angle = (0.01745329251F * riding.yBodyRot + (i == 2 ? -92.5F : i == 1 ? 92.5F : 0));
		double extraX = radius * Mth.sin((float) (Math.PI + angle));
		double extraZ = radius * Mth.cos(angle);
		double extraY = (riding.isCrouching() ? 1.2D : riding.isFallFlying() ? 0.25D : 1.4D);
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
			for (int k = 0; k < 20; ++k) {
				double d2 = this.getRandom().nextGaussian() * 0.02D;
				double d0 = this.getRandom().nextGaussian() * 0.02D;
				double d1 = this.getRandom().nextGaussian() * 0.02D;
				this.level().addParticle(ParticleTypes.POOF, this.getX() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), this.getY() + (double) (this.getRandom().nextFloat() * this.getBbHeight()), this.getZ() + (double) (this.getRandom().nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), d2, d0, d1);
			}
		} else {
			super.handleEntityEvent(id);
		}
	}

	protected SoundEvent getAmbientSound() {
		if (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_CHRISTMAS.get())) {
			return RatsSoundRegistry.RAT_SANTA.get();
		}
		if (RatsMod.ICEANDFIRE_LOADED && RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_DRAGON.get())) {
			SoundEvent possibleDragonSound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("iceandfire", "firedragon_child_idle"));
			if (possibleDragonSound != null) {
				return possibleDragonSound;
			}
		}
		return super.getAmbientSound();
	}

	protected SoundEvent getDeathSound() {
		if (RatsMod.ICEANDFIRE_LOADED && RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_DRAGON.get())) {
			SoundEvent possibleDragonSound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("iceandfire", "firedragon_child_death"));
			if (possibleDragonSound != null) {
				return possibleDragonSound;
			}
		}
		return super.getDeathSound();
	}

	protected SoundEvent getHurtSound(DamageSource source) {
		if (RatsMod.ICEANDFIRE_LOADED && RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_DRAGON.get())) {
			SoundEvent possibleDragonSound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("iceandfire", "firedragon_child_hurt"));
			if (possibleDragonSound != null) {
				return possibleDragonSound;
			}
		}
		return super.getHurtSound(source);
	}

	public boolean onHearFlute(Player player, RatCommand ratCommand) {
		if (this.isOwnedBy(player) && !this.isBaby() && !RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_NO_FLUTE.get())) {
			this.setCommand(ratCommand);
			return true;
		}
		return false;
	}

	// Проверяет, может ли крыса подобрать предмет с учётом чёрного/белого списка в улучшении.
	public boolean canRatPickupItem(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		boolean hasBlacklist = RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get());
		boolean hasWhitelist = RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_WHITELIST.get());
		if (!hasBlacklist && !hasWhitelist) {
			return true;
		}
		ItemStack filterUpgrade = hasBlacklist
				? RatUpgradeUtils.getUpgrade(this, RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get())
				: RatUpgradeUtils.getUpgrade(this, RatsItemRegistry.RAT_UPGRADE_WHITELIST.get());
		CompoundTag tag = filterUpgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (!tag.contains("Items", 9)) {
			return hasBlacklist;
		}
		ListTag list = tag.getList("Items", 10);
		if (list.isEmpty()) {
			return hasBlacklist;
		}
		String ourItemID = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(stack.getItem())).toString();
		for (int i = 0; i < list.size(); ++i) {
			String itemID = list.getCompound(i).getString("id");
			if (ourItemID.equals(itemID)) {
				return hasWhitelist;
			}
		}
		return hasBlacklist;
	}

	@Override
	public boolean isPushable() {
		return !this.isBaby() && !this.isInTube() && super.isPushable();
	}

	@Override
	public boolean isCurrentlyGlowing() {
		if (this.level().isClientSide() && ForgeClientEvents.isRatSelectedOnStaff(this)) return true;
		return super.isCurrentlyGlowing();
	}

	public void attemptTeleport(double x, double y, double z) {
		double d0 = this.getX();
		double d1 = this.getY();
		double d2 = this.getZ();
		this.setPos(x, y, z);
		this.level().broadcastEntityEvent(this, (byte) 84);
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
					this.setPos(this.getX(), this.getY() - 1, this.getZ());
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
			this.playSound(RatsSoundRegistry.RAT_TELEPORT.get(), 1, 1);
		}
	}

	public boolean isDirectPathBetweenPoints(Vec3 target) {
		BlockHitResult result = this.level().clip(new ClipContext(this.position(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		BlockPos pos = result.getBlockPos();
		BlockPos sidePos = result.getBlockPos().relative(result.getDirection());
		if (!this.level().isEmptyBlock(pos) || !this.level().isEmptyBlock(sidePos)) {
			return true;
		} else {
			return result.getType() == HitResult.Type.MISS;
		}
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return !RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_UNDEAD.get()) && super.canBeSeenAsEnemy();
	}

	@Override
	public boolean isNoAi() {
		return super.isNoAi() || this.getRespawnCountdown() > 0;
	}

	public boolean holdsItemInHandUpgrade() {
		boolean bool = RatUpgradeUtils.forEachUpgradeBool(this, stack -> stack instanceof HoldsItemUpgrade upgrade && !upgrade.isFakeHandRender(), false);

		return !this.isInWheel() && bool;
	}

	@Override
	public boolean shouldPlayIdleAnimations() {
		boolean bool = RatUpgradeUtils.forEachUpgradeBool(this, (stack) -> stack.playIdleAnimation(this), true);
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
		if (!this.level().isClientSide() && vehicle instanceof RatMount mount) {
			if (!RatUpgradeUtils.hasUpgrade(this, mount.getUpgradeItem())) {
				this.level().broadcastEntityEvent(this, (byte) 127);
				this.stopRiding();
				vehicle.discard();
			}
		}

		if (!this.attributeChanges.isEmpty()) {
			this.getAttributes().removeAttributeModifiers(this.attributeChanges);
			this.attributeChanges.clear();
		}

		this.applyRatGeneAttributeModifiers();

		RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof StatBoostingUpgrade, (stack, slot) ->
				((StatBoostingUpgrade) stack.getItem()).getAttributeBoosts().forEach((attribute, aDouble) -> this.tryIncreaseStat(stack.getHoverName().getString(), attribute, aDouble)));

		if (this.getHeldRF() > this.getRFTransferRate()) {
			this.setHeldRF(this.getRFTransferRate());
		}
		this.heal(this.getMaxHealth());
		this.setFlying(this.isFlying() && this.hasFlightUpgradeCached);
		this.setNoGravity(RatUpgradeUtils.hasUpgrade(this, RatlantisItemRegistry.RAT_UPGRADE_ETHEREAL.get()) || this.isFlying());
	}

	// Пересчитывает быстрые признаки улучшений; параметров нет, обновляет кеши для частых тиковых проверок.
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
			this.hasQuarryUpgradeForNavigation = any && RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_QUARRY.get());
			this.hasWorkerUpgradeCached = any && RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_WORKER.get());
			this.hasFlightUpgradeCached = any && RatUpgradeUtils.forEachUpgradeBool(this, (stack) -> stack.canFly(this), false);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось пересчитать кеш улучшений крысы {}", this.getUUID(), exception);
			throw exception;
		}
	}

	// Добавляет временный модификатор атрибута от улучшения; itemName — имя предмета, stat — атрибут, value — величина.
	public void tryIncreaseStat(String itemName, Holder<Attribute> stat, double value) {
		try {
			String statPath = stat.unwrapKey().orElseThrow().location().getPath();
			String safeName = itemName.replaceAll("[^a-z0-9_./-]", "_").toLowerCase(Locale.ROOT);
			ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, safeName + "_" + statPath);
			var attributeInstance = this.getAttribute(stat);
			if (attributeInstance == null) {
				RatsMod.LOGGER.error("tryIncreaseStat: у крысы {} нет атрибута {}", this.getUUID(), statPath);
				return;
			}
			AttributeModifier modifier = new AttributeModifier(modifierId, value, AttributeModifier.Operation.ADD_VALUE);
			attributeInstance.addTransientModifier(modifier);
			this.attributeChanges.put(stat, modifier);
		} catch (Exception e) {
			RatsMod.LOGGER.error("tryIncreaseStat: сбой для улучшения {}: {}", itemName, e.getMessage(), e);
		}
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		if (this.getRespawnCountdown() > 0) {
			return true;
		}
		AtomicBoolean upgradePrevented = new AtomicBoolean(false);
		RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof DamageImmunityUpgrade, (stack, slot) -> {
			if (((DamageImmunityUpgrade) stack.getItem()).isImmuneToDamageSource(this, source)) {
				upgradePrevented.set(true);
			}
		});

		if (RatUpgradeUtils.hasUpgrade(this, RatsItemRegistry.RAT_UPGRADE_CREATIVE.get())) {
			return source.getEntity() == null || source.getEntity() instanceof LivingEntity living && !this.isOwnedBy(living);
		}
		return upgradePrevented.get() || super.isInvulnerableTo(source);
	}

	@Override
	public boolean isInvulnerable() {
		return super.isInvulnerable() || this.getRespawnCountdown() > 0;
	}

	@Override
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
		return RatUpgradeUtils.forEachUpgradeBool(this, (stack) -> stack.shouldDepositItem(this, item), true);
	}

	public boolean shouldCollectItem(ItemStack item) {
		return RatUpgradeUtils.forEachUpgradeBool(this, (stack) -> stack.shouldCollectItem(this, item), true);
	}

	public int getRFTransferRate() {
		AtomicInteger energy = new AtomicInteger();
		RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof EnergyRatUpgradeItem, (stack, slot) ->
				energy.set(((EnergyRatUpgradeItem) stack.getItem()).getRFTransferRate()));

		return energy.get();
	}

	public int getMBTransferRate() {
		AtomicInteger fluid = new AtomicInteger();
		RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof BucketRatUpgradeItem, (stack, slot) ->
				fluid.set(((BucketRatUpgradeItem) stack.getItem()).getMbTransferRate()));

		return fluid.get();
	}

	@Override
	public boolean isAlliedTo(Entity entity) {
		LivingEntity livingentity = this.getOwner();
		if (entity == livingentity) {
			return true;
		} else if (entity instanceof RatMountBase mount) {
			return mount.getRat() != null && this.isAlliedTo(mount.getRat());
		} else if (livingentity != null && entity instanceof TamableAnimal animal) {
			return animal.isOwnedBy(livingentity);
		} else {
			return livingentity != null ? livingentity.isAlliedTo(entity) : super.isAlliedTo(entity);
		}
	}

	@Nullable
	public EntityType<?> getMountEntityType() {
		AtomicReference<EntityType<?>> type = new AtomicReference<>(null);
		RatUpgradeUtils.forEachUpgrade(this, item -> item instanceof MountRatUpgradeItem, (stack, slot) -> type.set(((MountRatUpgradeItem<?>) stack.getItem()).getEntityType()));
		return type.get();
	}

	public double getRatDistanceCenterSq(double x, double y, double z) {
		double d0 = this.getX() - x - 0.5D;
		double d1 = this.getY() - y - 0.5D;
		double d2 = this.getZ() - z - 0.5D;
		if (this.getVehicle() != null && getMountEntityType() != null && this.getVehicle().getType() == getMountEntityType()) {
			d0 = this.getVehicle().getX() - x - 0.5D;
			d1 = this.getVehicle().getY() - y - 0.5D;
			d2 = this.getVehicle().getZ() - z - 0.5D;
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

	@Override
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

	@Override
	public boolean canAttack(LivingEntity target) {
		return !this.isRidingSpecialMount() || !RatUtils.isRidingOrBeingRiddenBy(this, target);
	}

	public double getRatHarvestDistance(double expansion) {
		return (3.5F + expansion) * this.getRatDistanceModifier();
	}

	public double getRatDistanceModifier() {
		if (this.isRidingSpecialMount()) {
			Entity entity = this.getVehicle();
			if (entity != null) {
				if (entity instanceof RatBiplaneMount) {
					return 3.95D;
				}
				return 1.5D;
			}

		}
		return 1.0D;
	}

	public boolean hasFlightUpgrade() {
		return this.hasFlightUpgradeCached;
	}

	// Проверяет кеш улучшения работника; параметров нет, отдаёт true если у крысы есть рабочее улучшение.
	public boolean hasWorkerUpgrade() {
		return this.hasWorkerUpgradeCached;
	}
}
