package com.mik.rats_squirrel.compat;

import com.mik.rats_squirrel.fruit.FruitAdapter;
import com.mik.rats_squirrel.fruit.FruitKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cultural Delights fruiting avocado leaves.
 *
 * <p>Mature at {@code AGE_4 == 4}, harvested through {@code useWithoutItem}, and the block
 * resets itself to age 1. Fallback resets to age 1.</p>
 */
public final class CulturalDelightsAdapter implements FruitAdapter {
    private static final String CULTURAL_DELIGHTS_NAMESPACE = "culturaldelights";
    private final Map<ResourceLocation, FruitKind> kindCache = new ConcurrentHashMap<>();

    @Override
    public boolean matches(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!CULTURAL_DELIGHTS_NAMESPACE.equals(key.getNamespace())) {
            return false;
        }
        return state.hasProperty(BlockStateProperties.AGE_4)
                && (key.getPath().contains("fruiting") || key.getPath().contains("avocado") || state.is(BlockTags.LEAVES));
    }

    @Override
    public boolean isMature(ServerLevel level, BlockPos pos, BlockState state) {
        return matches(state) && state.getValue(BlockStateProperties.AGE_4) >= 4;
    }

    @Override
    public FruitKind kind(ServerLevel level, BlockPos pos, BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return kindCache.computeIfAbsent(key, id -> {
            Item item = state.getBlock().asItem();
            Item avocado = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CULTURAL_DELIGHTS_NAMESPACE, "avocado"));
            Item repItem = avocado != Items.AIR ? avocado : item;
            ItemStack rep = repItem != Items.AIR ? new ItemStack(repItem) : ItemStack.EMPTY;

            Set<Item> accepted = new HashSet<>();
            if (item != Items.AIR) accepted.add(item);
            if (avocado != Items.AIR) accepted.add(avocado);

            return new FruitKind(
                    id,
                    rep,
                    accepted,
                    s -> s.hasProperty(BlockStateProperties.AGE_4) && s.getValue(BlockStateProperties.AGE_4) >= 4,
                    true
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
        return true;
    }

    @Override
    public List<ItemStack> exactDrops(ServerLevel level, BlockPos pos, BlockState state, FakePlayer player) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, ItemStack.EMPTY);
        if (!drops.isEmpty()) {
            return drops;
        }
        Item avocado = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CULTURAL_DELIGHTS_NAMESPACE, "avocado"));
        if (avocado != Items.AIR) {
            return List.of(new ItemStack(avocado));
        }
        Item blockItem = state.getBlock().asItem();
        return blockItem != Items.AIR ? List.of(new ItemStack(blockItem)) : List.of();
    }

    @Override
    public void applyExactReset(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.hasProperty(BlockStateProperties.AGE_4)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.AGE_4, 1), 2);
        }
    }
}
