package com.mik.rats_squirrel.deposit;

/**
 * Result of validating a rat's configured deposit before harvesting starts.
 */
public enum DepositStatus {
    /** The deposit exists, belongs to the current dimension and can accept the probed stack. */
    OK,
    /** The rat has no deposit position configured. */
    MISSING,
    /** The configured deposit is in another dimension. */
    WRONG_DIM,
    /** The deposit block is protected from this rat. */
    PROTECTED,
    /** The deposit exists but has no room for the probed stack. */
    FULL,
    /** The deposit position has no block entity or item-handler capability. */
    NO_CAPABILITY,
    /** The deposit could accept the stack, but no path to it exists. */
    UNREACHABLE
}
