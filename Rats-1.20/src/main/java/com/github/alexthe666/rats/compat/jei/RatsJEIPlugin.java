package com.github.alexthe666.rats.compat.jei;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.compat.jei.ArcheologistRecipeCategory;
import com.github.alexthe666.rats.compat.jei.CauldronInfoHolder;
import com.github.alexthe666.rats.compat.jei.CauldronRecipeCategory;
import com.github.alexthe666.rats.compat.jei.ChefRecipeCategory;
import com.github.alexthe666.rats.compat.jei.RatCraftingTableTransferHandler;
import com.github.alexthe666.rats.compat.jei.RatsRecipeTypes;
import com.github.alexthe666.rats.registry.RatlantisItemRegistry;
import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.registry.RatsRecipeRegistry;
import com.github.alexthe666.rats.server.recipes.ArcheologistRecipe;
import com.github.alexthe666.rats.server.recipes.ChefRecipe;
import java.util.LinkedList;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import org.jetbrains.annotations.Nullable;

@JeiPlugin
@EventBusSubscriber(modid = "rats", value = Dist.CLIENT)
public class RatsJEIPlugin implements IModPlugin {
    public static final ResourceLocation MOD = ResourceLocation.fromNamespaceAndPath("rats", "rats");
    private static IJeiRuntime jeiRuntime = null;

    private void addDescription(IRecipeRegistration registry, ItemStack itemStack) {
        registry.addIngredientInfo(itemStack, (IIngredientType<ItemStack>) VanillaTypes.ITEM_STACK, Component.translatable(itemStack.getDescriptionId() + ".jei_desc"));
    }

    @Nullable
    private static RecipeManager resolveRecipeManager() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            return connection.getRecipeManager();
        }
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            return server.getRecipeManager();
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            return level.getRecipeManager();
        }
        return null;
    }

    public static void updateJeiRecipes(RecipeManager manager) {
        if (jeiRuntime != null && manager != null) {
            try {
                jeiRuntime.getRecipeManager().addRecipes(RatsRecipeTypes.CHEF, manager.getAllRecipesFor(RatsRecipeRegistry.CHEF.get()).stream().map(RecipeHolder::value).toList());
                jeiRuntime.getRecipeManager().addRecipes(RatsRecipeTypes.ARCHEOLOGIST, manager.getAllRecipesFor(RatsRecipeRegistry.ARCHEOLOGIST.get()).stream().map(RecipeHolder::value).toList());
            } catch (Exception e) {
                RatsMod.LOGGER.error("Failed to dynamically add recipes to JEI", e);
            }
        }
    }

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        updateJeiRecipes(event.getRecipeManager());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        RecipeManager manager = resolveRecipeManager();
        if (manager != null) {
            updateJeiRecipes(manager);
        }
        if (RatConfig.cheesemaking) {
            LinkedList<CauldronInfoHolder> cauldrons = new LinkedList<>();
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
        if (manager != null) {
            registry.addRecipes(RatsRecipeTypes.CHEF, manager.getAllRecipesFor(RatsRecipeRegistry.CHEF.get()).stream().map(RecipeHolder::value).toList());
            registry.addRecipes(RatsRecipeTypes.ARCHEOLOGIST, manager.getAllRecipesFor(RatsRecipeRegistry.ARCHEOLOGIST.get()).stream().map(RecipeHolder::value).toList());
        }

        this.addDescription(registry, new ItemStack(RatsItemRegistry.CHEESE.get()));
        this.addDescription(registry, new ItemStack(RatsItemRegistry.CHEESE_STICK.get()));
        this.addDescription(registry, new ItemStack(RatsItemRegistry.RAT_FLUTE.get()));
        this.addDescription(registry, new ItemStack(RatsItemRegistry.RAT_UPGRADE_BASIC.get()));
        this.addDescription(registry, new ItemStack(RatsItemRegistry.RAT_UPGRADE_CHEF.get()));
        this.addDescription(registry, new ItemStack(RatlantisItemRegistry.RAT_UPGRADE_ARCHEOLOGIST.get()));
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
        registry.addRecipeCategories(new ArcheologistRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
        registry.addRecipeCatalyst(new ItemStack(RatsItemRegistry.RAT_UPGRADE_CHEF.get()), RatsRecipeTypes.CHEF);
        registry.addRecipeCatalyst(new ItemStack(RatlantisItemRegistry.RAT_UPGRADE_ARCHEOLOGIST.get()), RatsRecipeTypes.ARCHEOLOGIST);
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
