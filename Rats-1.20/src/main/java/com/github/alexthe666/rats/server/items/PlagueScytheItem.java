package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.registry.RatsToolMaterialRegistry;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class PlagueScytheItem extends SwordItem {
    public PlagueScytheItem(Item.Properties properties) {
        super(RatsToolMaterialRegistry.PLAGUE_SCYTHE, properties.attributes(SwordItem.createAttributes(RatsToolMaterialRegistry.PLAGUE_SCYTHE, 5, -2.8F)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add((Component)Component.translatable((String)(this.getDescriptionId() + ".desc")).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility toolAction) {
        return toolAction == ItemAbilities.SWORD_SWEEP;
    }
}
