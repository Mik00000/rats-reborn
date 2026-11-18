package com.mik.rats_squirrel.ai.phase;

/**
 * One state of the squirrel harvest state machine.
 *
 * <p>Implementations must be stateless with respect to rat identity: all mutable data lives in
 * {@link HarvestContext}. A phase may request a transition, but only the goal performs it and
 * calls {@link #onExit(HarvestContext)} / {@link #onEnter(HarvestContext)}.</p>
 */
public interface HarvestPhase {
    /**
     * Called once per goal tick while this phase is active.
     *
     * @return a non-null result; use {@link PhaseResult#stay()} when no transition is needed
     */
    PhaseResult tick(HarvestContext context);

    /** Called after the goal enters this phase. */
    default void onEnter(HarvestContext context) {
    }

    /** Called before the goal leaves this phase, including on goal stop. */
    default void onExit(HarvestContext context) {
    }
}
