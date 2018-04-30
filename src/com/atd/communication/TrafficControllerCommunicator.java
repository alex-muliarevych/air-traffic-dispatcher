package com.atd.communication;

import com.atd.simulation.Airplane;
import com.atd.simulation.data.LandingRequest;
import com.atd.communication.data.Message;
import com.atd.simulation.RunwayState;
import com.atd.simulation.TrafficController;

import java.util.Map;

/**
 * Interface of Communicator for {@link TrafficController}s.
 */
public interface TrafficControllerCommunicator {

    void sendResponseToAirplane(Integer controllerId, String airplaneName,
                                Message.MessageType type, String text) throws InterruptedException;

    void registerForCommunication(Integer controllerId, TrafficController controller);

    void synchroniseDecisions(int controllerId, Map<RunwayState.RunwayType, LandingRequest> preparedDecisions) throws InterruptedException;
}
