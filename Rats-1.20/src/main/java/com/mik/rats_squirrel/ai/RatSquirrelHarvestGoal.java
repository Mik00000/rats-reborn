package com.mik.rats_squirrel.ai;

import com.github.alexthe666.rats.server.entity.ai.goal.harvest.BaseRatHarvestGoal;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.ai.phase.AcquirePhase;
import com.mik.rats_squirrel.ai.phase.CanopyTravelPhase;
import com.mik.rats_squirrel.ai.phase.ClimbTrunkPhase;
import com.mik.rats_squirrel.ai.phase.DescendTrunkPhase;
import com.mik.rats_squirrel.ai.phase.EmergencyDescentPhase;
import com.mik.rats_squirrel.ai.phase.GoToBushPhase;
import com.mik.rats_squirrel.ai.phase.GoToTrunkPhase;
import com.mik.rats_squirrel.ai.phase.HarvestActionPhase;
import com.mik.rats_squirrel.ai.phase.HarvestContext;
import com.mik.rats_squirrel.ai.phase.HarvestPhase;
import com.mik.rats_squirrel.ai.phase.LeapToTreePhase;
import com.mik.rats_squirrel.ai.phase.Phase;
import com.mik.rats_squirrel.ai.phase.PhaseResult;
import com.mik.rats_squirrel.ai.phase.ReturnToTrunkPhase;
import com.mik.rats_squirrel.canopy.CanopyMovementController;
import com.mik.rats_squirrel.config.SquirrelConfig;
import com.mik.rats_squirrel.deposit.DepositGate;
import com.mik.rats_squirrel.deposit.DepositStatus;
import com.mik.rats_squirrel.fruit.SquirrelHand;
import com.mik.rats_squirrel.tree.FailureBlacklist;
import com.mik.rats_squirrel.tree.HarvestTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates the squirrel harvest state machine.
 *
 * <p>This goal replaces the base harvest goal installed by {@code TamedRat.setupDynamicAI()}.
 * All navigation, scanning and harvesting details live in the phase handlers and their
 * collaborators; this class only:</p>
 * <ul>
 *   <li>accepts or rejects a goal start,</li>
 *   <li>owns the active task and phase timer,</li>
 *   <li>applies phase transitions, and</li>
 *   <li>guarantees that every movement flag is released on stop.</li>
 * </ul>
 *
 * <p>Start rules mirror the original Rats harvest goals
 * ({@code checkTheBasics(hasDeposit, hasDeposit)}): with a deposit bound the squirrel waits
 * until its hand can take more and no loose items are lying around; without a deposit it
 * always works and surplus fruit simply falls to the ground. The hand behaves as if the
 * Platter upgrade were installed (see {@link SquirrelHand}).</p>
 */
public class RatSquirrelHarvestGoal extends BaseRatHarvestGoal {
    private final Map<Phase, HarvestPhase> phases = new EnumMap<>(Phase.class);
    private final FailureBlacklist blacklist = new FailureBlacklist();
    private final CanopyMovementController movement;

    private Phase phase = Phase.ACQUIRE;
    private DepositStatus lastDepositStatus = DepositStatus.OK;

    @Nullable
    private HarvestContext context;
    @Nullable
    private HarvestTask task;
    @Nullable
    private HarvestTask pendingTask;
    /** Anchor of the cluster worked last; the selector favours it to avoid flip-flopping. */
    @Nullable
    private net.minecraft.core.BlockPos lastAnchor;
    private int lastAnchorTime;

    public RatSquirrelHarvestGoal(TamedRat rat) {
        super(rat);
        this.movement = new CanopyMovementController(rat);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.registerPhases();
    }

