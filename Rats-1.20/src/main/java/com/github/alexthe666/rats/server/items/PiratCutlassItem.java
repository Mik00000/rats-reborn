package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.registry.RatsToolMaterialRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public class PiratCutlassItem extends SwordItem {
    public PiratCutlassItem(Item.Properties properties, boolean ghost) {
        super(ghost ? RatsToolMaterialRegistry.GHOST_CUTLASS : RatsToolMaterialRegistry.CUTLASS, initProps(properties, ghost));
    }

    private static Item.Properties initProps(Item.Properties properties, boolean ghost) {
        Tier tier = ghost ? RatsToolMaterialRegistry.GHOST_CUTLASS : RatsToolMaterialRegistry.CUTLASS;
        return properties.attributes(SwordItem.createAttributes(tier, 3, -2.4F));
    }
}
