package com.github.alexthe666.rats.compat.jei;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.registry.RatsRecipeRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@JeiPlugin
public class RatsJEIPlugin implements IModPlugin {
	public static final ResourceLocation MOD = ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "rats");

	private void addDescription(IRecipeRegistration registry, ItemStack itemStack) {
		registry.addIngredientInfo(itemStack, VanillaTypes.ITEM_STACK, Component.translatable(itemStack.getDescriptionId() + ".jei_desc"));
	}

	@Nullable
	private static RecipeManager resolveRecipeManager() {
		var connection = Minecraft.getInstance().getConnection();
		if (connection != null) {
			return connection.getRecipeManager();
		}
		var server = Minecraft.getInstance().getSingleplayerServer();
		if (server != null) {
			return server.getRecipeManager();
		}
		var level = Minecraft.getInstance().level;
		if (level != null) {
			return level.getRecipeManager();
		}
		return null;
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime runtime) {
		RecipeManager manager = resolveRecipeManager();
		if (manager != null) {
			runtime.getRecipeManager().addRecipes(RatsRecipeTypes.CHEF, manager.getAllRecipesFor(RatsRecipeRegistry.CHEF.get()).stream().map(holder -> holder.value()).toList());
		}
		if (RatConfig.cheesemaking) {
			List<CauldronInfoHolder> cauldrons = new LinkedList<>();
			cauldrons.add(new CauldronInfoHolder(Items.MILK_BUCKET, Items.MILK_BUCKET, Items.CAULDRON, RatsBlockRegistry.MILK_CAULDRON.get()));
			cauldrons.add(new CauldronInfoHolder(Items.AIR, RatsBlockRegistry.BLOCK_OF_CHEESE.get(), RatsBlockRegistry.MILK_CAULDRON.get(), RatsBlockRegistry.CHEESE_CAULDRON.get()));
			cauldrons.add(new CauldronInfoHolder(Items.SUGAR, RatsBlockRegistry.BLOCK_OF_BLUE_CHEESE.get(), RatsBlockRegistry.CHEESE_CAULDRON.get(), RatsBlockRegistry.BLUE_CHEESE_CAULDRON.get()));
			cauldrons.add(new CauldronInfoHolder(Items.LAVA_BUCKET, RatsBlockRegistry.BLOCK_OF_NETHER_CHEESE.get(), RatsBlockRegistry.CHEESE_CAULDRON.get(), RatsBlockRegistry.NETHER_CHEESE_CAULDRON.get()));
			runtime.getRecipeManager().addRecipes(RatsRecipeTypes.CAULDRON, cauldrons);
		}
	}

	@Override
	public void registerRecipes(IRecipeRegistration registry) {
		RecipeManager manager = resolveRecipeManager();
		if (manager == null) {
			RatsMod.LOGGER.error("RatsJEIPlugin: RecipeManager недоступен — рецепты мода не зарегистрированы в JEI");
			return;
		}
		registry.addRecipes(RatsRecipeTypes.CHEF, manager.getAllRecipesFor(RatsRecipeRegistry.CHEF.get()).stream().map(holder -> holder.value()).toList());
		this.addDescription(registry, new ItemStack(RatsItemRegistry.CHEESE.get()));
		this.addDescription(registry, new ItemStack(RatsItemRegistry.CHEESE_STICK.get()));
		this.addDescription(registry, new ItemStack(RatsItemRegistry.RAT_FLUTE.get()));
		this.addDescription(registry, new ItemStack(RatsItemRegistry.RAT_UPGRADE_BASIC.get()));
		this.addDescription(registry, new ItemStack(RatsItemRegistry.RAT_UPGRADE_CHEF.get()));
		this.addDescription(registry, new ItemStack(RatsItemRegistry.RAT_UPGRADE_WHITELIST.get()));
		this.addDescription(registry, new ItemStack(RatsItemRegistry.RAT_UPGRADE_BLACKLIST.get()));
		this.addDescription(registry, new ItemStack(RatsBlockRegistry.RAT_TRAP.get()));
		this.addDescription(registry, new ItemStack(RatsBlockRegistry.RAT_CAGE.get()));
		this.addDescription(registry, new ItemStack(RatsBlockRegistry.RAT_CRAFTING_TABLE.get()));
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registry) {
		registry.addRecipeCategories(new ChefRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeCategories(new CauldronRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
		registry.addRecipeCatalyst(new ItemStack(RatsItemRegistry.RAT_UPGRADE_CHEF.get()), RatsRecipeTypes.CHEF);
		if (RatConfig.cheesemaking) {
			registry.addRecipeCatalyst(new ItemStack(Items.CAULDRON), RatsRecipeTypes.CAULDRON);
		}
		registry.addRecipeCatalyst(new ItemStack(RatsItemRegistry.RAT_UPGRADE_CRAFTING.get()), RecipeTypes.CRAFTING);
	}

	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
		registration.addUniversalRecipeTransferHandler(RatCraftingTableTransferHandler.INSTANCE);
	}

	@Override
	public ResourceLocation getPluginUid() {
		return MOD;
	}
}
