package com.github.alexthe666.rats.registry;

import com.github.alexthe666.rats.RatsMod;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class RatsArmorMaterialRegistry {

	private RatsArmorMaterialRegistry() {
	}

	public static final ArmorMaterial PIPER_HAT = create("piper_hat", 2, 15, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, () -> Ingredient.of(new ItemStack(RatsBlockRegistry.PIED_WOOL.get())));
	public static final ArmorMaterial CHEF_TOQUE = create("chef_toque", 1, 15, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, () -> Ingredient.of(new ItemStack(Blocks.WHITE_WOOL)));
	public static final ArmorMaterial PLAGUE_MASK = create("plague_mask", 3, 15, SoundEvents.ARMOR_EQUIP_GENERIC, 1.0F, 0.0F, () -> Ingredient.of(new ItemStack(RatsItemRegistry.PLAGUE_ESSENCE.get())));
	public static final ArmorMaterial RATLANTIS = create("ratlantis", 5, 17, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.1F, () -> Ingredient.of(new ItemStack(RatlantisItemRegistry.ORATCHALCUM_INGOT.get())));
	public static final ArmorMaterial FARMER_HAT = create("farmer_hat", 1, 15, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, () -> Ingredient.of(new ItemStack(Items.HAY_BLOCK)));
	public static final ArmorMaterial TOP_HAT = create("top_hat", 1, 15, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, () -> Ingredient.of(new ItemStack(Items.BLACK_WOOL)));
	public static final ArmorMaterial FEZ = create("fez", 1, 100, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, () -> Ingredient.of(new ItemStack(Items.RED_WOOL)));
	public static final ArmorMaterial SANTA_HAT = create("santa_hat", 1, 15, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, () -> Ingredient.of(new ItemStack(Items.RED_WOOL)));
	public static final ArmorMaterial HALO = create("halo", 1, 15, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, () -> Ingredient.of(new ItemStack(Items.GOLD_NUGGET)));
	public static final ArmorMaterial CROWN = create("crown", 1, 15, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, () -> Ingredient.of(new ItemStack(Items.GOLD_INGOT)));
	public static final ArmorMaterial GHOST_HAT = create("ghost_hat", 1, 15, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, () -> Ingredient.of(new ItemStack(RatlantisItemRegistry.GHOST_PIRAT_ECTOPLASM.get())));
	public static final ArmorMaterial GENERIC_HAT = create("generic_hat", 1, 100, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, () -> Ingredient.of(new ItemStack(Items.LEATHER)));

	private static ArmorMaterial create(String textureName, int helmetDefense, int enchantability, Holder<SoundEvent> equipSound, float toughness, float knockback, Supplier<Ingredient> repairMaterial) {
		Map<ArmorItem.Type, Integer> defense = Util.make(new EnumMap<>(ArmorItem.Type.class), map -> map.put(ArmorItem.Type.HELMET, helmetDefense));
		return new ArmorMaterial(
				defense,
				enchantability,
				equipSound,
				repairMaterial,
				List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, textureName))),
				toughness,
				knockback
		);
	}
}
