package com.atd.simulation.data;

import com.atd.simulation.RunwayState;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public class OtherTrafficControllerProposals {
    @Getter
    private Map<RunwayState.RunwayType, LandingRequest> otherControllerProposal;
    private AtomicBoolean empty;

    public OtherTrafficControllerProposals() {
        empty = new AtomicBoolean(true);
        otherControllerProposal = new HashMap<>();
    }

    public synchronized boolean isEmpty() {
        return empty.get();
    }

    public boolean reset() {
        boolean state = isEmpty();
        if (!state) {
            this.empty.set(true);
            return true;
        }
        return false;
    }

    public boolean updateProposals(Map<RunwayState.RunwayType, LandingRequest> otherControllerProposal) {
        boolean isReset = isEmpty();
        if (isReset) {
            this.otherControllerProposal = otherControllerProposal;
            empty.set(false);
        }
        return isReset;
    }
}
