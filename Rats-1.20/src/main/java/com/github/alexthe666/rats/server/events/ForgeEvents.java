package com.github.alexthe666.rats.server.events;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.data.tags.RatsEntityTags;
import com.github.alexthe666.rats.registry.*;
import com.github.alexthe666.rats.registry.worldgen.RatlantisDimensionRegistry;
import com.github.alexthe666.rats.server.block.RatHouseBlock;
import com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity;
import com.github.alexthe666.rats.server.capability.SelectedRat;
import com.github.alexthe666.rats.server.entity.misc.PlagueDoctor;
import com.github.alexthe666.rats.server.entity.projectile.PlagueShot;
import com.github.alexthe666.rats.server.entity.rat.DemonRat;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import com.github.alexthe666.rats.server.block.entity.RatQuarryBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.items.RatSackItem;
import com.github.alexthe666.rats.server.items.RatStaffItem;
import com.github.alexthe666.rats.server.message.DismountRatPacket;
import com.github.alexthe666.rats.server.message.ManageRatStaffPacket;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.message.SyncArmSwingPacket;
import com.github.alexthe666.rats.server.misc.RatTreeUtils;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import com.github.alexthe666.rats.server.misc.RatUtils;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import com.github.alexthe666.rats.server.world.PlagueDoctorSpawner;
import com.github.alexthe666.rats.server.world.RatProtectedTreeBlocksData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ModifyCustomSpawnersEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

@EventBusSubscriber(modid = RatsMod.MODID)
public class ForgeEvents {


	// Lifts a living entity along the side face of a rat_house; takes the entity, returns nothing.
	private static void applyRatHouseClimbAssist(LivingEntity living) {
		try {
			if (!living.horizontalCollision || living.isSpectator()) {
				return;
			}
			BlockPos housePos = findTouchedRatHouse(living);
			if (housePos == null) {
				return;
			}
			Vec3 movement = living.getDeltaMovement();
			double lift = living instanceof Player ? 0.2D : 0.16D;
			living.resetFallDistance();
			if (movement.y < lift) {
				living.setDeltaMovement(movement.x * 0.75D, lift, movement.z * 0.75D);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to handle entity {} climbing the rat house", living.getUUID(), exception);
			throw exception;
		}
	}

	// Finds a neighbouring house whose side face the entity bumped into; takes the entity, returns the house position or null.
	private static BlockPos findTouchedRatHouse(LivingEntity living) {
		try {
			BlockPos feet = living.blockPosition();
			for (int y = 0; y <= 1; y++) {
				BlockPos base = feet.above(y);
				for (Direction direction : Direction.Plane.HORIZONTAL) {
					BlockPos candidate = base.relative(direction);
					if (living.level().getBlockState(candidate).getBlock() instanceof RatHouseBlock && isBesideRatHouse(living, candidate)) {
						return candidate;
					}
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to find a house next to entity {}", living.getUUID(), exception);
			throw exception;
		}
	}

	// Checks the side shell of the house; takes the entity and house position, returns true if the entity stands at a side face.
	private static boolean isBesideRatHouse(LivingEntity living, BlockPos housePos) {
		try {
			double localX = living.getX() - housePos.getX();
			double localZ = living.getZ() - housePos.getZ();
			boolean nearHorizontalBounds = localX >= -0.45D && localX <= 1.45D && localZ >= -0.45D && localZ <= 1.45D;
			boolean outsideBody = localX < 0.05D || localX > 0.95D || localZ < 0.05D || localZ > 0.95D;
			boolean nearHeight = living.getY() >= housePos.getY() - 0.35D && living.getY() <= housePos.getY() + 1.85D;
			return nearHorizontalBounds && outsideBody && nearHeight;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check side face of house {} for entity {}", housePos, living.getUUID(), exception);
			throw exception;
		}
	}

	@SubscribeEvent
	public static void onPlayerInteractWithEntity(PlayerInteractEvent.EntityInteract event) {
		ItemStack heldItem = event.getEntity().getItemInHand(event.getHand());
		if (event.getTarget() instanceof Sheep sheep && sheep.getColor() != DyeColor.WHITE && event.getEntity().getItemInHand(event.getHand()).is(RatsBlockRegistry.DYE_SPONGE.get().asItem())) {
			sheep.setColor(DyeColor.WHITE);
			for (int i = 0; i < 8; i++) {
				double d0 = sheep.getRandom().nextGaussian() * 0.02D;
				double d1 = sheep.getRandom().nextGaussian() * 0.02D;
				double d2 = sheep.getRandom().nextGaussian() * 0.02D;
				sheep.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(RatsBlockRegistry.DYE_SPONGE.get())), sheep.getX() + (double) (sheep.getRandom().nextFloat() * sheep.getBbWidth() * 2.0F) - (double) sheep.getBbWidth(), sheep.getY() + (double) (sheep.getRandom().nextFloat() * sheep.getBbHeight() * 2.0F) - (double) sheep.getBbHeight(), sheep.getZ() + (double) (sheep.getRandom().nextFloat() * sheep.getBbWidth() * 2.0F) - (double) sheep.getBbWidth(), d0, d1, d2);
			}
			sheep.playSound(RatsSoundRegistry.DYE_SPONGE_USED.get(), 1.0F, 1.0F);

		}
		if (heldItem.is(RatsItemRegistry.PLAGUE_DOCTORATE.get())) {
			if (event.getTarget() instanceof Villager villager && !villager.isBaby() && (villager.getVillagerData().getProfession() == VillagerProfession.NITWIT || villager.getVillagerData().getProfession() == VillagerProfession.NONE)) {
				PlagueDoctor doctor = villager.convertTo(RatsEntityRegistry.PLAGUE_DOCTOR.get(), true);
				if (doctor != null) {
					doctor.setWillDespawn(false);
					event.getEntity().swing(event.getHand());
					event.getLevel().playSound(null, doctor.blockPosition(), RatsSoundRegistry.PLAGUE_DOCTOR_SUMMON.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
					if (!event.getEntity().isCreative()) {
						heldItem.shrink(1);
					}
				}
			}
		}
		if (event.getTarget() instanceof LivingEntity living && event.getEntity().hasEffect(BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(RatsEffectRegistry.PLAGUE.getKey()))) {
			living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(RatsEffectRegistry.PLAGUE.getKey()), 6000));
		}
	}

