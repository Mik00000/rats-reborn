package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatsMod;
import net.minecraft.world.entity.monster.ZombieVillager;

import java.lang.reflect.Method;
import java.util.UUID;

public final class ZombieVillagerConversionHelper {

	private static final Method START_CONVERTING = findStartConvertingMethod();

	private ZombieVillagerConversionHelper() {
	}

	private static Method findStartConvertingMethod() {
		try {
			Method method = ZombieVillager.class.getDeclaredMethod("startConverting", UUID.class, int.class);
			method.setAccessible(true);
			return method;
		} catch (ReflectiveOperationException e) {
			RatsMod.LOGGER.error("Failed to find ZombieVillager.startConverting: {}", e.toString());
			return null;
		}
	}

	public static boolean startConverting(ZombieVillager zombie, UUID ownerId, int duration) {
		if (START_CONVERTING == null) {
			return false;
		}
		try {
			START_CONVERTING.invoke(zombie, ownerId, duration);
			return true;
		} catch (ReflectiveOperationException e) {
			RatsMod.LOGGER.error("Failed to invoke ZombieVillager.startConverting: {}", e.toString());
			return false;
		}
	}
}