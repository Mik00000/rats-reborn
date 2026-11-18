package com.github.alexthe666.rats.server.items.upgrades;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.ChangesAIUpgrade;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.DamageImmunityUpgrade;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.StatBoostingUpgrade;
import com.mik.rats_squirrel.ai.RatSquirrelHarvestGoal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Map;

public class SquirrelRatUpgradeItem extends BaseRatUpgradeItem implements ChangesAIUpgrade, DamageImmunityUpgrade, StatBoostingUpgrade {

    public SquirrelRatUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isImmuneToDamageSource(TamedRat rat, DamageSource source) {
        return source.is(DamageTypes.FALL)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.SWEET_BERRY_BUSH);
    }

    @Override
    public List<Goal> addNewWorkGoals(TamedRat rat) {
        return List.of(new RatSquirrelHarvestGoal(rat));
    }

    @Override
    public Map<Holder<Attribute>, Double> getAttributeBoosts() {
        return Map.of(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.rats.rat_upgrade_squirrel.desc").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.rats.rat_upgrade_squirrel.synergy").withStyle(ChatFormatting.GOLD));
    }
}
