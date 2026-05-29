package com.github.alexthe666.rats.server.world;

import net.minecraft.world.level.block.grower.TreeGrower;

/**
 * В 1.21.1 произвольный {@link TreeGrower} регистрируется через кодек; до отдельной регистрации
 * используется джунглевый ростовик с мега-деревом как ближайший аналог.
 */
public final class PiratTreeGrower {

	public static final TreeGrower GROWER = TreeGrower.JUNGLE;

	private PiratTreeGrower() {
	}
}
