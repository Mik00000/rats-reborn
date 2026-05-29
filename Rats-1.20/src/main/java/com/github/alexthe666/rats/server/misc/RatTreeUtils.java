package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.world.RatProtectedTreeBlocksData;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MangroveRootsBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RatTreeUtils {

	public static final int MAX_TREE_LOGS = 4096;
	public static final int MAX_TREE_LEAVES = 8192;
	public static final int MAX_TREE_HEIGHT = 320;
	public static final int MIN_LEAVES_FOR_TREE = 4;

	public static final HashMap<BlockPos, Integer> highestleaf = new HashMap<>();

	private static final Direction[] LOG_SEARCH_DIRECTIONS = Direction.values();

	public record TreeScanResult(List<BlockPos> logs, List<BlockPos> leaves, List<BlockPos> stumps, int highestLeafY, boolean valid) {
		public static TreeScanResult invalid() {
			return new TreeScanResult(List.of(), List.of(), List.of(), 0, false);
		}
	}

	// Сканирует связное дерево от стартового бревна: собирает брёвна, листву и пни; принимает мир и позицию, отдаёт результат обхода.
	public static TreeScanResult scanTree(Level level, BlockPos start) {
		try {
			BlockState startState = level.getBlockState(start);
			if (!isTreeLog(startState) || isPlayerProtectedTreeBlock(level, start)) {
				return TreeScanResult.invalid();
			}

			List<BlockPos> logs = collectConnectedLogs(level, start, startState);
			if (logs.isEmpty()) {
				return TreeScanResult.invalid();
			}

			List<BlockPos> leaves = collectConnectedLeaves(level, logs);
			if (leaves.size() < MIN_LEAVES_FOR_TREE) {
				return TreeScanResult.invalid();
			}

			int highestLeafY = logs.getFirst().getY();
			for (BlockPos leaf : leaves) {
				if (leaf.getY() > highestLeafY) {
					highestLeafY = leaf.getY();
				}
			}
			for (BlockPos log : logs) {
				if (log.getY() > highestLeafY) {
					highestLeafY = log.getY();
				}
			}

			List<BlockPos> stumps = collectStumpBlocks(level, logs, startState);
			return new TreeScanResult(logs, leaves, stumps, highestLeafY, true);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось просканировать дерево в {}: {}", start, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Оценивает размер дерева для выбора цели; принимает мир и позицию бревна, отдаёт число брёвен или -1 если это не дерево.
	public static int calculateLogAmount(Level level, BlockPos pos) {
		try {
			TreeScanResult scan = scanTree(level, pos);
			if (!scan.valid()) {
				highestleaf.remove(pos);
				return -1;
			}
			highestleaf.put(pos.immutable(), scan.highestLeafY());
			return scan.logs().size();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось посчитать брёвна дерева в {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Собирает пни дерева у земли; принимает мир, список брёвен и тип опорного бревна, отдаёт позиции для посадки саженцев.
	public static List<BlockPos> getAllStumpBlocks(Level level, BlockPos pos, BlockState logtype) {
		try {
			TreeScanResult scan = scanTree(level, pos);
			if (!scan.valid()) {
				return List.of();
			}
			return scan.stumps();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось собрать пни дерева в {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Извлекает саженец из таблицы добычи листвы; принимает серверный мир и блок листвы, отдаёт блок саженца или null.
	@Nullable
	public static Block getSaplingFromLeaves(ServerLevel level, Block leaves) {
		try {
			ResourceKey<LootTable> lootKey = ResourceKey.create(Registries.LOOT_TABLE, BuiltInRegistries.BLOCK.getKey(leaves).withPrefix("blocks/"));
			LootTable loot = level.getServer().reloadableRegistries().getLootTable(lootKey);
			LootParams.Builder context = new LootParams.Builder(level)
					.withParameter(LootContextParams.TOOL, createMaxHoe(level))
					.withParameter(LootContextParams.BLOCK_STATE, leaves.defaultBlockState())
					.withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
					.withLuck(Float.MAX_VALUE);
			for (int i = 0; i < 25; i++) {
				ObjectArrayList<ItemStack> lootStacks = loot.getRandomItems(context.create(LootContextParamSets.BLOCK));
				for (ItemStack stack : lootStacks) {
					if (stack.is(ItemTags.SAPLINGS) || Block.byItem(stack.getItem()) instanceof SaplingBlock) {
						return Block.byItem(stack.getItem());
					}
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось получить саженец из листвы {}: {}", BuiltInRegistries.BLOCK.getKey(leaves), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Собирает все связные брёвна дерева для срубки; принимает мир, опорную позицию и уже известный список, отдаёт список брёвен.
	public static List<BlockPos> getLogsToBreak(Level level, BlockPos pos, List<BlockPos> logsToBreak, BlockState logType) {
		try {
			TreeScanResult scan = scanTree(level, pos);
			if (!scan.valid()) {
				return logsToBreak;
			}
			for (BlockPos logPos : scan.logs()) {
				if (!logsToBreak.contains(logPos)) {
					logsToBreak.add(logPos.immutable());
				}
			}
			return logsToBreak;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось собрать брёвна для срубки в {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Возвращает горизонтальный и вертикальный радиус обхода листвы по числу брёвен; принимает logcount, отдаёт пару радиусов.
	public static Pair<Integer, Integer> getHorizontalAndVerticalValue(int logcount) {
		int horizontal = 6;
		int vertical = 6;
		if (logcount >= 64) {
			horizontal = 24;
			vertical = 24;
		} else if (logcount >= 32) {
			horizontal = 16;
			vertical = 16;
		} else if (logcount >= 16) {
			horizontal = 12;
			vertical = 12;
		} else if (logcount >= 8) {
			horizontal = 8;
			vertical = 8;
		}
		return new Pair<>(horizontal, vertical);
	}

	// Проверяет, есть ли рядом с листвой брёвна; принимает мир и стартовую позицию листвы, отдаёт флаг связи и список просмотренных блоков.
	public static Pair<Boolean, List<BlockPos>> isConnectedToLogs(Level level, BlockPos startpos) {
		try {
			List<BlockPos> connected = getBlocksNextToEachOtherMaterial(level, startpos, 12);
			for (BlockPos connectedPos : connected) {
				if (isTreeLog(level.getBlockState(connectedPos))) {
					return new Pair<>(true, connected);
				}
			}
			return new Pair<>(false, connected);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить связь листвы с брёвнами в {}: {}", startpos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Проверяет, является ли блок бревном или корнем мангров; принимает состояние блока, отдаёт true для древесины.
	public static boolean isTreeLog(BlockState block) {
		return block.is(BlockTags.LOGS) || isTreeRoot(block);
	}

	// Сравнивает два блока бревна одного дерева с учётом модовых вариантов; принимает два состояния, отдаёт true если это одна порода.
	public static boolean areEqualLogTypes(BlockState one, BlockState two) {
		if (!isTreeLog(one) || !isTreeLog(two)) {
			return false;
		}
		if (isMangroveRootOrLog(one) && isMangroveRootOrLog(two)) {
			return true;
		}
		if (one.is(two.getBlock())) {
			return true;
		}
		return getWoodFamilyKey(one).equals(getWoodFamilyKey(two));
	}

	// Проверяет, является ли блок корнем мангров; принимает состояние блока, отдаёт true для MangroveRootsBlock.
	public static boolean isTreeRoot(BlockState block) {
		return block.getBlock() instanceof MangroveRootsBlock;
	}

	// Проверяет, относится ли блок к семейству мангровых брёвен; принимает состояние блока, отдаёт true для мангров.
	public static boolean isMangroveRootOrLog(BlockState block) {
		return block.getBlock() instanceof MangroveRootsBlock || block.is(BlockTags.MANGROVE_LOGS);
	}

	// Проверяет, является ли блок листвой или декоративной кроной; принимает состояние блока, отдаёт true для листвы.
	public static boolean isTreeLeaf(BlockState block) {
		return block.is(BlockTags.LEAVES) || block.is(BlockTags.WART_BLOCKS) || block.is(Blocks.SHROOMLIGHT);
	}

	// Проверяет, защищён ли древесный блок как поставленный игроком; принимает мир и позицию, отдаёт true для защищённых блоков.
	public static boolean isPlayerProtectedTreeBlock(Level level, BlockPos pos) {
		try {
			RatProtectedTreeBlocksData data = RatProtectedTreeBlocksData.get(level);
			return data != null && data.isProtected(pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить защиту древесного блока {}", pos, exception);
			throw exception;
		}
	}

	// Проверяет, можно ли лесорубу считать бревно частью дерева; принимает мир и позицию, отдаёт true только для незащищённого бревна.
	public static boolean isHarvestableTreeLog(Level level, BlockPos pos) {
		try {
			return isTreeLog(level.getBlockState(pos)) && !isPlayerProtectedTreeBlock(level, pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить доступность бревна {} для лесоруба", pos, exception);
			throw exception;
		}
	}

	// Проверяет, является ли листва натуральной для лесоруба; принимает состояние, отдаёт false для постоянной листвы игрока.
	public static boolean isNaturalTreeLeaf(BlockState state) {
		try {
			if (!isTreeLeaf(state)) {
				return false;
			}
			return !state.hasProperty(LeavesBlock.PERSISTENT) || !state.getValue(LeavesBlock.PERSISTENT);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить натуральность листвы {}", state, exception);
			throw exception;
		}
	}

	// Проверяет, можно ли лесорубу ломать листву; принимает мир и позицию, отдаёт true только для натуральной незащищённой листвы.
	public static boolean isHarvestableTreeLeaf(Level level, BlockPos pos) {
		try {
			return isNaturalTreeLeaf(level.getBlockState(pos)) && !isPlayerProtectedTreeBlock(level, pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить доступность листвы {} для лесоруба", pos, exception);
			throw exception;
		}
	}

	// Проверяет, стоит ли бревно у основания дерева на пригодном грунте; принимает мир и позицию, отдаёт true если это основание.
	public static boolean isTreeBase(Level level, BlockPos logPos) {
		try {
			BlockPos basePos = findLowestLogColumn(level, logPos);
			BlockPos below = basePos.below();
			BlockState belowState = level.getBlockState(below);
			if (belowState.is(BlockTags.DIRT) || belowState.is(BlockTags.SAND) || belowState.is(BlockTags.ICE) || belowState.is(BlockTags.VALID_SPAWN)) {
				return true;
			}
			return belowState.canOcclude() && !isTreeLog(belowState) && !isTreeLeaf(belowState);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось проверить основание дерева в {}: {}", logPos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Собирает связные блоки листвы и брёвен в радиусе; принимает мир, старт и максимальную дистанцию, отдаёт список позиций.
	public static List<BlockPos> getBlocksNextToEachOtherMaterial(Level level, BlockPos startpos, int maxDistance) {
		try {
			List<BlockPos> checkedBlocks = new ArrayList<>();
			List<BlockPos> foundBlocks = new ArrayList<>();
			BlockState startState = level.getBlockState(startpos);
			if (!isTreeLog(startState) && !isTreeLeaf(startState)) {
				return foundBlocks;
			}

			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(startpos);
			checkedBlocks.add(startpos);
			foundBlocks.add(startpos);

			while (!queue.isEmpty()) {
				BlockPos current = queue.removeFirst();
				if (!startpos.closerThan(current, maxDistance)) {
					continue;
				}
				for (Direction direction : LOG_SEARCH_DIRECTIONS) {
					BlockPos next = current.relative(direction);
					if (checkedBlocks.contains(next)) {
						continue;
					}
					checkedBlocks.add(next);
					BlockState nextState = level.getBlockState(next);
					if (isTreeLog(nextState) || isTreeLeaf(nextState)) {
						foundBlocks.add(next);
						queue.addLast(next);
					}
				}
			}
			return foundBlocks;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось собрать соседние блоки дерева от {}: {}", startpos, exception.getMessage(), exception);
			throw exception;
		}
	}

	private static List<BlockPos> collectConnectedLogs(Level level, BlockPos start, BlockState referenceState) {
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();
		List<BlockPos> logs = new ArrayList<>();
		queue.add(start.immutable());
		visited.add(start.immutable());

		while (!queue.isEmpty() && logs.size() < MAX_TREE_LOGS) {
			BlockPos current = queue.removeFirst();
			BlockState currentState = level.getBlockState(current);
			if (!isTreeLog(currentState) || !areEqualLogTypes(referenceState, currentState) || isPlayerProtectedTreeBlock(level, current)) {
				continue;
			}

			logs.add(current.immutable());
			if (current.getY() - start.getY() > MAX_TREE_HEIGHT) {
				continue;
			}

			for (Direction direction : LOG_SEARCH_DIRECTIONS) {
				BlockPos next = current.relative(direction);
				if (!visited.add(next)) {
					continue;
				}
				BlockState nextState = level.getBlockState(next);
				if (isTreeLog(nextState) && areEqualLogTypes(referenceState, nextState) && !isPlayerProtectedTreeBlock(level, next)) {
					if (isMangroveRootOrLog(referenceState) && direction == Direction.DOWN && current.getY() == start.getY() - 1) {
						continue;
					}
					queue.addLast(next);
				}
			}
		}

		if (logs.size() >= MAX_TREE_LOGS) {
			RatsMod.LOGGER.warn("Дерево в {} превысило лимит брёвен {} — срубка будет частичной", start, MAX_TREE_LOGS);
		}
		return logs;
	}

	private static List<BlockPos> collectConnectedLeaves(Level level, List<BlockPos> logs) {
		if (logs.isEmpty()) {
			return List.of();
		}

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (BlockPos log : logs) {
			minX = Math.min(minX, log.getX());
			minY = Math.min(minY, log.getY());
			minZ = Math.min(minZ, log.getZ());
			maxX = Math.max(maxX, log.getX());
			maxY = Math.max(maxY, log.getY());
			maxZ = Math.max(maxZ, log.getZ());
		}

		Pair<Integer, Integer> radius = getHorizontalAndVerticalValue(logs.size());
		int horizontal = radius.getFirst();
		int vertical = radius.getSecond();
		minX -= horizontal;
		minY -= 1;
		minZ -= horizontal;
		maxX += horizontal;
		maxY += vertical;
		maxZ += horizontal;

		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();
		List<BlockPos> leaves = new ArrayList<>();

		for (BlockPos log : logs) {
			for (Direction direction : LOG_SEARCH_DIRECTIONS) {
				BlockPos adjacent = log.relative(direction);
				if (!visited.add(adjacent)) {
					continue;
				}
				BlockState adjacentState = level.getBlockState(adjacent);
				if (isNaturalTreeLeaf(adjacentState) && !isPlayerProtectedTreeBlock(level, adjacent)) {
					queue.addLast(adjacent);
				}
			}
		}

		while (!queue.isEmpty() && leaves.size() < MAX_TREE_LEAVES) {
			BlockPos current = queue.removeFirst();
			if (current.getX() < minX || current.getY() < minY || current.getZ() < minZ || current.getX() > maxX || current.getY() > maxY || current.getZ() > maxZ) {
				continue;
			}

			BlockState currentState = level.getBlockState(current);
			if (!isNaturalTreeLeaf(currentState) || isPlayerProtectedTreeBlock(level, current)) {
				continue;
			}

			leaves.add(current.immutable());
			for (Direction direction : LOG_SEARCH_DIRECTIONS) {
				BlockPos next = current.relative(direction);
				if (!visited.add(next)) {
					continue;
				}
				BlockState nextState = level.getBlockState(next);
				if (isNaturalTreeLeaf(nextState) && !isPlayerProtectedTreeBlock(level, next)) {
					queue.addLast(next);
				}
			}
		}

		if (leaves.size() >= MAX_TREE_LEAVES) {
			RatsMod.LOGGER.warn("Крона дерева превысила лимит листвы {} — часть листвы останется", MAX_TREE_LEAVES);
		}
		return leaves;
	}

	private static List<BlockPos> collectStumpBlocks(Level level, List<BlockPos> logs, BlockState referenceState) {
		List<BlockPos> stumps = new ArrayList<>();
		for (BlockPos log : logs) {
			BlockPos below = log.below();
			BlockState belowState = level.getBlockState(below);
			if (!isTreeLog(belowState) && (belowState.is(BlockTags.DIRT) || belowState.canOcclude())) {
				BlockState logState = level.getBlockState(log);
				if (areEqualLogTypes(referenceState, logState)) {
					stumps.add(log.immutable());
				}
			}
		}
		return stumps;
	}

	private static BlockPos findLowestLogColumn(Level level, BlockPos logPos) {
		BlockPos cursor = logPos.immutable();
		BlockState cursorState = level.getBlockState(cursor);
		for (int depth = 0; depth < MAX_TREE_HEIGHT; depth++) {
			BlockPos below = cursor.below();
			BlockState belowState = level.getBlockState(below);
			if (isTreeLog(belowState) && areEqualLogTypes(cursorState, belowState)) {
				cursor = below.immutable();
				continue;
			}
			break;
		}
		return cursor;
	}

	private static String getWoodFamilyKey(BlockState state) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		String path = id.getPath().toLowerCase(Locale.ROOT);
		String family = path
				.replaceFirst("^stripped_", "")
				.replaceAll("_(log|wood|stem|trunk|branch|roots?|pillar|hyphae|stalk)(_[xyzudnsew])?$", "");
		return id.getNamespace() + ":" + family;
	}

	private static ItemStack createMaxHoe(ServerLevel level) {
		ItemStack hoe = new ItemStack(Items.NETHERITE_HOE);
		hoe.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), Byte.MAX_VALUE);
		return hoe;
	}
}
