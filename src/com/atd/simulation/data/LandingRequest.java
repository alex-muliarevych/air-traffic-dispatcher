package com.atd.simulation.data;

import com.atd.config.AirplaneData;
import com.sun.istack.internal.Nullable;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;

/**
 */
@Builder
@Getter
public class LandingRequest {
    private final String airplaneName;
    private final AirplaneData.AirplaneType airplaneType;
    private final AirplaneData.LandingType landingType;
    private final Date date;

    @Nullable
    public static LandingRequest selectMorePreferable(LandingRequest r1, LandingRequest r2) {
        // Nothing to compare, such state of request means it is in progress already and runway is occupied.
        if(r1.getAirplaneName().isEmpty() && r1.getDate() == null ||
                r2.getAirplaneName().isEmpty() && r2.getDate() == null) {
            return null;
        }
        if (r1.getLandingType() != r2.getLandingType()) {
            return r1.getLandingType() == AirplaneData.LandingType.EMERGENCY ? r1 : r2;
        }
        return r1.getDate().compareTo(r2.getDate()) > 0 ? r2 : r1;
    }

    // Used to track airplanes that were ordered to land.
    public static LandingRequest ALREADY_IN_PROGRESS = LandingRequest.builder().airplaneName("").build();

    @Override
    public String toString() {
        return airplaneName.isEmpty() ? "ALREADY_IN_PROGRESS" : "[" + airplaneName + ">"
                + airplaneType.toString() +"," + landingType.toString() + "]";
    }
}
