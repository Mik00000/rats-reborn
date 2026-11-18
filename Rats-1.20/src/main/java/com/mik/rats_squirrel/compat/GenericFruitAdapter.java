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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic adapter for modded leaves and crops with standard {@code AGE_3} or {@code AGE_4}.
 *
 * <p>Uses {@code useWithoutItem} exclusively. Does not use loot-table fallback and does not
 * perform manual state reset.</p>
 */
public final class GenericFruitAdapter implements FruitAdapter {
    private final Map<ResourceLocation, FruitKind> kindCache = new ConcurrentHashMap<>();

    @Override
    public boolean matches(BlockState state) {
        boolean isFoliageOrCrop = state.is(BlockTags.LEAVES)
                || state.getBlock() instanceof LeavesBlock
                || state.getBlock() instanceof CropBlock;
        return isFoliageOrCrop && (state.hasProperty(BlockStateProperties.AGE_3) || state.hasProperty(BlockStateProperties.AGE_4));
    }

    @Override
    public boolean isMature(ServerLevel level, BlockPos pos, BlockState state) {
        if (!matches(state)) {
            return false;
        }
        if (state.hasProperty(BlockStateProperties.AGE_3)) {
            return state.getValue(BlockStateProperties.AGE_3) >= 3;
        }
        if (state.hasProperty(BlockStateProperties.AGE_4)) {
            return state.getValue(BlockStateProperties.AGE_4) >= 4;
        }
        return false;
    }

    @Override
    public FruitKind kind(ServerLevel level, BlockPos pos, BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return kindCache.computeIfAbsent(key, id -> {
            boolean treeFruit = state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock;
            Item item = state.getBlock().asItem();
            ItemStack rep = item != Items.AIR ? new ItemStack(item) : ItemStack.EMPTY;
            Set<Item> accepted = item != Items.AIR ? Set.of(item) : Set.of();

            return new FruitKind(
                    id,
                    rep,
                    accepted,
                    s -> {
                        if (s.hasProperty(BlockStateProperties.AGE_3)) {
                            return s.getValue(BlockStateProperties.AGE_3) >= 3;
                        }
                        if (s.hasProperty(BlockStateProperties.AGE_4)) {
                            return s.getValue(BlockStateProperties.AGE_4) >= 4;
                        }
                        return false;
                    },
                    treeFruit
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
}
