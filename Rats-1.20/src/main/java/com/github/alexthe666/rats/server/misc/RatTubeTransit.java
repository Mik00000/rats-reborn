package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.ai.navigation.navigation.RatNavigation;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class RatTubeTransit {

	private static final int STUCK_TICK_LIMIT = 80;
	private static final int TICKS_PER_SEGMENT = 4;
	private static final double ENTRY_REACHED_DISTANCE_SQ = 1.75D;

	// Closes the utility class; takes and returns nothing.
	private RatTubeTransit() {
	}

	public enum Phase {
		APPROACH_ENTRY,
		ENTER_TUBE,
		TRAVERSE,
		EXIT_TUBE,
		FINISH
	}

	public static final class Session {
		private final RatTubePathHelper.TubeRoute route;
		private final Vec3 target;
		private final double speed;
		private Phase phase;
		private int segmentIndex;
		private int stuckTicks;
		private BlockPos lastFeetPos;
		private BlockPos currentStepFrom;
		private BlockPos currentStepTo;
		private int currentStepTick;

		// Creates the transit state; takes the route, final target and approach speed, returns nothing.
		private Session(RatTubePathHelper.TubeRoute route, Vec3 target, double speed) {
			this.route = route;
			this.target = target;
			this.speed = speed;
			this.phase = Phase.APPROACH_ENTRY;
			this.segmentIndex = 0;
			this.stuckTicks = 0;
			this.lastFeetPos = BlockPos.ZERO;
		}

		// Checks whether the current transit is for the same target; takes the target and tolerance, returns true on a match.
		public boolean targetsSame(Vec3 other, double tolerance) {
			return this.target.distanceToSqr(other) < tolerance * tolerance;
		}

		// Returns whether the transit is active; takes nothing.
		public boolean isActive() {
			return this.phase != null;
		}

		// Returns the position of the outer entrance cell; takes nothing.
		public BlockPos entryStandPos() {
			return this.route.entry().standPos();
		}

		// Returns the approach speed to the entrance; takes nothing.
		public double speed() {
			return this.speed;
		}

		// Returns true if the rat still has to walk to the entrance with regular navigation.
		public boolean needsVanillaApproach() {
			return this.phase == Phase.APPROACH_ENTRY;
		}
	}

	// Tries to create a tube transit; takes the rat, target and speed, returns true if the tube route was chosen.
	public static boolean tryStart(TamedRat rat, Vec3 target, double speed) {
		try {
			if (rat.level().isClientSide()) {
				return false;
			}
			Session existing = rat.getTubeTransit();
			if (existing != null && existing.isActive() && existing.targetsSame(target, 1.5D)) {
				return true;
			}
			Optional<RatTubePathHelper.TubeRoute> planned = RatTubePathHelper.planRoute(rat.level(), rat.blockPosition(), target);
			if (planned.isEmpty()) {
				return false;
			}
			Session session = new Session(planned.get(), target, speed);
			if (rat.isInTube()) {
				int idx = planned.get().segments().indexOf(rat.blockPosition());
				if (idx >= 0) {
					session.segmentIndex = idx + 1;
					session.phase = Phase.TRAVERSE;
				}
			}
			rat.setTubeTransit(session);
			RatsMod.LOGGER.debug("Rat {} chose a tube route: entry {}, exit {}, segments {}",
					rat.getUUID(), session.route.entry().standPos(), session.route.exit().standPos(), session.route.segments().size());
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to start a tube transit for rat {}: {}", rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Ticks the active tube transit; takes the rat, returns true while the transit holds control.
	public static boolean tick(TamedRat rat) {
		try {
			Session session = rat.getTubeTransit();
			if (session == null || !session.isActive()) {
				return false;
			}
			if (rat.level().isClientSide()) {
				return true;
			}
			BlockPos feet = rat.blockPosition();
			if (feet.equals(session.lastFeetPos)) {
				session.stuckTicks++;
			} else {
				session.stuckTicks = 0;
				session.lastFeetPos = feet;
			}
			if (session.stuckTicks >= STUCK_TICK_LIMIT && session.phase == Phase.APPROACH_ENTRY) {
				RatsMod.LOGGER.debug("Rat {} got stuck in front of the tube entrance at {}, resetting transit", rat.getUUID(), feet);
				rat.clearTubeTransit();
				return false;
			}
			switch (session.phase) {
				case APPROACH_ENTRY -> tickApproachEntry(rat, session);
				case ENTER_TUBE -> tickEnterTube(rat, session);
				case TRAVERSE -> tickTraverse(rat, session);
				case EXIT_TUBE -> tickExitTube(rat, session);
				case FINISH -> tickFinish(rat, session);
			}
			if (!session.isActive()) {
				rat.clearTubeTransit();
				return false;
			}
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Tube transit tick error for rat {}: {}", rat.getUUID(), exception.getMessage(), exception);
			rat.clearTubeTransit();
			throw exception;
		}
	}

	// Leads the rat to the outer entrance cell; takes the rat and session, returns nothing.
	private static void tickApproachEntry(TamedRat rat, Session session) {
		if (isAtEntrance(rat, session.route.entry())) {
			stopPathOnly(rat);
			session.phase = Phase.ENTER_TUBE;
			session.stuckTicks = 0;
			return;
		}
		driveVanillaApproach(rat, session);
	}

	// Moves the rat from the outer cell into the first segment; takes the rat and session, returns nothing.
	private static void tickEnterTube(TamedRat rat, Session session) {
		stopPathOnly(rat);
		rat.switchNavigator(2);
		if (advanceBlockStep(rat, session, session.route.entry().standPos(), session.route.entry().tubePos())) {
			session.segmentIndex = Math.max(1, session.route.segments().indexOf(session.route.entry().tubePos()) + 1);
			session.phase = Phase.TRAVERSE;
			clearCurrentStep(session);
		}
	}

	// Moves the rat along the list of tube segments; takes the rat and session, returns nothing.
	private static void tickTraverse(TamedRat rat, Session session) {
		if (session.segmentIndex >= session.route.segments().size()) {
			session.phase = Phase.EXIT_TUBE;
			clearCurrentStep(session);
			return;
		}
		BlockPos from = session.segmentIndex == 0 ? session.route.entry().tubePos() : session.route.segments().get(session.segmentIndex - 1);
		BlockPos to = session.route.segments().get(session.segmentIndex);
		if (advanceBlockStep(rat, session, from, to)) {
			session.segmentIndex++;
			clearCurrentStep(session);
		}
	}

	// Brings the rat out of the last segment into the outer exit cell; takes the rat and session, returns nothing.
	private static void tickExitTube(TamedRat rat, Session session) {
		stopPathOnly(rat);
		BlockPos from = session.route.exit().tubePos();
		BlockPos to = session.route.exit().standPos();
		if (advanceBlockStep(rat, session, from, to)) {
			session.phase = Phase.FINISH;
			clearCurrentStep(session);
		}
	}

	// Finishes the transit and resets the vertical state; takes the rat and session, returns nothing.
	private static void tickFinish(TamedRat rat, Session session) {
		session.phase = null;
		rat.climbingTube = false;
		rat.setTubeClimbDirection(0);
	}

	// Performs a smooth step between adjacent blocks; takes the rat, session, start and end block, returns true at the end of the step.
	private static boolean advanceBlockStep(TamedRat rat, Session session, BlockPos from, BlockPos to) {
		try {
			stopPathOnly(rat);
			if (session.currentStepFrom == null || session.currentStepTo == null || !session.currentStepFrom.equals(from) || !session.currentStepTo.equals(to)) {
				session.currentStepFrom = from;
				session.currentStepTo = to;
				session.currentStepTick = 0;
				faceToward(rat, from, to);
			}
			int nextTick = Math.min(TICKS_PER_SEGMENT, session.currentStepTick + 1);
			double progress = (double) nextTick / (double) TICKS_PER_SEGMENT;
			Vec3 fromCenter = Vec3.atBottomCenterOf(from);
			Vec3 toCenter = Vec3.atBottomCenterOf(to);
			double x = Mth.lerp(progress, fromCenter.x(), toCenter.x());
			double y = Mth.lerp(progress, fromCenter.y(), toCenter.y());
			double z = Mth.lerp(progress, fromCenter.z(), toCenter.z());
			faceToward(rat, from, to);
			rat.setDeltaMovement(Vec3.ZERO);
			rat.fallDistance = 0.0F;
			rat.setPos(x, y, z);
			session.currentStepTick = nextTick;
			return session.currentStepTick >= TICKS_PER_SEGMENT;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Failed to perform a tube step for rat {} {} -> {}: {}", rat.getUUID(), from, to, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Resets the current step between two blocks; takes the session, returns nothing.
	private static void clearCurrentStep(Session session) {
		session.currentStepFrom = null;
		session.currentStepTo = null;
		session.currentStepTick = 0;
	}

	// Maintains the regular path to the outer entrance cell; takes the rat and transit, returns nothing.
	private static void driveVanillaApproach(TamedRat rat, Session session) {
		PathNavigation navigation = rat.getNavigation();
		if (navigation instanceof RatNavigation ratNavigation) {
			BlockPos stand = session.entryStandPos();
			if (navigation.isDone() || navigation.isStuck() || rat.tickCount % 20 == 0) {
				ratNavigation.moveToWithoutTubePlanning(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, session.speed());
			}
		}
	}

	// Checks whether the rat reached the open entrance face; takes the rat and entrance, returns true when reached.
	private static boolean isAtEntrance(TamedRat rat, RatTubePathHelper.TubeEntrance entrance) {
		if (rat.blockPosition().equals(entrance.tubePos())) {
			return true;
		}
		Vec3 standCenter = Vec3.atBottomCenterOf(entrance.standPos());
		Vec3 tubeCenter = Vec3.atBottomCenterOf(entrance.tubePos());
		return rat.position().distanceToSqr(standCenter) <= ENTRY_REACHED_DISTANCE_SQ
				|| rat.position().distanceToSqr(tubeCenter) <= ENTRY_REACHED_DISTANCE_SQ;
	}

	// Stops only the current navigation path without resetting the transit; takes the rat.
	private static void stopPathOnly(TamedRat rat) {
		PathNavigation navigation = rat.getNavigation();
		if (navigation instanceof RatNavigation ratNavigation) {
			ratNavigation.stopPathOnly();
		} else {
			navigation.stop();
		}
	}

	// Turns the rat in the step direction; takes the rat, start and end block, returns nothing.
	private static void faceToward(TamedRat rat, BlockPos from, BlockPos to) {
		double dx = to.getX() - from.getX();
		double dz = to.getZ() - from.getZ();
		int dy = to.getY() - from.getY();
		if (dx == 0.0D && dz == 0.0D) {
			if (dy != 0) {
				rat.climbingTube = true;
				rat.setTubeClimbDirection(dy > 0 ? 1 : -1);
			}
			return;
		}
		float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
		rat.setYRot(yaw);
		rat.yBodyRot = yaw;
		rat.yHeadRot = yaw;
		rat.yRotO = yaw;
		rat.climbingTube = rat.isInTube() && dy != 0;
		if (rat.climbingTube) {
			rat.setTubeClimbDirection(dy > 0 ? 1 : -1);
		} else {
			rat.setTubeClimbDirection(0);
		}
	}
}
