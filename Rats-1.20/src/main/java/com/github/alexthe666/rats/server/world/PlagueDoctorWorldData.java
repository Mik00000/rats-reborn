package com.github.alexthe666.rats.server.world;

import com.github.alexthe666.rats.RatsMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlagueDoctorWorldData extends SavedData {
	private static final Logger LOGGER = RatsMod.LOGGER;
	private static final String IDENTIFIER = "rats_world_data";
	private int doctorSpawnDelay;
	private int doctorSpawnChance;
	private UUID doctorID;

	public PlagueDoctorWorldData() {
		this.setDirty();
	}

	/**
	 * Returns the saved plague doctor data for the given server level.
	 * Parameters: {@code level} - the level (only {@link ServerLevel}).
	 * Returns a {@link PlagueDoctorWorldData} instance or {@code null} on the client / on error.
	 * {@code getServer().getLevel(dimension)} must not be called while the level is being constructed - the level is not registered yet.
	 */
	@Nullable
	public static PlagueDoctorWorldData get(Level level) {
		try {
			if (!(level instanceof ServerLevel server)) {
				return null;
			}
			return server.getDataStorage().computeIfAbsent(
					new SavedData.Factory<>(PlagueDoctorWorldData::new, PlagueDoctorWorldData::load, null),
					IDENTIFIER);
		} catch (Exception e) {
			LOGGER.error("Failed to get PlagueDoctorWorldData for level {}: {}", level.dimension().location(), e.getMessage(), e);
			return null;
		}
	}

	public int getDoctorSpawnDelay() {
		return this.doctorSpawnDelay;
	}

	public void setDoctorSpawnDelay(int delay) {
		this.doctorSpawnDelay = delay;
		this.setDirty();
	}

	public int getDoctorSpawnChance() {
		return this.doctorSpawnChance;
	}

	public void setDoctorSpawnChance(int chance) {
		this.doctorSpawnChance = chance;
		this.setDirty();
	}

	public void setPlagueDoctorID(UUID id) {
		this.doctorID = id;
		this.setDirty();
	}

	public static PlagueDoctorWorldData load(CompoundTag tag, HolderLookup.Provider provider) {
		PlagueDoctorWorldData data = new PlagueDoctorWorldData();
		if (tag.contains("PlagueDoctorSpawnDelay", 99)) {
			data.doctorSpawnDelay = tag.getInt("PlagueDoctorSpawnDelay");
		}

		if (tag.contains("PlagueDoctorSpawnChance", 99)) {
			data.doctorSpawnChance = tag.getInt("PlagueDoctorSpawnChance");
		}

		if (tag.contains("PlagueDoctorId", 8)) {
			data.doctorID = UUID.fromString(tag.getString("PlagueDoctorId"));
		}

		return data;
	}

	@Override
	public CompoundTag save(CompoundTag compound, HolderLookup.Provider provider) {
		compound.putInt("PlagueDoctorSpawnDelay", this.doctorSpawnDelay);
		compound.putInt("PlagueDoctorSpawnChance", this.doctorSpawnChance);
		if (this.doctorID != null) {
			compound.putString("PlagueDoctorId", this.doctorID.toString());
		}
		return compound;
	}
}
