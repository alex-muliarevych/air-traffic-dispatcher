package com.atd.communication;

import com.atd.simulation.Airplane;
import com.atd.simulation.RunwayState;
import com.atd.simulation.TrafficController;

/**
 * Interface of Communicator for {@link Airplane}s.
 */
public interface AirplaneCommunicator {

    /**
     * Sends request for landing to dispatcher in {@link Communicator}, returns id of traffic controller, to which
     * airplane established connection and will be guide for landing.
     */
    int requestForLanding(String airplaneName) throws InterruptedException;

    /**
     * Sends message to Traffic controller with {@code controllerId} with provement of successfully finished landing
     * on a runway of passed-in {@link RunwayState.RunwayType}.
     */
    void confirmOfSuccessLanding(int controllerId, String airplaneName, RunwayState.RunwayType runwayType) throws InterruptedException;

    /**
     * Register - subscribe passed-in {@link Airplane} for communication using {@link Communicator}.
     */
    void registerForCommunication(Airplane airplane);
}
