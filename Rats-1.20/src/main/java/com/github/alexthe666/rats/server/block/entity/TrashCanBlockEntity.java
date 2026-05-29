package com.github.alexthe666.rats.server.block.entity;

import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.server.block.TrashCanBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TrashCanBlockEntity extends BlockEntity {
	public float lidProgress;
	public float prevLidProgress;

	public TrashCanBlockEntity(BlockPos pos, BlockState state) {
		super(RatsBlockEntityRegistry.TRASH_CAN.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, TrashCanBlockEntity te) {
		te.prevLidProgress = te.lidProgress;
		if (state.getValue(TrashCanBlock.OPEN) && te.lidProgress < 20.0F) {
			te.lidProgress += 2.0F;
		} else if (!state.getValue(TrashCanBlock.OPEN) && te.lidProgress > 0.0F) {
			te.lidProgress -= 2.0F;
		}
	}
}
