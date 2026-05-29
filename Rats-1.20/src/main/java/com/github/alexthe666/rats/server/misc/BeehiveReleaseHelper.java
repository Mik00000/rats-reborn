package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatsMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public final class BeehiveReleaseHelper {

	private static final Method RELEASE_ALL_OCCUPANTS = findReleaseMethod();

	private BeehiveReleaseHelper() {
	}

	private static Method findReleaseMethod() {
		try {
			Method method = BeehiveBlockEntity.class.getDeclaredMethod("releaseAllOccupants", BlockState.class, BeehiveBlockEntity.BeeReleaseStatus.class);
			method.setAccessible(true);
			return method;
		} catch (ReflectiveOperationException e) {
			RatsMod.LOGGER.error("Failed to find BeehiveBlockEntity.releaseAllOccupants: {}", e.toString());
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Entity> releaseAllOccupants(BeehiveBlockEntity beehive, BlockState state, BeehiveBlockEntity.BeeReleaseStatus status) {
		if (RELEASE_ALL_OCCUPANTS == null) {
			return Collections.emptyList();
		}
		try {
			return (List<Entity>) RELEASE_ALL_OCCUPANTS.invoke(beehive, state, status);
		} catch (ReflectiveOperationException e) {
			RatsMod.LOGGER.error("Failed to invoke BeehiveBlockEntity.releaseAllOccupants: {}", e.toString());
			return Collections.emptyList();
		}
	}
}