package com.github.alexthe666.rats.server.block.entity;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.server.block.RatTubeBlock;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class RatTubeBlockEntity extends BlockEntity {

	public Direction opening = null;
	public boolean isNode = false;
	private int color;

	public RatTubeBlockEntity(BlockPos pos, BlockState state) {
		super(RatsBlockEntityRegistry.RAT_TUBE.get(), pos, state);
	}

	// Tube block entity tick; entities do not get stuck in the block - movement is driven by RatTubeMoveControl.
	public static void tick(Level level, BlockPos pos, BlockState state, RatTubeBlockEntity te) {
		try {
			if (!level.isClientSide() && state.getBlock() instanceof RatTubeBlock) {
				for (TamedRat rat : level.getEntitiesOfClass(TamedRat.class, RatTubeBlock.createTubeBounds(pos))) {
					if (rat.isInTube() && rat.blockPosition().equals(pos)) {
						rat.onTubeSegmentTick(pos, state);
					}
				}
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Tube tick error at {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithId(registries);
	}

	@Override
	protected void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
		compound.putBoolean("RatNode", isNode);
		compound.putInt("OpenSide", opening == null ? -1 : opening.ordinal());
		compound.putInt("TubeColor", color);
		super.saveAdditional(compound, registries);
	}

	@Override
	protected void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {
		super.loadAdditional(compound, registries);
		isNode = compound.getBoolean("RatNode");
		int i = compound.getInt("OpenSide");
		if (i == -1) {
			opening = null;
		} else {
			opening = Direction.values()[Mth.clamp(i, 0, Direction.values().length - 1)];
		}
		color = compound.getInt("TubeColor");
	}

	// Remembers the side of the entrance fitting without opening a passage; takes the side and an active flag.
	public void setEntranceData(Direction side, boolean active) {
		if (active) {
			this.opening = side;
			this.isNode = true;
		} else {
			this.clearEntranceMount();
		}
	}

	// Resets the entrance fitting; takes nothing.
	public void clearEntranceMount() {
		this.opening = null;
		this.isNode = false;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int colorIn) {
		this.color = colorIn;
	}
}