	public static int getProtectorCount(LivingEntity entity) {
		int protectors = 0;
		if (entity.getItemBySlot(EquipmentSlot.HEAD).is(RatlantisItemRegistry.RATLANTIS_HELMET.get())) {
			protectors++;
		}
		if (entity.getItemBySlot(EquipmentSlot.CHEST).is(RatlantisItemRegistry.RATLANTIS_CHESTPLATE.get())) {
			protectors++;
		}
		if (entity.getItemBySlot(EquipmentSlot.LEGS).is(RatlantisItemRegistry.RATLANTIS_LEGGINGS.get())) {
			protectors++;
		}
		if (entity.getItemBySlot(EquipmentSlot.FEET).is(RatlantisItemRegistry.RATLANTIS_BOOTS.get())) {
			protectors++;
		}
		return protectors;
	}

	@SubscribeEvent
	public static void piglinsDontAttackGoldRatsEver(LivingChangeTargetEvent event) {
		if (event.getEntity() instanceof Piglin) {
			if (event.getNewAboutToBeSetTarget() instanceof TamedRat rat && RatUpgradeUtils.hasUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_IDOL.get())) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public static void sculkedRatsDontEmitVibrations(VanillaGameEvent event) {
		if (event.getCause() instanceof TamedRat rat && RatUpgradeUtils.hasUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_SCULKED.get())) {
			event.setCanceled(true);
		}
	}


	// Intercepts the death of a rat bound to a house; takes the death event, starts the respawn timer or lets a regular death through.

