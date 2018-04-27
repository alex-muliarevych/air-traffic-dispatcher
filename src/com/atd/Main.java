package com.atd;

import com.atd.simulation.TrafficSimulationExecutor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static void main(String[] args) throws InterruptedException {
        log.info("Simulation started.");
        TrafficSimulationExecutor simulationExecutor = new TrafficSimulationExecutor();
        // TODO: currently hardcode config.txt, could be passed as argument.
        simulationExecutor.simulate("config.txt");
        log.info("Simulation finished.");
    }
}
