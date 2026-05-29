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

	// Создаёт блок крысиного домика; принимает свойства блока, задаёт FACING по умолчанию на север.
	public RatHouseBlock(BlockBehaviour.Properties properties) {
		super(properties);
		try {
			this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось инициализировать блок rat_house: {}", exception.getMessage(), exception);
			throw exception;
		}
	}

	// Возвращает кодек блока для сериализации; ничего не принимает, отдаёт MapCodec.
	@Override
	protected MapCodec<? extends RatHouseBlock> codec() {
		return simpleCodec(RatHouseBlock::new);
	}

	// Возвращает позицию входа перед дыркой домика; принимает состояние и позицию блока, отдаёт позицию у входа.
	public static BlockPos getEntryPos(BlockState state, BlockPos pos) {
		try {
			Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
			return pos.relative(facing);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось вычислить вход домика в {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Возвращает центр входа перед домиком; принимает состояние и позицию блока, отдаёт точку движения крысы.
	public static Vec3 getEntryCenter(BlockState state, BlockPos pos) {
		try {
			return Vec3.atBottomCenterOf(getEntryPos(state, pos)).add(0.0D, 0.1D, 0.0D);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось вычислить центр входа домика в {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Возвращает форму блока (полный куб); принимает состояние, мир, позицию и контекст столкновения.
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return FULL_SHAPE;
	}

	// Возвращает коллизию блока (полный куб); принимает состояние, мир, позицию и контекст столкновения.
	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return CLIMBABLE_COLLISION_SHAPE;
	}

	// Проверяет, может ли сущность использовать боковую грань домика как лестницу; принимает состояние, мир, позицию и сущность, отдаёт true около боковой грани.
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
			RatsMod.LOGGER.error("Не удалось проверить лестницу домика в {} для сущности {}: {}", pos, entity == null ? "null" : entity.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Показывает частицу над занятым домиком; принимает состояние, мир, позицию и случайный генератор, ничего не отдаёт.
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		try {
			if (level.getBlockEntity(pos) instanceof RatHouseBlockEntity house && house.hasStoredRat() && random.nextInt(8) == 0) {
				Vec3 entry = getEntryCenter(state, pos);
				level.addParticle(ParticleTypes.HAPPY_VILLAGER, entry.x, entry.y + 0.35D, entry.z, 0.0D, 0.02D, 0.0D);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось показать индикатор занятого домика в {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Обрабатывает ПКМ по домику: пустой рукой достаёт крысу из домика на плечо игрока.
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
				throw new IllegalStateException("У домика нет RatHouseBlockEntity");
			}
			return house.releaseRatToPlayer(player) ? ItemInteractionResult.CONSUME : ItemInteractionResult.CONSUME;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось обработать ПКМ по rat_house в {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Выставляет ориентацию при установке: дырка (FACING) смотрит на игрока; принимает контекст размещения, отдаёт BlockState.
	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		try {
			Direction playerFacing = context.getHorizontalDirection();
			Direction holeFacing = playerFacing.getOpposite();
			return this.defaultBlockState().setValue(FACING, holeFacing);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выставить FACING для rat_house в {}: {}", context.getClickedPos(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Поворачивает блок; принимает состояние и поворот, отдаёт новое состояние.
	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	// Отражает блок; принимает состояние и зеркало, отдаёт новое состояние.
	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	// Возвращает тип отрисовки — обычная JSON-модель; принимает состояние.
	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}


	// Возвращает тикер block entity домика; принимает мир, состояние и тип block entity.
	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, RatsBlockEntityRegistry.RAT_HOUSE.get(), RatHouseBlockEntity::tick);
	}

	// Регистрирует свойства состояния блока; принимает строитель StateDefinition.
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	// Создаёт block entity домика; принимает позицию и состояние, отдаёт RatHouseBlockEntity или null.
	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		try {
			return new RatHouseBlockEntity(pos, state);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось создать block entity rat_house в {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Удаляет block entity при замене блока; принимает старое/новое состояние, мир, позицию и флаг перемещения.
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
			RatsMod.LOGGER.error("Не удалось удалить rat_house в {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}
}
