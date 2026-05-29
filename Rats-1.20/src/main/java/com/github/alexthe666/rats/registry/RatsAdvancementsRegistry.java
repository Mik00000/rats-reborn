package com.github.alexthe666.rats.registry;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.advancements.BlackDeathSummonedTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = RatsMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class RatsAdvancementsRegistry {

	public static BlackDeathSummonedTrigger BLACK_DEATH_SUMMONED;

	// Регистрирует триггеры достижений на этапе RegisterEvent, до заморозки реестра.
	@SubscribeEvent
	public static void registerTriggers(RegisterEvent event) {
		if (!event.getRegistryKey().equals(Registries.TRIGGER_TYPE)) {
			return;
		}
		event.register(Registries.TRIGGER_TYPE, BlackDeathSummonedTrigger.ID, () -> {
			BlackDeathSummonedTrigger trigger = new BlackDeathSummonedTrigger();
			BLACK_DEATH_SUMMONED = trigger;
			return trigger;
		});
	}
}
