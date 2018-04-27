package com.atd.simulation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * TODO:
 */
@Slf4j
@AllArgsConstructor
public class TrafficController implements Callable<Void> {

    private Integer id;

    @Override
    public Void call() throws Exception {
        log.info("Traffic controller with id '{}' started", id);
        return null;
    }
}
