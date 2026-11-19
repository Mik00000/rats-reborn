package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.block.RatCageBlock;
import com.github.alexthe666.rats.server.block.RatTubeBlock;
import com.github.alexthe666.rats.server.block.entity.RatTubeBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RatTubePathHelper {

	private static final int MAX_NETWORK_SCAN = 512;
	private static final int ROUTE_SEARCH_RADIUS = 24;
	private static final double TUBE_SEGMENT_ROUTE_COST = 0.35D;
	private static final double TUBE_EQUAL_ROUTE_BIAS = 0.5D;

	private RatTubePathHelper() {
	}

	/** Entry/exit point: a tube segment and the outside cell where the rat stands before entering. */
	public record TubeEntrance(BlockPos tubePos, BlockPos standPos, Direction openSide) {
	}

	/** Route through the tube network: entry, inner segments, exit, final target. */
	public record TubeRoute(TubeEntrance entry, List<BlockPos> segments, TubeEntrance exit, Vec3 target) {
	}

	public static BooleanProperty getOpenProperty(Direction direction) {
		return switch (direction) {
			case DOWN -> RatTubeBlock.OPEN_DOWN;
			case UP -> RatTubeBlock.OPEN_UP;
			case NORTH -> RatTubeBlock.OPEN_NORTH;
			case SOUTH -> RatTubeBlock.OPEN_SOUTH;
			case WEST -> RatTubeBlock.OPEN_WEST;
			case EAST -> RatTubeBlock.OPEN_EAST;
		};
	}

	public static BooleanProperty getConnectProperty(Direction direction) {
		return switch (direction) {
			case DOWN -> RatTubeBlock.DOWN;
			case UP -> RatTubeBlock.UP;
			case NORTH -> RatTubeBlock.NORTH;
			case SOUTH -> RatTubeBlock.SOUTH;
			case WEST -> RatTubeBlock.WEST;
			case EAST -> RatTubeBlock.EAST;
		};
	}

	public static boolean isPassageOpen(BlockState state, Direction direction) {
		if (!(state.getBlock() instanceof RatTubeBlock)) {
			return false;
		}
		return state.getValue(getOpenProperty(direction));
	}

	public static boolean hasOpenPassage(BlockState state) {
		if (!(state.getBlock() instanceof RatTubeBlock)) {
			return false;
		}
		for (BooleanProperty openProp : RatTubeBlock.ALL_OPEN_PROPS) {
			if (state.getValue(openProp)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isTubeLink(BlockGetter level, BlockPos pos, Direction direction) {
		BlockState neighbor = level.getBlockState(pos.relative(direction));
		return neighbor.getBlock() instanceof RatTubeBlock || neighbor.getBlock() instanceof RatCageBlock;
	}

	public static boolean canMountEntrance(BlockGetter level, BlockPos pos, Direction direction) {
		BlockState neighbor = level.getBlockState(pos.relative(direction));
		return !(neighbor.getBlock() instanceof RatTubeBlock);
	}

	public static boolean isTubeSegment(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof RatTubeBlock;
	}

	public static boolean isConnectedTo(BlockState state, Direction direction) {
		if (!(state.getBlock() instanceof RatTubeBlock)) {
			return false;
		}
		return state.getValue(getConnectProperty(direction));
	}

	public static boolean isInTubeNetworkWithPassage(Level level, BlockPos pos) {
		try {
			if (!isTubeSegment(level, pos)) {
				return false;
			}
			Set<BlockPos> visited = new HashSet<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(pos);
			visited.add(pos);
			int scanned = 0;
			while (!queue.isEmpty() && scanned < MAX_NETWORK_SCAN) {
				BlockPos current = queue.poll();
				scanned++;
				BlockState state = level.getBlockState(current);
				if (!(state.getBlock() instanceof RatTubeBlock)) {
					continue;
				}
				if (hasOpenPassage(state)) {
					return true;
				}
				for (Direction direction : Direction.values()) {
					BlockPos next = current.relative(direction);
					if (visited.add(next) && isTubeSegment(level, next)) {
						queue.add(next);
					}
				}
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Tube network scan failed at {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	public static boolean canEnterFrom(BlockGetter level, BlockPos tubePos, Direction fromStandToTube) {
		BlockState tubeState = level.getBlockState(tubePos);
		if (!(tubeState.getBlock() instanceof RatTubeBlock)) {
			return false;
		}
		return isPassageOpen(tubeState, fromStandToTube.getOpposite());
	}

	public static boolean isPassableForRat(BlockGetter level, BlockPos pos, TamedRat rat) {
		try {
			if (rat != null && rat.isInTube()) {
				return isTubeSegment(level, pos) || RatUtils.isOpenRatTube(level, pos);
			}
			if (isTubeSegment(level, pos)) {
				if (level instanceof Level world) {
					return isInTubeNetworkWithPassage(world, pos);
				}
				return hasOpenPassage(level.getBlockState(pos));
			}
			if (RatUtils.isOpenRatTube(level, pos)) {
				return true;
			}
			for (Direction direction : Direction.values()) {
				BlockPos tubePos = pos.relative(direction);
				if (isTubeSegment(level, tubePos) && canEnterFrom(level, tubePos, direction)) {
					return true;
				}
			}
			return false;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Tube passability failed at {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	@Nullable
	public static PathType resolveTubePathType(BlockGetter level, BlockPos pos, TamedRat rat) {
		try {
			if (rat == null) {
				return null;
			}
			if (isTubeSegment(level, pos) && isPassableForRat(level, pos, rat)) {
				return PathType.RAIL;
			}
			if (!isTubeSegment(level, pos)) {
				for (Direction direction : Direction.values()) {
					BlockPos tubePos = pos.relative(direction);
					if (!isTubeSegment(level, tubePos) || !canEnterFrom(level, tubePos, direction)) {
						continue;
					}
					if (level instanceof Level world && isInTubeNetworkWithPassage(world, tubePos)) {
						return PathType.RAIL;
					}
				}
			}
			return null;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Tube path type failed at {}: {}", pos, exception.getMessage(), exception);
			throw exception;
		}
	}

	public static boolean isPathfindableTube(BlockState state, BlockPos pos, BlockGetter level) {
		if (!(state.getBlock() instanceof RatTubeBlock)) {
			return false;
		}
		if (level instanceof Level world && isInTubeNetworkWithPassage(world, pos)) {
			return true;
		}
		return state.isPathfindable(PathComputationType.LAND);
	}

	public static void setEntranceMount(Level level, BlockPos pos, Direction side) {
		if (level.getBlockEntity(pos) instanceof RatTubeBlockEntity tube) {
			tube.setEntranceData(side, true);
		}
	}

	public static void clearEntranceMount(Level level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof RatTubeBlockEntity tube) {
			tube.clearEntranceMount();
		}
	}

	/**
	 * Builds a tube route from the rat to the target if the network with open entrances gives a shorter path.
	 * Takes the level, rat foot position and target; returns a route or empty.
	 */
	public static Optional<TubeRoute> planRoute(Level level, BlockPos ratFeet, Vec3 target) {
		try {
			BlockPos goalBlock = BlockPos.containing(target.x, target.y, target.z);
			BlockPos networkSeed = findNearestTubeSegment(level, ratFeet, ROUTE_SEARCH_RADIUS);
			if (networkSeed == null) {
				networkSeed = findNearestTubeSegment(level, goalBlock, ROUTE_SEARCH_RADIUS);
			}
			if (networkSeed == null) {
				return Optional.empty();
			}
			if (!isInTubeNetworkWithPassage(level, networkSeed)) {
				return Optional.empty();
			}
			List<TubeEntrance> entrances = collectNetworkEntrances(level, networkSeed);
			if (entrances.isEmpty()) {
				return Optional.empty();
			}
			double directCost = manhattanDistance(ratFeet, goalBlock);
			TubeRoute best = null;
			double bestCost = directCost + TUBE_EQUAL_ROUTE_BIAS;
			for (TubeEntrance entry : entrances) {
				for (TubeEntrance exit : entrances) {
					if (entry.tubePos().equals(exit.tubePos()) && entry.openSide() == exit.openSide()) {
						continue;
					}
					List<BlockPos> segments = buildTubeSegmentPath(level, entry.tubePos(), exit.tubePos());
					if (segments.isEmpty()) {
						continue;
					}
					double cost = manhattanDistance(entry.standPos(), ratFeet)
							+ segments.size() * TUBE_SEGMENT_ROUTE_COST
							+ manhattanDistance(exit.standPos(), goalBlock);
					if (cost <= bestCost) {
						bestCost = cost;
						best = new TubeRoute(entry, segments, exit, target);
					}
				}
			}
			if (best == null) {
				return Optional.empty();
			}
			RatsMod.LOGGER.debug("Tube route planned: {} segments, entry {}, exit {}", best.segments().size(), best.entry().standPos(), best.exit().standPos());
			return Optional.of(best);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Tube route planning failed at {} -> {}: {}", ratFeet, target, exception.getMessage(), exception);
			throw exception;
		}
	}

	/**
	 * Collects all entries/exits of the tube network (an open face towards a non-tube).
	 * Takes the level and any segment of the network; returns the list of entries.
	 */
	public static List<TubeEntrance> collectNetworkEntrances(Level level, BlockPos networkSeed) {
		try {
			Set<BlockPos> network = collectTubeNetwork(level, networkSeed);
			List<TubeEntrance> entrances = new ArrayList<>();
			for (BlockPos tubePos : network) {
				BlockState tubeState = level.getBlockState(tubePos);
				if (!(tubeState.getBlock() instanceof RatTubeBlock)) {
					continue;
				}
				for (Direction openSide : Direction.values()) {
					if (!isPassageOpen(tubeState, openSide)) {
						continue;
					}
					if (!canMountEntrance(level, tubePos, openSide)) {
						continue;
					}
					entrances.add(new TubeEntrance(tubePos, tubePos.relative(openSide), openSide));
				}
			}
			return entrances;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Tube entrance collection failed at {}: {}", networkSeed, exception.getMessage(), exception);
			throw exception;
		}
	}

	/**
	 * BFS path through adjacent tube segments.
	 * Takes the level and two segments; returns the list of segments from start to end inclusive.
	 */
	public static List<BlockPos> buildTubeSegmentPath(Level level, BlockPos start, BlockPos end) {
		try {
			if (!isTubeSegment(level, start) || !isTubeSegment(level, end)) {
				return List.of();
			}
			if (start.equals(end)) {
				return List.of(start);
			}
			Map<BlockPos, BlockPos> parent = new HashMap<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			parent.put(start, null);
			queue.add(start);
			while (!queue.isEmpty() && parent.size() < MAX_NETWORK_SCAN) {
				BlockPos current = queue.poll();
				if (current.equals(end)) {
					break;
				}
				for (Direction direction : Direction.values()) {
					BlockPos next = current.relative(direction);
					if (!isTubeSegment(level, next) || parent.containsKey(next)) {
						continue;
					}
					parent.put(next, current);
					queue.add(next);
				}
			}
			if (!parent.containsKey(end)) {
				return List.of();
			}
			List<BlockPos> path = new ArrayList<>();
			BlockPos cursor = end;
			while (cursor != null) {
				path.add(cursor);
				cursor = parent.get(cursor);
			}
			Collections.reverse(path);
			return path;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Tube segment path failed {} -> {}: {}", start, end, exception.getMessage(), exception);
			throw exception;
		}
	}

	@Nullable
	public static BlockPos findNearestTubeSegment(Level level, BlockPos origin, int radius) {
		try {
			BlockPos nearest = null;
			double nearestDist = Double.MAX_VALUE;
			for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
				if (!isTubeSegment(level, pos)) {
					continue;
				}
				double dist = pos.distSqr(origin);
				if (dist < nearestDist) {
					nearestDist = dist;
					nearest = pos.immutable();
				}
			}
			return nearest;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Nearest tube search failed at {}: {}", origin, exception.getMessage(), exception);
			throw exception;
		}
	}

	private static Set<BlockPos> collectTubeNetwork(Level level, BlockPos seed) {
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(seed);
		visited.add(seed);
		int scanned = 0;
		while (!queue.isEmpty() && scanned < MAX_NETWORK_SCAN) {
			BlockPos current = queue.poll();
			scanned++;
			for (Direction direction : Direction.values()) {
				BlockPos next = current.relative(direction);
				if (visited.add(next) && isTubeSegment(level, next)) {
					queue.add(next);
				}
			}
		}
		return visited;
	}

	private static int manhattanDistance(BlockPos first, BlockPos second) {
		return Math.abs(first.getX() - second.getX())
				+ Math.abs(first.getY() - second.getY())
				+ Math.abs(first.getZ() - second.getZ());
	}
}
