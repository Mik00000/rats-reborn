package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.registry.RatlantisEntityRegistry;
import com.github.alexthe666.rats.registry.RatsToolMaterialRegistry;
import com.github.alexthe666.rats.server.entity.misc.RatProtector;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;

public class RatlantisSwordItem extends SwordItem {
    public RatlantisSwordItem(Item.Properties properties) {
        super(RatsToolMaterialRegistry.RATLANTIS, properties.attributes(SwordItem.createAttributes(RatsToolMaterialRegistry.RATLANTIS, 4, -2.4F)));
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        LivingEntity living;
        if (entity instanceof LivingEntity && (living = (LivingEntity)entity).isAlive() && !(living instanceof RatProtector) && player.swingTime == 0) {
            RatProtector protector = new RatProtector((EntityType<? extends AbstractRat>)((EntityType)RatlantisEntityRegistry.RAT_PROTECTOR.get()), entity.level());
            protector.moveTo(player.getX(), player.getY() + 1.0, player.getZ(), player.getYRot(), player.getXRot());
            protector.setTarget(living);
            entity.level().addFreshEntity((Entity)protector);
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add((Component)Component.translatable((String)(this.getDescriptionId() + ".desc0")).withStyle(ChatFormatting.YELLOW));
        tooltip.add((Component)Component.translatable((String)(this.getDescriptionId() + ".desc1")).withStyle(ChatFormatting.GRAY));
    }
}
