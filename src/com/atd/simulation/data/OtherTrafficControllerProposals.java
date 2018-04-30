package com.atd.simulation.data;

import com.atd.simulation.RunwayState;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Container of proposals, stored from another one {@link com.atd.simulation.TrafficController}, used for decision
 * synchronisation and common selection of proposals for lading to execute.
 */
public class OtherTrafficControllerProposals {
    @Getter
    private Map<RunwayState.RunwayType, LandingRequest> otherControllerProposal;
    private AtomicBoolean empty;

    public OtherTrafficControllerProposals() {
        empty = new AtomicBoolean(true);
        otherControllerProposal = new HashMap<>();
    }

    /**
     * Check if proposal data from another traffic controller present.
     */
    public synchronized boolean isEmpty() {
        return empty.get();
    }

    /**
     * Reset state, to mark proposal data as empty. Returns 'true' if succeed, otherwise - 'false'.
     */
    public boolean reset() {
        boolean state = isEmpty();
        if (!state) {
            this.empty.set(true);
            return true;
        }
        return false;
    }

    /**
     * Update and store new passed-in proposal data. Returns 'true' if succeed, otherwise - 'false'.
     */
    public boolean updateProposals(Map<RunwayState.RunwayType, LandingRequest> otherControllerProposal) {
        boolean isReset = isEmpty();
        if (isReset) {
            this.otherControllerProposal = otherControllerProposal;
            empty.set(false);
        }
        return isReset;
    }
}
