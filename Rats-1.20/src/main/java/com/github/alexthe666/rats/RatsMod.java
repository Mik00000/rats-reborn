package com.github.alexthe666.rats;

import com.github.alexthe666.rats.registry.*;
import com.github.alexthe666.rats.registry.worldgen.RatlantisFeatureRegistry;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod(RatsMod.MODID)
public class RatsMod {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final String MODID = "rats";
	public static final Rarity RATLANTIS_SPECIAL = Rarity.EPIC;
	public static final MobCategory RATS = MobCategory.CREATURE;

	public static final BlockSetType PIRAT_WOOD_SET = BlockSetType.register(new BlockSetType(ResourceLocation.fromNamespaceAndPath(MODID, "pirat").toString(), true, true, true, BlockSetType.PressurePlateSensitivity.EVERYTHING, SoundType.NETHER_WOOD, SoundEvents.NETHER_WOOD_DOOR_CLOSE, SoundEvents.NETHER_WOOD_DOOR_OPEN, SoundEvents.NETHER_WOOD_TRAPDOOR_CLOSE, SoundEvents.NETHER_WOOD_TRAPDOOR_OPEN, SoundEvents.NETHER_WOOD_PRESSURE_PLATE_CLICK_OFF, SoundEvents.NETHER_WOOD_PRESSURE_PLATE_CLICK_ON, SoundEvents.NETHER_WOOD_BUTTON_CLICK_OFF, SoundEvents.NETHER_WOOD_BUTTON_CLICK_ON));
	public static final WoodType PIRAT_WOOD_TYPE = WoodType.register(new WoodType(ResourceLocation.fromNamespaceAndPath(MODID, "pirat").toString(), PIRAT_WOOD_SET, SoundType.NETHER_WOOD, SoundType.NETHER_WOOD_HANGING_SIGN, SoundEvents.NETHER_WOOD_FENCE_GATE_CLOSE, SoundEvents.NETHER_WOOD_FENCE_GATE_OPEN));

	public static final ResourceKey<StructureProcessorList> EMPTY_PROCESSOR = ResourceKey.create(Registries.PROCESSOR_LIST, ResourceLocation.withDefaultNamespace("empty"));

