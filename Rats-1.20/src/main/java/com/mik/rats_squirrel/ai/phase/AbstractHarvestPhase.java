package com.mik.rats_squirrel.ai.phase;

/**
 * Convenience base for Phase 0 phase handlers.
 *
 * <p>Every concrete phase inherits a no-op tick that keeps the phase active. Phase 1-3 replace
 * individual overrides without changing the state-machine wiring.</p>
 */
public abstract class AbstractHarvestPhase implements HarvestPhase {
    @Override
    public PhaseResult tick(HarvestContext context) {
        return PhaseResult.stay();
    }
}
