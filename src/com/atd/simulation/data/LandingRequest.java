package com.atd.simulation.data;

import com.atd.config.AirplaneData;
import com.sun.istack.internal.Nullable;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;

/**
 * Container of data stored based on request for landing from Airplane.
 */
@Builder
@Getter
public class LandingRequest {
    private final String airplaneName;
    private final AirplaneData.AirplaneType airplaneType;
    private final AirplaneData.LandingType landingType;
    /**
     * {@link Date} of request storage, used for ordering and decentralised decision on which request to execute first,
     * if other criteria are the same.
     */
    private final Date date;

    /**
     * Returns {@link LandingRequest} which is more preferable between two passed-in for comparison.
     */
    @Nullable
    public static LandingRequest selectMorePreferable(LandingRequest r1, LandingRequest r2) {
        // Nothing to compare, such state of request means it is in ALREADY_IN_PROGRESS state and runway is occupied.
        if (r1.getAirplaneName().isEmpty() && r1.getDate() == null ||
                r2.getAirplaneName().isEmpty() && r2.getDate() == null) {
            return null;
        }
        // Emergency type request is more preferable.
        if (r1.getLandingType() != r2.getLandingType()) {
            return r1.getLandingType() == AirplaneData.LandingType.EMERGENCY ? r1 : r2;
        }
        // If all previous criteria are same, compare by date of request's storage.
        return r1.getDate().compareTo(r2.getDate()) > 0 ? r2 : r1;
    }

    // Used to track airplanes that were ordered to land. It means runway is occupied currently.
    public static LandingRequest ALREADY_IN_PROGRESS = LandingRequest.builder().airplaneName("").build();

    @Override
    public String toString() {
        return airplaneName.isEmpty() ? "ALREADY_IN_PROGRESS" : "[" + airplaneName + ">"
                + airplaneType.toString() +"," + landingType.toString() + "]";
    }
}
