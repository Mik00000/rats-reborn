package com.mik.rats_squirrel.ai.phase;

/**
 * Phases of the squirrel harvest state machine.
 *
 * <p>{@link #ACQUIRE} is entered from {@code canUse}; {@link #FINISHED} is terminal and causes
 * {@code canContinueToUse} to return {@code false} so that {@code stop()} can release the
 * movement flags before the base deposit goal runs.</p>
 */
public enum Phase {
    ACQUIRE,
    GO_TO_BUSH,
    GO_TO_TRUNK,
    CLIMB_TRUNK,
    CANOPY_TRAVEL,
    HARVEST,
    LEAP_TO_TREE,
    RETURN_TO_TRUNK,
    DESCEND_TRUNK,
    EMERGENCY_DESCENT,
    FINISHED
}
