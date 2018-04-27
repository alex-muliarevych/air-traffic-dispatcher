package com.atd.config;

import lombok.Builder;
import lombok.Getter;

/**
 * Data about airplane.
 */
@Builder
@Getter
public class AirplaneData {
    private final String airplaneName;
    private final AirplaneType airplaneType;
    private final LandingType landingType;
    // Offset from start of program till request for landing.
    private final Integer noOfSeconds;

    public enum AirplaneType {
        LARGE,
        REGULAR
    }

    public enum LandingType {
        EMERGENCY,
        NORMAL
    }
}
