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

	// Закрывает служебный класс; ничего не принимает и не отдаёт.
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

		// Создаёт состояние транзита; принимает маршрут, конечную цель и скорость подхода, ничего не отдаёт.
		private Session(RatTubePathHelper.TubeRoute route, Vec3 target, double speed) {
			this.route = route;
			this.target = target;
			this.speed = speed;
			this.phase = Phase.APPROACH_ENTRY;
			this.segmentIndex = 0;
			this.stuckTicks = 0;
			this.lastFeetPos = BlockPos.ZERO;
		}

		// Проверяет, относится ли текущий транзит к той же цели; принимает цель и допуск, отдаёт true при совпадении.
		public boolean targetsSame(Vec3 other, double tolerance) {
			return this.target.distanceToSqr(other) < tolerance * tolerance;
		}

		// Возвращает активность транзита; ничего не принимает.
		public boolean isActive() {
			return this.phase != null;
		}

		// Возвращает позицию внешней клетки входа; ничего не принимает.
		public BlockPos entryStandPos() {
			return this.route.entry().standPos();
		}

		// Возвращает скорость подхода к входу; ничего не принимает.
		public double speed() {
			return this.speed;
		}

		// Возвращает true, если крыса ещё должна идти к входу обычной навигацией.
		public boolean needsVanillaApproach() {
			return this.phase == Phase.APPROACH_ENTRY;
		}
	}

	// Пытается создать транзит по трубе; принимает крысу, цель и скорость, отдаёт true если маршрут по трубе выбран.
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
			RatsMod.LOGGER.debug("Крыса {} выбрала маршрут по трубе: вход {}, выход {}, сегментов {}",
					rat.getUUID(), session.route.entry().standPos(), session.route.exit().standPos(), session.route.segments().size());
			return true;
		} catch (RuntimeException exception) {
			RatsMod.LOGGER.error("Не удалось начать транзит по трубе для крысы {}: {}", rat.getUUID(), exception.getMessage(), exception);
			throw exception;
		}
	}

	// Тикает активный транзит по трубе; принимает крысу, отдаёт true пока транзит удерживает управление.
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
				RatsMod.LOGGER.debug("Крыса {} зависла перед входом трубы в {}, сброс транзита", rat.getUUID(), feet);
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
			RatsMod.LOGGER.error("Ошибка тика транзита по трубе для крысы {}: {}", rat.getUUID(), exception.getMessage(), exception);
			rat.clearTubeTransit();
			throw exception;
		}
	}

	// Ведёт крысу к внешней клетке входа; принимает крысу и сессию, ничего не отдаёт.
	private static void tickApproachEntry(TamedRat rat, Session session) {
		if (isAtEntrance(rat, session.route.entry())) {
			stopPathOnly(rat);
			session.phase = Phase.ENTER_TUBE;
			session.stuckTicks = 0;
			return;
		}
		driveVanillaApproach(rat, session);
	}

	// Переводит крысу из внешней клетки внутрь первого сегмента; принимает крысу и сессию, ничего не отдаёт.
	private static void tickEnterTube(TamedRat rat, Session session) {
		stopPathOnly(rat);
		rat.switchNavigator(2);
		if (advanceBlockStep(rat, session, session.route.entry().standPos(), session.route.entry().tubePos())) {
			session.segmentIndex = Math.max(1, session.route.segments().indexOf(session.route.entry().tubePos()) + 1);
			session.phase = Phase.TRAVERSE;
			clearCurrentStep(session);
		}
	}

	// Двигает крысу по списку сегментов трубы; принимает крысу и сессию, ничего не отдаёт.
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

	// Выводит крысу из последнего сегмента во внешнюю клетку выхода; принимает крысу и сессию, ничего не отдаёт.
	private static void tickExitTube(TamedRat rat, Session session) {
		stopPathOnly(rat);
		BlockPos from = session.route.exit().tubePos();
		BlockPos to = session.route.exit().standPos();
		if (advanceBlockStep(rat, session, from, to)) {
			session.phase = Phase.FINISH;
			clearCurrentStep(session);
		}
	}

	// Завершает транзит и сбрасывает вертикальное состояние; принимает крысу и сессию, ничего не отдаёт.
	private static void tickFinish(TamedRat rat, Session session) {
		session.phase = null;
		rat.climbingTube = false;
		rat.setTubeClimbDirection(0);
	}

	// Выполняет плавный шаг между соседними блоками; принимает крысу, сессию, начальный и конечный блок, отдаёт true в конце шага.
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
			RatsMod.LOGGER.error("Не удалось выполнить шаг крысы {} по трубе {} -> {}: {}", rat.getUUID(), from, to, exception.getMessage(), exception);
			throw exception;
		}
	}

	// Сбрасывает текущий шаг между двумя блоками; принимает сессию, ничего не отдаёт.
	private static void clearCurrentStep(Session session) {
		session.currentStepFrom = null;
		session.currentStepTo = null;
		session.currentStepTick = 0;
	}

	// Поддерживает обычный путь до внешней клетки входа; принимает крысу и транзит, ничего не отдаёт.
	private static void driveVanillaApproach(TamedRat rat, Session session) {
		PathNavigation navigation = rat.getNavigation();
		if (navigation instanceof RatNavigation ratNavigation) {
			BlockPos stand = session.entryStandPos();
			if (navigation.isDone() || navigation.isStuck() || rat.tickCount % 20 == 0) {
				ratNavigation.moveToWithoutTubePlanning(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, session.speed());
			}
		}
	}

	// Проверяет, дошла ли крыса до открытой грани входа; принимает крысу и вход, отдаёт true при достижении.
	private static boolean isAtEntrance(TamedRat rat, RatTubePathHelper.TubeEntrance entrance) {
		if (rat.blockPosition().equals(entrance.tubePos())) {
			return true;
		}
		Vec3 standCenter = Vec3.atBottomCenterOf(entrance.standPos());
		Vec3 tubeCenter = Vec3.atBottomCenterOf(entrance.tubePos());
		return rat.position().distanceToSqr(standCenter) <= ENTRY_REACHED_DISTANCE_SQ
				|| rat.position().distanceToSqr(tubeCenter) <= ENTRY_REACHED_DISTANCE_SQ;
	}

	// Останавливает только текущий путь навигации без сброса транзита; принимает крысу.
	private static void stopPathOnly(TamedRat rat) {
		PathNavigation navigation = rat.getNavigation();
		if (navigation instanceof RatNavigation ratNavigation) {
			ratNavigation.stopPathOnly();
		} else {
			navigation.stop();
		}
	}

	// Поворачивает крысу по направлению шага; принимает крысу, начальный и конечный блок, ничего не отдаёт.
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
