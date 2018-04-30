package com.atd.communication;

import com.atd.simulation.Airplane;
import com.atd.simulation.RunwayState;

/**
 */
public interface AirplaneCommunicator {

    Integer requestForLanding(String airplaneName) throws InterruptedException;

    void confirmOfSuccessLanding(Integer controllerId, String airplaneName, RunwayState.RunwayType runwayType) throws InterruptedException;

    void registerForCommunication(String airplaneName, Airplane airplane);
}