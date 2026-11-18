package com.mik.rats_squirrel.compat;

import com.mik.rats_squirrel.fruit.FruitAdapter;
import com.mik.rats_squirrel.fruit.FruitKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;
import java.util.Set;

/**
 * Vanilla sweet berry bushes and cave vines.
 *
 * <p>Sweet berries are mature at {@code AGE >= 2} and reset to age 1. Cave vines use
 * {@code BERRIES == true} and reset through {@code useWithoutItem}.</p>
 */
public final class VanillaFruitAdapter implements FruitAdapter {
    private static final ResourceLocation SWEET_BERRIES_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "sweet_berries");
    private static final ResourceLocation GLOW_BERRIES_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "glow_berries");

    private static final FruitKind SWEET_BERRIES_KIND = new FruitKind(
            SWEET_BERRIES_ID,
            new ItemStack(Items.SWEET_BERRIES),
            Set.of(Items.SWEET_BERRIES),
            state -> state.getBlock() instanceof SweetBerryBushBlock && state.getValue(SweetBerryBushBlock.AGE) >= 2,
            false
    );

    private static final FruitKind GLOW_BERRIES_KIND = new FruitKind(
            GLOW_BERRIES_ID,
            new ItemStack(Items.GLOW_BERRIES),
            Set.of(Items.GLOW_BERRIES),
            state -> state.hasProperty(CaveVines.BERRIES) && state.getValue(CaveVines.BERRIES),
            true
    );

    @Override
    public boolean matches(BlockState state) {
        return state.getBlock() instanceof SweetBerryBushBlock || state.hasProperty(CaveVines.BERRIES);
    }

    @Override
    public boolean isMature(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 2;
        }
        if (state.hasProperty(CaveVines.BERRIES)) {
            return state.getValue(CaveVines.BERRIES);
        }
        return false;
    }

    @Override
    public FruitKind kind(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof SweetBerryBushBlock) {
            return SWEET_BERRIES_KIND;
        }
        if (state.hasProperty(CaveVines.BERRIES)) {
            return GLOW_BERRIES_KIND;
        }
        return FruitKind.placeholder(SWEET_BERRIES_ID, false);
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
        if (state.getBlock() instanceof SweetBerryBushBlock) {
            int age = state.getValue(SweetBerryBushBlock.AGE);
            return List.of(new ItemStack(Items.SWEET_BERRIES, age == 3 ? 2 : 1));
        }
        if (state.hasProperty(CaveVines.BERRIES)) {
            return List.of(new ItemStack(Items.GLOW_BERRIES, 1));
        }
        return List.of();
    }

    @Override
    public void applyExactReset(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof SweetBerryBushBlock) {
            level.setBlock(pos, state.setValue(SweetBerryBushBlock.AGE, 1), 2);
        } else if (state.hasProperty(CaveVines.BERRIES)) {
            level.setBlock(pos, state.setValue(CaveVines.BERRIES, false), 2);
        }
    }
}
