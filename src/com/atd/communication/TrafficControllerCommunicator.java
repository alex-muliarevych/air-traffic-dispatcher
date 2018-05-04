package com.atd.communication;

import com.atd.communication.data.Message;
import com.atd.simulation.RunwayState;
import com.atd.simulation.TrafficController;
import com.atd.simulation.data.LandingRequest;

import java.util.Map;

/**
 * Interface of Communicator for {@link TrafficController}s.
 */
public interface TrafficControllerCommunicator {

    /**
     * Register - subscribe passed-in {@link TrafficController} for communication using {@link Communicator}.
     */
    void registerForCommunication(TrafficController controller);

    /**
     * Sends message from traffic controller with {@code controllerId} to Airplane with name {@code airplaneName}
     * of passed-in type {@link Message.MessageType}, which contains {@code text} value.
     * Used for sending messages like 'wait around airport' or 'land on a defined runway'.
     */
    void sendResponseToAirplane(int controllerId, String airplaneName,
                                Message.MessageType type, String text) throws InterruptedException;

    /**
     * Send from Traffic controller with {@code controllerId} request to other traffic controller for data proposals
     * synchronisation, represented by {@code preparedDecisions}.
     */
    void synchroniseDecisions(int controllerId, Map<RunwayState.RunwayType, LandingRequest> preparedDecisions) throws InterruptedException;
}
