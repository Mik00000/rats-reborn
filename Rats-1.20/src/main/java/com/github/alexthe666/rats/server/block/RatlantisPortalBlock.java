package com.github.alexthe666.rats.server.block;

import com.github.alexthe666.rats.registry.RatlantisBlockRegistry;
import com.github.alexthe666.rats.registry.worldgen.RatlantisDimensionRegistry;
import com.github.alexthe666.rats.data.tags.RatsBlockTags;
import com.github.alexthe666.rats.server.block.CustomItemRarity;
import com.github.alexthe666.rats.server.block.entity.RatlantisPortalBlockEntity;
import com.github.alexthe666.rats.server.world.RatlantisTeleporter;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.portal.DimensionTransition;
import org.jetbrains.annotations.Nullable;

public class RatlantisPortalBlock
extends BaseEntityBlock
implements CustomItemRarity,
Portal {
    public RatlantisPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends RatlantisPortalBlock> codec() {
        return RatlantisPortalBlock.simpleCodec(RatlantisPortalBlock::new);
    }

    @Override
    public Rarity getRarity() {
        return Rarity.EPIC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!entity.isPassenger() && !entity.isVehicle() && entity.canUsePortal(true)) {
            if (entity.isOnPortalCooldown()) {
                entity.setPortalCooldown();
            } else {
                entity.setAsInsidePortal(this, pos);
            }
        }
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        if (entity.isPassenger() || entity.isVehicle() || !entity.canUsePortal(true)) {
            return null;
        }
        ResourceKey<Level> destination = entity.level().dimension() == RatlantisDimensionRegistry.DIMENSION_KEY ? Level.OVERWORLD : RatlantisDimensionRegistry.DIMENSION_KEY;
        MinecraftServer server = entity.getServer();
        if (server == null) {
            return null;
        }
        ServerLevel dest = server.getLevel(destination);
        if (dest == null) {
            return null;
        }
        return RatlantisTeleporter.createTransition(entity, dest);
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        if (entity instanceof Player player) {
            return player.getAbilities().invulnerable ? 1 : 40;
        }
        return 0;
    }

    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.CONFUSION;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!this.canSurviveAt(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    public boolean canSurviveAt(Level level, BlockPos pos) {
        return !(!level.getBlockState(pos.above()).is((Block)RatlantisBlockRegistry.RATLANTIS_PORTAL.get()) && !level.getBlockState(pos.above()).is(RatsBlockTags.MARBLED_CHEESE) || !level.getBlockState(pos.below()).is((Block)RatlantisBlockRegistry.RATLANTIS_PORTAL.get()) && !level.getBlockState(pos.below()).is(RatsBlockTags.MARBLED_CHEESE));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RatlantisPortalBlockEntity) {
            for (int j = 0; j < 2; ++j) {
                double d0 = (float)pos.getX() + random.nextFloat();
                double d1 = (float)pos.getY() + random.nextFloat();
                double d2 = (float)pos.getZ() + random.nextFloat();
                double d3 = ((double)random.nextFloat() - 0.5) * 0.5;
                double d4 = ((double)random.nextFloat() - 0.5) * 0.5;
                double d5 = ((double)random.nextFloat() - 0.5) * 0.5;
                level.addParticle((ParticleOptions)ParticleTypes.END_ROD, d0, d1, d2, d3, d4, d5);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RatlantisPortalBlockEntity(pos, state);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
