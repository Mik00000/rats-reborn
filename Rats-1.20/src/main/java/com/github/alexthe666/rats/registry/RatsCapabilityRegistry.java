package com.github.alexthe666.rats.registry;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.capability.SelectedRat;
import com.github.alexthe666.rats.server.capability.SelectedRatCapability;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class RatsCapabilityRegistry {
	public static final EntityCapability<SelectedRat, Void> SELECTED_RAT = EntityCapability.createVoid(SelectedRat.ID, SelectedRat.class);
	private static final Map<Player, SelectedRatCapability> SELECTED_RAT_BY_PLAYER = Collections.synchronizedMap(new WeakHashMap<>());

	// Регистрирует capability-поставщики Rats для NeoForge 1.21.1; принимает событие регистрации и ничего не возвращает.
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		try {
			event.registerEntity(SELECTED_RAT, EntityType.PLAYER, (player, context) -> getSelectedRatCapability((Player) player));

			event.registerEntity(Capabilities.ItemHandler.ENTITY, RatsEntityRegistry.TAMED_RAT.get(), (rat, context) -> rat.getItemHandler());
			event.registerEntity(Capabilities.ItemHandler.ENTITY_AUTOMATION, RatsEntityRegistry.TAMED_RAT.get(), (rat, context) -> rat.getItemHandler());

			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RatsBlockEntityRegistry.AUTO_CURDLER.get(), (curdler, side) -> curdler.getItemHandler(side));
			event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RatsBlockEntityRegistry.AUTO_CURDLER.get(), (curdler, side) -> curdler.getFluidHandler());
			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RatsBlockEntityRegistry.RAT_CHEESE_FEEDER.get(), (feeder, side) -> feeder.getItemHandler(side));

			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RatsBlockEntityRegistry.UPGRADE_COMBINER.get(), (combiner, side) -> combiner.getItemHandler(side));
			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RatsBlockEntityRegistry.RAT_QUARRY.get(), (quarry, side) -> quarry.getItemHandler(side));
			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RatsBlockEntityRegistry.RAT_CRAFTING_TABLE.get(), (table, side) -> table.getItemHandler(side));

			event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, RatsBlockEntityRegistry.RAT_CAGE_WHEEL.get(), (wheel, side) -> wheel.getEnergyStorage());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось зарегистрировать capability Rats для NeoForge 1.21.1", exception);
			throw exception;
		}
	}

	// Возвращает capability выбранной крысы для игрока; принимает игрока, отдаёт объект состояния выбранной крысы.
	public static SelectedRatCapability getSelectedRatCapability(Player player) {
		try {
			return SELECTED_RAT_BY_PLAYER.computeIfAbsent(player, key -> {
				SelectedRatCapability capability = new SelectedRatCapability();
				capability.setPlayer(key);
				return capability;
			});
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить capability выбранной крысы для игрока {}", player.getGameProfile().getName(), exception);
			throw exception;
		}
	}
}
