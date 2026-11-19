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

	// Scans the connected tree from the starting log: collects logs, leaves and stumps; takes the level and position, returns the traversal result.
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
			RatsMod.LOGGER.error("Failed to scan the tree at {}: {}", start, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Estimates tree size for target selection; takes the level and log position, returns the number of logs or -1 if it is not a tree.
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
			RatsMod.LOGGER.error("Failed to count tree logs at {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Collects tree stumps at ground level; takes the level, log list and base log type, returns positions for planting saplings.
	public static List<BlockPos> getAllStumpBlocks(Level level, BlockPos pos, BlockState logtype) {
		try {
			TreeScanResult scan = scanTree(level, pos);
			if (!scan.valid()) {
				return List.of();
			}
			return scan.stumps();
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to collect tree stumps at {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Gets a sapling from the leaves loot table; takes the server level and leaves block, returns the sapling block or null.
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
			RatsMod.LOGGER.error("Failed to get a sapling from leaves {}: {}", BuiltInRegistries.BLOCK.getKey(leaves), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Collects all connected tree logs to chop; takes the level, base position and already known list, returns the list of logs.
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
			RatsMod.LOGGER.error("Failed to collect logs to chop at {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Returns the horizontal and vertical leaf traversal radius based on the log count; takes logcount, returns a pair of radii.
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

	// Checks whether there are logs near the leaves; takes the level and leaf start position, returns a connection flag and the list of visited blocks.
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
			RatsMod.LOGGER.error("Failed to check leaf-to-log connection at {}: {}", startpos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Checks whether a block is a log or mangrove roots; takes the block state, returns true for wood.
	public static boolean isTreeLog(BlockState block) {
		return block.is(BlockTags.LOGS) || isTreeRoot(block);
	}

	// Compares two log blocks of the same tree including modded variants; takes two states, returns true if they are the same wood type.
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

	// Checks whether a block is mangrove roots; takes the block state, returns true for MangroveRootsBlock.
	public static boolean isTreeRoot(BlockState block) {
		return block.getBlock() instanceof MangroveRootsBlock;
	}

	// Checks whether a block belongs to the mangrove log family; takes the block state, returns true for mangroves.
	public static boolean isMangroveRootOrLog(BlockState block) {
		return block.getBlock() instanceof MangroveRootsBlock || block.is(BlockTags.MANGROVE_LOGS);
	}

	// Checks whether a block is leaves or a decorative canopy; takes the block state, returns true for leaves.
	public static boolean isTreeLeaf(BlockState block) {
		return block.is(BlockTags.LEAVES) || block.is(BlockTags.WART_BLOCKS) || block.is(Blocks.SHROOMLIGHT);
	}

	// Checks whether a wood block is protected as player-placed; takes the level and position, returns true for protected blocks.
	public static boolean isPlayerProtectedTreeBlock(Level level, BlockPos pos) {
		try {
			RatProtectedTreeBlocksData data = RatProtectedTreeBlocksData.get(level);
			return data != null && data.isProtected(pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check protection of wood block {}", pos, exception);
			throw exception;
		}
	}

	// Checks whether the lumberjack may count a log as part of the tree; takes the level and position, returns true only for an unprotected log.
	public static boolean isHarvestableTreeLog(Level level, BlockPos pos) {
		try {
			return isTreeLog(level.getBlockState(pos)) && !isPlayerProtectedTreeBlock(level, pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check availability of log {} for the lumberjack", pos, exception);
			throw exception;
		}
	}

	// Checks whether leaves are natural for the lumberjack; takes the state, returns false for persistent player leaves.
	public static boolean isNaturalTreeLeaf(BlockState state) {
		try {
			if (!isTreeLeaf(state)) {
				return false;
			}
			return !state.hasProperty(LeavesBlock.PERSISTENT) || !state.getValue(LeavesBlock.PERSISTENT);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check whether leaves {} are natural", state, exception);
			throw exception;
		}
	}

	// Checks whether the lumberjack may break leaves; takes the level and position, returns true only for natural unprotected leaves.
	public static boolean isHarvestableTreeLeaf(Level level, BlockPos pos) {
		try {
			return isNaturalTreeLeaf(level.getBlockState(pos)) && !isPlayerProtectedTreeBlock(level, pos);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to check availability of leaves {} for the lumberjack", pos, exception);
			throw exception;
		}
	}

	// Checks whether a log is at the base of a tree on suitable ground; takes the level and position, returns true if it is the base.
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
			RatsMod.LOGGER.error("Failed to check the tree base at {}: {}", logPos, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Collects connected leaf and log blocks within a radius; takes the level, start and maximum distance, returns a list of positions.
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
			RatsMod.LOGGER.error("Failed to collect neighbouring tree blocks from {}: {}", startpos, exception.getMessage(), exception);
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
			RatsMod.LOGGER.warn("Tree at {} exceeded the log limit {} - chopping will be partial", start, MAX_TREE_LOGS);
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
			RatsMod.LOGGER.warn("Tree canopy exceeded the leaf limit {} - some leaves will remain", MAX_TREE_LEAVES);
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