    private void registerPhases() {
        this.phases.put(Phase.ACQUIRE, new AcquirePhase());
        this.phases.put(Phase.GO_TO_BUSH, new GoToBushPhase());
        this.phases.put(Phase.GO_TO_TRUNK, new GoToTrunkPhase());
        this.phases.put(Phase.CLIMB_TRUNK, new ClimbTrunkPhase());
        this.phases.put(Phase.CANOPY_TRAVEL, new CanopyTravelPhase());
        this.phases.put(Phase.HARVEST, new HarvestActionPhase());
        this.phases.put(Phase.LEAP_TO_TREE, new LeapToTreePhase());
        this.phases.put(Phase.RETURN_TO_TRUNK, new ReturnToTrunkPhase());
        this.phases.put(Phase.DESCEND_TRUNK, new DescendTrunkPhase());
        this.phases.put(Phase.EMERGENCY_DESCENT, new EmergencyDescentPhase());
    }

    @Override
    public boolean canUse() {
        this.pendingTask = null;
        if (!this.isReadyToFire()) {
            return false;
        }
        if (!(this.rat.level() instanceof ServerLevel)) {
            return false;
        }
        boolean hasDeposit = this.rat.getDepositPos().isPresent();
        if (!this.checkTheBasics(hasDeposit, hasDeposit)) {
            return false;
        }
        this.lastDepositStatus = hasDeposit
                ? DepositGate.check(this.rat, this.rat.getMainHandItem())
                : DepositStatus.MISSING;

        Optional<HarvestTask> acquired = this.tryAcquireTask();
        if (acquired.isEmpty()) {
            return false;
        }

        this.pendingTask = acquired.get();
        if (SquirrelConfig.debugLogging()) {
            HarvestTask t = this.pendingTask;
            com.mik.rats_squirrel.RatsSquirrelMod.LOGGER.info("[squirrel {}] new task: {} {} at {} with {} ripe {} (first {}), hand {}x{}",
                    this.rat.getId(), t.cluster().isBush() ? "bush" : "tree", t.cluster().id(), t.cluster().anchor(),
                    t.fruitQueue().size(), t.kind().id(), t.currentFruit(), this.rat.getMainHandItem().getCount(), this.rat.getMainHandItem().getItem());
        }
        return true;
    }

    /** Built-in Platter: the hand may keep collecting until it holds a full stack. */
    @Override
    protected boolean checkIfItemCanBeHeld() {
        return SquirrelHand.canHoldMore(this.rat);
    }

