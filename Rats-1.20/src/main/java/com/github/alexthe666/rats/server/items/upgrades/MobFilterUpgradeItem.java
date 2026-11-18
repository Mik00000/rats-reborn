package com.github.alexthe666.rats.server.items.upgrades;

import com.github.alexthe666.rats.server.misc.RatsItemStackData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

public class MobFilterUpgradeItem extends BaseRatUpgradeItem {
   public MobFilterUpgradeItem(Properties properties) {
      super(properties, 1, 2);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      if (level.isClientSide() && player.getItemInHand(hand).is(this)) {
         com.github.alexthe666.rats.client.RatsClientHooks.openMobFilterScreen(hand);
         return InteractionResultHolder.success(player.getItemInHand(hand));
      } else {
         return super.use(level, player, hand);
      }
   }

   public static boolean isWhitelist(ItemStack stack) {
      return RatsItemStackData.hasTag(stack) && RatsItemStackData.getTag(stack).getBoolean("Whitelist");
   }

   public static void setWhitelist(ItemStack stack, boolean whitelist) {
      RatsItemStackData.getOrCreateTag(stack).putBoolean("Whitelist", whitelist);
   }

   public static List<String> getSelectedMobs(ItemStack stack) {
      if (!RatsItemStackData.hasTag(stack)) {
         return new ArrayList<>();
      } else {
         List<String> mobs = new ArrayList<>();
         ListTag tag = RatsItemStackData.getTag(stack).getList("Mobs", 8);

         for (int i = 0; i < tag.size(); i++) {
            String s = tag.getString(i);
            mobs.add(s);
         }

         return mobs;
      }
   }

   public static void setMobs(ItemStack stack, List<String> mobs) {
      ListTag tag = new ListTag();

      for (String mob : mobs) {
         ResourceLocation mobId = ResourceLocation.tryParse(mob);
         if (mobId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(mobId)) {
            tag.add(StringTag.valueOf(mob));
         }
      }

      RatsItemStackData.getOrCreateTag(stack).put("Mobs", tag);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(
         Component.translatable(
               "item.rats.rat_upgrade_mob_filter.mode",
               new Object[]{
                  Component.translatable(isWhitelist(stack) ? "item.rats.rat_upgrade_mob_filter.whitelist" : "item.rats.rat_upgrade_mob_filter.blacklist")
               }
            )
            .withStyle(ChatFormatting.GRAY)
      );
      tooltip.add(Component.translatable("item.rats.rat_upgrade_mob_filter.selected_mobs").withStyle(ChatFormatting.GRAY));
      if (!getSelectedMobs(stack).isEmpty()) {
         List<String> mobs = getSelectedMobs(stack);

         for (int i = 0; i < mobs.size() && i < 3; i++) {
            tooltip.add(CommonComponents.space().append(Component.literal(mobs.get(i)).withStyle(ChatFormatting.GRAY)));
         }

         if (mobs.size() > 3) {
            tooltip.add(CommonComponents.space().append(Component.translatable("rats.and_more", new Object[]{mobs.size() - 3}).withStyle(ChatFormatting.GRAY)));
         }
      }
   }
}
