package com.mik.rats_squirrel.canopy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A* search through one tree.
 *
 * <p>Nodes are {@link CanopyGraph#isClingNode cling nodes}: passable blocks touching foliage or
 * bark, so routes stay on the tree. Moves are 6 cardinal, 4 horizontal diagonals and 8
 * vertical diagonals; a diagonal is allowed only when both cardinal blocks it cuts past are
 * passable.</p>
 *
 * <p>{@link #findNearest} searches toward many fruits at once and returns the one with the
 * cheapest real route. This both orders the harvest queue (nearest by path, not by straight
 * line) and proves reachability in a single search.</p>
 */
public final class CanopyPathfinder {
    /** Squared block distance from a node to a fruit at which the fruit counts as reachable. */
    public static final double FRUIT_REACH_SQR = 3.0D;

    private static final int[][] MOVES = buildMoves();

    private final CanopyGraph graph;

    public CanopyPathfinder(CanopyGraph graph) {
        this.graph = graph;
    }

    /** Result of a multi-goal search. */
    public record Route(List<BlockPos> path, BlockPos goal) {
    }

    /**
     * Legacy single-goal search inside a cube around {@code start}.
     */
    public Optional<List<BlockPos>> findPath(ServerLevel level, BlockPos start, BlockPos goal, int radius, int maxNodes) {
        int horiz = Math.max(radius, 8);
        AABB bounds = new AABB(start).inflate(horiz, 8, horiz);
        return search(level, start, List.of(goal), bounds, maxNodes, 4.0D).map(Route::path);
    }

    /** Single-goal search that ends within {@code reachSqr} of {@code goal}. */
    public Optional<List<BlockPos>> findPathTo(ServerLevel level, BlockPos start, BlockPos goal, AABB bounds, int maxNodes, double reachSqr) {
        return search(level, start, List.of(goal), bounds.minmax(new AABB(goal)), maxNodes, reachSqr).map(Route::path);
    }

    /** Finds the fruit with the cheapest route and the route to it. */
    public Optional<Route> findNearest(ServerLevel level, BlockPos start, Collection<BlockPos> goals, AABB bounds, int maxNodes) {
        if (goals.isEmpty()) {
            return Optional.empty();
        }
        return search(level, start, goals, bounds, maxNodes, FRUIT_REACH_SQR);
    }

    private Optional<Route> search(ServerLevel level, BlockPos start, Collection<BlockPos> goals, AABB bounds, int maxNodes, double reachSqr) {
        if (maxNodes <= 0) {
            throw new IllegalArgumentException("maxNodes must be positive");
        }
        BlockPos origin = start.immutable();
        AABB area = bounds.minmax(new AABB(origin));
        List<BlockPos> goalList = List.copyOf(goals);
        double reach = Math.sqrt(reachSqr);

        BlockPos immediate = reachedGoal(origin, goalList, reachSqr);
        if (immediate != null) {
            return Optional.of(new Route(List.of(origin), immediate));
        }

        PriorityQueue<Node> open = new PriorityQueue<>();
        Map<BlockPos, Double> best = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();
        Map<BlockPos, Boolean> clingCache = new HashMap<>();

        open.add(new Node(origin, 0.0D, heuristic(origin, goalList, reach), null));
        best.put(origin, 0.0D);
        int expansions = 0;

        while (!open.isEmpty() && expansions < maxNodes) {
            Node current = open.poll();
            if (!closed.add(current.pos)) {
                continue;
            }
            expansions++;

            BlockPos hit = reachedGoal(current.pos, goalList, reachSqr);
            if (hit != null) {
                return Optional.of(new Route(reconstruct(current), hit));
            }

            for (int[] move : MOVES) {
                BlockPos next = current.pos.offset(move[0], move[1], move[2]);
                if (closed.contains(next) || !contains(area, next)) {
                    continue;
                }
                if (!clingCache.computeIfAbsent(next, p -> graph.isClingNode(level, p))) {
                    continue;
                }
                if (!cornersClear(level, current.pos, move)) {
                    continue;
                }
                double g = current.g + stepCost(level.getBlockState(next), move);
                Double known = best.get(next);
                if (known == null || g < known) {
                    best.put(next, g);
                    open.add(new Node(next, g, g + heuristic(next, goalList, reach), current));
                }
            }
        }
        return Optional.empty();
    }

    private boolean cornersClear(ServerLevel level, BlockPos from, int[] move) {
        int nonZero = (move[0] != 0 ? 1 : 0) + (move[1] != 0 ? 1 : 0) + (move[2] != 0 ? 1 : 0);
        if (nonZero < 2) {
            return true;
        }
        if (move[0] != 0 && !graph.isPassable(level.getBlockState(from.offset(move[0], 0, 0)))) {
            return false;
        }
        if (move[1] != 0 && !graph.isPassable(level.getBlockState(from.offset(0, move[1], 0)))) {
            return false;
        }
        return move[2] == 0 || graph.isPassable(level.getBlockState(from.offset(0, 0, move[2])));
    }

    private double stepCost(BlockState state, int[] move) {
        int nonZero = (move[0] != 0 ? 1 : 0) + (move[1] != 0 ? 1 : 0) + (move[2] != 0 ? 1 : 0);
        double base = switch (nonZero) {
            case 1 -> move[1] != 0 ? 1.2D : 1.0D;
            default -> move[1] != 0 ? 1.5D : 1.414D;
        };
        // Prefer moving along foliage over hopping through air pockets.
        return graph.isCanopyPart(state) ? base : base * 1.15D;
    }

    private static BlockPos reachedGoal(BlockPos pos, List<BlockPos> goals, double reachSqr) {
        for (BlockPos goal : goals) {
            if (pos.distSqr(goal) <= reachSqr) {
                return goal;
            }
        }
        return null;
    }

    private static double heuristic(BlockPos from, List<BlockPos> goals, double reach) {
        double min = Double.MAX_VALUE;
        for (BlockPos goal : goals) {
            double dx = from.getX() - goal.getX();
            double dy = from.getY() - goal.getY();
            double dz = from.getZ() - goal.getZ();
            min = Math.min(min, Math.sqrt(dx * dx + dy * dy + dz * dz));
        }
        return Math.max(0.0D, min - reach);
    }

    private static boolean contains(AABB box, BlockPos pos) {
        return pos.getX() >= Math.floor(box.minX) && pos.getX() <= Math.ceil(box.maxX)
                && pos.getY() >= Math.floor(box.minY) && pos.getY() <= Math.ceil(box.maxY)
                && pos.getZ() >= Math.floor(box.minZ) && pos.getZ() <= Math.ceil(box.maxZ);
    }

    private static List<BlockPos> reconstruct(Node end) {
        List<BlockPos> path = new ArrayList<>();
        for (Node node = end; node != null; node = node.parent) {
            path.add(node.pos);
        }
        Collections.reverse(path);
        return path;
    }

    private static int[][] buildMoves() {
        List<int[]> moves = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int nonZero = (dx != 0 ? 1 : 0) + (dy != 0 ? 1 : 0) + (dz != 0 ? 1 : 0);
                    if (nonZero == 1 || nonZero == 2) {
                        moves.add(new int[]{dx, dy, dz});
                    }
                }
            }
        }
        return moves.toArray(new int[0][]);
    }

    private static final class Node implements Comparable<Node> {
        final BlockPos pos;
        final double g;
        final double f;
        final Node parent;

        Node(BlockPos pos, double g, double f, Node parent) {
            this.pos = pos;
            this.g = g;
            this.f = f;
            this.parent = parent;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f, other.f);
        }
    }

    public CanopyGraph graph() {
        return graph;
    }
}
