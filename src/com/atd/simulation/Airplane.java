package com.atd.simulation;

import com.atd.config.AirplaneData;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * TODO:
 */
@Slf4j
@AllArgsConstructor
public class Airplane implements Callable<Void> {

    private AirplaneData data;

    @Override
    public Void call() throws Exception {
        log.debug("Thread for airplane '{}' started", data.getAirplaneName());
        Thread.sleep(data.getNoOfSeconds() * 1000);
        log.info("{} is going to land.", data.getAirplaneName());
        return null;
    }
}