	// Logs a rat death from falling; takes the death event, returns nothing.
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void logRatFallDeath(LivingDeathEvent event) {
		try {
			if (event.getEntity() instanceof TamedRat rat && event.getSource().is(DamageTypes.FALL)) {
				RatsMod.LOGGER.warn("Rat {} died from falling: fallDistance={}, position={}", rat.getUUID(), rat.fallDistance, rat.blockPosition());
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to log a rat fall death", exception);
			throw exception;
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void respawnRatAtHouse(LivingDeathEvent event) {
		try {
			if (!(event.getEntity() instanceof TamedRat rat) || rat.level().isClientSide() || RatUpgradeUtils.hasUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_ANGEL.get())) {
				return;
			}
			if (rat.getBoundHousePos().isEmpty() || !rat.getBoundHousePos().get().dimension().equals(rat.level().dimension())) {
				return;
			}
			BlockPos housePos = rat.getBoundHousePos().get().pos();
			BlockEntity blockEntity = rat.level().getBlockEntity(housePos);
			if (!(blockEntity instanceof RatHouseBlockEntity house)) {
				RatsMod.LOGGER.warn("Rat {} died bound to {}, but there is no house there", rat.getUUID(), housePos);
				return;
			}
			if (!house.beginRespawn(rat, 1200)) {
				return;
			}
			event.setCanceled(true);
			rat.playSound(RatsSoundRegistry.RAT_DIE.get());
			if (rat.getOwner() instanceof Player player) {
				player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_RESPAWN_STARTED, rat.getName()), true);
			}
			rat.discard();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to send the rat to respawn at the house", exception);
			throw exception;
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void spawnAngelRat(LivingDeathEvent event) {
		if (event.getEntity() instanceof TamedRat rat && RatUpgradeUtils.hasUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_ANGEL.get())) {
			event.setCanceled(true);
			rat.spawnAngelCopy();
			rat.playSound(RatsSoundRegistry.RAT_DIE.get());
			if (rat.getOwner() instanceof Player player) {
				player.sendSystemMessage(Component.translatable(RatsLangConstants.RAT_ANGEL_RESPAWN, rat.getName().getString()));
			}
			rat.discard();
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void ejectRatsOutOfSack(ItemExpireEvent event) {
		if (event.getEntity().getItem().is(RatsItemRegistry.RAT_SACK.get()) && RatSackItem.getRatsInSack(event.getEntity().getItem()) > 0) {
			RatSackItem.ejectRatsFromSack(event.getEntity().getItem(), event.getEntity().level(), event.getEntity().blockPosition());
		}
	}

	//complete hack workaround for rats not cooking food dropped by victims if they kill them in 1 shot
	@SubscribeEvent
	public static void hackySetFireFix(LivingIncomingDamageEvent event) {
		if (!event.getEntity().fireImmune()) {
			if (event.getSource().getEntity() instanceof TamedRat rat && RatUpgradeUtils.hasUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_DEMON.get())) {
				event.getEntity().igniteForSeconds(1);
			}
		}
	}

	@SubscribeEvent
	public static void checkIfPlagueCanApplyToMob(MobEffectEvent.Applicable event) {
		if (event.getEffectInstance().getEffect().is(RatsEffectRegistry.PLAGUE) && (!RatConfig.plagueSpread || event.getEntity().getType().is(RatsEntityTags.PLAGUE_IMMUNE))) {
			event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
		}
	}

	@SubscribeEvent
	public static void onHitEntity(LivingIncomingDamageEvent event) {
		if (event.getSource().getDirectEntity() instanceof LivingEntity living && living.hasEffect(BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(RatsEffectRegistry.PLAGUE.getKey()))) {
			living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(RatsEffectRegistry.PLAGUE.getKey()), 6000));
		}
		int protectors = getProtectorCount(event.getEntity());
		if (protectors > 0) {
			if (event.getSource() != null && event.getSource().getEntity() != null) {
				Entity trueSource = event.getSource().getEntity();
				if (trueSource.distanceTo(event.getEntity()) < 4.0D) {
					trueSource.hurt(event.getEntity().damageSources().magic(), 0.75F * protectors);
					Vec3 vec3d = trueSource.getDeltaMovement();
					double strength = 0.15D * protectors;
					Vec3 vec3d1 = (new Vec3(event.getEntity().getX() - trueSource.getX(), 0.0D, event.getEntity().getZ() - trueSource.getZ())).normalize().scale(strength);
					trueSource.setDeltaMovement(vec3d.x / 2.0D - vec3d1.x, trueSource.onGround() ? Math.min(0.4D, vec3d.y / 2.0D + strength) : vec3d.y, vec3d.z / 2.0D - vec3d1.z);
				}
			}
		}
	}

