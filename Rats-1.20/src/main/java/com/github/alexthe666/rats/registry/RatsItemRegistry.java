/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.food.FoodProperties$Builder
 *  net.minecraft.world.item.BannerPatternItem
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.Rarity
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.registries.DeferredHolder
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package com.github.alexthe666.rats.registry;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.data.tags.RatsBannerPatternTags;
import com.github.alexthe666.rats.registry.RatsArmorMaterialRegistry;
import com.github.alexthe666.rats.registry.RatsEffectRegistry;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.server.entity.mount.RatBeastMount;
import com.github.alexthe666.rats.server.entity.mount.RatChickenMount;
import com.github.alexthe666.rats.server.entity.mount.RatGolemMount;
import com.github.alexthe666.rats.server.entity.mount.RatStriderMount;
import com.github.alexthe666.rats.server.items.GildedRatFluteItem;
import com.github.alexthe666.rats.server.items.HatItem;
import com.github.alexthe666.rats.server.items.LoreTagItem;
import com.github.alexthe666.rats.server.items.OreRatNuggetItem;
import com.github.alexthe666.rats.server.items.PartyHatItem;
import com.github.alexthe666.rats.server.items.PlagueHealerItem;
import com.github.alexthe666.rats.server.items.PlagueLeechItem;
import com.github.alexthe666.rats.server.items.PlagueScytheItem;
import com.github.alexthe666.rats.server.items.PurifyingLiquidItem;
import com.github.alexthe666.rats.server.items.RatArrowItem;
import com.github.alexthe666.rats.server.items.RatCaptureNetItem;
import com.github.alexthe666.rats.server.items.RatDecorationItem;
import com.github.alexthe666.rats.server.items.RatFluteItem;
import com.github.alexthe666.rats.server.items.RatHammockItem;
import com.github.alexthe666.rats.server.items.RatIglooItem;
import com.github.alexthe666.rats.server.items.RatListUpgradeItem;
import com.github.alexthe666.rats.server.items.RatPapersItem;
import com.github.alexthe666.rats.server.items.RatSackItem;
import com.github.alexthe666.rats.server.items.RatStaffItem;
import com.github.alexthe666.rats.server.items.RatTubeItem;
import com.github.alexthe666.rats.server.items.RatWhistleItem;
import com.github.alexthe666.rats.server.items.RatbowEssenceItem;
import com.github.alexthe666.rats.server.items.upgrades.AquaticRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.AristocratRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.AsbestosRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.BaseFlightRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.BaseRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.BeeRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.BottlerRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.BreederRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.BucketRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.CarratRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.ChefRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.ChristmasRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.CombinedRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.CraftingRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.CreateWorkerRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.CreativeRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.DJRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.DemonRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.DragonRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.EnchanterRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.EnderRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.EnergyRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.FarmerRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.FishermanRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.IdolRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.JuryRiggedRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.LumberjackRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.MilkerRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.MobFilterUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.MountRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.OreDoublingRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.PickpocketRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.PlacerRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.PlatterRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.PoisonRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.ProtectionRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.QuarryRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.RangedWeaponRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.RatGardenerUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.SculkedRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.ShearsRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.SquirrelRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.StatBoostingRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.StriderMountRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.SupportRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.TNTRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.TNTSurvivorRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.TickAccelRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.TimeManipuRatorUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.UndeadRatUpgradeItem;
import com.github.alexthe666.rats.server.items.upgrades.UnderwaterRatUpgradeItem;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RatsItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create((ResourceKey)Registries.ITEM, (String)"rats");
    public static final DeferredHolder<Item, Item> CHEESE = ITEMS.register("cheese", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.5f).build())));
    public static final DeferredHolder<Item, Item> RAW_RAT = ITEMS.register("raw_rat", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build())));
    public static final DeferredHolder<Item, Item> COOKED_RAT = ITEMS.register("cooked_rat", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build())));
    public static final DeferredHolder<Item, Item> RAT_PELT = ITEMS.register("rat_pelt", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAT_PAW = ITEMS.register("rat_paw", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CHEESE_STICK = ITEMS.register("cheese_stick", () -> new RatStaffItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> RADIUS_STICK = ITEMS.register("radius_stick", () -> new RatStaffItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> PATROL_STICK = ITEMS.register("patrol_stick", () -> new RatStaffItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> RAT_WHISTLE = ITEMS.register("rat_whistle", () -> new RatWhistleItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> RAT_FLUTE = ITEMS.register("rat_flute", () -> new RatFluteItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> GILDED_RAT_FLUTE = ITEMS.register("gilded_rat_flute", () -> new GildedRatFluteItem(new Item.Properties().durability(100)));
    public static final DeferredHolder<Item, Item> CHEF_TOQUE = ITEMS.register("chef_toque", () -> new HatItem(new Item.Properties().stacksTo(1), RatsArmorMaterialRegistry.CHEF_TOQUE, 0));
    public static final DeferredHolder<Item, Item> PIPER_HAT = ITEMS.register("piper_hat", () -> new HatItem(new Item.Properties().stacksTo(1), RatsArmorMaterialRegistry.PIPER_HAT, 1));
    public static final DeferredHolder<Item, Item> STRING_CHEESE = ITEMS.register("string_cheese", () -> new LoreTagItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4f).fast().build()), 1));
    public static final DeferredHolder<Item, Item> CREATIVE_CHEESE = ITEMS.register("creative_cheese", () -> new LoreTagItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), 1, true));
    public static final DeferredHolder<Item, Item> BLUE_CHEESE = ITEMS.register("blue_cheese", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.5f).build())));
    public static final DeferredHolder<Item, Item> NETHER_CHEESE = ITEMS.register("nether_cheese", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.5f).build()).fireResistant()){

        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (!entity.fireImmune()) {
                entity.igniteForSeconds(5.0f);
            }
            return super.finishUsingItem(stack, level, entity);
        }
    });
    public static final DeferredHolder<Item, Item> ASSORTED_VEGETABLES = ITEMS.register("assorted_vegetables", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.3f).build())));
    public static final DeferredHolder<Item, Item> RAT_BURGER = ITEMS.register("rat_burger", () -> new LoreTagItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(1.0f).build()), 1));
    public static final DeferredHolder<Item, Item> RAT_SACK = ITEMS.register("rat_sack", () -> new RatSackItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> RAT_ARROW = ITEMS.register("rat_arrow", () -> new RatArrowItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> RAT_CAPTURE_NET = ITEMS.register("rat_capture_net", () -> new RatCaptureNetItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> FEATHERY_WING = ITEMS.register("feathery_wing", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> DRAGON_WING = ITEMS.register("dragon_wing", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PLASTIC_WASTE = ITEMS.register("plastic_waste", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_PLASTIC = ITEMS.register("raw_plastic", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CONTAMINATED_FOOD = ITEMS.register("contaminated_food", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.5f).effect(new MobEffectInstance(RatsEffectRegistry.PLAGUE, 2400), 0.3f).effect(new MobEffectInstance(MobEffects.POISON, 2400), 0.3f).effect(new MobEffectInstance(MobEffects.HUNGER, 2400), 0.3f).effect(new MobEffectInstance(MobEffects.CONFUSION, 2400), 0.3f).effect(new MobEffectInstance(MobEffects.WEAKNESS, 2400), 0.3f).effect(new MobEffectInstance(MobEffects.WITHER, 2400), 0.3f).effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2400), 0.3f).effect(new MobEffectInstance(MobEffects.UNLUCK, 2400), 1.0f).build())));
    public static final DeferredHolder<Item, Item> PURIFYING_LIQUID = ITEMS.register("purifying_liquid", () -> new PurifyingLiquidItem(new Item.Properties().stacksTo(1), false));
    public static final DeferredHolder<Item, Item> CRIMSON_FLUID = ITEMS.register("crimson_liquid", () -> new PurifyingLiquidItem(new Item.Properties().stacksTo(1), true));
    public static final DeferredHolder<Item, Item> PLAGUE_ESSENCE = ITEMS.register("plague_essence", () -> new LoreTagItem(new Item.Properties().rarity(Rarity.UNCOMMON), 1));
    public static final DeferredHolder<Item, Item> PLAGUE_DOCTORATE = ITEMS.register("plague_doctorate", () -> new LoreTagItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), 1));
    public static final DeferredHolder<Item, Item> HERB_BUNDLE = ITEMS.register("herb_bundle", () -> new PlagueHealerItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).build()), 0.1f));
    public static final DeferredHolder<Item, Item> TREACLE = ITEMS.register("treacle", () -> new PlagueHealerItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.6f).build()), 0.25f));
    public static final DeferredHolder<Item, Item> PLAGUE_LEECH = ITEMS.register("plague_leech", () -> new PlagueLeechItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PLAGUE_STEW = ITEMS.register("plague_stew", () -> new PlagueHealerItem(new Item.Properties().stacksTo(1).craftRemainder(Items.BOWL).food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).build()), 1.0f));
    public static final DeferredHolder<Item, Item> RAT_SKULL = ITEMS.register("rat_skull", () -> new LoreTagItem(new Item.Properties(), 1));
    public static final DeferredHolder<Item, Item> GOLDEN_RAT_SKULL = ITEMS.register("golden_rat_skull", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CORRUPT_RAT_SKULL = ITEMS.register("corrupt_rat_skull", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PLAGUE_TOME = ITEMS.register("plague_tome", () -> new LoreTagItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant(), 2, true));
    public static final DeferredHolder<Item, Item> PLAGUE_SCYTHE = ITEMS.register("plague_scythe", () -> new PlagueScytheItem(new Item.Properties().rarity(Rarity.RARE).fireResistant()));
    public static final DeferredHolder<Item, Item> FILTH = ITEMS.register("filth", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FILTH_CORRUPTION = ITEMS.register("filth_corruption", () -> new LoreTagItem(new Item.Properties().rarity(Rarity.RARE), 1));
    public static final DeferredHolder<Item, Item> TANGLED_RAT_TAILS = ITEMS.register("tangled_rat_tails", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> CHARGED_CREEPER_CHUNK = ITEMS.register("charged_creeper_chunk", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, Item> RATBOW_ESSENCE = ITEMS.register("ratbow_essence", () -> new RatbowEssenceItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAT_PAPERS = ITEMS.register("rat_papers", () -> new RatPapersItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item>[] RAT_HAMMOCKS = new DeferredHolder[16];
    public static final DeferredHolder<Item, Item>[] RAT_TUBES = new DeferredHolder[16];
    public static final DeferredHolder<Item, Item>[] RAT_IGLOOS = new DeferredHolder[16];
    public static final DeferredHolder<Item, Item> RAT_WATER_BOTTLE;
    public static final DeferredHolder<Item, Item> RAT_SEED_BOWL;
    public static final DeferredHolder<Item, Item> RAT_BREEDING_LANTERN;
    public static final DeferredHolder<Item, Item> RAT_WHEEL;
    public static final DeferredHolder<Item, Item> CONFIT_BYALDI;
    public static final DeferredHolder<Item, Item> POTATO_PANCAKE;
    public static final DeferredHolder<Item, Item> LITTLE_BLACK_SQUASH_BALLS;
    public static final DeferredHolder<Item, Item> LITTLE_BLACK_WORM;
    public static final DeferredHolder<Item, Item> CENTIPEDE;
    public static final DeferredHolder<Item, Item> POTATO_KNISHES;
    public static final DeferredHolder<Item, Item> TINY_COIN;
    public static final DeferredHolder<Item, Item> TOKEN_FRAGMENT;
    public static final DeferredHolder<Item, Item> TOKEN_PIECE;
    public static final DeferredHolder<Item, Item> ARCHEOLOGIST_HAT;
    public static final DeferredHolder<Item, Item> FARMER_HAT;
    public static final DeferredHolder<Item, Item> FISHERMAN_HAT;
    public static final DeferredHolder<Item, Item> RAT_FEZ;
    public static final DeferredHolder<Item, Item> TOP_HAT;
    public static final DeferredHolder<Item, Item> SANTA_HAT;
    public static final DeferredHolder<Item, Item> HALO_HAT;
    public static final DeferredHolder<Item, Item> PARTY_HAT;
    public static final DeferredHolder<Item, Item> PIRAT_HAT;
    public static final DeferredHolder<Item, Item> RAT_KING_CROWN;
    public static final DeferredHolder<Item, Item> PLAGUE_DOCTOR_MASK;
    public static final DeferredHolder<Item, Item> BLACK_DEATH_MASK;
    public static final DeferredHolder<Item, Item> EXTERMINATOR_HAT;
    public static final DeferredHolder<Item, Item> MUSIC_DISC_MICE_ON_VENUS;
    public static final DeferredHolder<Item, Item> MUSIC_DISC_LIVING_MICE;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_BASIC;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_CREATIVE;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_JURY_RIGGED;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_SPEED;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_PLATTER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_HEALTH;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_ARMOR;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_BOW;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_CROSSBOW;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_DEMON;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_STRENGTH;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_REMEDY;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_WARRIOR;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_GOD;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_CHEF;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_CRAFTING;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_WORKER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_BLACKLIST;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_WHITELIST;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_MOB_FILTER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_REPLANTER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_FLIGHT;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_DRAGON;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_BEE;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_AQUATIC;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_ENDER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_LUMBERJACK;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_QUARRY;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_FARMER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_SQUIRREL;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_BREEDER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_FISHERMAN;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_TICK_ACCELERATOR;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_TIME_MANIPULATOR;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_ASBESTOS;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_UNDERWATER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_POISON;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_DAMAGE_PROTECTION;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_ORE_DOUBLING;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_BASIC_ENERGY;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_ADVANCED_ENERGY;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_ELITE_ENERGY;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_EXTREME_ENERGY;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_BOTTLER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_BUCKET;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_BIG_BUCKET;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_MILKER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_SHEARS;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_ARISTOCRAT;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_TNT;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_TNT_SURVIVOR;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_PLACER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_VOODOO;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_ENCHANTER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_DISENCHANTER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_CHRISTMAS;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_NO_FLUTE;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_CARRAT;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_DJ;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_SUPPORT;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_ANGEL;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_UNDEAD;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_PICKPOCKET;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_GARDENER;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_IDOL;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_SCULKED;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_BASIC_MOUNT;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_CHICKEN_MOUNT;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_GOLEM_MOUNT;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_STRIDER_MOUNT;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_BEAST_MOUNT;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_COMBINED;
    public static final DeferredHolder<Item, Item> RAT_UPGRADE_COMBINED_CREATIVE;
    public static final DeferredHolder<Item, Item> RAT_NUGGET;
    public static final DeferredHolder<Item, Item> RAT_NUGGET_ORE;
    public static final DeferredHolder<Item, Item> RAT_BANNER_PATTERN;
    public static final DeferredHolder<Item, Item> CHEESE_BANNER_PATTERN;
    public static final DeferredHolder<Item, Item> RAC_BANNER_PATTERN;

    static {
        for (int i = 0; i < 16; ++i) {
            int finalI = i;
            RatsItemRegistry.RAT_TUBES[i] = ITEMS.register("rat_tube_" + DyeColor.byId((int)i).getName(), () -> new RatTubeItem(new Item.Properties(), DyeColor.byId((int)finalI)));
            RatsItemRegistry.RAT_IGLOOS[i] = ITEMS.register("rat_igloo_" + DyeColor.byId((int)i).getName(), () -> new RatIglooItem(new Item.Properties(), DyeColor.byId((int)finalI)));
            RatsItemRegistry.RAT_HAMMOCKS[i] = ITEMS.register("rat_hammock_" + DyeColor.byId((int)i).getName(), () -> new RatHammockItem(new Item.Properties(), DyeColor.byId((int)finalI)));
        }
        RAT_WATER_BOTTLE = ITEMS.register("rat_water_bottle", () -> new RatDecorationItem(new Item.Properties()));
        RAT_SEED_BOWL = ITEMS.register("rat_seed_bowl", () -> new RatDecorationItem(new Item.Properties()));
        RAT_BREEDING_LANTERN = ITEMS.register("rat_breeding_lantern", () -> new RatDecorationItem(new Item.Properties()));
        RAT_WHEEL = ITEMS.register("rat_wheel", () -> new RatDecorationItem(new Item.Properties()));
        CONFIT_BYALDI = ITEMS.register("confit_byaldi", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(50).saturationModifier(1.0f).effect(new MobEffectInstance(RatsEffectRegistry.SYNESTHESIA, 2400), 1.0f).alwaysEdible().build())));
        POTATO_PANCAKE = ITEMS.register("potato_pancake", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.5f).alwaysEdible().build())));
        LITTLE_BLACK_SQUASH_BALLS = ITEMS.register("little_black_squash_balls", () -> new Item(new Item.Properties()));
        LITTLE_BLACK_WORM = ITEMS.register("little_black_worm", () -> new LoreTagItem(new Item.Properties(), 1));
        CENTIPEDE = ITEMS.register("centipede", () -> new Item(new Item.Properties()));
        POTATO_KNISHES = ITEMS.register("potato_knishes", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(50).saturationModifier(1.0f).effect(new MobEffectInstance(RatsEffectRegistry.SYNESTHESIA, 2400), 1.0f).alwaysEdible().build())));
        TINY_COIN = ITEMS.register("tiny_coin", () -> new Item(new Item.Properties()));
        TOKEN_FRAGMENT = ITEMS.register("token_fragment", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
        TOKEN_PIECE = ITEMS.register("token_piece", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
        ARCHEOLOGIST_HAT = ITEMS.register("archeologist_hat", () -> new HatItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), RatsArmorMaterialRegistry.GENERIC_HAT, 1));
        FARMER_HAT = ITEMS.register("farmer_hat", () -> new HatItem(new Item.Properties().stacksTo(1), RatsArmorMaterialRegistry.FARMER_HAT, 0));
        FISHERMAN_HAT = ITEMS.register("fisherman_hat", () -> new HatItem(new Item.Properties().stacksTo(1), RatsArmorMaterialRegistry.GENERIC_HAT, 0));
        RAT_FEZ = ITEMS.register("rat_fez", () -> new HatItem(new Item.Properties().stacksTo(1), RatsArmorMaterialRegistry.FEZ, 2));
        TOP_HAT = ITEMS.register("top_hat", () -> new HatItem(new Item.Properties().stacksTo(1), RatsArmorMaterialRegistry.TOP_HAT, 0));
        SANTA_HAT = ITEMS.register("santa_hat", () -> new HatItem(new Item.Properties().stacksTo(1), RatsArmorMaterialRegistry.SANTA_HAT, 0));
        HALO_HAT = ITEMS.register("halo_hat", () -> new HatItem(new Item.Properties().stacksTo(1), RatsArmorMaterialRegistry.HALO, 0));
        PARTY_HAT = ITEMS.register("party_hat", () -> new PartyHatItem(new Item.Properties().stacksTo(1)));
        PIRAT_HAT = ITEMS.register("pirat_hat", () -> new HatItem(new Item.Properties().stacksTo(1), RatsArmorMaterialRegistry.GENERIC_HAT, 0));
        RAT_KING_CROWN = ITEMS.register("rat_king_crown", () -> new HatItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), RatsArmorMaterialRegistry.CROWN, 0));
        PLAGUE_DOCTOR_MASK = ITEMS.register("plague_doctor_mask", () -> new HatItem(new Item.Properties().stacksTo(1), RatsArmorMaterialRegistry.PLAGUE_MASK, 1));
        BLACK_DEATH_MASK = ITEMS.register("black_death_mask", () -> new HatItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.RARE), RatsArmorMaterialRegistry.PLAGUE_MASK, 1));
        EXTERMINATOR_HAT = ITEMS.register("exterminator_hat", () -> new HatItem(new Item.Properties().stacksTo(1), RatsArmorMaterialRegistry.GENERIC_HAT, 0));
        MUSIC_DISC_MICE_ON_VENUS = ITEMS.register("music_disc_mice_on_venus", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
        MUSIC_DISC_LIVING_MICE = ITEMS.register("music_disc_living_mice", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
        RAT_UPGRADE_BASIC = ITEMS.register("rat_upgrade_basic", () -> new BaseRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_CREATIVE = ITEMS.register("rat_upgrade_creative", () -> new CreativeRatUpgradeItem(new Item.Properties().fireResistant()));
        RAT_UPGRADE_JURY_RIGGED = ITEMS.register("rat_upgrade_jury_rigged", () -> new JuryRiggedRatUpgradeItem(new Item.Properties().stacksTo(1)));
        RAT_UPGRADE_SPEED = ITEMS.register("rat_upgrade_speed", () -> new StatBoostingRatUpgradeItem(new Item.Properties(), 0, () -> Map.of(Attributes.MOVEMENT_SPEED, 0.5)));
        RAT_UPGRADE_PLATTER = ITEMS.register("rat_upgrade_platter", () -> new PlatterRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_HEALTH = ITEMS.register("rat_upgrade_health", () -> new StatBoostingRatUpgradeItem(new Item.Properties(), 0, () -> Map.of(Attributes.MAX_HEALTH, 20.0)));
        RAT_UPGRADE_ARMOR = ITEMS.register("rat_upgrade_armor", () -> new StatBoostingRatUpgradeItem(new Item.Properties(), 0, () -> Map.of(Attributes.ARMOR, 10.0)));
        RAT_UPGRADE_BOW = ITEMS.register("rat_upgrade_bow", () -> new RangedWeaponRatUpgradeItem(new Item.Properties(), false));
        RAT_UPGRADE_CROSSBOW = ITEMS.register("rat_upgrade_crossbow", () -> new RangedWeaponRatUpgradeItem(new Item.Properties(), true));
        RAT_UPGRADE_DEMON = ITEMS.register("rat_upgrade_demon", () -> new DemonRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_STRENGTH = ITEMS.register("rat_upgrade_strength", () -> new StatBoostingRatUpgradeItem(new Item.Properties(), 0, () -> Map.of(Attributes.ATTACK_DAMAGE, 5.0)));
        RAT_UPGRADE_REMEDY = ITEMS.register("rat_upgrade_remedy", () -> new StatBoostingRatUpgradeItem(new Item.Properties(), 2, 1, Map::of, true));
        RAT_UPGRADE_WARRIOR = ITEMS.register("rat_upgrade_warrior", () -> new StatBoostingRatUpgradeItem(new Item.Properties(), 2, () -> Map.of(Attributes.MAX_HEALTH, RatConfig.warriorHealthUpgrade, Attributes.ARMOR, RatConfig.warriorArmorUpgrade, Attributes.ATTACK_DAMAGE, RatConfig.warriorDamageUpgrade)));
        RAT_UPGRADE_GOD = ITEMS.register("rat_upgrade_god", () -> new StatBoostingRatUpgradeItem(new Item.Properties().fireResistant(), 3, () -> Map.of(Attributes.MAX_HEALTH, RatConfig.godHealthUpgrade, Attributes.ARMOR, RatConfig.godArmorUpgrade, Attributes.ATTACK_DAMAGE, RatConfig.godDamageUpgrade)));
        RAT_UPGRADE_CHEF = ITEMS.register("rat_upgrade_chef", () -> new ChefRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_CRAFTING = ITEMS.register("rat_upgrade_crafting", () -> new CraftingRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_WORKER = ITEMS.register("rat_upgrade_worker", () -> new CreateWorkerRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_BLACKLIST = ITEMS.register("rat_upgrade_blacklist", () -> new RatListUpgradeItem(new Item.Properties().stacksTo(1), 0, 3));
        RAT_UPGRADE_WHITELIST = ITEMS.register("rat_upgrade_whitelist", () -> new RatListUpgradeItem(new Item.Properties().stacksTo(1), 0, 3));
        RAT_UPGRADE_MOB_FILTER = ITEMS.register("rat_upgrade_mob_filter", () -> new MobFilterUpgradeItem(new Item.Properties().stacksTo(1)));
        RAT_UPGRADE_REPLANTER = ITEMS.register("rat_upgrade_replanter", () -> new BaseRatUpgradeItem(new Item.Properties(), 0, 1));
        RAT_UPGRADE_FLIGHT = ITEMS.register("rat_upgrade_flight", () -> new BaseFlightRatUpgradeItem(new Item.Properties(), 0, 3){

            @Override
            public ItemStack getWing() {
                return ((Item)FEATHERY_WING.get()).getDefaultInstance();
            }
        });
        RAT_UPGRADE_DRAGON = ITEMS.register("rat_upgrade_dragon", () -> new DragonRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_BEE = ITEMS.register("rat_upgrade_bee", () -> new BeeRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_AQUATIC = ITEMS.register("rat_upgrade_aquatic", () -> new AquaticRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_ENDER = ITEMS.register("rat_upgrade_ender", () -> new EnderRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_LUMBERJACK = ITEMS.register("rat_upgrade_lumberjack", () -> new LumberjackRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_QUARRY = ITEMS.register("rat_upgrade_quarry", () -> new QuarryRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_FARMER = ITEMS.register("rat_upgrade_farmer", () -> new FarmerRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_SQUIRREL = ITEMS.register("rat_upgrade_squirrel", () -> new SquirrelRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_BREEDER = ITEMS.register("rat_upgrade_breeder", () -> new BreederRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_FISHERMAN = ITEMS.register("rat_upgrade_fisherman", () -> new FishermanRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_TICK_ACCELERATOR = ITEMS.register("rat_upgrade_tick_accelerator", () -> new TickAccelRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_TIME_MANIPULATOR = ITEMS.register("rat_upgrade_time_manipulator", () -> new TimeManipuRatorUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_ASBESTOS = ITEMS.register("rat_upgrade_asbestos", () -> new AsbestosRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_UNDERWATER = ITEMS.register("rat_upgrade_underwater", () -> new UnderwaterRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_POISON = ITEMS.register("rat_upgrade_poison", () -> new PoisonRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_DAMAGE_PROTECTION = ITEMS.register("rat_upgrade_damage_protection", () -> new ProtectionRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_ORE_DOUBLING = ITEMS.register("rat_upgrade_ore_doubling", () -> new OreDoublingRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_BASIC_ENERGY = ITEMS.register("rat_upgrade_basic_energy", () -> new EnergyRatUpgradeItem(new Item.Properties(), 0, RatConfig.ratRFTransferBasic, RatConfig.ratChargeBasic));
        RAT_UPGRADE_ADVANCED_ENERGY = ITEMS.register("rat_upgrade_advanced_energy", () -> new EnergyRatUpgradeItem(new Item.Properties(), 1, RatConfig.ratRFTransferAdvanced, RatConfig.ratChargeAdvanced));
        RAT_UPGRADE_ELITE_ENERGY = ITEMS.register("rat_upgrade_elite_energy", () -> new EnergyRatUpgradeItem(new Item.Properties(), 2, RatConfig.ratRFTransferElite, RatConfig.ratChargeElite));
        RAT_UPGRADE_EXTREME_ENERGY = ITEMS.register("rat_upgrade_extreme_energy", () -> new EnergyRatUpgradeItem(new Item.Properties().fireResistant(), 3, RatConfig.ratRFTransferExtreme, RatConfig.ratChargeExtreme));
        RAT_UPGRADE_BOTTLER = ITEMS.register("rat_upgrade_bottler", () -> new BottlerRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_BUCKET = ITEMS.register("rat_upgrade_bucket", () -> new BucketRatUpgradeItem(new Item.Properties(), 0, 2, 1000));
        RAT_UPGRADE_BIG_BUCKET = ITEMS.register("rat_upgrade_big_bucket", () -> new BucketRatUpgradeItem(new Item.Properties(), 2, 2, 5000));
        RAT_UPGRADE_MILKER = ITEMS.register("rat_upgrade_milker", () -> new MilkerRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_SHEARS = ITEMS.register("rat_upgrade_shears", () -> new ShearsRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_ARISTOCRAT = ITEMS.register("rat_upgrade_aristocrat", () -> new AristocratRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_TNT = ITEMS.register("rat_upgrade_tnt", () -> new TNTRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_TNT_SURVIVOR = ITEMS.register("rat_upgrade_tnt_survivor", () -> new TNTSurvivorRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_PLACER = ITEMS.register("rat_upgrade_placer", () -> new PlacerRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_VOODOO = ITEMS.register("rat_upgrade_voodoo", () -> new StatBoostingRatUpgradeItem(new Item.Properties(), 2, 0, () -> Map.of(Attributes.MAX_HEALTH, RatConfig.voodooHealthUpgrade), true));
        RAT_UPGRADE_ENCHANTER = ITEMS.register("rat_upgrade_enchanter", () -> new EnchanterRatUpgradeItem(new Item.Properties(), 1, 2));
        RAT_UPGRADE_DISENCHANTER = ITEMS.register("rat_upgrade_disenchanter", () -> new EnchanterRatUpgradeItem(new Item.Properties(), 1, 1));
        RAT_UPGRADE_CHRISTMAS = ITEMS.register("rat_upgrade_christmas", () -> new ChristmasRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_NO_FLUTE = ITEMS.register("rat_upgrade_no_flute", () -> new BaseRatUpgradeItem(new Item.Properties(), 0, 1));
        RAT_UPGRADE_CARRAT = ITEMS.register("rat_upgrade_carrat", () -> new CarratRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_DJ = ITEMS.register("rat_upgrade_dj", () -> new DJRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_SUPPORT = ITEMS.register("rat_upgrade_support", () -> new SupportRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_ANGEL = ITEMS.register("rat_upgrade_angel", () -> new BaseRatUpgradeItem(new Item.Properties(), 2, 2));
        RAT_UPGRADE_UNDEAD = ITEMS.register("rat_upgrade_undead", () -> new UndeadRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_PICKPOCKET = ITEMS.register("rat_upgrade_pickpocket", () -> new PickpocketRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_GARDENER = ITEMS.register("rat_upgrade_gardener", () -> new RatGardenerUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_IDOL = ITEMS.register("rat_upgrade_idol", () -> new IdolRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_SCULKED = ITEMS.register("rat_upgrade_sculked", () -> new SculkedRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_BASIC_MOUNT = ITEMS.register("rat_upgrade_basic_mount", () -> new BaseRatUpgradeItem(new Item.Properties()));
        RAT_UPGRADE_CHICKEN_MOUNT = ITEMS.register("rat_upgrade_chicken_mount", () -> new MountRatUpgradeItem<RatChickenMount>(new Item.Properties(), 2, 2, RatsEntityRegistry.RAT_MOUNT_CHICKEN));
        RAT_UPGRADE_GOLEM_MOUNT = ITEMS.register("rat_upgrade_golem_mount", () -> new MountRatUpgradeItem<RatGolemMount>(new Item.Properties(), 2, 2, RatsEntityRegistry.RAT_MOUNT_GOLEM));
        RAT_UPGRADE_STRIDER_MOUNT = ITEMS.register("rat_upgrade_strider_mount", () -> new StriderMountRatUpgradeItem<RatStriderMount>(new Item.Properties(), 2, 2, RatsEntityRegistry.RAT_STRIDER_MOUNT));
        RAT_UPGRADE_BEAST_MOUNT = ITEMS.register("rat_upgrade_beast_mount", () -> new MountRatUpgradeItem<RatBeastMount>(new Item.Properties(), 2, 2, RatsEntityRegistry.RAT_MOUNT_BEAST));
        RAT_UPGRADE_COMBINED = ITEMS.register("rat_upgrade_combined", () -> new CombinedRatUpgradeItem(new Item.Properties().stacksTo(1)));
        RAT_UPGRADE_COMBINED_CREATIVE = ITEMS.register("rat_upgrade_combined_creative", () -> new CombinedRatUpgradeItem(new Item.Properties().stacksTo(1).fireResistant()));
        RAT_NUGGET = ITEMS.register("rat_nugget", () -> new Item(new Item.Properties()));
        RAT_NUGGET_ORE = ITEMS.register("rat_nugget_ore", () -> new OreRatNuggetItem(new Item.Properties()));
        RAT_BANNER_PATTERN = ITEMS.register("rat_banner_pattern", () -> new BannerPatternItem(RatsBannerPatternTags.RAT_BANNER_PATTERN, new Item.Properties().stacksTo(1)));
        CHEESE_BANNER_PATTERN = ITEMS.register("cheese_banner_pattern", () -> new BannerPatternItem(RatsBannerPatternTags.CHEESE_BANNER_PATTERN, new Item.Properties().stacksTo(1)));
        RAC_BANNER_PATTERN = ITEMS.register("rat_and_crossbones_banner_pattern", () -> new BannerPatternItem(RatsBannerPatternTags.RAC_BANNER_PATTERN, new Item.Properties().stacksTo(1)));
    }
}

