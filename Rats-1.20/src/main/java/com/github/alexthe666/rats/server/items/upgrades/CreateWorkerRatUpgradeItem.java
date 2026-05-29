package com.github.alexthe666.rats.server.items.upgrades;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CreateWorkerRatUpgradeItem extends BaseRatUpgradeItem {
	// Создаёт улучшение работяги Create; принимает свойства предмета и задаёт длину описания.
	public CreateWorkerRatUpgradeItem(Item.Properties properties) {
		super(properties, 1, 3);
	}

	// Проверяет, должна ли крыса складывать предмет; принимает крысу и стек, отдаёт true для обычного транспорта.
	@Override
	public boolean shouldDepositItem(TamedRat rat, ItemStack stack) {
		try {
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить сдачу предмета работягой Create {} для крысы {}", stack, rat.getUUID(), exception);
			throw exception;
		}
	}
}
