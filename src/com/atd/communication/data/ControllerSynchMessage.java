package com.atd.communication.data;

import com.atd.simulation.RunwayState;
import com.atd.simulation.data.LandingRequest;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

/**
 * Message used for synchronisation type communication between 2 traffic controllers.
 */
@Getter
@Builder
public class ControllerSynchMessage implements CommunicationMessage {
    /**
     * Container of {@link LandingRequest}s mapped to each runway, defined for processing by traffic controller.
     */
    private Map<RunwayState.RunwayType, LandingRequest> requestForProcessing;
    private Message message;

    @Override
    public String toString() {
        return message.toString() + ", PROPOSALS: ["
                + Optional.ofNullable(requestForProcessing.get(RunwayState.RunwayType.SHORT))
                .map(LandingRequest::toString).orElse("NULL") + ","
                + Optional.ofNullable(requestForProcessing.get(RunwayState.RunwayType.LONG))
                .map(LandingRequest::toString).orElse("NULL")
                + "]";
    }

    @Override
    public Object getData() {
        return requestForProcessing;
    }
}
