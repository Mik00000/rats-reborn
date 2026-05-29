package com.github.alexthe666.rats.server.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class RatsRecipeSerializer<T extends BaseRatRecipe> implements RecipeSerializer<T> {

	private final SingleItemMaker<T> factory;
	private final MapCodec<T> codec;
	private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

	public RatsRecipeSerializer(SingleItemMaker<T> factory) {
		this.factory = factory;
		this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.STRING.optionalFieldOf("group", "").forGetter(BaseRatRecipe::getGroup),
				Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(BaseRatRecipe::getInputIngredient),
				ItemStack.CODEC.fieldOf("result").forGetter(BaseRatRecipe::getResultStack)
		).apply(instance, (group, ingredient, result) -> this.factory.create(ResourceLocation.fromNamespaceAndPath("rats", "codec"), group, ingredient, result)));
		this.streamCodec = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8,
				BaseRatRecipe::getGroup,
				Ingredient.CONTENTS_STREAM_CODEC,
				BaseRatRecipe::getInputIngredient,
				ItemStack.STREAM_CODEC,
				BaseRatRecipe::getResultStack,
				(group, ingredient, result) -> this.factory.create(ResourceLocation.fromNamespaceAndPath("rats", "stream"), group, ingredient, result)
		);
	}

	@Override
	public MapCodec<T> codec() {
		return this.codec;
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
		return this.streamCodec;
	}

	public interface SingleItemMaker<T extends BaseRatRecipe> {
		T create(ResourceLocation id, String group, Ingredient input, ItemStack output);
	}
}
