package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsSoundRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatsItemStackData;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;

public class RatPapersItem extends Item {
   public RatPapersItem(Properties properties) {
      super(properties);
   }

   public boolean isFoil(ItemStack stack) {
      return isEntityBound(stack);
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      if (!isEntityBound(stack)) {
         tooltip.add(Component.translatable("item.rats.rat_papers.desc0").withStyle(ChatFormatting.GRAY));
         tooltip.add(Component.translatable("item.rats.rat_papers.desc1").withStyle(ChatFormatting.GRAY));
      }

      tooltip.add(Component.translatable("item.rats.rat_papers.desc2").withStyle(ChatFormatting.GRAY));
      if (RatsItemStackData.hasTag(stack)) {
         String ratName = Component.translatable("entity.rats.tamed_rat").getString();
         String entity = RatsItemStackData.getTag(stack).getString("RatName");
         Component rat = Component.empty();
         if (RatsItemStackData.getTag(stack).hasUUID("RatUUID")) {
            if (entity.isEmpty()) {
               rat = Component.literal(ratName + " (" + RatsItemStackData.getTag(stack).getUUID("RatUUID") + ")").withStyle(ChatFormatting.GRAY);
            } else {
               rat = Component.literal(entity).withStyle(ChatFormatting.GRAY);
            }
         }

         tooltip.add(Component.translatable("item.rats.rat_papers.bound_rat", new Object[]{rat.getString()}).withStyle(ChatFormatting.GRAY));
      }
   }

   public static boolean isEntityBound(ItemStack stack) {
      return RatsItemStackData.getOrCreateTag(stack).hasUUID("RatUUID");
   }

   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      CompoundTag nbt = RatsItemStackData.getOrCreateTag(stack);
      if (target instanceof Player transferTo) {
         try {
            if (nbt.hasUUID("RatUUID")) {
               UUID ratUUID = nbt.getUUID("RatUUID");
               if (!target.level().isClientSide()
                  && target.level().getServer().getLevel(target.level().dimension()).getEntity(ratUUID) instanceof TamedRat rat
                  && rat.isTame()
                  && rat.isOwnedBy(player)) {
                  rat.tame(transferTo);
                  stack.shrink(1);
                  player.level().playSound(player, player.blockPosition(), (SoundEvent)RatsSoundRegistry.RAT_TRANSFER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
               }

               return InteractionResult.sidedSuccess(player.level().isClientSide());
            }
         } catch (Exception var10) {
            player.displayClientMessage(Component.literal("Couldnt transfer ownership! Check the log and report this!").withStyle(ChatFormatting.RED), true);
            RatsMod.LOGGER.error("Couldnt transfer rat ownership to {}!", transferTo.getStringUUID(), var10);
         }

         return InteractionResult.PASS;
      } else {
         if (target instanceof TamedRat rat && rat.isOwnedBy(player)) {
            if (rat.hasCustomName()) {
               nbt.putString("RatName", rat.getCustomName().getString());
            }

            nbt.putUUID("RatUUID", rat.getUUID());
            RatsItemStackData.setTag(stack, nbt);
            return InteractionResult.sidedSuccess(player.level().isClientSide());
         }

         return InteractionResult.PASS;
      }
   }
}
