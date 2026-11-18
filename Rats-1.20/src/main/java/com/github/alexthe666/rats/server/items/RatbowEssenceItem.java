package com.github.alexthe666.rats.server.items;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import org.apache.commons.lang3.text.WordUtils;

public class RatbowEssenceItem extends Item {
   public RatbowEssenceItem(Properties properties) {
      super(properties);
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      com.github.alexthe666.rats.client.RatsClientHooks.appendRatbowEssenceHoverText(this, stack, context, tooltip, flag);
   }
}
