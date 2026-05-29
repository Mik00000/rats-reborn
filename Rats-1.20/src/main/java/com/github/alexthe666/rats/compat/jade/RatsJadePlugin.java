package com.github.alexthe666.rats.compat.jade;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.block.RatHouseBlock;
import com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class RatsJadePlugin implements IWailaPlugin {
	private static final RatHouseProvider RAT_HOUSE_PROVIDER = new RatHouseProvider();

	// Регистрирует серверные данные Jade; принимает общий регистратор, ничего не отдаёт.
	@Override
	public void register(IWailaCommonRegistration registration) {
		try {
			registration.registerBlockDataProvider(RAT_HOUSE_PROVIDER, RatHouseBlockEntity.class);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось зарегистрировать серверные данные Jade для домика крысы", exception);
			throw exception;
		}
	}

	// Регистрирует клиентскую строку Jade; принимает клиентский регистратор, ничего не отдаёт.
	@Override
	public void registerClient(IWailaClientRegistration registration) {
		try {
			registration.addConfig(RatHouseProvider.UID, true);
			registration.registerBlockComponent(RAT_HOUSE_PROVIDER, RatHouseBlock.class);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось зарегистрировать клиентский компонент Jade для домика крысы", exception);
			throw exception;
		}
	}

	private static class RatHouseProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
		private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(RatsMod.MODID, "rat_house");
		private static final String COUNT_KEY = "RatHouseRatCount";
		private static final String NAME_KEY = "RatHouseRatName";
		private static final String RESPAWN_KEY = "RatHouseRespawnTicks";

		// Возвращает идентификатор провайдера Jade; параметров нет, отдаёт ResourceLocation.
		@Override
		public ResourceLocation getUid() {
			return UID;
		}

		// Передаёт Jade данные домика с сервера; принимает NBT и доступ к блоку, ничего не отдаёт.
		@Override
		public void appendServerData(CompoundTag data, BlockAccessor accessor) {
			try {
				BlockEntity blockEntity = accessor.getBlockEntity();
				if (blockEntity instanceof RatHouseBlockEntity house) {
					data.putInt(COUNT_KEY, house.getStoredRatCount());
					data.putString(NAME_KEY, house.getStoredRatName());
					data.putInt(RESPAWN_KEY, house.getPendingRespawnTicks());
				}
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось собрать данные Jade для домика крысы в {}", accessor.getPosition(), exception);
				throw exception;
			}
		}

		// Добавляет строки в подсказку Jade; принимает подсказку, доступ к блоку и конфиг, ничего не отдаёт.
		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			try {
				CompoundTag data = accessor.getServerData();
				int count = data.getInt(COUNT_KEY);
				String name = data.getString(NAME_KEY);
				int respawnTicks = data.getInt(RESPAWN_KEY);
				if (!data.contains(COUNT_KEY) && accessor.getBlockEntity() instanceof RatHouseBlockEntity house) {
					count = house.getStoredRatCount();
					name = house.getStoredRatName();
					respawnTicks = house.getPendingRespawnTicks();
				}
				if (respawnTicks > 0) {
					tooltip.add(Component.translatable("jade.rats.rat_house.respawn", Math.max(1, (respawnTicks + 19) / 20)));
					return;
				}
				if (count <= 0) {
					tooltip.add(Component.translatable("jade.rats.rat_house.empty"));
					return;
				}
				if (name.isBlank()) {
					tooltip.add(Component.translatable("jade.rats.rat_house.occupied", count, 1));
					return;
				}
				tooltip.add(Component.translatable("jade.rats.rat_house.rat", name));
			} catch (RuntimeException exception) {
				RatsMod.LOGGER.error("Не удалось вывести подсказку Jade для домика крысы в {}", accessor.getPosition(), exception);
				throw exception;
			}
		}
	}
}
