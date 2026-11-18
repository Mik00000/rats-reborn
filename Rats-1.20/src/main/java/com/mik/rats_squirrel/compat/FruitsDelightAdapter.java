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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fruits Delight leaves, bushes and durian.
 *
 * <p>Handles:
 * <ul>
 *   <li>{@code PassableLeavesBlock}: property {@code type=fruits}, persistent=false.</li>
 *   <li>{@code DurianLeavesBlock}: property {@code fruit=fruits}, persistent=false. Drops falling block.</li>
 *   <li>{@code FruitBushBlock}: {@code AGE_4 == 4}, resets to age 2.</li>
 *   <li>{@code DoubleFruitBushBlock}: lower half {@code AGE_4 == 4}, resets both halves to (2, 2).</li>
 * </ul>
 * </p>
 */
public final class FruitsDelightAdapter implements FruitAdapter {
    private static final String FRUITS_DELIGHT_NAMESPACE = "fruitsdelight";
    private final Map<ResourceLocation, FruitKind> kindCache = new ConcurrentHashMap<>();

    @Override
    public boolean matches(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!FRUITS_DELIGHT_NAMESPACE.equals(key.getNamespace())) {
            return false;
        }
        return isPassableLeaves(state) || isDurianLeaves(state) || isSingleBush(state) || isDoubleBush(state);
    }

    @Override
    public BlockPos actualPos(ServerLevel level, BlockPos pos, BlockState state) {
        if (isDoubleBush(state) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    @Override
    public boolean isMature(ServerLevel level, BlockPos pos, BlockState state) {
        if (!matches(state)) {
            return false;
        }
        if (isPassableLeaves(state)) {
            return !state.getValue(BlockStateProperties.PERSISTENT) && isPropertyFruits(state, "type");
        }
        if (isDurianLeaves(state)) {
            return !state.getValue(BlockStateProperties.PERSISTENT) && isPropertyFruits(state, "fruit");
        }
        if (isSingleBush(state)) {
            return state.getValue(BlockStateProperties.AGE_4) >= 4;
        }
        if (isDoubleBush(state)) {
            BlockPos lowerPos = actualPos(level, pos, state);
            BlockState lowerState = level.getBlockState(lowerPos);
            return lowerState.hasProperty(BlockStateProperties.AGE_4)
                    && lowerState.getValue(BlockStateProperties.AGE_4) >= 4;
        }
        return false;
    }

    @Override
    public FruitKind kind(ServerLevel level, BlockPos pos, BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return kindCache.computeIfAbsent(key, id -> {
            boolean treeFruit = isPassableLeaves(state) || isDurianLeaves(state);
            Item item = state.getBlock().asItem();
            ItemStack rep = item != Items.AIR ? new ItemStack(item) : ItemStack.EMPTY;
            Set<Item> accepted = item != Items.AIR ? Set.of(item) : Set.of();

            return new FruitKind(
                    id,
                    rep,
                    accepted,
                    s -> {
                        if (isPassableLeaves(s)) {
                            return !s.getValue(BlockStateProperties.PERSISTENT) && isPropertyFruits(s, "type");
                        }
                        if (isDurianLeaves(s)) {
                            return !s.getValue(BlockStateProperties.PERSISTENT) && isPropertyFruits(s, "fruit");
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
        return true;
    }

    @Override
    public boolean supportsExactFallback(BlockState state) {
        return isSingleBush(state) || isDoubleBush(state);
    }

    @Override
    public List<ItemStack> exactDrops(ServerLevel level, BlockPos pos, BlockState state, FakePlayer player) {
        if (!supportsExactFallback(state)) {
            return List.of();
        }
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, ItemStack.EMPTY);
        if (!drops.isEmpty()) {
            return drops;
        }
        Item item = state.getBlock().asItem();
        return item != Items.AIR ? List.of(new ItemStack(item)) : List.of();
    }

    @Override
    public void applyExactReset(ServerLevel level, BlockPos pos, BlockState state) {
        if (isSingleBush(state)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.AGE_4, 2), 2);
        } else if (isDoubleBush(state)) {
            BlockPos lower = actualPos(level, pos, state);
            BlockState lowerState = level.getBlockState(lower);
            if (lowerState.hasProperty(BlockStateProperties.AGE_4) && lowerState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                level.setBlock(lower, lowerState.setValue(BlockStateProperties.AGE_4, 2).setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), 2);
            }
            BlockPos upper = lower.above();
            BlockState upperState = level.getBlockState(upper);
            if (upperState.hasProperty(BlockStateProperties.AGE_4) && upperState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                level.setBlock(upper, upperState.setValue(BlockStateProperties.AGE_4, 2).setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 2);
            }
        }
    }

    private static boolean isPassableLeaves(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if ("type".equals(prop.getName())) {
                return state.hasProperty(BlockStateProperties.PERSISTENT);
            }
        }
        return false;
    }

    private static boolean isDurianLeaves(BlockState state) {
        boolean hasFruit = false;
        boolean hasLeaf = false;
        for (Property<?> prop : state.getProperties()) {
            if ("fruit".equals(prop.getName())) {
                hasFruit = true;
            }
            if ("leaf".equals(prop.getName())) {
                hasLeaf = true;
            }
        }
        return hasFruit && hasLeaf && state.hasProperty(BlockStateProperties.PERSISTENT);
    }

    private static boolean isSingleBush(BlockState state) {
        return state.hasProperty(BlockStateProperties.AGE_4) && !state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF);
    }

    private static boolean isDoubleBush(BlockState state) {
        return state.hasProperty(BlockStateProperties.AGE_4) && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF);
    }

    private static boolean isPropertyFruits(BlockState state, String propertyName) {
        for (Property<?> prop : state.getProperties()) {
            if (propertyName.equals(prop.getName())) {
                Comparable<?> val = state.getValue(prop);
                return val != null && "fruits".equalsIgnoreCase(val.toString());
            }
        }
        return false;
    }
}
