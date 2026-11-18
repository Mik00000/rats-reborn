package com.mik.rats_squirrel.compat;

import com.mik.rats_squirrel.fruit.FruitAdapter;
import com.mik.rats_squirrel.fruit.FruitKind;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Package-private Phase 0 scaffold for optional-mod adapters.
 *
 * <p>Subclasses only supply their identity and tree/bush classification. Phase 1 replaces the
 * {@code matches}/{@code isMature}/{@code interact} bodies with the real adapter logic while
 * keeping the same public class names.</p>
 */
abstract class StubFruitAdapter implements FruitAdapter {
    private final FruitKind placeholder;

    protected StubFruitAdapter(ResourceLocation id, boolean treeFruit) {
        this.placeholder = FruitKind.placeholder(id, treeFruit);
    }

    @Override
    public boolean matches(BlockState state) {
        return false;
    }

    @Override
    public boolean isMature(ServerLevel level, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public FruitKind kind(ServerLevel level, BlockPos pos, BlockState state) {
        return placeholder;
    }

    @Override
    public InteractionResult interact(ServerLevel level, BlockPos pos, BlockState state, FakePlayer player) {
        return InteractionResult.PASS;
    }
}
