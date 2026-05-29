package com.github.alexthe666.rats.server.block;

import com.mojang.serialization.MapCodec;


import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.server.block.entity.RatCraftingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class RatCraftingTableBlock extends BaseEntityBlock {

	public RatCraftingTableBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}
	@Override
	protected MapCodec<? extends RatCraftingTableBlock> codec() {
		return simpleCodec(RatCraftingTableBlock::new);
	}


	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof RatCraftingTableBlockEntity table) {
			for (int i = 0; i < table.bufferHandler.getSlots(); i++) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), table.bufferHandler.getStackInSlot(i));
			}
			level.updateNeighbourForOutputSignal(pos, this);
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RatCraftingTableBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, RatsBlockEntityRegistry.RAT_CRAFTING_TABLE.get(), RatCraftingTableBlockEntity::tick);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
		BlockEntity be = level.getBlockEntity(pos);
		if (stack.has(DataComponents.CUSTOM_NAME) && be instanceof RatCraftingTableBlockEntity table) {
			table.setCustomName(stack.getHoverName());
		}
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!player.isCrouching()) {
			if (level.isClientSide()) {
				return ItemInteractionResult.SUCCESS;
			}
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof RatCraftingTableBlockEntity table) {
				((ServerPlayer) player).openMenu(table, pos);
				player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
				return ItemInteractionResult.CONSUME;
			}
		}
		return ItemInteractionResult.FAIL;
	}
}
