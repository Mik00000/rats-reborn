package com.mik.rats_squirrel.ai.phase;

import org.jetbrains.annotations.Nullable;

/**
 * Result of one phase tick.
 *
 * <p>A result either requests staying in the current phase, a transition to another phase, or
 * a clean stop of the whole goal. Transitions are applied by the goal, never by a phase
 * directly.</p>
 */
public record PhaseResult(@Nullable Phase next, boolean finished) {
    public PhaseResult {
        if (finished && next != null) {
            throw new IllegalArgumentException("a finished result cannot also request a transition");
        }
    }

    /** Keeps the current phase active. */
    public static PhaseResult stay() {
        return new PhaseResult(null, false);
    }

    /** Requests a transition to the supplied phase. */
    public static PhaseResult transition(Phase next) {
        if (next == null) {
            throw new IllegalArgumentException("next phase must not be null");
        }
        return new PhaseResult(next, false);
    }

    /** Requests a clean goal stop. */
    public static PhaseResult finish() {
        return new PhaseResult(null, true);
    }

    /** Returns {@code true} when a transition is requested. */
    public boolean requestsTransition() {
        return next != null;
    }
}
