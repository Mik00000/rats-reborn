package com.github.alexthe666.rats.server.block;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.server.block.entity.RatCheeseFeederBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class RatCheeseFeederBlock extends BaseEntityBlock {
	public static final BooleanProperty HAS_CHEESE = BooleanProperty.create("has_cheese");
	private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);

	// Creates the feeder block; takes block properties, sets up the empty state.
	public RatCheeseFeederBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.getStateDefinition().any().setValue(HAS_CHEESE, false));
	}

	// Returns the block codec for state serialization; takes nothing.
	@Override
	protected MapCodec<? extends RatCheeseFeederBlock> codec() {
		return simpleCodec(RatCheeseFeederBlock::new);
	}

	// Handles block replacement; takes the old and new state, drops cheese only when the block is actually destroyed.
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		try {
			if (!state.is(newState.getBlock())) {
				BlockEntity blockEntity = level.getBlockEntity(pos);
				if (blockEntity instanceof RatCheeseFeederBlockEntity feeder) {
					Containers.dropContents(level, pos, feeder);
					level.updateNeighbourForOutputSignal(pos, this);
				}
				super.onRemove(state, level, pos, newState, isMoving);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to remove the cheese feeder at {}", pos, exception);
			throw exception;
		}
	}

	// Returns the feeder shape; takes the state, level, position and collision context.
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	// Handles right click: cheese goes into the feeder, other items open the inventory.
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		try {
			if (player.isCrouching()) {
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			}
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (!(blockEntity instanceof RatCheeseFeederBlockEntity feeder)) {
				throw new IllegalStateException("The feeder has no block entity");
			}
			if (!stack.isEmpty() && RatCheeseFeederBlockEntity.isAllowedCheese(stack)) {
				if (level.isClientSide()) {
					return ItemInteractionResult.SUCCESS;
				}
				int inserted = feeder.addCheeseFrom(stack);
				if (inserted <= 0) {
					RatsMod.LOGGER.warn("Player {} could not put cheese into feeder {}: inventory is full", player.getGameProfile().getName(), pos);
					return ItemInteractionResult.CONSUME;
				}
				level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.0F);
				return ItemInteractionResult.CONSUME;
			}
			if (level.isClientSide()) {
				return ItemInteractionResult.SUCCESS;
			}
			player.openMenu(state.getMenuProvider(level, pos));
			return ItemInteractionResult.CONSUME;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to handle the cheese feeder at {}", pos, exception);
			throw exception;
		}
	}

	// Returns the block render shape; takes the state, returns the regular model.
	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	// Creates the feeder block entity; takes the block position and state.
	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RatCheeseFeederBlockEntity(pos, state);
	}

	// Returns the block entity ticker; takes the level, state and block entity type.
	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, RatsBlockEntityRegistry.RAT_CHEESE_FEEDER.get(), RatCheeseFeederBlockEntity::tick);
	}

	// Adds block state properties; takes the state builder, returns nothing.
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HAS_CHEESE);
	}
}
