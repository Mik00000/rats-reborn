package com.github.alexthe666.rats.server.world;

import com.github.alexthe666.rats.RatsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RatProtectedTreeBlocksData extends SavedData {
	private static final String IDENTIFIER = "rats_protected_tree_blocks";
	private final Set<Long> protectedBlocks = new HashSet<>();

	// Creates empty storage for protected tree blocks; no parameters.
	public RatProtectedTreeBlocksData() {
	}

	// Returns the protected block data for a server level; takes the level, returns the data or null on the client.
	@Nullable
	public static RatProtectedTreeBlocksData get(Level level) {
		try {
			if (!(level instanceof ServerLevel serverLevel)) {
				return null;
			}
			return serverLevel.getDataStorage().computeIfAbsent(
					new SavedData.Factory<>(RatProtectedTreeBlocksData::new, RatProtectedTreeBlocksData::load, null),
					IDENTIFIER
			);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to get the protected wood block list for {}", level.dimension().location(), exception);
			throw exception;
		}
	}

	// Loads the protected wood block list; takes NBT and registries, returns the filled data.
	public static RatProtectedTreeBlocksData load(CompoundTag tag, HolderLookup.Provider provider) {
		try {
			RatProtectedTreeBlocksData data = new RatProtectedTreeBlocksData();
			Arrays.stream(tag.getLongArray("ProtectedTreeBlocks")).forEach(data.protectedBlocks::add);
			return data;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to load the protected wood block list", exception);
			throw exception;
		}
	}

	// Marks a block as player-placed; takes the position, returns nothing.
	public void protect(BlockPos pos) {
		try {
			this.protectedBlocks.add(pos.asLong());
			this.setDirty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to protect wood block {}", pos, exception);
			throw exception;
		}
	}

	// Removes protection from a block; takes the position, returns nothing.
	public void unprotect(BlockPos pos) {
		try {
			if (this.protectedBlocks.remove(pos.asLong())) {
				this.setDirty();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to remove protection from wood block {}", pos, exception);
			throw exception;
		}
	}

	// Checks a block's protection from the lumberjack; takes the position, returns true if the block was placed by a player.
	public boolean isProtected(BlockPos pos) {
		try {
			return this.protectedBlocks.contains(pos.asLong());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check protection of wood block {}", pos, exception);
			throw exception;
		}
	}

	// Saves protected blocks to NBT; takes the tag and registries, returns the tag.
	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
		try {
			long[] blocks = this.protectedBlocks.stream().mapToLong(Long::longValue).toArray();
			tag.putLongArray("ProtectedTreeBlocks", blocks);
			return tag;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to save the protected wood block list", exception);
			throw exception;
		}
	}
}
