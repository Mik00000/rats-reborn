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

	// Создаёт блок кормушки; принимает свойства блока, настраивает пустое состояние.
	public RatCheeseFeederBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.getStateDefinition().any().setValue(HAS_CHEESE, false));
	}

	// Возвращает кодек блока для сохранения состояния; ничего не принимает.
	@Override
	protected MapCodec<? extends RatCheeseFeederBlock> codec() {
		return simpleCodec(RatCheeseFeederBlock::new);
	}

	// Обрабатывает замену блока; принимает старое и новое состояние, выбрасывает сыр только при реальном разрушении блока.
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
			RatsMod.LOGGER.error("Не удалось удалить кормушку для сыра в {}", pos, exception);
			throw exception;
		}
	}

	// Возвращает форму кормушки; принимает состояние, мир, позицию и контекст столкновения.
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	// Обрабатывает ПКМ: сыр кладёт в кормушку, остальные предметы открывают инвентарь.
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		try {
			if (player.isCrouching()) {
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			}
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (!(blockEntity instanceof RatCheeseFeederBlockEntity feeder)) {
				throw new IllegalStateException("У кормушки нет block entity");
			}
			if (!stack.isEmpty() && RatCheeseFeederBlockEntity.isAllowedCheese(stack)) {
				if (level.isClientSide()) {
					return ItemInteractionResult.SUCCESS;
				}
				int inserted = feeder.addCheeseFrom(stack);
				if (inserted <= 0) {
					RatsMod.LOGGER.warn("Игрок {} не смог положить сыр в кормушку {}: инвентарь полон", player.getGameProfile().getName(), pos);
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
			RatsMod.LOGGER.error("Не удалось обработать кормушку для сыра в {}", pos, exception);
			throw exception;
		}
	}

	// Возвращает тип отрисовки блока; принимает состояние, отдаёт обычную модель.
	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	// Создаёт block entity кормушки; принимает позицию и состояние блока.
	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RatCheeseFeederBlockEntity(pos, state);
	}

	// Возвращает тикер block entity; принимает мир, состояние и тип block entity.
	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, RatsBlockEntityRegistry.RAT_CHEESE_FEEDER.get(), RatCheeseFeederBlockEntity::tick);
	}

	// Добавляет свойства состояния блока; принимает строитель состояния, ничего не отдаёт.
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HAS_CHEESE);
	}
}
