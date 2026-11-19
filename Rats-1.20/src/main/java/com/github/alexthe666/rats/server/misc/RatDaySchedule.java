package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import net.minecraft.world.level.Level;

public final class RatDaySchedule {
	public static final int MORNING_WORK_START = 0;
	public static final int DAY_SLEEP_START = 4000;
	public static final int DAY_SLEEP_END = 8000;
	public static final int EVENING_WORK_END = 12000;

	// Prevents instantiating the utility class; no parameters, returns nothing.
	private RatDaySchedule() {
	}

	// Returns the rat schedule phase; takes the level, returns the phase based on the time of day and the daytime mode setting.
	public static Phase getPhase(Level level) {
		try {
			int dayTime = (int) (level.getDayTime() % 24000L);
			boolean sleepTime = dayTime >= DAY_SLEEP_START && dayTime < DAY_SLEEP_END;
			if (RatConfig.diurnalRats) {
				sleepTime = dayTime >= 16000 && dayTime < 20000;
			}
			if (sleepTime) {
				return Phase.DAY_SLEEP;
			}
			if (dayTime >= MORNING_WORK_START && dayTime < DAY_SLEEP_START) {
				return Phase.MORNING_WORK;
			}
			if (dayTime >= DAY_SLEEP_END && dayTime < EVENING_WORK_END) {
				return Phase.EVENING_WORK;
			}
			return Phase.NIGHT_WORK;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to determine the rat schedule phase", exception);
			throw exception;
		}
	}

	// Checks whether rats should sleep; takes the level, returns true only within the sleep window.
	public static boolean shouldSleep(Level level) {
		try {
			return getPhase(level) == Phase.DAY_SLEEP;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the rat sleep window", exception);
			throw exception;
		}
	}

	// Checks whether rats can work; takes the level, returns true outside the sleep window.
	public static boolean canWork(Level level) {
		try {
			return !shouldSleep(level);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check the rat work window", exception);
			throw exception;
		}
	}

	public enum Phase {
		MORNING_WORK,
		DAY_SLEEP,
		EVENING_WORK,
		NIGHT_WORK
	}
}
