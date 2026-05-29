package com.github.alexthe666.rats.server.block;

import com.github.alexthe666.rats.registry.RatsSoundRegistry;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CheeseCauldronBlock extends AbstractCauldronBlock {

	public static final MapCodec<CheeseCauldronBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BlockBehaviour.propertiesCodec(),
			BuiltInRegistries.BLOCK.byNameCodec().fieldOf("drop").forGetter(block -> block.dropBlock),
			CauldronInteraction.CODEC.fieldOf("interactions").forGetter(block -> block.interactions)
	).apply(instance, CheeseCauldronBlock::new));

	private final Block dropBlock;
	private final CauldronInteraction.InteractionMap interactions;

	public CheeseCauldronBlock(BlockBehaviour.Properties properties, Block dropBlock, CauldronInteraction.InteractionMap interactions) {
		super(properties, interactions);
		this.dropBlock = dropBlock;
		this.interactions = interactions;
	}

	@Override
	protected MapCodec<? extends CheeseCauldronBlock> codec() {
		return CODEC;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (super.useItemOn(stack, state, level, pos, player, hand, hit) == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
			level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
			level.playSound(null, pos, RatsSoundRegistry.CHEESE_CAULDRON_EMPTY.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
			level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
			if (!player.getInventory().add(new ItemStack(this.dropBlock))) {
				player.drop(new ItemStack(this.dropBlock), false);
			}
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public VoxelShape getInteractionShape(BlockState state, BlockGetter getter, BlockPos pos) {
		return this.getShape(state, getter, pos, CollisionContext.empty());
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
		return Shapes.join(Shapes.block(), box(2.0D, 15.0D, 2.0D, 14.0D, 16.0D, 14.0D), BooleanOp.ONLY_FIRST);
	}

	@Override
	protected double getContentHeight(BlockState state) {
		return 0.9375D;
	}

	@Override
	public boolean isFull(BlockState state) {
		return true;
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return new ItemStack(Items.CAULDRON);
	}
}