    /** Acquires the next reachable harvest task from the level-shared fruit index. */
    protected Optional<HarvestTask> tryAcquireTask() {
        if (!(this.rat.level() instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        int gameTime = (int) serverLevel.getGameTime();
        this.blacklist.cleanup(gameTime);

        com.mik.rats_squirrel.tree.LevelFruitIndex index = com.mik.rats_squirrel.tree.LevelFruitIndex.get(serverLevel);
        com.mik.rats_squirrel.tree.TargetSelector selector = new com.mik.rats_squirrel.tree.TargetSelector(index.scanner(), this.blacklist);
        net.minecraft.core.BlockPos sticky = this.lastAnchor != null && gameTime - this.lastAnchorTime < 1200 ? this.lastAnchor : null;
        return selector.select(this.rat, gameTime, sticky);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.phase == Phase.FINISHED) {
            return false;
        }

        // RETURN/DESCEND/EMERGENCY must never be aborted by hunger, schedule or a command change.
        if (this.isSafeExitPhase()) {
            return true;
        }

        // Phase 3: an interrupted canopy goal safely transitions to RETURN_TO_TRUNK instead of dropping.
        if (this.isInCanopyOrClimbing()) {
            if (!this.checkTheBasics(false, false) || this.task == null) {
                this.transitionTo(Phase.RETURN_TO_TRUNK);
            }
            return true;
        }

        if (this.phase == Phase.ACQUIRE) {
            return this.checkTheBasics(false, false);
        }

        return this.checkTheBasics(false, false) && this.task != null;
    }

    @Override
    public void start() {
        super.start();
        this.phase = Phase.ACQUIRE;
        this.context = null;
        this.task = this.pendingTask;
        this.pendingTask = null;

        if (!(this.rat.level() instanceof ServerLevel serverLevel)) {
            this.phase = Phase.FINISHED;
            return;
        }

        this.context = new HarvestContext(this.rat, serverLevel, this.movement, this.blacklist);
        this.context.setTask(this.task);

        if (this.task == null) {
            this.phase = Phase.FINISHED;
            return;
        }

        HarvestPhase acquire = this.phases.get(Phase.ACQUIRE);
        if (acquire != null) {
            acquire.onEnter(this.context);
        }
    }

    @Override
    public void tick() {
        if (!(this.rat.level() instanceof ServerLevel serverLevel)) {
            this.phase = Phase.FINISHED;
            return;
        }

        if (this.context == null || this.context.level() != serverLevel) {
            this.context = new HarvestContext(this.rat, serverLevel, this.movement, this.blacklist);
            this.context.setTask(this.task);
        }

        if (this.phase == Phase.FINISHED) {
            return;
        }

        HarvestPhase handler = this.phases.get(this.phase);
        if (handler == null) {
            this.phase = Phase.FINISHED;
            return;
        }

        PhaseResult result = handler.tick(this.context);
        if (result.finished()) {
            this.phase = Phase.FINISHED;
            return;
        }

        Phase next = result.next();
        if (next != null) {
            this.transitionTo(next);
            return;
        }

        this.context.advancePhaseTick();
    }

    private void transitionTo(Phase next) {
        if (this.phase == next) {
            return;
        }

        if (SquirrelConfig.debugLogging()) {
            com.mik.rats_squirrel.RatsSquirrelMod.LOGGER.info("[squirrel {}] {} -> {} after {} ticks",
                    this.rat.getId(), this.phase, next, this.context == null ? 0 : this.context.ticksInPhase());
        }

        if (this.context != null) {
            HarvestPhase previous = this.phases.get(this.phase);
            if (previous != null) {
                previous.onExit(this.context);
            }
        }

        this.phase = next;

        if (this.context == null) {
            return;
        }

        this.context.resetPhaseTicks();
        HarvestPhase following = this.phases.get(next);
        if (following != null) {
            following.onEnter(this.context);
        }
    }

    @Override
    public void stop() {
        if (this.context != null) {
            HarvestPhase current = this.phases.get(this.phase);
            if (current != null) {
                current.onExit(this.context);
            }
        }

        // Releasing every flag is the safe baseline; an interrupted rat in a tree falls onto
        // the leaves (fall damage is immune) and resumes canopy travel on the next start.
        this.movement.stopAndRelease();

        if (this.rat.crafting) {
            this.rat.crafting = false;
            this.rat.level().broadcastEntityEvent(this.rat, (byte) 86);
        }

        if (this.task != null) {
            this.lastAnchor = this.task.cluster().anchor();
            this.lastAnchorTime = (int) this.rat.level().getGameTime();
        }
        this.context = null;
        this.task = null;
        this.pendingTask = null;
        this.phase = Phase.ACQUIRE;
        super.stop();
    }

    private boolean isSafeExitPhase() {
        return this.phase == Phase.RETURN_TO_TRUNK
                || this.phase == Phase.DESCEND_TRUNK
                || this.phase == Phase.EMERGENCY_DESCENT;
    }

    private boolean isInCanopyOrClimbing() {
        return this.movement.isInTree()
                || this.phase == Phase.CLIMB_TRUNK
                || this.phase == Phase.CANOPY_TRAVEL
                || this.phase == Phase.HARVEST
                || this.phase == Phase.LEAP_TO_TREE;
    }

    /** Current state-machine phase. */
    public Phase phase() {
        return this.phase;
    }

    /** Active task, or {@code null} before acquisition and after stop. */
    @Nullable
    public HarvestTask task() {
        return this.task;
    }

    /** Per-goal failure blacklist; debug commands may inspect or clear it. */
    public FailureBlacklist blacklist() {
        return this.blacklist;
    }

    /** Movement controller owned by this goal. */
    public CanopyMovementController movement() {
        return this.movement;
    }

    /** Ticks spent in the current phase. */
    public int ticksInPhase() {
        return this.context == null ? 0 : this.context.ticksInPhase();
    }

    /** Last evaluated deposit status. */
    public DepositStatus lastDepositStatus() {
        return this.lastDepositStatus;
    }
}
