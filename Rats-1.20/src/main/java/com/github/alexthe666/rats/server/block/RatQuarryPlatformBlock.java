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
		// Создаёт блок платформы карьера; принимает свойства блока, ничего не возвращает.
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(DISTANCE, 0).setValue(WATERLOGGED, false).setValue(BOTTOM, false));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
		// Возвращает визуальную форму платформы; принимает состояние, мир, позицию и контекст, отдаёт тонкую верхнюю плиту.
		return SHAPE;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
		// Возвращает отображаемую форму платформы; принимает состояние, мир, позицию и контекст, отдаёт форму блока.
		return this.getShape(state, getter, pos, context);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
		// Возвращает физическую форму платформы; принимает состояние, мир, позицию и контекст, отдаёт форму строительных лесов.
		return super.getCollisionShape(state, getter, pos, context);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader reader, BlockPos pos) {
		// Проверяет устойчивость платформы; принимает состояние, мир и позицию, отдаёт true без ванильного каскадного разрушения лесов.
		return true;
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
		// Обновляет соседей платформы; принимает состояния и позиции, возвращает устойчивую сухую платформу без самоуничтожения от воды.
		return state.setValue(DISTANCE, 0).setValue(WATERLOGGED, false).setValue(BOTTOM, false);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		// Обрабатывает отложенный тик платформы; принимает состояние, мир, позицию и случайность, сохраняет платформу после старых тиков лесов.
		if (level.getBlockState(pos).is(this)) {
			level.setBlock(pos, state.setValue(DISTANCE, 0).setValue(WATERLOGGED, false).setValue(BOTTOM, false), 3);
		}
	}

	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDist) {
		// Обрабатывает падение на платформу; принимает уровень, состояние, позицию, сущность и высоту падения, не возвращает значение.
		if (!(entity instanceof TamedRat)) {
			super.fallOn(level, state, pos, entity, fallDist);
		}
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		// Проверяет обычную проходимость платформы; принимает состояние и тип пути, отдаёт поведение строительных лесов.
		return super.isPathfindable(state, type);
	}
}