	public static final GameRules.Key<GameRules.BooleanValue> SPAWN_RATS = GameRules.register("doRatSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SPAWN_PIPERS = GameRules.register("doPiperSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SPAWN_PLAGUE_DOCTORS = GameRules.register("doPlagueDoctorSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));

	public static boolean ICEANDFIRE_LOADED;
	public static boolean CREATE_LOADED;
	public static boolean FARMERS_DELIGHT_LOADED;
	public static boolean RATLANTIS_DATAPACK_ENABLED = false;
	public static final List<Item> RATLANTIS_ITEMS = new ArrayList<>();
	public static final Set<Item> RATLANTIS_SURVIVAL_ITEMS = new HashSet<>();
	private static final List<Pair<String, Component>> MOB_CACHE = new ArrayList<>();

	// Создаёт главный объект мода; принимает шину событий и контейнер мода, регистрирует реестры и совместимости.
	public RatsMod(IEventBus bus, ModContainer modContainer) {
		ICEANDFIRE_LOADED = ModList.get().isLoaded("iceandfire");
		CREATE_LOADED = ModList.get().isLoaded("create");
		FARMERS_DELIGHT_LOADED = ModList.get().isLoaded("farmersdelight");
		modContainer.registerConfig(ModConfig.Type.CLIENT, ConfigHolder.CLIENT_SPEC);
		modContainer.registerConfig(ModConfig.Type.COMMON, ConfigHolder.SERVER_SPEC);
		//melk
		NeoForgeMod.enableMilkFluid();

		RatVariantRegistry.RAT_VARIANTS.register(bus);

		RatsBannerPatternRegistry.PATTERNS.register(bus);
		RatsBlockRegistry.BLOCKS.register(bus);
		RatsBlockEntityRegistry.BLOCK_ENTITIES.register(bus);
		RatsEntityRegistry.ENTITIES.register(bus);
		RatsItemRegistry.ITEMS.register(bus);
		RatsDataSerializerRegistry.DATA_SERIALIZERS.register(bus);
		RatsEffectRegistry.MOB_EFFECTS.register(bus);
		RatsLootRegistry.CONDITIONS.register(bus);
		RatsLootRegistry.LOOT_MODIFIERS.register(bus);
		RatsMenuRegistry.MENUS.register(bus);
		RatsParticleRegistry.PARTICLES.register(bus);
		RatsVillagerRegistry.POIS.register(bus);
		RatsVillagerRegistry.PROFESSIONS.register(bus);
		RatsRecipeRegistry.RECIPES.register(bus);
		RatsRecipeRegistry.SERIALIZERS.register(bus);
		RatsSoundRegistry.SOUNDS.register(bus);
		RatsCreativeTabRegistry.TABS.register(bus);

		RatlantisBlockRegistry.BLOCKS.register(bus);
		RatlantisBlockEntityRegistry.BLOCK_ENTITIES.register(bus);
		RatlantisFeatureRegistry.CARVERS.register(bus);
		RatlantisEntityRegistry.ENTITIES.register(bus);
		RatlantisFeatureRegistry.FEATURES.register(bus);
		RatlantisItemRegistry.ITEMS.register(bus);
		RatlantisFeatureRegistry.PROCESSORS.register(bus);
		RatlantisFeatureRegistry.TRUNK_PLACERS.register(bus);

		bus.addListener(RatsCapabilityRegistry::registerCapabilities);
		bus.addListener(RatsNetworkHandler::registerPayloads);
		bus.addListener(this::reloadConfigs);
		bus.addListener(this::setup);
		NeoForge.EVENT_BUS.addListener(this::addPetShops);
		bus.addListener(this::addRatlantisDatapack);
	}

	/**
	 * Регистрирует встроенный набор данных Ratlantis для экрана создания мира.
	 * Параметры: {@code event} — событие NeoForge при сборке репозитория паков.
	 * Путь должен совпадать с каталогом в JAR: {@code data/minecraft/datapacks/ratlantis}.
	 */
	public void addRatlantisDatapack(AddPackFindersEvent event) {
		// Ratlantis по умолчанию не подключается: измерение убрано, контент перенесён во вкладку Rats.
	}

	public void reloadConfigs(ModConfigEvent event) {
		if (event.getConfig().getSpec() == ConfigHolder.SERVER_SPEC) {
			RatConfig.bakeServer();
			LOGGER.debug("Reloading Rats Server Config!");
		}
		if (event.getConfig().getSpec() == ConfigHolder.CLIENT_SPEC) {
			RatConfig.bakeClient();
			LOGGER.debug("Reloading Rats Client Config!");
		}
	}

	private void setup(FMLCommonSetupEvent event) {
		RatsNetworkHandler.init();
		RatsUpgradeConflictRegistry.init();
		event.enqueueWork(() -> {
			RatsCauldronRegistry.init();
			RatsDispenserRegistry.init();

			FlowerPotBlock pot = (FlowerPotBlock) Blocks.FLOWER_POT;
			pot.addPlant(RatlantisBlockRegistry.RATGLOVE_FLOWER.getId(), RatlantisBlockRegistry.POTTED_RATGLOVE_FLOWER);
			pot.addPlant(RatlantisBlockRegistry.PIRAT_SAPLING.getId(), RatlantisBlockRegistry.POTTED_PIRAT_SAPLING);
		});
		//wooooo caches ftw
		if (RATLANTIS_ITEMS.isEmpty()) {
			RatlantisItemRegistry.ITEMS.getEntries().forEach(item -> RATLANTIS_ITEMS.add(item.get()));
		}
		if (RATLANTIS_SURVIVAL_ITEMS.isEmpty()) {
			registerRatlantisSurvivalItems(
					RatlantisBlockRegistry.ORATCHALCUM_ORE.get().asItem(),
					RatlantisBlockRegistry.ORATCHALCUM_BLOCK.get().asItem(),
					RatlantisItemRegistry.RAW_ORATCHALCUM.get(),
					RatlantisItemRegistry.ORATCHALCUM_INGOT.get(),
					RatlantisItemRegistry.ORATCHALCUM_NUGGET.get(),
					RatlantisItemRegistry.RATLANTIS_HELMET.get(),
					RatlantisItemRegistry.RATLANTIS_CHESTPLATE.get(),
					RatlantisItemRegistry.RATLANTIS_LEGGINGS.get(),
					RatlantisItemRegistry.RATLANTIS_BOOTS.get(),
					RatlantisItemRegistry.RATLANTIS_SWORD.get(),
					RatlantisItemRegistry.RATLANTIS_PICKAXE.get(),
					RatlantisItemRegistry.RATLANTIS_AXE.get(),
					RatlantisItemRegistry.RATLANTIS_SHOVEL.get(),
					RatlantisItemRegistry.RATLANTIS_HOE.get(),
					RatlantisItemRegistry.RATLANTIS_BOW.get(),
					RatlantisItemRegistry.MILITARY_HAT.get(),
					RatlantisItemRegistry.BIPLANE_WING.get(),
					RatlantisItemRegistry.RAT_UPGRADE_BIPLANE_MOUNT.get()
			);
		}
	}

	// Регистрирует предметы Ratlantis, оставшиеся в выживании с обычными подсказками.
	private static void registerRatlantisSurvivalItems(Item... items) {
		for (Item item : items) {
			RATLANTIS_SURVIVAL_ITEMS.add(item);
		}
	}

	//code take from TelepathicGrunt's gist: https://gist.github.com/TelepathicGrunt/4fdbc445ebcbcbeb43ac748f4b18f342
	//1.18.2 version used and modified so it works in 1.19.4
	public void addPetShops(ServerAboutToStartEvent event) {
		Registry<StructureTemplatePool> templatePoolRegistry = event.getServer().registryAccess().registry(Registries.TEMPLATE_POOL).orElseThrow();
		Registry<StructureProcessorList> processorListRegistry = event.getServer().registryAccess().registry(Registries.PROCESSOR_LIST).orElseThrow();

		if (RatConfig.villagePetShops) {
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/plains/houses"), "rats:pet_shops/plains", RatConfig.villagePetShopWeight, ProcessorLists.MOSSIFY_10_PERCENT);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/snowy/houses"), "rats:pet_shops/snowy", RatConfig.villagePetShopWeight, EMPTY_PROCESSOR);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/savanna/houses"), "rats:pet_shops/savanna", RatConfig.villagePetShopWeight, EMPTY_PROCESSOR);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/taiga/houses"), "rats:pet_shops/taiga", RatConfig.villagePetShopWeight, ProcessorLists.MOSSIFY_10_PERCENT);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/desert/houses"), "rats:pet_shops/desert", RatConfig.villagePetShopWeight, EMPTY_PROCESSOR);

			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/plains/zombie/houses"), "rats:pet_shops/zombie_plains", RatConfig.zombieVillagePetShopWeight, ProcessorLists.ZOMBIE_PLAINS);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/snowy/zombie/houses"), "rats:pet_shops/zombie_snowy", RatConfig.zombieVillagePetShopWeight, ProcessorLists.ZOMBIE_SNOWY);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/savanna/zombie/houses"), "rats:pet_shops/zombie_savanna", RatConfig.zombieVillagePetShopWeight, ProcessorLists.ZOMBIE_SAVANNA);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/taiga/zombie/houses"), "rats:pet_shops/zombie_taiga", RatConfig.zombieVillagePetShopWeight, ProcessorLists.ZOMBIE_TAIGA);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/desert/zombie/houses"), "rats:pet_shops/zombie_desert", RatConfig.zombieVillagePetShopWeight, ProcessorLists.ZOMBIE_DESERT);
		}

		if (RatConfig.villageGarbageHeaps) {
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/plains/houses"), "rats:garbage_heaps/plains", RatConfig.villageGarbageHeapWeight, ProcessorLists.MOSSIFY_10_PERCENT);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/snowy/houses"), "rats:garbage_heaps/snowy", RatConfig.villageGarbageHeapWeight, EMPTY_PROCESSOR);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/savanna/houses"), "rats:garbage_heaps/savanna", RatConfig.villageGarbageHeapWeight, EMPTY_PROCESSOR);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/taiga/houses"), "rats:garbage_heaps/taiga", RatConfig.villageGarbageHeapWeight, ProcessorLists.MOSSIFY_10_PERCENT);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/desert/houses"), "rats:garbage_heaps/desert", RatConfig.villageGarbageHeapWeight, EMPTY_PROCESSOR);

			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/plains/zombie/houses"), "rats:garbage_heaps/plains", RatConfig.zombieVillageGarbageHeapWeight, ProcessorLists.ZOMBIE_PLAINS);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/snowy/zombie/houses"), "rats:garbage_heaps/snowy", RatConfig.zombieVillageGarbageHeapWeight, ProcessorLists.ZOMBIE_SNOWY);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/savanna/zombie/houses"), "rats:garbage_heaps/savanna", RatConfig.zombieVillageGarbageHeapWeight, ProcessorLists.ZOMBIE_SAVANNA);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/taiga/zombie/houses"), "rats:garbage_heaps/taiga", RatConfig.zombieVillageGarbageHeapWeight, ProcessorLists.ZOMBIE_TAIGA);
			this.addBuildingToPool(templatePoolRegistry, processorListRegistry, ResourceLocation.parse("minecraft:village/desert/zombie/houses"), "rats:garbage_heaps/desert", RatConfig.zombieVillageGarbageHeapWeight, ProcessorLists.ZOMBIE_DESERT);
		}
	}

	//code take from TelepathicGrunt's gist: https://gist.github.com/TelepathicGrunt/4fdbc445ebcbcbeb43ac748f4b18f342
	//1.18.2 version used, and modified, so it works in 1.19.4
	//additions: a StructureProcessorList parameter to allow us to add a processor. (original code always used an empty processor, but some houses actually use processors)
	private void addBuildingToPool(Registry<StructureTemplatePool> templatePoolRegistry, Registry<StructureProcessorList> processorListRegistry, ResourceLocation poolRL, String nbtPieceRL, int weight, ResourceKey<StructureProcessorList> processor) {
		// port 1.21: вставка в пулы деревень переносится в JSON worldgen/template_pool
		LOGGER.debug("Rats: пропуск runtime-вставки {} в {} (порт 1.21)", nbtPieceRL, poolRL);
	}

	/**
	 * Кэш id мобов для GUI фильтра: без создания сущностей (избегаем крашей сторонних модов при entityType.create).
	 * Параметры: уровень (не null на клиенте с миром). Отдаёт отсортированный список пар id + имя.
	 */
	public static List<Pair<String, Component>> getCachedMobList(@Nullable Level level) {
		if (level == null) {
			return List.of();
		}
		if (MOB_CACHE.isEmpty()) {
			List<Pair<String, Component>> unsortedCache = new ArrayList<>();
			for (var holder : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.holders().toList()) {
				try {
					EntityType<?> entityType = holder.value();
					if (entityType == EntityType.PLAYER) {
						continue;
					}
					MobCategory category = entityType.getCategory();
					if (category == MobCategory.MISC) {
						continue;
					}
					unsortedCache.add(Pair.of(holder.key().location().toString(), entityType.getDescription()));
				} catch (Throwable e) {
					LOGGER.error("Не удалось добавить тип сущности {} в кэш фильтра мобов: {}", holder.key().location(), e.getMessage(), e);
				}
			}
			MOB_CACHE.addAll(unsortedCache.stream().sorted(Comparator.comparing(o -> o.getSecond().getString())).toList());
			LOGGER.debug("В кэш фильтра мобов записано {} id.", MOB_CACHE.size());
		}
		return MOB_CACHE;
	}
}