	@SubscribeEvent
	public static void addPlagueDoctorSpawning(ModifyCustomSpawnersEvent event) {
		if (event.getLevel().dimension().equals(Level.OVERWORLD)) {
			event.addCustomSpawner(new PlagueDoctorSpawner(event.getLevel()));
		}
	}

	@SubscribeEvent
	public static void maybeSendPlayerWarning(PlayerEvent.PlayerLoggedInEvent event) {
		if (!event.getEntity().level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
			CompoundTag playerData = event.getEntity().getPersistentData();
			CompoundTag data = playerData.getCompound(Player.PERSISTED_NBT_TAG);
			if (!data.getBoolean("rats_griefing_warning")) {
				event.getEntity().displayClientMessage(Component.translatable(RatsLangConstants.MOB_GRIEFING_WARNING).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
				data.putBoolean("rats_griefing_warning", true);
				playerData.put(Player.PERSISTED_NBT_TAG, data);
			}
		}
	}

	@SubscribeEvent
	public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
		if (event.getEntity() instanceof TamedRat rat && !event.getLevel().isClientSide()) {
			rat.tryReloadPendingSaveData();
			rat.scheduleUpgradeRefreshAfterLoad();
		}
		if (event.getEntity() != null && event.getEntity() instanceof IronGolem golem && RatConfig.golemsTargetRats) {
			golem.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(golem, Rat.class, true, RatUtils.UNTAMED_RAT_SELECTOR));
		}

