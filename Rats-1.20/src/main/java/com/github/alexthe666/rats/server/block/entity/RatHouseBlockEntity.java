package com.github.alexthe666.rats.server.block.entity;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RatHouseBlockEntity extends BlockEntity {
	@Nullable
	private UUID boundRatUuid;
	@Nullable
	private CompoundTag storedRatTag;
	@Nullable
	private CompoundTag pendingRespawnTag;
	private int pendingRespawnTicks;

	// Creates an empty house block entity; takes the block position and state.
	public RatHouseBlockEntity(BlockPos pos, BlockState state) {
		super(RatsBlockEntityRegistry.RAT_HOUSE.get(), pos, state);
	}


	// Ticks the house: updates respawn and releases a sleeping rat after the sleep window; takes the level, position, state and block entity.
	public static void tick(Level level, BlockPos pos, BlockState state, RatHouseBlockEntity house) {
		try {
			if (level.isClientSide()) {
				return;
			}
			if (house.pendingRespawnTag != null) {
				if (house.pendingRespawnTicks > 0) {
					house.pendingRespawnTicks--;
					house.setChangedAndSync();
					return;
				}
				Vec3 entry = com.github.alexthe666.rats.server.block.RatHouseBlock.getEntryCenter(state, pos);
				house.releasePendingRespawn(entry.x, entry.y, entry.z);
				return;
			}
			if (house.storedRatTag != null && !RatDaySchedule.shouldSleep(level)) {
				Vec3 entry = com.github.alexthe666.rats.server.block.RatHouseBlock.getEntryCenter(state, pos);
				house.releaseSleepingRatToWorld(entry.x, entry.y, entry.z);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to update house {}", pos, exception);
			throw exception;
		}
	}

	// Checks whether the house's live slot is occupied; no parameters, returns true if a rat is stored inside.
	public boolean hasStoredRat() {
		return this.storedRatTag != null;
	}

	// Returns the number of rats inside the house; no parameters, returns 0 or 1 for Jade and visual checks.
	public int getStoredRatCount() {
		try {
			return this.storedRatTag == null ? 0 : 1;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to get the rat count of house {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}


	// Checks whether the house is waiting for a rat to respawn; no parameters, returns true while the timer is active.
	public boolean hasPendingRespawn() {
		try {
			return this.pendingRespawnTag != null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the pending respawn of house {}", this.worldPosition, exception);
			throw exception;
		}
	}

	// Returns the remaining respawn time; no parameters, returns a number of ticks.
	public int getPendingRespawnTicks() {
		try {
			return this.pendingRespawnTicks;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to get the respawn timer of house {}", this.worldPosition, exception);
			throw exception;
		}
	}

	// Returns the name of the rat inside the house; no parameters, returns the stored name or an empty string.
	public String getStoredRatName() {
		try {
			if (this.storedRatTag == null) {
				return "";
			}
			String storedName = this.storedRatTag.getString("StoredRatName");
			if (!storedName.isBlank()) {
				return storedName;
			}
			if (this.storedRatTag.contains("CustomName", Tag.TAG_STRING)) {
				return this.storedRatTag.getString("CustomName");
			}
			return this.boundRatUuid == null ? "" : this.boundRatUuid.toString();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to get the name of the rat in house {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Checks whether the house can accept a specific rat; takes the rat, returns true if the binding matches.
	public boolean canAcceptRat(TamedRat rat) {
		try {
			return this.boundRatUuid == null || this.boundRatUuid.equals(rat.getUUID());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the binding of house {} for rat {}", this.worldPosition, rat.getUUID(), exception);
			throw exception;
		}
	}

	// Binds the house to a rat; takes the rat and player, returns true if the binding was written.
	public boolean bindRat(TamedRat rat, Player player) {
		try {
			if (this.boundRatUuid != null && !this.boundRatUuid.equals(rat.getUUID())) {
				player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_OCCUPIED), true);
				RatsMod.LOGGER.warn("Player {} tried to bind rat {} to occupied house {} (occupied by {})", player.getGameProfile().getName(), rat.getUUID(), this.worldPosition, this.boundRatUuid);
				return false;
			}
			this.boundRatUuid = rat.getUUID();
			rat.setBoundHousePos(GlobalPos.of(player.level().dimension(), this.worldPosition));
			this.setChangedAndSync();
			player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_BIND, rat.getName()), true);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to bind rat {} to house {}: {}", rat.getUUID(), this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Hides a rat in the house; takes a live rat, returns true if the NBT was saved and the entity removed from the world.
	public boolean storeRat(TamedRat rat) {
		try {
			if (this.storedRatTag != null) {
				RatsMod.LOGGER.warn("House {} already contains a rat, new rat {} was not placed", this.worldPosition, rat.getUUID());
				return false;
			}
			if (!this.canAcceptRat(rat)) {
				RatsMod.LOGGER.warn("House {} refused rat {}: bound to UUID {}", this.worldPosition, rat.getUUID(), this.boundRatUuid);
				return false;
			}
			CompoundTag tag = new CompoundTag();
			rat.addAdditionalSaveData(tag);
			tag.putUUID("StoredRatUuid", rat.getUUID());
			tag.putString("StoredRatName", rat.getName().getString());
			this.boundRatUuid = rat.getUUID();
			this.storedRatTag = tag;
			rat.stopRiding();
			rat.getNavigation().stop();
			rat.discard();
			this.setChangedAndSync();
			Level level = this.getLevel();
			if (level != null) {
				level.playSound(null, this.worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.7F, 1.1F);
			}
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to put rat {} into house {}: {}", rat.getUUID(), this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}



	// Starts respawning a dead rat at the house; takes the rat and delay, returns true if the timer was accepted.
	public boolean beginRespawn(TamedRat rat, int ticks) {
		try {
			if (this.storedRatTag != null || this.pendingRespawnTag != null) {
				RatsMod.LOGGER.warn("House {} did not accept respawn of rat {}: stored={}, pending={}", this.worldPosition, rat.getUUID(), this.storedRatTag != null, this.pendingRespawnTag != null);
				return false;
			}
			if (!this.canAcceptRat(rat)) {
				RatsMod.LOGGER.warn("House {} refused respawn of rat {}: bound to UUID {}", this.worldPosition, rat.getUUID(), this.boundRatUuid);
				return false;
			}
			CompoundTag tag = new CompoundTag();
			rat.addAdditionalSaveData(tag);
			tag.putUUID("StoredRatUuid", rat.getUUID());
			tag.putString("StoredRatName", rat.getName().getString());
			tag.putShort("HurtTime", (short) 0);
			tag.putInt("HurtByTimestamp", 0);
			tag.putShort("DeathTime", (short) 0);
			this.boundRatUuid = rat.getUUID();
			this.pendingRespawnTag = tag;
			this.pendingRespawnTicks = Math.max(1, ticks);
			this.setChangedAndSync();
			RatsMod.LOGGER.info("House {} started respawning rat {} in {} ticks", this.worldPosition, rat.getUUID(), this.pendingRespawnTicks);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to start respawning rat {} at house {}", rat.getUUID(), this.worldPosition, exception);
			throw exception;
		}
	}

	// Releases a rat that was waiting to respawn; takes spawn coordinates, returns true on success.
	private boolean releasePendingRespawn(double x, double y, double z) {
		try {
			Level level = this.getLevel();
			if (!(level instanceof ServerLevel) || this.pendingRespawnTag == null) {
				return false;
			}
			TamedRat rat = RatsEntityRegistry.TAMED_RAT.get().create(level);
			if (rat == null) {
				throw new IllegalStateException("Failed to create a TamedRat from the registry");
			}
			CompoundTag tag = this.pendingRespawnTag.copy();
			rat.readAdditionalSaveData(tag);
			rat.setHealth(rat.getMaxHealth());
			rat.setRespawnCountdown(0);
			rat.setBoundHousePos(GlobalPos.of(level.dimension(), this.worldPosition));
			rat.setPos(x, y, z);
			level.addFreshEntity(rat);
			this.boundRatUuid = rat.getUUID();
			this.pendingRespawnTag = null;
			this.pendingRespawnTicks = 0;
			this.setChangedAndSync();
			level.playSound(null, this.worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
			RatsMod.LOGGER.info("Rat {} returned at house {}", rat.getUUID(), this.worldPosition);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to release the respawning rat from house {}", this.worldPosition, exception);
			throw exception;
		}
	}


	// Releases a woken rat at the entrance; takes spawn coordinates, returns true if released.
	private boolean releaseSleepingRatToWorld(double x, double y, double z) {
		try {
			Level level = this.getLevel();
			if (!(level instanceof ServerLevel) || this.storedRatTag == null) {
				return false;
			}
			TamedRat rat = RatsEntityRegistry.TAMED_RAT.get().create(level);
			if (rat == null) {
				throw new IllegalStateException("Failed to create a TamedRat from the registry");
			}
			CompoundTag tag = this.storedRatTag.copy();
			rat.readAdditionalSaveData(tag);
			rat.setHealth(Math.max(1.0F, rat.getMaxHealth()));
			rat.setRespawnCountdown(0);
			rat.setBoundHousePos(GlobalPos.of(level.dimension(), this.worldPosition));
			rat.setPos(x, y, z);
			level.addFreshEntity(rat);
			this.boundRatUuid = rat.getUUID();
			this.storedRatTag = null;
			this.setChangedAndSync();
			level.playSound(null, this.worldPosition, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.7F, 1.15F);
			RatsMod.LOGGER.info("Rat {} woke up and left house {}", rat.getUUID(), this.worldPosition);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to release the woken rat from house {}", this.worldPosition, exception);
			throw exception;
		}
	}

	// Releases the stored rat into the world when the house is destroyed; takes the spawn position, returns true if released.
	public boolean releaseRatToWorld(double x, double y, double z) {
		try {
			Level level = this.getLevel();
			if (level == null) {
				throw new IllegalStateException("The house has no level");
			}
			if (level.isClientSide() || this.storedRatTag == null && this.pendingRespawnTag == null) {
				return false;
			}
			TamedRat rat = RatsEntityRegistry.TAMED_RAT.get().create(level);
			if (rat == null) {
				throw new IllegalStateException("Failed to create a TamedRat from the registry");
			}
			CompoundTag tag = (this.storedRatTag != null ? this.storedRatTag : this.pendingRespawnTag).copy();
			rat.readAdditionalSaveData(tag);
			rat.setHealth(Math.max(1.0F, rat.getMaxHealth()));
			rat.setRespawnCountdown(0);
			rat.setBoundHousePos(null);
			rat.setPos(x, y, z);
			level.addFreshEntity(rat);
			this.boundRatUuid = null;
			this.storedRatTag = null;
			this.pendingRespawnTag = null;
			this.pendingRespawnTicks = 0;
			this.setChangedAndSync();
			level.playSound(null, this.worldPosition, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F);
			RatsMod.LOGGER.info("Rat {} released from destroyed house {}", rat.getUUID(), this.worldPosition);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to release the rat from destroyed house {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Takes the stored rat out of the house onto the player's shoulder; takes the player, returns true on success.
	public boolean releaseRatToPlayer(Player player) {
		try {
			Level level = this.getLevel();
			if (level == null) {
				throw new IllegalStateException("The house has no level");
			}
			if (this.storedRatTag == null) {
				player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_EMPTY), true);
				return false;
			}
			if (player.getPassengers().size() >= 3) {
				player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_SHOULDER_FULL), true);
				return false;
			}
			TamedRat rat = RatsEntityRegistry.TAMED_RAT.get().create(level);
			if (rat == null) {
				throw new IllegalStateException("Failed to create a TamedRat from the registry");
			}
			CompoundTag tag = this.storedRatTag.copy();
			rat.readAdditionalSaveData(tag);
			rat.setBoundHousePos(GlobalPos.of(level.dimension(), this.worldPosition));
			rat.setPos(player.getX(), player.getY(), player.getZ());
			level.addFreshEntity(rat);
			rat.startRiding(player, true);
			this.boundRatUuid = rat.getUUID();
			this.storedRatTag = null;
			this.pendingRespawnTag = null;
			this.pendingRespawnTicks = 0;
			this.setChangedAndSync();
			level.playSound(null, this.worldPosition, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.7F, 1.2F);
			player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_WITHDRAW, rat.getName()), true);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to take the rat out of house {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}


	// Removes the house-to-rat binding; takes the rat and player, returns true if the house was freed.
	public boolean clearBinding(TamedRat rat, Player player) {
		try {
			if (this.storedRatTag != null) {
				RatsMod.LOGGER.warn("House {} does not unbind rat {}: the rat is already inside", this.worldPosition, rat.getUUID());
				return false;
			}
			if (this.boundRatUuid == null || !this.boundRatUuid.equals(rat.getUUID())) {
				return false;
			}
			this.boundRatUuid = null;
			rat.setBoundHousePos(null);
			this.setChangedAndSync();
			player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_UNBIND, rat.getName()), true);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to unbind rat {} from house {}: {}", rat.getUUID(), this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Called when the block is removed from the world; takes nothing, logs the house state.
	public void onRemoved() {
		try {
			RatsMod.LOGGER.debug("Removed rat_house block entity at {}, storedRat={}", this.worldPosition, this.storedRatTag != null);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Error removing rat_house block entity at {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Saves the house NBT; takes the tag and HolderLookup, returns a CompoundTag.
	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		try {
			super.saveAdditional(tag, registries);
			if (this.boundRatUuid != null) {
				tag.putUUID("BoundRatUuid", this.boundRatUuid);
			}
			if (this.storedRatTag != null) {
				tag.put("StoredRat", this.storedRatTag.copy());
			}
			if (this.pendingRespawnTag != null) {
				tag.put("PendingRespawnRat", this.pendingRespawnTag.copy());
				tag.putInt("PendingRespawnTicks", this.pendingRespawnTicks);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to save rat_house NBT at {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Loads the house NBT; takes the tag and HolderLookup.
	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		try {
			super.loadAdditional(tag, registries);
			this.boundRatUuid = tag.hasUUID("BoundRatUuid") ? tag.getUUID("BoundRatUuid") : null;
			this.storedRatTag = tag.contains("StoredRat", Tag.TAG_COMPOUND) ? tag.getCompound("StoredRat").copy() : null;
			this.pendingRespawnTag = tag.contains("PendingRespawnRat", Tag.TAG_COMPOUND) ? tag.getCompound("PendingRespawnRat").copy() : null;
			this.pendingRespawnTicks = tag.getInt("PendingRespawnTicks");
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to load rat_house NBT at {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Builds the client sync packet; no parameters, returns a ClientboundBlockEntityDataPacket.
	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		try {
			return ClientboundBlockEntityDataPacket.create(this);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to create the rat_house update packet at {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Builds the client sync tag; takes a HolderLookup, returns a CompoundTag.
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		try {
			return this.saveWithoutMetadata(registries);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to build the rat_house updateTag at {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Marks the house as changed and sends a block update to clients; no parameters.
	private void setChangedAndSync() {
		try {
			this.setChanged();
			Level level = this.getLevel();
			if (level != null) {
				BlockState state = level.getBlockState(this.worldPosition);
				level.sendBlockUpdated(this.worldPosition, state, state, 3);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to sync rat_house at {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}
}
