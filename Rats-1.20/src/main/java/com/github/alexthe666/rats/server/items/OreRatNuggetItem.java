package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.misc.RatsItemStackData;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

public class OreRatNuggetItem extends Item {

	public OreRatNuggetItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
		return InteractionResult.PASS;
	}

	// Читает OreItem из NBT кусочка; принимает стек, запасной стек и реестр, отдаёт исходную руду.
	public static ItemStack getStoredItem(ItemStack poopItem, ItemStack fallback, HolderLookup.Provider provider) {
		try {
			CompoundTag poopTag = RatsItemStackData.getTag(poopItem);
			if (poopTag.contains("OreItem")) {
				ItemStack oreItem = RatsItemStackData.parse(provider, poopTag.getCompound("OreItem"));
				if (oreItem.isEmpty()) {
					return fallback;
				}
				return oreItem;
			}
			return fallback;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка чтения руды из кусочка {}: {}", poopItem.getItem(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Возвращает предмет-источник для плавки: NBT кусочка или сам переданный стек (блок руды в лапе крысы).
	public static ItemStack resolveOreSource(ItemStack stack, HolderLookup.Provider provider) {
		try {
			if (stack.isEmpty()) {
				return ItemStack.EMPTY;
			}
			ItemStack stored = getStoredItem(stack, ItemStack.EMPTY, provider);
			return stored.isEmpty() ? stack : stored;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка определения источника руды для {}: {}", stack.getItem(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Возвращает результат плавки для кусочка или блока руды; принимает уровень и стек, отдаёт слиток или EMPTY.
	public static ItemStack getIngot(Level level, ItemStack nuggetStack) {
		try {
			ItemStack ore = resolveOreSource(nuggetStack, level.registryAccess());
			if (ore.isEmpty()) {
				return ItemStack.EMPTY;
			}
			SingleRecipeInput input = new SingleRecipeInput(ore);
			var recipeHolder = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
			SmeltingRecipe recipe = recipeHolder.map(holder -> holder.value()).orElse(null);
			if (recipe != null && !recipe.getResultItem(level.registryAccess()).isEmpty()) {
				return recipe.getResultItem(level.registryAccess()).copy();
			}
			return ItemStack.EMPTY;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка плавки руды {}: {}", nuggetStack.getItem(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Упаковывает результат плавки в кусочек руды с NBT; принимает ресурс и реестр, отдаёт стек кусочка.
	public static ItemStack saveResourceToNugget(ItemStack resource, HolderLookup.Provider provider) {
		try {
			ItemStack stack = new ItemStack(RatsItemRegistry.RAT_NUGGET_ORE.get());
			CompoundTag nuggetTag = new CompoundTag();
			nuggetTag.put("OreItem", RatsItemStackData.save(provider, resource));
			RatsItemStackData.setTag(stack, nuggetTag);
			return stack;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка упаковки руды в кусочек {}: {}", resource.getItem(), exception.getMessage(), exception);
			throw exception;
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		ItemStack ingot = getStoredItem(stack, ItemStack.EMPTY, context.registries());
		if (!ingot.isEmpty()) {
			tooltip.add(Component.translatable(RatsLangConstants.ORE_NUGGET_CONTAINS, ingot.getDisplayName().getString()).withStyle(ChatFormatting.GRAY));
		}
		tooltip.add(Component.translatable("item.rats.rat_nugget_ore.desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}
}
