package com.mik.rats_squirrel.compat;

import com.mik.rats_squirrel.fruit.FruitAdapter;
import com.mik.rats_squirrel.fruit.FruitKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cocktails Delight grape crops and wild grapes.
 *
 * <p>Mature at {@code age >= 3} (crops) or {@code SweetBerryBushBlock.AGE >= 2} (wild grapes).
 * Harvested through {@code useWithoutItem} only. No fallback.</p>
 */
public final class CocktailsDelightAdapter implements FruitAdapter {
    private static final String COCKTAILS_DELIGHT_NAMESPACE = "cocktailsdelight";
    private final Map<ResourceLocation, FruitKind> kindCache = new ConcurrentHashMap<>();

    @Override
    public boolean matches(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!COCKTAILS_DELIGHT_NAMESPACE.equals(key.getNamespace())) {
            return false;
        }
        return key.getPath().contains("grape") && getAgeProperty(state) != null;
    }

    @Override
    public boolean isMature(ServerLevel level, BlockPos pos, BlockState state) {
        if (!matches(state)) {
            return false;
        }
        IntegerProperty ageProp = getAgeProperty(state);
        if (ageProp == null) {
            return false;
        }
        int threshold = state.hasProperty(SweetBerryBushBlock.AGE) ? 2 : 3;
        return state.getValue(ageProp) >= threshold;
    }

    @Override
    public FruitKind kind(ServerLevel level, BlockPos pos, BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return kindCache.computeIfAbsent(key, id -> {
            String path = id.getPath();
            String fruitName = path.contains("white") ? "white_grapes" : "red_grapes";
            Item grapesItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(COCKTAILS_DELIGHT_NAMESPACE, fruitName));
            Item blockItem = state.getBlock().asItem();
            Item repItem = grapesItem != Items.AIR ? grapesItem : blockItem;
            ItemStack rep = repItem != Items.AIR ? new ItemStack(repItem) : ItemStack.EMPTY;

            Set<Item> accepted = new HashSet<>();
            if (grapesItem != Items.AIR) accepted.add(grapesItem);
            if (blockItem != Items.AIR) accepted.add(blockItem);

            return new FruitKind(
                    id,
                    rep,
                    accepted,
                    s -> {
                        IntegerProperty p = getAgeProperty(s);
                        if (p == null) return false;
                        int th = s.hasProperty(SweetBerryBushBlock.AGE) ? 2 : 3;
                        return s.getValue(p) >= th;
                    },
                    false
            );
        });
    }

    @Override
    public InteractionResult interact(ServerLevel level, BlockPos pos, BlockState state, FakePlayer player) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        return state.useWithoutItem(level, player, hit);
    }

    @Override
    public boolean supportsExactFallback() {
        return false;
    }

    private static IntegerProperty getAgeProperty(BlockState state) {
        if (state.hasProperty(SweetBerryBushBlock.AGE)) {
            return SweetBerryBushBlock.AGE;
        }
        for (Property<?> prop : state.getProperties()) {
            if ("age".equals(prop.getName()) && prop instanceof IntegerProperty intProp) {
                return intProp;
            }
        }
        return null;
    }
}
