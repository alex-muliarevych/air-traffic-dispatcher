package com.atd.simulation;

import com.atd.communication.Communicator;
import com.atd.config.AirplaneData;
import com.atd.config.ConfigurationReader;
import com.atd.simulation.data.AirplaneLandingReport;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class TrafficSimulationExecutor {

    public List<AirplaneLandingReport> simulate(String path) throws InterruptedException {
        URL resource = getClass().getClassLoader().getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException(String.format("No config file found by path '%s'", path));
        }
        List<AirplaneData> airplaneData = ConfigurationReader.read(Paths.get(resource.getPath()));

        ExecutorService executorService = Executors.newFixedThreadPool(airplaneData.size() + 2);


        RunwayState runwayState = new RunwayState();
        Communicator communicator = new Communicator();

        // Initialize tasks.
        List<TrafficController> trafficControllers =
                IntStream.of(0, 1)
                        .mapToObj(id -> new TrafficController(id, communicator))
                        .collect(Collectors.toList());
        List<Airplane> airplanes =
                airplaneData.stream().map(data -> new Airplane(data, communicator, runwayState))
                        .collect(Collectors.toList());

        // Run execution of tasks.
        List<Callable<Void>> tasks =
                ImmutableList.<Callable<Void>>builder().addAll(trafficControllers).addAll(airplanes).build();
        executorService.invokeAll(tasks);
        return airplanes.stream()
                        .map(e -> AirplaneLandingReport.builder()
                                                       .airplaneName(e.getData().getAirplaneName())
                                                       .chosenRunwayIndex(e.getChosenRunwayIndex())
                                                       .landed(e.isLanded())
                                                       .executionTime(e.getExecutionTime())
                                                       .startOffsetInSecs(Long.valueOf(e.getData().getNoOfSeconds()))
                                                       .build())
                        .collect(Collectors.toList());
    }
}
