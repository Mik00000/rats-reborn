package com.github.alexthe666.rats.server.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PartyHatItem extends Item {
	public PartyHatItem(Properties properties) {
		super(properties);
	}

	public int getColor(ItemStack stack) {
		DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
		return 0xFF000000 | (dyedColor != null ? dyedColor.rgb() : 0x25C9E7);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		if (!stack.has(DataComponents.DYED_COLOR)) {
			tooltip.add(Component.translatable("item.rats.party_hat.desc").withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public @Nullable EquipmentSlot getEquipmentSlot(ItemStack stack) {
		return EquipmentSlot.HEAD;
	}

	public boolean canEquip(ItemStack stack, EquipmentSlot slot, Entity entity) {
		return slot == EquipmentSlot.HEAD;
	}
}
