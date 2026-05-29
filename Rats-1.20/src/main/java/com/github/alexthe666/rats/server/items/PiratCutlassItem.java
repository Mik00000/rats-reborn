package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.registry.RatsToolMaterialRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;

public class PiratCutlassItem extends SwordItem {

	public PiratCutlassItem(Item.Properties properties, boolean ghost) {
		super(ghost ? RatsToolMaterialRegistry.GHOST_CUTLASS : RatsToolMaterialRegistry.CUTLASS, properties);
	}
}
