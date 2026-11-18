package com.mik.rats_squirrel.compat;

import com.mik.rats_squirrel.fruit.FruitAdapter;
import com.mik.rats_squirrel.fruit.FruitKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create Winery grape bushes.
 *
 * <p>Targets only the base {@code RedGrapeBushBlock}/{@code WhiteGrapeBushBlock} with
 * {@code age == 3}. Harvests exclusively through a right-click block event because the mod's
 * {@code GrapeBushHarvestProcedure} handles loot drops and age reset to 1. Never calls
 * {@code useWithoutItem} or fallback.</p>
 */
public final class CreateWineryAdapter implements FruitAdapter {
    private static final String CREATE_WINERY_NAMESPACE = "create_winery";
    private static final TagKey<Block> GRAPE_BUSHES_TAG = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(CREATE_WINERY_NAMESPACE, "grape_bushes")
    );
    private final Map<ResourceLocation, FruitKind> kindCache = new ConcurrentHashMap<>();

    @Override
    public boolean matches(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!CREATE_WINERY_NAMESPACE.equals(key.getNamespace())) {
            return false;
        }
        String path = key.getPath();
        if (path.contains("stage_3") || path.contains("stage3")) {
            return false;
        }
        boolean isBush = state.is(GRAPE_BUSHES_TAG) || "red_grape_bush".equals(path) || "white_grape_bush".equals(path);
        return isBush && getAgeProperty(state) != null;
    }

    @Override
    public boolean isMature(ServerLevel level, BlockPos pos, BlockState state) {
        if (!matches(state)) {
            return false;
        }
        IntegerProperty ageProp = getAgeProperty(state);
        return ageProp != null && state.getValue(ageProp) >= 3;
    }

    @Override
    public FruitKind kind(ServerLevel level, BlockPos pos, BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return kindCache.computeIfAbsent(key, id -> {
            String path = id.getPath();
            String fruitName = path.contains("white") ? "white_grapes" : "red_grapes";
            Item grapesItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CREATE_WINERY_NAMESPACE, fruitName));
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
                        return p != null && s.getValue(p) >= 3;
                    },
                    false
            );
        });
    }

    @Override
    public InteractionResult interact(ServerLevel level, BlockPos pos, BlockState state, FakePlayer player) {
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        PlayerInteractEvent.RightClickBlock evt = CommonHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hitResult);
        if (evt.isCanceled()) {
            return evt.getCancellationResult();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean usesBlockEvent() {
        return true;
    }

    @Override
    public boolean supportsExactFallback() {
        return false;
    }

    private static IntegerProperty getAgeProperty(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if ("age".equals(prop.getName()) && prop instanceof IntegerProperty intProp) {
                return intProp;
            }
        }
        return null;
    }
}
