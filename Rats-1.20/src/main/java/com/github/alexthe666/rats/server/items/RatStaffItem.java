package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsCapabilityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.capability.SelectedRat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.message.ManageRatStaffPacket;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;

public class RatStaffItem extends LoreTagItem {
   public RatStaffItem(Properties properties) {
      super(properties, 2, false);
   }

   public int getStaff(ItemStack stack) {
      if (stack.is((Item)RatsItemRegistry.PATROL_STICK.get())) {
         return 2;
      } else {
         return stack.is((Item)RatsItemRegistry.RADIUS_STICK.get()) ? 1 : 0;
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);

      try {
         HitResult hitResult = getPlayerPOVHitResult(level, player, Fluid.ANY);
         if (hitResult.getType() != Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
         } else {
            BlockHitResult blockHit = (BlockHitResult)hitResult;
            BlockPos pos = blockHit.getBlockPos();
            if (level.getFluidState(pos).isEmpty()) {
               return InteractionResultHolder.pass(stack);
            } else {
               SelectedRat selectedRat = (SelectedRat)player.getCapability(RatsCapabilityRegistry.SELECTED_RAT);
               TamedRat rat = selectedRat == null ? null : selectedRat.getSelectedRat();
               if (rat == null) {
                  player.displayClientMessage(Component.translatable("item.rats.staff.no_rat").withStyle(ChatFormatting.RED), true);
                  return InteractionResultHolder.fail(stack);
               } else {
                  player.swing(hand);
                  if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                     RatsNetworkHandler.CHANNEL
                        .send(
                           RatsNetworkHandler.playerTarget(() -> serverPlayer),
                           new ManageRatStaffPacket(rat.getId(), pos, blockHit.getDirection().ordinal(), false, true, this.getStaff(stack))
                        );
                  }

                  return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
               }
            }
         }
      } catch (RuntimeException var11) {
         RatsMod.LOGGER.error("Player {} failed to open the rat staff on a fluid", player.getUUID(), var11);
         throw var11;
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      Level level = context.level();
      if (level != null && level.isClientSide()) {
         com.github.alexthe666.rats.client.RatsClientHooks.appendStaffHoverText(stack, context, tooltip, flag);
      }
   }
}
