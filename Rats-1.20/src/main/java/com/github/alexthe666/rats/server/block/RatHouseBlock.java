package com.github.alexthe666.rats.server.block;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class RatHouseBlock extends BaseEntityBlock {

	public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
	private static final VoxelShape FULL_SHAPE = Shapes.block();
	private static final VoxelShape CLIMBABLE_COLLISION_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

	// Creates the rat house block; takes block properties, sets FACING to north by default.
	public RatHouseBlock(BlockBehaviour.Properties properties) {
		super(properties);
		try {
			this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to initialize the rat_house block: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	// Returns the block codec for serialization; takes nothing, returns a MapCodec.
	@Override
	protected MapCodec<? extends RatHouseBlock> codec() {
		return simpleCodec(RatHouseBlock::new);
	}

	// Returns the entrance position in front of the house hole; takes the block state and position, returns the position at the entrance.
	public static BlockPos getEntryPos(BlockState state, BlockPos pos) {
		try {
			Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
			return pos.relative(facing);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to compute the house entrance at {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Returns the center of the entrance in front of the house; takes the block state and position, returns the rat movement target.
	public static Vec3 getEntryCenter(BlockState state, BlockPos pos) {
		try {
			return Vec3.atBottomCenterOf(getEntryPos(state, pos)).add(0.0D, 0.1D, 0.0D);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to compute the house entrance center at {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Returns the block shape (full cube); takes the state, level, position and collision context.
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return FULL_SHAPE;
	}

	// Returns the block collision (full cube); takes the state, level, position and collision context.
	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return CLIMBABLE_COLLISION_SHAPE;
	}

	// Checks whether an entity can use the house side face as a ladder; takes the state, level, position and entity, returns true near a side face.
	@Override
	public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
		try {
			if (entity == null) {
				return false;
			}
			double localX = entity.getX() - pos.getX();
			double localZ = entity.getZ() - pos.getZ();
			double entityY = entity.getY();
			boolean insideHorizontalBounds = localX >= -0.1D && localX <= 1.1D && localZ >= -0.1D && localZ <= 1.1D;
			boolean nearSide = localX <= 0.18D || localX >= 0.82D || localZ <= 0.18D || localZ >= 0.82D;
			boolean nearHeight = entityY >= pos.getY() - 0.35D && entityY <= pos.getY() + 1.65D;
			return insideHorizontalBounds && nearSide && nearHeight;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the house ladder at {} for entity {}: {}", pos, entity == null ? "null" : entity.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Shows a particle above an occupied house; takes the state, level, position and random source, returns nothing.
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		try {
			if (level.getBlockEntity(pos) instanceof RatHouseBlockEntity house && house.hasStoredRat() && random.nextInt(8) == 0) {
				Vec3 entry = getEntryCenter(state, pos);
				level.addParticle(ParticleTypes.HAPPY_VILLAGER, entry.x, entry.y + 0.35D, entry.z, 0.0D, 0.02D, 0.0D);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to show the occupied house indicator at {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Handles right click on the house: with an empty hand, takes the rat out of the house onto the player's shoulder.
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		try {
			if (!stack.isEmpty() || player.isCrouching()) {
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			}
			if (level.isClientSide()) {
				return ItemInteractionResult.SUCCESS;
			}
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (!(blockEntity instanceof RatHouseBlockEntity house)) {
				throw new IllegalStateException("The house has no RatHouseBlockEntity");
			}
			return house.releaseRatToPlayer(player) ? ItemInteractionResult.CONSUME : ItemInteractionResult.CONSUME;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to handle right click on rat_house at {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Sets the orientation on placement: the hole (FACING) faces the player; takes the placement context, returns a BlockState.
	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		try {
			Direction playerFacing = context.getHorizontalDirection();
			Direction holeFacing = playerFacing.getOpposite();
			return this.defaultBlockState().setValue(FACING, holeFacing);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to set FACING for rat_house at {}: {}", context.getClickedPos(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Rotates the block; takes the state and rotation, returns the new state.
	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	// Mirrors the block; takes the state and mirror, returns the new state.
	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	// Returns the render shape - a regular JSON model; takes the state.
	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}


	// Returns the house block entity ticker; takes the level, state and block entity type.
	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, RatsBlockEntityRegistry.RAT_HOUSE.get(), RatHouseBlockEntity::tick);
	}

	// Registers block state properties; takes the StateDefinition builder.
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	// Creates the house block entity; takes the position and state, returns a RatHouseBlockEntity or null.
	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		try {
			return new RatHouseBlockEntity(pos, state);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to create the rat_house block entity at {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Removes the block entity when the block is replaced; takes the old/new state, level, position and moved flag.
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		try {
			if (!state.is(newState.getBlock())) {
				BlockEntity blockEntity = level.getBlockEntity(pos);
				if (blockEntity instanceof RatHouseBlockEntity house) {
					Vec3 releasePos = getEntryCenter(state, pos);
					house.releaseRatToWorld(releasePos.x, releasePos.y, releasePos.z);
					house.onRemoved();
				}
			}
			super.onRemove(state, level, pos, newState, isMoving);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to remove rat_house at {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}
}
