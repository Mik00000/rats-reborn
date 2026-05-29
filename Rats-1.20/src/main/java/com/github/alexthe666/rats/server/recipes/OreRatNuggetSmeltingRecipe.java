package com.github.alexthe666.rats.server.recipes;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.registry.RatsRecipeRegistry;
import com.github.alexthe666.rats.server.items.OreRatNuggetItem;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class OreRatNuggetSmeltingRecipe extends AbstractCookingRecipe {

    public static final OreRatNuggetSmeltingRecipe INSTANCE = new OreRatNuggetSmeltingRecipe();

    public static final MapCodec<OreRatNuggetSmeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RecordCodecBuilder.point(INSTANCE)
    ).apply(instance, recipe -> recipe));

    @Nullable
    private static Level matchLevel;

    private OreRatNuggetSmeltingRecipe() {
        super(RecipeType.SMELTING, "", CookingBookCategory.MISC, Ingredient.of(RatsItemRegistry.RAT_NUGGET_ORE.get()), new ItemStack(Items.IRON_INGOT), 0.7F, 200);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        try {
            matchLevel = level;
            return input.item().is(RatsItemRegistry.RAT_NUGGET_ORE.get()) && !OreRatNuggetItem.getIngot(level, input.item()).isEmpty();
        } catch (Exception e) {
            RatsMod.LOGGER.error("OreRatNuggetSmeltingRecipe.matches: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider provider) {
        try {
            if (matchLevel == null) {
                throw new IllegalStateException("matchLevel is not initialized");
            }
            return OreRatNuggetItem.getIngot(matchLevel, input.item()).copy();
        } catch (Exception e) {
            RatsMod.LOGGER.error("OreRatNuggetSmeltingRecipe.assemble: {}", e.getMessage(), e);
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return new ItemStack(Items.IRON_INGOT);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RatsRecipeRegistry.ORE_NUGGET_SMELTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.SMELTING;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}