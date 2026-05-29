package com.github.alexthe666.rats.server.effect;

import com.github.alexthe666.rats.RatsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class ConfitByaldiMobEffect extends MobEffect {

	public ConfitByaldiMobEffect() {
		super(MobEffectCategory.BENEFICIAL, 0XFFDD59);
		this.addAttributeModifier(Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "confit_speed"), 1.0D, AttributeModifier.Operation.ADD_VALUE);
	}

	@Override
	public boolean applyEffectTick(LivingEntity living, int amplifier) {
		if (living.getHealth() < living.getMaxHealth()) {
			living.heal(1.0F);
		}
		if (living instanceof Player player) {
			player.getFoodData().eat(1, 0.1F);
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % 40 == 0;
	}

	@Override
	public void onMobRemoved(LivingEntity living, int amplifier, Entity.RemovalReason reason) {
		super.onMobRemoved(living, amplifier, reason);
		living.setAbsorptionAmount(living.getAbsorptionAmount() - (float) (20 * (amplifier + 1)));
		if (living.getHealth() > living.getMaxHealth()) {
			living.setHealth(living.getMaxHealth());
		}
	}

	@Override
	public void onEffectAdded(LivingEntity living, int amplifier) {
		living.setAbsorptionAmount(living.getAbsorptionAmount() + (float) (20 * (amplifier + 1)));
		super.onEffectAdded(living, amplifier);
	}
}
