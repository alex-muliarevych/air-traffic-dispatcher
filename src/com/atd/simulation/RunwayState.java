package com.atd.simulation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Tracking of runway state, can't be monitoring from airplane or traffic controller.
 * Used to track for critical situations, like crash of airplanes, when 2 of them are landing at same time on a same
 * runway.
 */
@Slf4j
public class RunwayState {

    /**
     * Initialized with null values, null means no plane is landing currently, if some airplane start to land
     * related runaway.
     */
    private String[] airplaneNamesPerRunaways = {null, null};

    public synchronized void landOnRunaway(String airplaneName, RunwayType type) {
        String currentAirplane = airplaneNamesPerRunaways[type.index];
        if (currentAirplane != null) {
            log.error("Airplanes crashed: airplane '{}' was on airplane while airplane '' tried to land",
                    currentAirplane, airplaneName);
            throw new RuntimeException("Critical error appears during simulation.");
        }
        airplaneNamesPerRunaways[type.index] = airplaneName;
    }

    public synchronized void finishLanding(RunwayType type) {
        airplaneNamesPerRunaways[type.index] = null;
    }

    @Getter
    public enum RunwayType {
        SHORT(0),
        LONG(1);

        private int index;

        RunwayType(int index) {
            this.index = index;
        }
    }
}
