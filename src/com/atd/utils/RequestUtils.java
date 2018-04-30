package com.atd.utils;

import com.atd.config.AirplaneData;
import com.atd.simulation.RunwayState;
import com.atd.simulation.data.LandingRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 */
public class RequestUtils {

    public static RunwayState.RunwayType defineMatchRunway(Boolean[] runwayStates, AirplaneData.AirplaneType airplaneType) {
        if (airplaneType == AirplaneData.AirplaneType.LARGE) {
            return runwayStates[RunwayState.RunwayType.LONG.getIndex()] ? RunwayState.RunwayType.LONG : null;
        }
        if(runwayStates[RunwayState.RunwayType.SHORT.getIndex()]) {
            return RunwayState.RunwayType.SHORT;
        }
        if(runwayStates[RunwayState.RunwayType.LONG.getIndex()]) {
            return RunwayState.RunwayType.LONG;
        }
        return null;
    }

    public static boolean anyRunwayAvailable(Boolean[] runwayStates) {
        return runwayStates[0] || runwayStates[1];
    }

    public static Map<RunwayState.RunwayType, LandingRequest> getSynchronisedProposals(
            Map<RunwayState.RunwayType, LandingRequest> proposals,
            Map<RunwayState.RunwayType, LandingRequest> otherProposals) {
        Map<RunwayState.RunwayType, LandingRequest> synchronisedProposals = new HashMap<>();
        synchroniseProposal(RunwayState.RunwayType.SHORT, proposals, otherProposals, synchronisedProposals);
        synchroniseProposal(RunwayState.RunwayType.LONG, proposals, otherProposals, synchronisedProposals);
        return synchronisedProposals;
    }

    private static void synchroniseProposal(RunwayState.RunwayType runwayType,
                                            Map<RunwayState.RunwayType, LandingRequest> proposals,
                                            Map<RunwayState.RunwayType, LandingRequest> otherProposals,
                                            Map<RunwayState.RunwayType, LandingRequest> synchronisedProposals) {
        if (proposals.containsKey(runwayType)) {
            LandingRequest request = proposals.get(runwayType);
            LandingRequest otherRequest = otherProposals.get(runwayType);
            LandingRequest selectedRequest = otherRequest == null ? request :
                    LandingRequest.selectMorePreferable(request, otherRequest);
            if (Objects.equals(selectedRequest, request)) {
                synchronisedProposals.put(runwayType, request);
            }
        }
    }
}
