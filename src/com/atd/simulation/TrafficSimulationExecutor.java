package com.atd.simulation;

import com.atd.config.AirplaneData;
import com.atd.config.ConfigurationReader;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class TrafficSimulationExecutor {

    public void simulate(String path) throws InterruptedException {
        URL resource = getClass().getClassLoader().getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException(String.format("No config file found by path '%s'", path));
        }
        List<AirplaneData> airplaneData = ConfigurationReader.read(Paths.get(resource.getPath()));

        ExecutorService executorService = Executors.newFixedThreadPool(airplaneData.size() + 2);

        // Run execution of tasks.
        executorService.invokeAll(
                Stream.concat(airplaneData.stream().map(Airplane::new),
                        IntStream.of(0, 1).mapToObj(TrafficController::new))
                        .collect(Collectors.toList()));
    }
}
