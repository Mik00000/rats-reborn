package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatsItemStackData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class RatSackItem extends Item {
   public RatSackItem(Properties properties) {
      super(properties);
   }

   public static void packRatIntoSack(ItemStack sack, TamedRat rat, int ratCount) {
      CompoundTag tag = RatsItemStackData.getTag(sack);
      if (tag == null) {
         tag = new CompoundTag();
      }

      CompoundTag ratTag = new CompoundTag();
      rat.addAdditionalSaveData(ratTag);
      if (rat.hasCustomName()) {
         ratTag.putString("CustomName", Serializer.toJson(rat.getCustomName(), rat.level().registryAccess()));
      }

      tag.put("Rat_" + ratCount, ratTag);
      RatsItemStackData.setTag(sack, tag);
   }

   public static int getRatsInSack(ItemStack sack) {
      int ratCount = 0;
      if (RatsItemStackData.getTag(sack) != null) {
         for (String tagInfo : RatsItemStackData.getTag(sack).getAllKeys()) {
            if (tagInfo.contains("Rat")) {
               ratCount++;
            }
         }
      }

      return ratCount;
   }

   public static int ejectRatsFromSack(ItemStack stack, Level level, BlockPos pos) {
      int ratCount = 0;
      if (RatsItemStackData.getTag(stack) != null) {
         for (String tagInfo : RatsItemStackData.getTag(stack).getAllKeys()) {
            if (tagInfo.contains("Rat")) {
               ratCount++;
               CompoundTag ratTag = RatsItemStackData.getTag(stack).getCompound(tagInfo);
               TamedRat rat = new TamedRat((EntityType<? extends TamableAnimal>)RatsEntityRegistry.TAMED_RAT.get(), level);
               rat.readAdditionalSaveData(ratTag);
               if (!ratTag.getString("CustomName").isEmpty()) {
                  rat.setCustomName(Serializer.fromJson(ratTag.getString("CustomName"), level.registryAccess()));
               }

               rat.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
               if (!level.isClientSide()) {
                  level.addFreshEntity(rat);
               }
            }
         }
      }

      return ratCount;
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      int ratCount = 0;
      List<String> ratNames = new ArrayList<>();
      if (RatsItemStackData.getTag(stack) != null) {
         for (String tagInfo : RatsItemStackData.getTag(stack).getAllKeys()) {
            if (tagInfo.contains("Rat")) {
               CompoundTag ratTag = RatsItemStackData.getTag(stack).getCompound(tagInfo);
               ratCount++;
               String ratName = Component.translatable("entity.rats.rat").getString();
               if (!ratTag.getString("CustomName").isEmpty()) {
                  Component ratNameTag = Serializer.fromJson(ratTag.getString("CustomName"), context.level().registryAccess());
                  if (ratNameTag != null) {
                     ratName = ratNameTag.getString();
                  }
               }

               ratNames.add(ratName);
            }
         }
      }

      tooltip.add(Component.translatable("item.rats.rat_sack.contains", new Object[]{ratCount, RatConfig.ratSackCapacity}).withStyle(ChatFormatting.GRAY));
      if (!ratNames.isEmpty()) {
         for (int i = 0; i < ratNames.size() && i < 3; i++) {
            tooltip.add(Component.literal(ratNames.get(i)).withStyle(ChatFormatting.GRAY));
         }

         if (ratNames.size() > 3) {
            tooltip.add(Component.translatable("rats.and_more", new Object[]{ratNames.size() - 3}).withStyle(ChatFormatting.GRAY));
         }
      }
   }

   public InteractionResult useOn(UseOnContext context) {
      ItemStack stack = context.getPlayer().getItemInHand(context.getHand());
      if (stack.is((Item)RatsItemRegistry.RAT_SACK.get()) && getRatsInSack(stack) > 0) {
         int ratCount = ejectRatsFromSack(stack, context.getLevel(), context.getClickedPos().relative(context.getClickedFace()));
         if (ratCount > 0) {
            context.getPlayer().displayClientMessage(Component.translatable("item.rats.rat_sack.release", new Object[]{ratCount}), true);
            RatsItemStackData.setTag(stack, new CompoundTag());
            return InteractionResult.SUCCESS;
         }
      }

      return InteractionResult.PASS;
   }

   public void onDestroyed(ItemEntity entity, DamageSource source) {
      ItemStack stack = entity.getItem();
      if (getRatsInSack(stack) > 0) {
         ejectRatsFromSack(stack, entity.level(), entity.blockPosition());
      }
   }
}
