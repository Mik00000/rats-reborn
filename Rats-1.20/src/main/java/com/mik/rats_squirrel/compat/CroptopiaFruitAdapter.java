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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Croptopia leaf crops.
 *
 * <p>Mature when {@code AGE_3 == 3} and {@code PERSISTENT == false}. Harvested through
 * {@code useWithoutItem} only; the block resets itself to age 0. Does not use generic reset
 * or loot-table fallback.</p>
 */
public final class CroptopiaFruitAdapter implements FruitAdapter {
    private static final String CROPTOPIA_NAMESPACE = "croptopia";
    private final Map<ResourceLocation, FruitKind> kindCache = new ConcurrentHashMap<>();

    @Override
    public boolean matches(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return CROPTOPIA_NAMESPACE.equals(key.getNamespace())
                && state.hasProperty(BlockStateProperties.AGE_3)
                && state.hasProperty(BlockStateProperties.PERSISTENT);
    }

    @Override
    public boolean isMature(ServerLevel level, BlockPos pos, BlockState state) {
        return matches(state)
                && !state.getValue(BlockStateProperties.PERSISTENT)
                && state.getValue(BlockStateProperties.AGE_3) >= 3;
    }

    @Override
    public FruitKind kind(ServerLevel level, BlockPos pos, BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return kindCache.computeIfAbsent(key, id -> {
            Item item = state.getBlock().asItem();
            ItemStack rep = item != Items.AIR ? new ItemStack(item) : ItemStack.EMPTY;
            Set<Item> accepted = item != Items.AIR ? Set.of(item) : Set.of();
            return new FruitKind(
                    id,
                    rep,
                    accepted,
                    s -> s.hasProperty(BlockStateProperties.PERSISTENT)
                            && !s.getValue(BlockStateProperties.PERSISTENT)
                            && s.hasProperty(BlockStateProperties.AGE_3)
                            && s.getValue(BlockStateProperties.AGE_3) >= 3,
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
        return false;
    }
}
