package com.github.alexthe666.rats.server.world;

import com.github.alexthe666.rats.RatsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RatProtectedTreeBlocksData extends SavedData {
	private static final String IDENTIFIER = "rats_protected_tree_blocks";
	private final Set<Long> protectedBlocks = new HashSet<>();

	// Создаёт пустое хранилище защищённых блоков дерева; параметров нет.
	public RatProtectedTreeBlocksData() {
	}

	// Возвращает данные защищённых блоков для серверного мира; принимает уровень, отдаёт данные или null на клиенте.
	@Nullable
	public static RatProtectedTreeBlocksData get(Level level) {
		try {
			if (!(level instanceof ServerLevel serverLevel)) {
				return null;
			}
			return serverLevel.getDataStorage().computeIfAbsent(
					new SavedData.Factory<>(RatProtectedTreeBlocksData::new, RatProtectedTreeBlocksData::load, null),
					IDENTIFIER
			);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить список защищённых древесных блоков для {}", level.dimension().location(), exception);
			throw exception;
		}
	}

	// Загружает список защищённых древесных блоков; принимает NBT и реестры, отдаёт заполненные данные.
	public static RatProtectedTreeBlocksData load(CompoundTag tag, HolderLookup.Provider provider) {
		try {
			RatProtectedTreeBlocksData data = new RatProtectedTreeBlocksData();
			Arrays.stream(tag.getLongArray("ProtectedTreeBlocks")).forEach(data.protectedBlocks::add);
			return data;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось загрузить список защищённых древесных блоков", exception);
			throw exception;
		}
	}

	// Помечает блок как поставленный игроком; принимает позицию, ничего не отдаёт.
	public void protect(BlockPos pos) {
		try {
			this.protectedBlocks.add(pos.asLong());
			this.setDirty();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось защитить древесный блок {}", pos, exception);
			throw exception;
		}
	}

	// Снимает защиту с блока; принимает позицию, ничего не отдаёт.
	public void unprotect(BlockPos pos) {
		try {
			if (this.protectedBlocks.remove(pos.asLong())) {
				this.setDirty();
			}
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось снять защиту с древесного блока {}", pos, exception);
			throw exception;
		}
	}

	// Проверяет защиту блока от лесоруба; принимает позицию, отдаёт true если блок поставлен игроком.
	public boolean isProtected(BlockPos pos) {
		try {
			return this.protectedBlocks.contains(pos.asLong());
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить защиту древесного блока {}", pos, exception);
			throw exception;
		}
	}

	// Сохраняет защищённые блоки в NBT; принимает тег и реестры, отдаёт тег.
	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
		try {
			long[] blocks = this.protectedBlocks.stream().mapToLong(Long::longValue).toArray();
			tag.putLongArray("ProtectedTreeBlocks", blocks);
			return tag;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось сохранить список защищённых древесных блоков", exception);
			throw exception;
		}
	}
}
