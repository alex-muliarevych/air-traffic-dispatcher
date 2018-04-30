package com.atd.simulation;

import com.atd.simulation.data.LandingRequest;
import lombok.Getter;

import java.util.LinkedList;

/**
 * Storage of landing requests.
 */
@Getter
public class LandingRequestStorage {

    private final LinkedList<LandingRequest> emergencyRequests;
    private final LinkedList<LandingRequest> normalRequests;

    public LandingRequestStorage() {
        emergencyRequests = new LinkedList<>();
        normalRequests = new LinkedList<>();
    }

    public void removeLandingRequestFromQueue(LandingRequest request) {
        if (emergencyRequests.contains(request)) {
            emergencyRequests.remove(request);
        }
        if (normalRequests.contains(request)) {
            normalRequests.remove(request);
        }
    }
}
