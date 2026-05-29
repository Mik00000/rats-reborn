package com.github.alexthe666.rats.server.entity.ai.navigation.evaluator;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.block.*;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatTubePathHelper;
import com.github.alexthe666.rats.server.misc.RatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.Nullable;

public class RatNodeEvaluator extends WalkNodeEvaluator {

	@Override
	public PathType getPathType(PathfindingContext context, int x, int y, int z) {
		// Оценка узла для объёма крысы; принимает контекст и координаты, отдаёт тип узла.
		try {
			if (this.mob instanceof TamedRat rat) {
				PathType tubeType = RatTubePathHelper.resolveTubePathType(context.level(), new BlockPos(x, y, z), rat);
				if (tubeType != null) {
					return tubeType;
				}
			}
			return super.getPathType(context, x, y, z);
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось оценить узел пути крысы (getPathType) на {}, {}, {}", x, y, z, exception);
			throw exception;
		}
	}

	@Override
	public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
		// Определяет проходимость узла для крысы; принимает контекст, координаты и моба, отдаёт тип узла.
		try {
			PathType types = super.getPathTypeOfMob(context, x, y, z, mob);
			BlockPos pos = new BlockPos(x, y, z);
			Block block = context.getBlockState(pos).getBlock();
			Block blockBelow = context.getBlockState(pos.below()).getBlock();
			if (mob instanceof TamedRat rat) {
				PathType tubeType = RatTubePathHelper.resolveTubePathType(context.level(), pos, rat);
				if (tubeType != null) {
					types = tubeType;
				} else if (block instanceof RatHoleBlock || block instanceof RatTrapBlock || block instanceof RatCageBlock || RatUtils.isOpenRatTube(context.level(), pos)) {
					types = PathType.WALKABLE;
				}
				if (block instanceof RatQuarryPlatformBlock) {
					types = PathType.OPEN;
				}
				if (blockBelow instanceof RatQuarryPlatformBlock && context.getBlockState(pos).isAir()) {
					types = PathType.WALKABLE;
				}
				if (block instanceof SlabBlock) {
					types = PathType.WALKABLE;
				}

				if (types == PathType.DOOR_WOOD_CLOSED && this.canOpenDoors() && this.canPassDoors()) {
					types = PathType.WALKABLE_DOOR;
				}

				if (types == PathType.DOOR_OPEN && !this.canPassDoors()) {
					types = PathType.BLOCKED;
				}

				if (types == PathType.RAIL && !(block instanceof BaseRailBlock) && !(blockBelow instanceof BaseRailBlock) && !(block instanceof RatTubeBlock)) {
					types = PathType.UNPASSABLE_RAIL;
				}

				if (rat.isInCage()) {
					if (block instanceof RatCageBlock || block instanceof RatTubeBlock) {
						types = PathType.WALKABLE;
					} else {
						types = PathType.BLOCKED;
					}
				}
			}
			return types;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось оценить узел пути крысы на {}, {}, {}", x, y, z, exception);
			throw exception;
		}
	}

	@Override
	@Nullable
	protected Node findAcceptedNode(int x, int y, int z, int verticalDelta, double nodeFloorLevel, Direction direction, PathType pathType) {
		// Принимает координаты узла и тип; отдаёт узел пути или null. Для трубы — узел внутри сегмента без провала вниз.
		try {
			Node node = super.findAcceptedNode(x, y, z, verticalDelta, nodeFloorLevel, direction, pathType);
			if (node != null || !(this.mob instanceof TamedRat rat)) {
				return node;
			}
			PathType tubeType = RatTubePathHelper.resolveTubePathType(this.currentContext.level(), new BlockPos(x, y, z), rat);
			if (tubeType == null || !RatTubePathHelper.isTubeSegment(this.currentContext.level(), new BlockPos(x, y, z))) {
				return null;
			}
			float malus = rat.getPathfindingMalus(tubeType);
			if (malus >= 0.0F) {
				return null;
			}
			Node fallback = this.getNode(x, y, z);
			fallback.type = tubeType;
			fallback.costMalus = malus;
			return fallback;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось принять узел пути крысы на {}, {}, {}", x, y, z, exception);
			throw exception;
		}
	}
}
