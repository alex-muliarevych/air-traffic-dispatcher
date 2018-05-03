package com.atd.simulation.data;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Container of report data for airplane landing process.
 * Contains information about time spend for landing per each airplane and status of landing.
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode(exclude = {"executionTime", "chosenRunwayIndex"})
public class AirplaneLandingReport {
    private final boolean landed;
    private final String airplaneName;
    private Long startOffsetInSecs;
    // Dynamic property, could be changed depends on processing order and access time to traffic controllers.
    private Long executionTime;
    // Dynamic property, could be changed depends on processing order and access time to traffic controllers.
    private Integer chosenRunwayIndex;
}