		if (event.getEntity() != null && RatUtils.isPredator(event.getEntity()) && event.getEntity() instanceof Animal animal) {
			animal.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(animal, Rat.class, true, RatUtils.UNTAMED_RAT_SELECTOR));
		}

		if (event.getEntity() != null && event.getEntity() instanceof Husk husk) {
			if (husk.getRandom().nextFloat() < 0.12F) {
				husk.setItemSlot(EquipmentSlot.HEAD, new ItemStack(RatsItemRegistry.ARCHEOLOGIST_HAT.get()));
				husk.setDropChance(EquipmentSlot.HEAD, 1.0F);
			}
		}
		if (event.getEntity() != null && event.getEntity() instanceof AbstractSkeleton skele && event.getEntity().level().getBiome(event.getEntity().blockPosition()).is(BiomeTags.IS_JUNGLE)) {
			if (skele.getRandom().nextFloat() < 0.25F) {
				skele.setItemSlot(EquipmentSlot.HEAD, new ItemStack(RatsItemRegistry.ARCHEOLOGIST_HAT.get()));
				skele.setDropChance(EquipmentSlot.HEAD, 1.0F);
			}
		}
	}

	@SubscribeEvent
	public static void spawnStriderJockeys(FinalizeSpawnEvent event) {
		//Carry On passes null into their difficulty when firing this event so we have to check this unfortunately. They shouldnt be doing this.
		if (event.getDifficulty() == null) return;
		if (event.getDifficulty().getDifficulty() != Difficulty.PEACEFUL && (event.getSpawnType() == MobSpawnType.CHUNK_GENERATION || event.getSpawnType() == MobSpawnType.NATURAL)) {
			if (event.getEntity() instanceof Strider strider && event.getEntity().getType() == EntityType.STRIDER) {
				if (!strider.isBaby() && !strider.isSaddled() && strider.getPassengers().isEmpty() && strider.getRandom().nextFloat() < 0.1F) {
					DemonRat demonRat = RatsEntityRegistry.DEMON_RAT.get().create(event.getLevel().getLevel());
					if (demonRat != null) {
						demonRat.moveTo(strider.getX(), strider.getY(), strider.getZ(), strider.getYRot(), 0.0F);
						EventHooks.finalizeMobSpawn(demonRat, event.getLevel(), event.getDifficulty(), MobSpawnType.JOCKEY, null);
						demonRat.startRiding(strider, true);
						demonRat.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WARPED_FUNGUS_ON_A_STICK));
						strider.equipSaddle(ItemStack.EMPTY, SoundSource.NEUTRAL);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void makeCentipede(PlayerInteractEvent.RightClickBlock event) {
		if (event.getItemStack().is(RatsItemRegistry.LITTLE_BLACK_WORM.get()) && event.getLevel().getBlockState(event.getHitVec().getBlockPos()).is(Blocks.COARSE_DIRT)) {
			event.getItemStack().shrink(1);
			ItemStack centipede = new ItemStack(RatsItemRegistry.CENTIPEDE.get());
			if (!event.getEntity().addItem(centipede.copy())) {
				event.getEntity().drop(centipede, false);
			}
			event.getEntity().swing(event.getHand());
			event.getLevel().setBlockAndUpdate(event.getHitVec().getBlockPos(), Blocks.DIRT.defaultBlockState());
			event.getLevel().playSound(null, event.getHitVec().getBlockPos(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 2.0F, 1.0F);
		}
	}

	@SubscribeEvent
	public static void ratlantisFishing(ItemFishedEvent event) {
		FishingHook hook = event.getHookEntity();
		Player player = event.getEntity();
		Level level = player.level();
		if (RatsMod.RATLANTIS_DATAPACK_ENABLED && level.dimension().equals(RatlantisDimensionRegistry.DIMENSION_KEY)) {
			event.getDrops().clear();
			LootParams.Builder builder = (new LootParams.Builder((ServerLevel) level))
					.withParameter(LootContextParams.ORIGIN, hook.position())
					.withParameter(LootContextParams.TOOL, checkHandsForRod(player))
					.withParameter(LootContextParams.ATTACKING_ENTITY, player)
					.withParameter(LootContextParams.THIS_ENTITY, hook)
					.withLuck(player.getLuck());
			ResourceKey<LootTable> ratlantisFish = ResourceKey.create(Registries.LOOT_TABLE, RatsLootRegistry.RATLANTIS_FISH);
			LootTable loottable = level.getServer().reloadableRegistries().getLootTable(ratlantisFish);
			List<ItemStack> list = loottable.getRandomItems(builder.create(LootContextParamSets.FISHING));
			event.getDrops().addAll(list);
		}
	}

	private static ItemStack checkHandsForRod(Player player) {
		if (player.getMainHandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
			return player.getMainHandItem();
		} else if (player.getOffhandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
			return player.getOffhandItem();
		}
		return ItemStack.EMPTY;
	}

	@SubscribeEvent
	public static void initVillagerTrades(VillagerTradesEvent event) {
		//1-2, 5-10, 10-20, 15, 30
		if (event.getType() == RatsVillagerRegistry.PET_SHOP_OWNER.get()) {
			event.getTrades().get(1).add(new VillagerTrades.EmeraldForItems(Items.BONE, 12, 12, 1));
			event.getTrades().get(1).add(new VillagerTrades.EmeraldForItems(Items.ROTTEN_FLESH, 11, 12, 1));
			event.getTrades().get(1).add(new VillagerTrades.EmeraldForItems(Items.COD, 10, 12, 1));
			event.getTrades().get(1).add(new VillagerTrades.EmeraldForItems(Items.STRING, 12, 12, 1));
			event.getTrades().get(1).add(new VillagerTrades.EmeraldForItems(Items.EGG, 16, 12, 1));
			event.getTrades().get(1).add(new VillagerTrades.EmeraldForItems(RatsItemRegistry.RAW_RAT.get(), 11, 12, 1));
			event.getTrades().get(1).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.CHEESE.get(), 1, 5, 1));

			event.getTrades().get(2).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.COOKED_RAT.get(), 1, 5, 5));
			event.getTrades().get(2).add(new VillagerTrades.ItemsAndEmeraldsToItems(RatsBlockRegistry.GARBAGE_PILE.get().asItem(), 10, 3, RatsItemRegistry.PLASTIC_WASTE.get(), 10, 12, 5, 0.05F));
			event.getTrades().get(2).add(new VillagerTrades.ItemsForEmeralds(RatsBlockRegistry.MARBLED_CHEESE_RAW.get().asItem(), 1, 8, 5));
			event.getTrades().get(2).add(new VillagerTrades.ItemsForEmeralds(RatsBlockRegistry.GARBAGE_PILE.get().asItem(), 1, 6, 5));
			event.getTrades().get(2).add(new VillagerTrades.EmeraldForItems(RatsItemRegistry.RAW_PLASTIC.get(), 5, 12, 5));
			event.getTrades().get(2).add(new VillagerTrades.EmeraldForItems(RatsItemRegistry.RAT_FLUTE.get(), 1, 12, 5));

			event.getTrades().get(3).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.ARCHEOLOGIST_HAT.get(), 12, 1, 10));
			event.getTrades().get(3).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.SANTA_HAT.get(), 20, 1, 15));
			event.getTrades().get(3).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.PARTY_HAT.get(), 20, 1, 15));
			event.getTrades().get(3).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.HALO_HAT.get(), 8, 1, 10));
			event.getTrades().get(3).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.EXTERMINATOR_HAT.get(), 14, 1, 10));

			event.getTrades().get(4).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.RAT_UPGRADE_BASIC.get(), 5, 1, 20));
			event.getTrades().get(4).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.RAT_UPGRADE_JURY_RIGGED.get(), 16, 1, 20));
			event.getTrades().get(4).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get(), 8, 1, 20));
			event.getTrades().get(4).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.RAT_UPGRADE_WHITELIST.get(), 8, 1, 20));
			event.getTrades().get(4).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.RAT_UPGRADE_SPEED.get(), 10, 1, 20));
			event.getTrades().get(4).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.RAT_UPGRADE_HEALTH.get(), 10, 1, 20));
			event.getTrades().get(4).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.RAT_UPGRADE_ARMOR.get(), 10, 1, 20));

			event.getTrades().get(5).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.PLAGUE_DOCTORATE.get(), 10, 1, 30));
			event.getTrades().get(5).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.PLAGUE_TOME.get(), 32, 1, 30));
			event.getTrades().get(5).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.RAT_PAPERS.get(), 5, 1, 30));
			event.getTrades().get(5).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.CHARGED_CREEPER_CHUNK.get(), 3, 1, 30));
			event.getTrades().get(5).add(new VillagerTrades.ItemsForEmeralds(RatsItemRegistry.TINY_COIN.get(), 1, 8, 30));

		}
	}

	@SubscribeEvent
	public static void onPlayerLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
		if (event.getLevel().isClientSide()) {
			if (event.getEntity().isShiftKeyDown() && !event.getEntity().getPassengers().isEmpty()) {
				for (Entity passenger : event.getEntity().getPassengers()) {
					if (passenger instanceof TamedRat) {
						passenger.stopRiding();
						Vec3 dismountPos = passenger.getDismountLocationForPassenger(event.getEntity());
						passenger.setPos(dismountPos.x(), dismountPos.y(), dismountPos.z());
						RatsNetworkHandler.CHANNEL.sendToServer(new DismountRatPacket(passenger.getId()));
					}
				}
			}
			ItemStack swingStack = event.getItemStack();
			handleArmSwing(swingStack, event.getEntity());
			if (!swingStack.isEmpty()) {
				RatsNetworkHandler.CHANNEL.sendToServer(new SyncArmSwingPacket(swingStack));
			}
		}
	}

	@SubscribeEvent
	public static void onLivingHurt(LivingDamageEvent.Pre event) {
		if (event.getEntity() instanceof Player) {
			List<TamedRat> list = event.getEntity().level().getEntitiesOfClass(TamedRat.class, event.getEntity().getBoundingBox().inflate(RatConfig.ratVoodooDistance), rat -> rat.isTame() && rat.isOwnedBy(event.getEntity()) && !rat.isInvulnerable() && !rat.isInvulnerableTo(event.getSource()) && RatUpgradeUtils.hasUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_VOODOO.get()));
			if (!list.isEmpty()) {
				float damage = event.getNewDamage() / list.size();
				event.setNewDamage(0.0F);
				for (TamedRat rat : list) {
					rat.hurt(event.getSource(), damage);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onLivingHeal(LivingHealEvent event) {
		if (event.getEntity().hasEffect(BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(RatsEffectRegistry.PLAGUE.getKey()))) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onLivingUpdate(EntityTickEvent.Post event) {
		if (event.getEntity() instanceof LivingEntity living) {
			applyRatHouseClimbAssist(living);
		}
		if (event.getEntity() instanceof LivingEntity living && living.level().isClientSide() && living.hasEffect(BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(RatsEffectRegistry.PLAGUE.getKey()))) {
			RandomSource rand = event.getEntity().getRandom();
			if (rand.nextInt(4) == 0) {
				int entitySize = 1;
				if (living.getBoundingBox().getSize() > 0) {
					entitySize = Math.max(1, (int) living.getBoundingBox().getSize());
				}
				for (int i = 0; i < entitySize; i++) {
					float motionX = rand.nextFloat() * 0.1F - 0.05F;
					float motionZ = rand.nextFloat() * 0.1F - 0.05F;

					living.level().addParticle(RatsParticleRegistry.FLEA.get(),
							living.getX() + (double) (rand.nextFloat() * living.getBbWidth() * 2F) - (double) living.getBbWidth(),
							living.getY() + (double) (rand.nextFloat() * living.getBbHeight()),
							living.getZ() + (double) (rand.nextFloat() * living.getBbWidth() * 2F) - (double) living.getBbWidth(),
							motionX, 0.0F, motionZ);
				}
			}
		}
	}


	@SubscribeEvent
	public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
		ItemStack stack = event.getEntity().getItemInHand(event.getHand());
		if (stack.getItem() instanceof RatStaffItem staff) {
			event.setUseBlock(TriState.FALSE);
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
			TamedRat rat = null;
			SelectedRat selectedRat = event.getEntity().getCapability(RatsCapabilityRegistry.SELECTED_RAT);
			if (selectedRat != null) {
				rat = selectedRat.getSelectedRat();
			}
			if (rat != null) {
				event.getEntity().swing(event.getHand());
				if (!event.getLevel().isClientSide()) {
					RatsNetworkHandler.CHANNEL.send(RatsNetworkHandler.playerTarget(() -> (ServerPlayer) event.getEntity()), new ManageRatStaffPacket(rat.getId(), event.getPos(), event.getFace().ordinal(), false, true, staff.getStaff(stack)));
				}
			} else {
				event.getEntity().displayClientMessage(Component.translatable(RatsLangConstants.RAT_STAFF_NO_RAT).withStyle(ChatFormatting.RED), true);
			}
		}
	}

	// Visits quarries in neighbouring chunks; takes the level, position and handler, returns nothing.
	private static void forNearbyQuarries(Level level, BlockPos pos, java.util.function.Consumer<RatQuarryBlockEntity> consumer) {
		int chunkX = pos.getX() >> 4;
		int chunkZ = pos.getZ() >> 4;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (!level.hasChunk(chunkX + dx, chunkZ + dz)) {
					continue;
				}
				LevelChunk chunk = level.getChunk(chunkX + dx, chunkZ + dz);
				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					if (blockEntity instanceof RatQuarryBlockEntity quarry) {
						consumer.accept(quarry);
					}
				}
			}
		}
	}

	// Marks blocks placed by a player in the quarry area so rats do not mine them; takes the block place event, returns nothing.
	@SubscribeEvent
	public static void onQuarryPlayerBlockPlace(BlockEvent.EntityPlaceEvent event) {
		try {
			if (event.getLevel().isClientSide()) {
				return;
			}
			Level level = (Level) event.getLevel();
			BlockPos placedPos = event.getPos();
			forNearbyQuarries(level, placedPos, quarry -> quarry.markPlayerPlacedBlock(level, placedPos));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to handle a player placing a block in the quarry at {}", event.getPos(), exception);
			throw exception;
		}
	}

	// Marks player-placed logs and leaves so the lumberjack leaves them alone; takes the block place event, returns nothing.
	@SubscribeEvent
	public static void onTreeBlockPlacedByPlayer(BlockEvent.EntityPlaceEvent event) {
		try {
			if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player)) {
				return;
			}
			BlockState placedState = event.getPlacedBlock();
			if (!RatTreeUtils.isTreeLog(placedState) && !RatTreeUtils.isTreeLeaf(placedState)) {
				return;
			}
			Level level = (Level) event.getLevel();
			RatProtectedTreeBlocksData data = RatProtectedTreeBlocksData.get(level);
			if (data == null) {
				throw new IllegalStateException("The protected wood block list is not available on the server");
			}
			data.protect(event.getPos());
			RatsMod.LOGGER.debug("Block {} at {} is protected from the lumberjack as player-placed", placedState.getBlock(), event.getPos());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to mark player wood block {}", event.getPos(), exception);
			throw exception;
		}
	}

	// Prevents breaking the indestructible quarry ladder; takes the block break event, returns nothing.
	@SubscribeEvent
	public static void onQuarryLadderBreak(BlockEvent.BreakEvent event) {
		try {
			if (event.getLevel().isClientSide()) {
				return;
			}
			Level level = (Level) event.getLevel();
			BlockPos brokenPos = event.getPos();
			forNearbyQuarries(level, brokenPos, quarry -> {
				if (quarry.isQuarryLadderProtected(level, brokenPos)) {
					event.setCanceled(true);
					RatsMod.LOGGER.warn("Quarry {} prevented breaking the protected ladder at {}", quarry.getBlockPos(), brokenPos);
				}
			});
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to handle quarry ladder protection at {}", event.getPos(), exception);
			throw exception;
		}
	}

	// Clears the player block mark after breaking; takes the block break event, returns nothing.
	@SubscribeEvent
	public static void onQuarryPlayerBlockBreak(BlockEvent.BreakEvent event) {
		try {
			if (event.getLevel().isClientSide() || event.isCanceled()) {
				return;
			}
			Level level = (Level) event.getLevel();
			BlockPos brokenPos = event.getPos();
			forNearbyQuarries(level, brokenPos, quarry -> quarry.clearPlayerPlacedBlock(brokenPos));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to handle a player breaking a block in the quarry at {}", event.getPos(), exception);
			throw exception;
		}
	}

	// Removes lumberjack protection after logs or leaves are broken; takes the block break event, returns nothing.
	@SubscribeEvent
	public static void onProtectedTreeBlockBreak(BlockEvent.BreakEvent event) {
		try {
			if (event.getLevel().isClientSide() || event.isCanceled()) {
				return;
			}
			BlockState brokenState = event.getState();
			if (!RatTreeUtils.isTreeLog(brokenState) && !RatTreeUtils.isTreeLeaf(brokenState)) {
				return;
			}
			Level level = (Level) event.getLevel();
			RatProtectedTreeBlocksData data = RatProtectedTreeBlocksData.get(level);
			if (data == null) {
				throw new IllegalStateException("The protected wood block list is not available on the server");
			}
			data.unprotect(event.getPos());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to remove protection of wood block {}", event.getPos(), exception);
			throw exception;
		}
	}

	public static void handleArmSwing(ItemStack stack, Player player) {
		if (stack.is(RatsItemRegistry.PLAGUE_SCYTHE.get())) {
			if (player.swingTime == 0 && !player.isSpectator()) {
				ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
				double totalDmg = 0;
				for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
					if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
						totalDmg += entry.modifier().amount();
					}
				}
				player.playSound(RatsSoundRegistry.PLAGUE_CLOUD_SHOOT.get(), 1, 1);
				PlagueShot shot = new PlagueShot(RatsEntityRegistry.PLAGUE_SHOT.get(), player.level(), player, totalDmg * 0.5F);
				Vec3 vec3 = player.getViewVector(1.0F);

				shot.shoot(vec3.x(), vec3.y(), vec3.z(), 1.0F, 0.5F);
				player.level().addFreshEntity(shot);
			}
		}
	}

}
