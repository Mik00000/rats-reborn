package com.github.alexthe666.rats.server.block;

import com.github.alexthe666.rats.registry.RatsBlockRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("deprecation")
public class RatQuarryPlatformBlock extends ScaffoldingBlock {

	private static final VoxelShape SHAPE = Block.box(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D);

	public RatQuarryPlatformBlock(BlockBehaviour.Properties properties) {
		// Creates the quarry platform block; takes block properties, returns nothing.
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(DISTANCE, 0).setValue(WATERLOGGED, false).setValue(BOTTOM, false));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
		// Returns the platform visual shape; takes the state, level, position and context, returns a thin top slab.
		return SHAPE;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
		// Returns the platform display shape; takes the state, level, position and context, returns the block shape.
		return this.getShape(state, getter, pos, context);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
		// Returns the platform physical shape; takes the state, level, position and context, returns the scaffolding shape.
		return super.getCollisionShape(state, getter, pos, context);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader reader, BlockPos pos) {
		// Checks platform stability; takes the state, level and position, returns true without vanilla cascading scaffolding destruction.
		return true;
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
		// Updates platform neighbours; takes states and positions, returns a stable dry platform that does not self-destruct from water.
		return state.setValue(DISTANCE, 0).setValue(WATERLOGGED, false).setValue(BOTTOM, false);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		// Handles a scheduled platform tick; takes the state, level, position and random, keeps the platform after old scaffolding ticks.
		if (level.getBlockState(pos).is(this)) {
			level.setBlock(pos, state.setValue(DISTANCE, 0).setValue(WATERLOGGED, false).setValue(BOTTOM, false), 3);
		}
	}

	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDist) {
		// Handles falling onto the platform; takes the level, state, position, entity and fall height, returns nothing.
		if (!(entity instanceof TamedRat)) {
			super.fallOn(level, state, pos, entity, fallDist);
		}
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		// Checks regular platform pathability; takes the state and path type, returns scaffolding behaviour.
		return super.isPathfindable(state, type);
	}
}
