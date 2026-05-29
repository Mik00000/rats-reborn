package com.github.alexthe666.rats.server.block.entity;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import com.github.alexthe666.rats.server.misc.RatsLangConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RatHouseBlockEntity extends BlockEntity {
	@Nullable
	private UUID boundRatUuid;
	@Nullable
	private CompoundTag storedRatTag;
	@Nullable
	private CompoundTag pendingRespawnTag;
	private int pendingRespawnTicks;

	// Создаёт пустую сущность блока домика; принимает позицию и состояние блока.
	public RatHouseBlockEntity(BlockPos pos, BlockState state) {
		super(RatsBlockEntityRegistry.RAT_HOUSE.get(), pos, state);
	}


	// Тикает домик: обновляет респавн и выпускает спящую крысу после окна сна; принимает мир, позицию, состояние и block entity.
	public static void tick(Level level, BlockPos pos, BlockState state, RatHouseBlockEntity house) {
		try {
			if (level.isClientSide()) {
				return;
			}
			if (house.pendingRespawnTag != null) {
				if (house.pendingRespawnTicks > 0) {
					house.pendingRespawnTicks--;
					house.setChangedAndSync();
					return;
				}
				Vec3 entry = com.github.alexthe666.rats.server.block.RatHouseBlock.getEntryCenter(state, pos);
				house.releasePendingRespawn(entry.x, entry.y, entry.z);
				return;
			}
			if (house.storedRatTag != null && !RatDaySchedule.shouldSleep(level)) {
				Vec3 entry = com.github.alexthe666.rats.server.block.RatHouseBlock.getEntryCenter(state, pos);
				house.releaseSleepingRatToWorld(entry.x, entry.y, entry.z);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось обновить домик {}", pos, exception);
			throw exception;
		}
	}

	// Проверяет, занята ли живая ячейка домика; параметров нет, отдаёт true если внутри сохранена крыса.
	public boolean hasStoredRat() {
		return this.storedRatTag != null;
	}

	// Возвращает количество крыс внутри домика; параметров нет, отдаёт 0 или 1 для Jade и визуальных проверок.
	public int getStoredRatCount() {
		try {
			return this.storedRatTag == null ? 0 : 1;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить число крыс в домике {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}


	// Проверяет, ждёт ли домик респавна крысы; параметров нет, отдаёт true при активном таймере.
	public boolean hasPendingRespawn() {
		try {
			return this.pendingRespawnTag != null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить ожидающий респавн домика {}", this.worldPosition, exception);
			throw exception;
		}
	}

	// Возвращает оставшееся время респавна; параметров нет, отдаёт число тиков.
	public int getPendingRespawnTicks() {
		try {
			return this.pendingRespawnTicks;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить таймер респавна домика {}", this.worldPosition, exception);
			throw exception;
		}
	}

	// Возвращает имя крысы внутри домика; параметров нет, отдаёт сохранённое имя или пустую строку.
	public String getStoredRatName() {
		try {
			if (this.storedRatTag == null) {
				return "";
			}
			String storedName = this.storedRatTag.getString("StoredRatName");
			if (!storedName.isBlank()) {
				return storedName;
			}
			if (this.storedRatTag.contains("CustomName", Tag.TAG_STRING)) {
				return this.storedRatTag.getString("CustomName");
			}
			return this.boundRatUuid == null ? "" : this.boundRatUuid.toString();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить имя крысы в домике {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Проверяет, может ли домик принять конкретную крысу; принимает крысу, отдаёт true при совпадении привязки.
	public boolean canAcceptRat(TamedRat rat) {
		try {
			return this.boundRatUuid == null || this.boundRatUuid.equals(rat.getUUID());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить привязку домика {} для крысы {}", this.worldPosition, rat.getUUID(), exception);
			throw exception;
		}
	}

	// Привязывает домик к крысе; принимает крысу и игрока, отдаёт true если привязка записана.
	public boolean bindRat(TamedRat rat, Player player) {
		try {
			if (this.boundRatUuid != null && !this.boundRatUuid.equals(rat.getUUID())) {
				player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_OCCUPIED), true);
				RatsMod.LOGGER.warn("Игрок {} пытался привязать крысу {} к занятому домику {} (занят {})", player.getGameProfile().getName(), rat.getUUID(), this.worldPosition, this.boundRatUuid);
				return false;
			}
			this.boundRatUuid = rat.getUUID();
			rat.setBoundHousePos(GlobalPos.of(player.level().dimension(), this.worldPosition));
			this.setChangedAndSync();
			player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_BIND, rat.getName()), true);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось привязать крысу {} к домику {}: {}", rat.getUUID(), this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Прячет крысу в домик; принимает живую крысу, отдаёт true если NBT сохранён и сущность удалена из мира.
	public boolean storeRat(TamedRat rat) {
		try {
			if (this.storedRatTag != null) {
				RatsMod.LOGGER.warn("Домик {} уже содержит крысу, новая крыса {} не помещена", this.worldPosition, rat.getUUID());
				return false;
			}
			if (!this.canAcceptRat(rat)) {
				RatsMod.LOGGER.warn("Домик {} отказал крысе {}: привязан UUID {}", this.worldPosition, rat.getUUID(), this.boundRatUuid);
				return false;
			}
			CompoundTag tag = new CompoundTag();
			rat.addAdditionalSaveData(tag);
			tag.putUUID("StoredRatUuid", rat.getUUID());
			tag.putString("StoredRatName", rat.getName().getString());
			this.boundRatUuid = rat.getUUID();
			this.storedRatTag = tag;
			rat.stopRiding();
			rat.getNavigation().stop();
			rat.discard();
			this.setChangedAndSync();
			Level level = this.getLevel();
			if (level != null) {
				level.playSound(null, this.worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.7F, 1.1F);
			}
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось поместить крысу {} в домик {}: {}", rat.getUUID(), this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}



	// Запускает респавн умершей крысы у домика; принимает крысу и задержку, отдаёт true если таймер принят.
	public boolean beginRespawn(TamedRat rat, int ticks) {
		try {
			if (this.storedRatTag != null || this.pendingRespawnTag != null) {
				RatsMod.LOGGER.warn("Домик {} не принял респавн крысы {}: stored={}, pending={}", this.worldPosition, rat.getUUID(), this.storedRatTag != null, this.pendingRespawnTag != null);
				return false;
			}
			if (!this.canAcceptRat(rat)) {
				RatsMod.LOGGER.warn("Домик {} отказал в респавне крысы {}: привязан UUID {}", this.worldPosition, rat.getUUID(), this.boundRatUuid);
				return false;
			}
			CompoundTag tag = new CompoundTag();
			rat.addAdditionalSaveData(tag);
			tag.putUUID("StoredRatUuid", rat.getUUID());
			tag.putString("StoredRatName", rat.getName().getString());
			tag.putShort("HurtTime", (short) 0);
			tag.putInt("HurtByTimestamp", 0);
			tag.putShort("DeathTime", (short) 0);
			this.boundRatUuid = rat.getUUID();
			this.pendingRespawnTag = tag;
			this.pendingRespawnTicks = Math.max(1, ticks);
			this.setChangedAndSync();
			RatsMod.LOGGER.info("Домик {} начал респавн крысы {} через {} тиков", this.worldPosition, rat.getUUID(), this.pendingRespawnTicks);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось запустить респавн крысы {} у домика {}", rat.getUUID(), this.worldPosition, exception);
			throw exception;
		}
	}

	// Выпускает крысу, ожидавшую респавна; принимает координаты появления, отдаёт true при успехе.
	private boolean releasePendingRespawn(double x, double y, double z) {
		try {
			Level level = this.getLevel();
			if (!(level instanceof ServerLevel) || this.pendingRespawnTag == null) {
				return false;
			}
			TamedRat rat = RatsEntityRegistry.TAMED_RAT.get().create(level);
			if (rat == null) {
				throw new IllegalStateException("Не удалось создать TamedRat из реестра");
			}
			CompoundTag tag = this.pendingRespawnTag.copy();
			rat.readAdditionalSaveData(tag);
			rat.setHealth(rat.getMaxHealth());
			rat.setRespawnCountdown(0);
			rat.setBoundHousePos(GlobalPos.of(level.dimension(), this.worldPosition));
			rat.setPos(x, y, z);
			level.addFreshEntity(rat);
			this.boundRatUuid = rat.getUUID();
			this.pendingRespawnTag = null;
			this.pendingRespawnTicks = 0;
			this.setChangedAndSync();
			level.playSound(null, this.worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
			RatsMod.LOGGER.info("Крыса {} вернулась у домика {}", rat.getUUID(), this.worldPosition);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выпустить ожидающую респавна крысу из домика {}", this.worldPosition, exception);
			throw exception;
		}
	}


	// Выпускает проснувшуюся крысу у входа; принимает координаты появления, отдаёт true при успешном выпуске.
	private boolean releaseSleepingRatToWorld(double x, double y, double z) {
		try {
			Level level = this.getLevel();
			if (!(level instanceof ServerLevel) || this.storedRatTag == null) {
				return false;
			}
			TamedRat rat = RatsEntityRegistry.TAMED_RAT.get().create(level);
			if (rat == null) {
				throw new IllegalStateException("Не удалось создать TamedRat из реестра");
			}
			CompoundTag tag = this.storedRatTag.copy();
			rat.readAdditionalSaveData(tag);
			rat.setHealth(Math.max(1.0F, rat.getMaxHealth()));
			rat.setRespawnCountdown(0);
			rat.setBoundHousePos(GlobalPos.of(level.dimension(), this.worldPosition));
			rat.setPos(x, y, z);
			level.addFreshEntity(rat);
			this.boundRatUuid = rat.getUUID();
			this.storedRatTag = null;
			this.setChangedAndSync();
			level.playSound(null, this.worldPosition, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.7F, 1.15F);
			RatsMod.LOGGER.info("Крыса {} проснулась и вышла из домика {}", rat.getUUID(), this.worldPosition);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выпустить проснувшуюся крысу из домика {}", this.worldPosition, exception);
			throw exception;
		}
	}

	// Выпускает сохранённую крысу в мир при разрушении домика; принимает позицию появления, отдаёт true при успешном выпуске.
	public boolean releaseRatToWorld(double x, double y, double z) {
		try {
			Level level = this.getLevel();
			if (level == null) {
				throw new IllegalStateException("У домика нет уровня");
			}
			if (level.isClientSide() || this.storedRatTag == null && this.pendingRespawnTag == null) {
				return false;
			}
			TamedRat rat = RatsEntityRegistry.TAMED_RAT.get().create(level);
			if (rat == null) {
				throw new IllegalStateException("Не удалось создать TamedRat из реестра");
			}
			CompoundTag tag = (this.storedRatTag != null ? this.storedRatTag : this.pendingRespawnTag).copy();
			rat.readAdditionalSaveData(tag);
			rat.setHealth(Math.max(1.0F, rat.getMaxHealth()));
			rat.setRespawnCountdown(0);
			rat.setBoundHousePos(null);
			rat.setPos(x, y, z);
			level.addFreshEntity(rat);
			this.boundRatUuid = null;
			this.storedRatTag = null;
			this.pendingRespawnTag = null;
			this.pendingRespawnTicks = 0;
			this.setChangedAndSync();
			level.playSound(null, this.worldPosition, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F);
			RatsMod.LOGGER.info("Крыса {} выпущена из разрушенного домика {}", rat.getUUID(), this.worldPosition);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось выпустить крысу из разрушенного домика {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Достаёт сохранённую крысу из домика на плечо игрока; принимает игрока, отдаёт true при успехе.
	public boolean releaseRatToPlayer(Player player) {
		try {
			Level level = this.getLevel();
			if (level == null) {
				throw new IllegalStateException("У домика нет уровня");
			}
			if (this.storedRatTag == null) {
				player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_EMPTY), true);
				return false;
			}
			if (player.getPassengers().size() >= 3) {
				player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_SHOULDER_FULL), true);
				return false;
			}
			TamedRat rat = RatsEntityRegistry.TAMED_RAT.get().create(level);
			if (rat == null) {
				throw new IllegalStateException("Не удалось создать TamedRat из реестра");
			}
			CompoundTag tag = this.storedRatTag.copy();
			rat.readAdditionalSaveData(tag);
			rat.setBoundHousePos(GlobalPos.of(level.dimension(), this.worldPosition));
			rat.setPos(player.getX(), player.getY(), player.getZ());
			level.addFreshEntity(rat);
			rat.startRiding(player, true);
			this.boundRatUuid = rat.getUUID();
			this.storedRatTag = null;
			this.pendingRespawnTag = null;
			this.pendingRespawnTicks = 0;
			this.setChangedAndSync();
			level.playSound(null, this.worldPosition, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.7F, 1.2F);
			player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_WITHDRAW, rat.getName()), true);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось достать крысу из домика {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}


	// Снимает привязку домика к крысе; принимает крысу и игрока, отдаёт true если домик освобождён.
	public boolean clearBinding(TamedRat rat, Player player) {
		try {
			if (this.storedRatTag != null) {
				RatsMod.LOGGER.warn("Домик {} не снимает привязку крысы {}: крыса уже внутри", this.worldPosition, rat.getUUID());
				return false;
			}
			if (this.boundRatUuid == null || !this.boundRatUuid.equals(rat.getUUID())) {
				return false;
			}
			this.boundRatUuid = null;
			rat.setBoundHousePos(null);
			this.setChangedAndSync();
			player.displayClientMessage(Component.translatable(RatsLangConstants.RAT_HOUSE_UNBIND, rat.getName()), true);
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось снять привязку крысы {} от домика {}: {}", rat.getUUID(), this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Вызывается при удалении блока из мира; ничего не принимает, пишет состояние домика в лог.
	public void onRemoved() {
		try {
			RatsMod.LOGGER.debug("Удалён block entity rat_house в {}, storedRat={}", this.worldPosition, this.storedRatTag != null);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Ошибка при удалении block entity rat_house в {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Сохраняет NBT домика; принимает тег и HolderLookup, отдаёт CompoundTag.
	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		try {
			super.saveAdditional(tag, registries);
			if (this.boundRatUuid != null) {
				tag.putUUID("BoundRatUuid", this.boundRatUuid);
			}
			if (this.storedRatTag != null) {
				tag.put("StoredRat", this.storedRatTag.copy());
			}
			if (this.pendingRespawnTag != null) {
				tag.put("PendingRespawnRat", this.pendingRespawnTag.copy());
				tag.putInt("PendingRespawnTicks", this.pendingRespawnTicks);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось сохранить NBT rat_house в {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Загружает NBT домика; принимает тег и HolderLookup.
	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		try {
			super.loadAdditional(tag, registries);
			this.boundRatUuid = tag.hasUUID("BoundRatUuid") ? tag.getUUID("BoundRatUuid") : null;
			this.storedRatTag = tag.contains("StoredRat", Tag.TAG_COMPOUND) ? tag.getCompound("StoredRat").copy() : null;
			this.pendingRespawnTag = tag.contains("PendingRespawnRat", Tag.TAG_COMPOUND) ? tag.getCompound("PendingRespawnRat").copy() : null;
			this.pendingRespawnTicks = tag.getInt("PendingRespawnTicks");
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось загрузить NBT rat_house в {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Формирует пакет синхронизации клиенту; параметров нет, отдаёт ClientboundBlockEntityDataPacket.
	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		try {
			return ClientboundBlockEntityDataPacket.create(this);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось создать пакет обновления rat_house в {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Формирует тег для синхронизации клиенту; принимает HolderLookup, отдаёт CompoundTag.
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		try {
			return this.saveWithoutMetadata(registries);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось собрать updateTag rat_house в {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Помечает домик изменённым и отправляет обновление блока клиентам; параметров нет.
	private void setChangedAndSync() {
		try {
			this.setChanged();
			Level level = this.getLevel();
			if (level != null) {
				BlockState state = level.getBlockState(this.worldPosition);
				level.sendBlockUpdated(this.worldPosition, state, state, 3);
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось синхронизировать rat_house в {}: {}", this.worldPosition, exception.getMessage(), exception);
			throw exception;
		}
	}
}
