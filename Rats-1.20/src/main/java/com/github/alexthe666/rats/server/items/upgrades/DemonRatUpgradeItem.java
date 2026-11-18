package com.github.alexthe666.rats.server.items.upgrades;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.ChangesOverlayUpgrade;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.DamageImmunityUpgrade;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.GlowingEyesUpgrade;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.PostAttackUpgrade;
import com.github.alexthe666.rats.server.misc.RatsItemStackData;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

public class DemonRatUpgradeItem
   extends StatBoostingRatUpgradeItem
   implements GlowingEyesUpgrade,
   DamageImmunityUpgrade,
   PostAttackUpgrade,
   ChangesOverlayUpgrade {
   public DemonRatUpgradeItem(Properties properties) {
      super(properties, 1, 2, () -> Map.of(Attributes.MAX_HEALTH, RatConfig.demonHealthUpgrade, Attributes.ATTACK_DAMAGE, RatConfig.demonDamageUpgrade), false);
   }

   public static boolean isSoulVersion(ItemStack stack) {
      return RatsItemStackData.hasTag(stack) && RatsItemStackData.getTag(stack).getBoolean("Soul");
   }

   public static ItemStack getDemonUpgrade(boolean soul) {
      ItemStack stack = new ItemStack((ItemLike)RatsItemRegistry.RAT_UPGRADE_DEMON.get());
      CompoundTag tag = RatsItemStackData.getOrCreateTag(stack);
      tag.putBoolean("Soul", soul);
      RatsItemStackData.setTag(stack, tag);
      return stack;
   }

   @Override
   public RenderType getEyeTexture(ItemStack stack) {
      return isSoulVersion(stack)
         ? RenderType.eyes(ResourceLocation.fromNamespaceAndPath("rats", "textures/entity/demon_rat/soul_demon_rat_eye.png"))
         : RenderType.eyes(ResourceLocation.fromNamespaceAndPath("rats", "textures/entity/demon_rat/demon_rat_eye.png"));
   }

   @Override
   public boolean isImmuneToDamageSource(TamedRat rat, DamageSource source) {
      return source.is(DamageTypeTags.IS_FIRE);
   }

   @Override
   public void afterHit(TamedRat rat, LivingEntity target) {
      if (!target.fireImmune()) {
         target.igniteForSeconds(10.0F);
      }
   }

   @Nullable
   @Override
   public RenderType getOverlayTexture(ItemStack stack, TamedRat rat, float partialTicks) {
      return isSoulVersion(stack)
         ? RenderType.entitySmoothCutout(ResourceLocation.fromNamespaceAndPath("rats", "textures/entity/demon_rat/soul_demon_rat.png"))
         : RenderType.entitySmoothCutout(ResourceLocation.fromNamespaceAndPath("rats", "textures/entity/demon_rat/demon_rat.png"));
   }
}
