package com.github.alexthe666.rats.registry;

import com.github.alexthe666.rats.server.items.OreRatNuggetItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(
   modid = "rats"
)
public class RatsCreativeTabRegistry {
   public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "rats");
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RATS = TABS.register(
      "rats",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rats.rats"))
            .icon(() -> new ItemStack((ItemLike)RatsItemRegistry.CHEESE.get()))
            .displayItems((parameters, output) -> {
               output.accept((ItemLike)RatsItemRegistry.CHEESE.get());
               output.accept((ItemLike)RatsItemRegistry.BLUE_CHEESE.get());
               output.accept((ItemLike)RatsItemRegistry.NETHER_CHEESE.get());
               output.accept((ItemLike)RatsBlockRegistry.BLOCK_OF_CHEESE.get());
               output.accept((ItemLike)RatsBlockRegistry.BLOCK_OF_BLUE_CHEESE.get());
               output.accept((ItemLike)RatsBlockRegistry.BLOCK_OF_NETHER_CHEESE.get());
               output.accept((ItemLike)RatsBlockRegistry.AUTO_CURDLER.get());
               output.accept((ItemLike)RatsBlockRegistry.RAT_CHEESE_FEEDER.get());
               output.accept((ItemLike)RatsBlockRegistry.RAT_HOUSE.get());

               for (DeferredHolder<Block, Block> house : RatsBlockRegistry.RAT_HOUSES) {
                  output.accept((ItemLike)house.get());
               }

               output.accept((ItemLike)RatsBlockRegistry.RAT_CRAFTING_TABLE.get());
               output.accept((ItemLike)RatsItemRegistry.RAW_RAT.get());
               output.accept((ItemLike)RatsItemRegistry.COOKED_RAT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_PELT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_PAW.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_SKULL.get());
               output.accept((ItemLike)RatsItemRegistry.GOLDEN_RAT_SKULL.get());
               output.accept((ItemLike)RatsItemRegistry.CORRUPT_RAT_SKULL.get());
               output.accept((ItemLike)RatsItemRegistry.TANGLED_RAT_TAILS.get());
               output.accept((ItemLike)RatsBlockRegistry.TRASH_CAN.get());
               output.accept((ItemLike)RatsBlockRegistry.GARBAGE_PILE.get());
               output.accept((ItemLike)RatsBlockRegistry.CURSED_GARBAGE.get());
               output.accept((ItemLike)RatsBlockRegistry.PURIFIED_GARBAGE.get());
               output.accept((ItemLike)RatsBlockRegistry.PIED_GARBAGE.get());
               output.accept((ItemLike)RatsBlockRegistry.COMPRESSED_GARBAGE.get());
               output.accept((ItemLike)RatsItemRegistry.FILTH.get());
               output.accept((ItemLike)RatsItemRegistry.FILTH_CORRUPTION.get());
               output.accept((ItemLike)RatsItemRegistry.STRING_CHEESE.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_BURGER.get());
               output.accept((ItemLike)RatsItemRegistry.ASSORTED_VEGETABLES.get());
               output.accept((ItemLike)RatsItemRegistry.CONFIT_BYALDI.get());
               output.accept((ItemLike)RatsItemRegistry.POTATO_PANCAKE.get());
               output.accept((ItemLike)RatsItemRegistry.LITTLE_BLACK_SQUASH_BALLS.get());
               output.accept((ItemLike)RatsItemRegistry.POTATO_KNISHES.get());
               output.accept((ItemLike)RatsItemRegistry.HERB_BUNDLE.get());
               output.accept((ItemLike)RatsItemRegistry.TREACLE.get());
               output.accept((ItemLike)RatsItemRegistry.PLAGUE_LEECH.get());
               output.accept((ItemLike)RatsItemRegistry.PLAGUE_STEW.get());
               output.accept((ItemLike)RatsItemRegistry.PLAGUE_DOCTORATE.get());
               output.accept((ItemLike)RatsItemRegistry.PLAGUE_TOME.get());
               output.accept((ItemLike)RatsItemRegistry.PLAGUE_ESSENCE.get());
               output.accept((ItemLike)RatsItemRegistry.PURIFYING_LIQUID.get());
               output.accept((ItemLike)RatsItemRegistry.CRIMSON_FLUID.get());
               output.accept((ItemLike)RatsItemRegistry.CHEF_TOQUE.get());
               output.accept((ItemLike)RatsItemRegistry.PIPER_HAT.get());
               output.accept((ItemLike)RatsItemRegistry.ARCHEOLOGIST_HAT.get());
               output.accept((ItemLike)RatsItemRegistry.FARMER_HAT.get());
               output.accept((ItemLike)RatsItemRegistry.FISHERMAN_HAT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_FEZ.get());
               output.accept((ItemLike)RatsItemRegistry.TOP_HAT.get());
               output.accept((ItemLike)RatsItemRegistry.SANTA_HAT.get());
               output.accept((ItemLike)RatsItemRegistry.HALO_HAT.get());
               output.accept((ItemLike)RatsItemRegistry.PARTY_HAT.get());
               output.accept((ItemLike)RatsItemRegistry.PIRAT_HAT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_KING_CROWN.get());
               output.accept((ItemLike)RatsItemRegistry.PLAGUE_DOCTOR_MASK.get());
               output.accept((ItemLike)RatsItemRegistry.BLACK_DEATH_MASK.get());
               output.accept((ItemLike)RatsItemRegistry.EXTERMINATOR_HAT.get());
               output.accept((ItemLike)RatlantisItemRegistry.MILITARY_HAT.get());
               output.accept((ItemLike)RatlantisBlockRegistry.ORATCHALCUM_ORE.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAW_ORATCHALCUM.get());
               output.accept((ItemLike)RatlantisItemRegistry.ORATCHALCUM_INGOT.get());
               output.accept((ItemLike)RatlantisItemRegistry.ORATCHALCUM_NUGGET.get());
               output.accept((ItemLike)RatlantisBlockRegistry.ORATCHALCUM_BLOCK.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_HELMET.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_CHESTPLATE.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_LEGGINGS.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_BOOTS.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_SWORD.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_PICKAXE.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_AXE.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_SHOVEL.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_HOE.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_BOW.get());
               output.accept((ItemLike)RatsItemRegistry.CHEESE_STICK.get());
               output.accept((ItemLike)RatsItemRegistry.RADIUS_STICK.get());
               output.accept((ItemLike)RatsItemRegistry.PATROL_STICK.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_WHISTLE.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_FLUTE.get());
               output.accept((ItemLike)RatsItemRegistry.GILDED_RAT_FLUTE.get());
               output.accept((ItemLike)RatsItemRegistry.PLAGUE_SCYTHE.get());
               output.accept((ItemLike)RatsItemRegistry.MUSIC_DISC_MICE_ON_VENUS.get());
               output.accept((ItemLike)RatsItemRegistry.MUSIC_DISC_LIVING_MICE.get());
               output.accept((ItemLike)RatsBlockRegistry.RAT_CAGE.get());
               output.accept((ItemLike)RatsItemRegistry.PLASTIC_WASTE.get());
               output.accept((ItemLike)RatsItemRegistry.RAW_PLASTIC.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_BREEDING_LANTERN.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_SEED_BOWL.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_WATER_BOTTLE.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_WHEEL.get());
               registerColoredItems(output, "rat_tube");
               registerColoredItems(output, "rat_igloo");
               registerColoredItems(output, "rat_hammock");
               output.accept((ItemLike)RatsItemRegistry.RAT_CAPTURE_NET.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_SACK.get());
               output.accept((ItemLike)RatsItemRegistry.FEATHERY_WING.get());
               output.accept((ItemLike)RatsItemRegistry.DRAGON_WING.get());
               output.accept((ItemLike)RatsItemRegistry.CHARGED_CREEPER_CHUNK.get());
               output.accept((ItemLike)RatsItemRegistry.RATBOW_ESSENCE.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_PAPERS.get());
               output.accept((ItemLike)RatsItemRegistry.TINY_COIN.get());
               output.accept((ItemLike)RatsItemRegistry.TOKEN_FRAGMENT.get());
               output.accept((ItemLike)RatsItemRegistry.TOKEN_PIECE.get());
               output.accept((ItemLike)RatlantisBlockRegistry.CHUNKY_CHEESE_TOKEN.get());
               output.accept((ItemLike)RatsBlockRegistry.MARBLED_CHEESE_RAW.get());
               output.accept((ItemLike)RatsBlockRegistry.FISH_BARREL.get());
               output.accept((ItemLike)RatsBlockRegistry.DYE_SPONGE.get());
               output.accept((ItemLike)RatsBlockRegistry.PIED_WOOL.get());
               output.accept((ItemLike)RatsBlockRegistry.JACK_O_RATERN.get());
               output.accept((ItemLike)RatsBlockRegistry.MANHOLE.get());
               output.accept((ItemLike)RatsBlockRegistry.RAT_QUARRY.get());
               output.accept((ItemLike)RatsBlockRegistry.RAT_ATTRACTOR.get());
               output.accept((ItemLike)RatsBlockRegistry.RAT_UPGRADE_BLOCK.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_BANNER_PATTERN.get());
               output.accept((ItemLike)RatsItemRegistry.CHEESE_BANNER_PATTERN.get());
               output.accept((ItemLike)RatsItemRegistry.RAC_BANNER_PATTERN.get());
               output.accept(Objects.requireNonNull((Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("rats", "rat_spawn_egg"))));
               output.accept(Objects.requireNonNull((Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("rats", "demon_rat_spawn_egg"))));
               output.accept(Objects.requireNonNull((Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("rats", "rat_king_spawn_egg"))));
               output.accept(Objects.requireNonNull((Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("rats", "pied_piper_spawn_egg"))));
               output.accept(Objects.requireNonNull((Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("rats", "plague_doctor_spawn_egg"))));
               output.accept(Objects.requireNonNull((Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("rats", "black_death_spawn_egg"))));
               output.accept(Objects.requireNonNull((Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("rats", "plague_cloud_spawn_egg"))));
               output.accept(Objects.requireNonNull((Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("rats", "plague_beast_spawn_egg"))));
               registerOreNuggets(output);
            })
            .build()
   );

   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> UPGRADES = TABS.register(
      "rats_upgrades",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rats.upgrades"))
            .icon(() -> new ItemStack((ItemLike)RatsItemRegistry.RAT_UPGRADE_BASIC.get()))
            .displayItems((parameters, output) -> {
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_BASIC.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_SPEED.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_HEALTH.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_REMEDY.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_ARMOR.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_STRENGTH.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_WARRIOR.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_GOD.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_BOW.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_CROSSBOW.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_FLIGHT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_DRAGON.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_BEE.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_DEMON.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_ENDER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_UNDEAD.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_CARRAT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_NO_FLUTE.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_DJ.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_ANGEL.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_ASBESTOS.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_POISON.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_UNDERWATER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_DAMAGE_PROTECTION.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_TNT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_TNT_SURVIVOR.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_ARISTOCRAT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_VOODOO.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_SUPPORT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_BREEDER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_FISHERMAN.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_LUMBERJACK.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_FARMER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_SQUIRREL.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_QUARRY.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_REPLANTER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_SHEARS.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_PICKPOCKET.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_GARDENER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_IDOL.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_SCULKED.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_DISENCHANTER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_ENCHANTER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_PLACER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_CRAFTING.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_WORKER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_CHEF.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_CHRISTMAS.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_ORE_DOUBLING.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_BOTTLER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_BUCKET.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_BIG_BUCKET.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_MILKER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_WHITELIST.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_MOB_FILTER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_PLATTER.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_BASIC_MOUNT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_CHICKEN_MOUNT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_GOLEM_MOUNT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_STRIDER_MOUNT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_BEAST_MOUNT.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_BASIC_RATLANTEAN.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_FERAL_BITE.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_ARCHEOLOGIST.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_BUCCANEER.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_RATINATOR.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_PSYCHIC.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_ETHEREAL.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_NONBELIEVER.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_AUTOMATON_MOUNT.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_BIPLANE_MOUNT.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_JURY_RIGGED.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_AQUATIC.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_BASIC_ENERGY.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_ADVANCED_ENERGY.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_ELITE_ENERGY.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_EXTREME_ENERGY.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_TICK_ACCELERATOR.get());
               output.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_TIME_MANIPULATOR.get());
            })
            .build()
   );

   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RATLANTIS = TABS.register(
      "ratlantis",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rats.ratlantis"))
            .icon(() -> new ItemStack((ItemLike)RatlantisBlockRegistry.CHUNKY_CHEESE_TOKEN.get()))
            .displayItems((parameters, output) -> {
               output.accept((ItemLike)RatlantisBlockRegistry.CHUNKY_CHEESE_TOKEN.get());
               output.accept((ItemLike)RatlantisBlockRegistry.RATGLOVE_FLOWER.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATGLOVE_PETALS.get());
               output.accept((ItemLike)RatlantisItemRegistry.FERAL_RAT_CLAW.get());
               output.accept((ItemLike)RatlantisItemRegistry.FERAL_BAGH_NAKHS.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTEAN_FLAME.get());
               output.accept((ItemLike)RatlantisItemRegistry.GEM_OF_RATLANTIS.get());
               output.accept((ItemLike)RatlantisBlockRegistry.RATLANTEAN_GEM_ORE.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAW_ORATCHALCUM.get());
               output.accept((ItemLike)RatlantisItemRegistry.ORATCHALCUM_NUGGET.get());
               output.accept((ItemLike)RatlantisItemRegistry.ORATCHALCUM_INGOT.get());
               output.accept((ItemLike)RatlantisBlockRegistry.ORATCHALCUM_ORE.get());
               output.accept((ItemLike)RatlantisBlockRegistry.ORATCHALCUM_BLOCK.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_HELMET.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_CHESTPLATE.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_LEGGINGS.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_BOOTS.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_SWORD.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_PICKAXE.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_AXE.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_SHOVEL.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_HOE.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_BOW.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_TOGA.get());
               output.accept((ItemLike)RatlantisItemRegistry.PIRAT_CUTLASS.get());
               output.accept((ItemLike)RatlantisItemRegistry.CHEESE_CANNONBALL.get());
               output.accept((ItemLike)RatlantisItemRegistry.GHOST_PIRAT_HAT.get());
               output.accept((ItemLike)RatlantisItemRegistry.GHOST_PIRAT_ECTOPLASM.get());
               output.accept((ItemLike)RatlantisItemRegistry.GHOST_PIRAT_CUTLASS.get());
               output.accept((ItemLike)RatlantisItemRegistry.VIAL_OF_SENTIENCE.get());
               output.accept((ItemLike)RatlantisBlockRegistry.DUTCHRAT_BELL.get());
               output.accept((ItemLike)RatlantisBlockRegistry.AIR_RAID_SIREN.get());
               output.accept((ItemLike)RatlantisItemRegistry.ARCANE_TECHNOLOGY.get());
               output.accept((ItemLike)RatlantisItemRegistry.ANCIENT_SAWBLADE.get());
               output.accept((ItemLike)RatlantisItemRegistry.PSIONIC_RAT_BRAIN.get());
               output.accept((ItemLike)RatlantisItemRegistry.DUTCHRAT_WHEEL.get());
               output.accept((ItemLike)RatlantisItemRegistry.AVIATOR_HAT.get());
               output.accept((ItemLike)RatlantisItemRegistry.MILITARY_HAT.get());
               output.accept((ItemLike)RatlantisItemRegistry.BIPLANE_WING.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATFISH.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATFISH_BUCKET.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATBOT_BARREL.get());
               output.accept((ItemLike)RatlantisItemRegistry.CHARGED_RATBOT_BARREL.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATTLING_GUN.get());
               output.accept((ItemLike)RatlantisItemRegistry.RATLANTIS_RAT_SKULL.get());
               output.accept((ItemLike)RatlantisItemRegistry.IDOL_OF_RATLANTIS.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAS_BANNER_PATTERN.get());
               output.accept((ItemLike)RatlantisBlockRegistry.CHEESE_ORE.get());
               output.accept((ItemLike)RatlantisBlockRegistry.BRAIN_BLOCK.get());
               output.accept((ItemLike)RatlantisBlockRegistry.COMPRESSED_RAT.get());
               output.accept((ItemLike)RatlantisBlockRegistry.RATLANTIS_REACTOR.get());
               output.accept((ItemLike)RatlantisBlockRegistry.RATLANTIS_PORTAL.get());
               output.accept((ItemLike)RatlantisBlockRegistry.RATLANTIS_UPGRADE_BLOCK.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_STAIRS.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_SLAB.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_BRICK.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_BRICK_STAIRS.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_BRICK_SLAB.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_BRICK_CRACKED.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_BRICK_CRACKED_STAIRS.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_BRICK_CRACKED_SLAB.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_BRICK_MOSSY.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_BRICK_MOSSY_STAIRS.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_BRICK_MOSSY_SLAB.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_CHISELED.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_BRICK_CHISELED.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_PILLAR.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_TILE.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_GOLEM_CORE.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_RAT_HEAD.get());
               output.accept((ItemLike)RatlantisBlockRegistry.BLACK_MARBLED_CHEESE.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_GRASS.get());
               output.accept((ItemLike)RatlantisBlockRegistry.MARBLED_CHEESE_DIRT.get());
               output.accept((ItemLike)RatlantisBlockRegistry.PIRAT_SAPLING.get());
               output.accept((ItemLike)RatlantisBlockRegistry.PIRAT_LEAVES.get());
               output.accept((ItemLike)RatlantisBlockRegistry.PIRAT_LOG.get());
               output.accept((ItemLike)RatlantisBlockRegistry.PIRAT_WOOD.get());
               output.accept((ItemLike)RatlantisBlockRegistry.STRIPPED_PIRAT_LOG.get());
               output.accept((ItemLike)RatlantisBlockRegistry.STRIPPED_PIRAT_WOOD.get());
               output.accept((ItemLike)RatlantisBlockRegistry.PIRAT_PLANKS.get());
               output.accept((ItemLike)RatlantisBlockRegistry.PIRAT_STAIRS.get());
               output.accept((ItemLike)RatlantisBlockRegistry.PIRAT_SLAB.get());
               output.accept((ItemLike)RatlantisBlockRegistry.PIRAT_FENCE.get());
               output.accept((ItemLike)RatlantisBlockRegistry.PIRAT_FENCE_GATE.get());
               output.accept((ItemLike)RatlantisBlockRegistry.PIRAT_DOOR.get());
               output.accept((ItemLike)RatlantisBlockRegistry.PIRAT_TRAPDOOR.get());
               output.accept((ItemLike)RatlantisBlockRegistry.PIRAT_PRESSURE_PLATE.get());
               output.accept((ItemLike)RatlantisBlockRegistry.PIRAT_BUTTON.get());
               output.accept((ItemLike)RatlantisItemRegistry.PIRAT_SIGN.get());
               output.accept((ItemLike)RatlantisItemRegistry.PIRAT_HANGING_SIGN.get());
               output.accept((ItemLike)RatlantisItemRegistry.PIRAT_BOAT.get());
               output.accept((ItemLike)RatlantisItemRegistry.PIRAT_CHEST_BOAT.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_BASIC_RATLANTEAN.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_FERAL_BITE.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_ARCHEOLOGIST.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_BUCCANEER.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_RATINATOR.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_PSYCHIC.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_ETHEREAL.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_NONBELIEVER.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_AUTOMATON_MOUNT.get());
               output.accept((ItemLike)RatlantisItemRegistry.RAT_UPGRADE_BIPLANE_MOUNT.get());

               acceptIfPresent(output, "rats", "ratlantean_spirit_spawn_egg");
               acceptIfPresent(output, "rats", "ratlantean_automaton_spawn_egg");
               acceptIfPresent(output, "rats", "feral_ratlantean_spawn_egg");
               acceptIfPresent(output, "rats", "neo_ratlantean_spawn_egg");
               acceptIfPresent(output, "rats", "pirat_spawn_egg");
               acceptIfPresent(output, "rats", "ghost_pirat_spawn_egg");
               acceptIfPresent(output, "rats", "dutchrat_spawn_egg");
               acceptIfPresent(output, "rats", "ratfish_spawn_egg");
               acceptIfPresent(output, "rats", "ratlantean_ratbot_spawn_egg");
               acceptIfPresent(output, "rats", "rat_baron_spawn_egg");
            })
            .build()
   );

   private static void acceptIfPresent(Output output, String namespace, String path) {
      Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
      if (item != null && item != Items.AIR) {
         output.accept(item);
      }
   }

   private static void registerColoredItems(Output output, String itemType) {
      for (DyeColor color : DyeColor.values()) {
         Item item = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("rats", itemType + "_" + color.getName()));
         if (item != null && item != Items.AIR) {
            output.accept(item);
         }
      }
   }

   private static void registerOreNuggets(Output output) {
      output.accept((ItemLike)RatsItemRegistry.RAT_NUGGET.get());
      List<ItemStack> uniqueOres = new ArrayList<>();
      Level oreLevel = null;
      if (EffectiveSide.get().isServer()) {
         if (ServerLifecycleHooks.getCurrentServer() != null) {
            oreLevel = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
         }
      } else {
         oreLevel = com.github.alexthe666.rats.client.RatsClientHooks.getClientLevel();
      }

      Level levelForOres = oreLevel;
      if (levelForOres != null) {
         BuiltInRegistries.ITEM.getTag(net.neoforged.neoforge.common.Tags.Items.ORES).ifPresent(oresTag -> {
            for (Holder<Item> holder : oresTag) {
               Item item = (Item)holder.value();
               ItemStack oreDrop = OreRatNuggetItem.getIngot(levelForOres, new ItemStack(item));
               if (!uniqueOres.contains(oreDrop) && !oreDrop.isEmpty()) {
                  uniqueOres.add(oreDrop);
               }
            }
         });
         uniqueOres.sort(Comparator.comparing(o -> o.getDisplayName().getString()));

         for (ItemStack ore : uniqueOres) {
            output.accept(OreRatNuggetItem.saveResourceToNugget(ore, levelForOres.registryAccess()));
         }
      }
   }

   @SubscribeEvent
   public static void registerToExistingTabs(BuildCreativeModeTabContentsEvent event) {
      if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS && event.hasPermissions()) {
         event.accept((ItemLike)RatsItemRegistry.CREATIVE_CHEESE.get());
         event.accept((ItemLike)RatsItemRegistry.RAT_UPGRADE_CREATIVE.get());
      }
   }
}
