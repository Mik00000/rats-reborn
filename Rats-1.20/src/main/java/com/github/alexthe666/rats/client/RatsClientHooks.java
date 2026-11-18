package com.github.alexthe666.rats.client;

import com.github.alexthe666.rats.client.events.ForgeClientEvents;
import com.github.alexthe666.rats.client.events.ModClientEvents;
import com.github.alexthe666.rats.client.render.RatsRenderType;
import com.github.alexthe666.rats.registry.RatsCapabilityRegistry;
import com.github.alexthe666.rats.server.capability.SelectedRat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.text.WordUtils;

public class RatsClientHooks {
   public static boolean isRatSelectedOnStaff(TamedRat rat) {
      return ForgeClientEvents.isRatSelectedOnStaff(rat);
   }

   public static float getPartialTick() {
      return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
   }

   public static void openMobFilterScreen(InteractionHand hand) {
      ModClientEvents.openMobFilterScreen(hand);
   }

   public static Level getClientLevel() {
      return ModClientEvents.getClientLevel();
   }

   public static void appendStaffHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      if (Minecraft.getInstance().player == null) {
         return;
      }
      SelectedRat selectedRat = (SelectedRat)Minecraft.getInstance().player.getCapability(RatsCapabilityRegistry.SELECTED_RAT);
      if (selectedRat != null) {
         TamedRat rat = selectedRat.getSelectedRat();
         if (rat != null) {
            tooltip.add(
               Component.translatable("item.rats.cheese_staff.bound_rat", new Object[]{rat.getDisplayName(), rat.getUUID().toString()})
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})
            );
         }
      }
   }

   public static void appendRatbowEssenceHoverText(Item item, ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      RatsRenderType.GlintType type = RatsRenderType.GlintType.getGlintBasedOnKeyword(stack.getHoverName().getString());
      if (type != null && type.changesItemTexture()) {
         tooltip.add(
            Component.translatable("item.rats.ratbow_essence.flag", new Object[]{WordUtils.capitalize(type.name().toLowerCase(Locale.ROOT))})
               .withStyle(ChatFormatting.GRAY)
         );
      } else {
         tooltip.add(Component.translatable(item.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
      }
   }
}
