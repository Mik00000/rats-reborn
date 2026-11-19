package com.github.alexthe666.rats.server.world;

import net.minecraft.world.level.block.grower.TreeGrower;

/**
 * In 1.21.1 a custom {@link TreeGrower} is registered via a codec; until it is registered separately
 * the jungle grower with a mega tree is used as the closest equivalent.
 */
public final class PiratTreeGrower {

	public static final TreeGrower GROWER = TreeGrower.JUNGLE;

	private PiratTreeGrower() {
	}
}
