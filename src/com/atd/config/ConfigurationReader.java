package com.atd.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reader of configuration data from input text file.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigurationReader {

    /**
     * Pattern to detect configuration line, which determines data related to type of airplane, its name,
     * planning type and time of request for planning, passed from start of application.
     * Example of format:
     * <pre>
     *     Plane-3, Large, Normal, 4
     * </pre>
     */
    private static final Pattern CONFIG_LINE_DATA =
            Pattern.compile("(?<airplaneName>\\S+),\\s*(?<airplaneType>Regular|Large)," +
                    "\\s*(?<landingType>Normal|Emergency),\\s*(?<seconds>\\d+)");

    /**
     * Returns parsed list of {@link AirplaneData}s from file of passed-in {@link Path}.
     */
    public static List<AirplaneData> read(Path path) {
        try (InputStream inputStream = Files.newInputStream(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines()
                    .map(ConfigurationReader::processConfigLine)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Reading configuration file failed with exception.", ex.getCause());
        }
        return Collections.emptyList();
    }

    /**
     * Returns parsed Optional {@link AirplaneData} from passed-in configuration {@code line}.
     */
    private static Optional<AirplaneData> processConfigLine(String line) {
        Matcher m = CONFIG_LINE_DATA.matcher(line);
        if (!m.find()) {
            log.error("Corrupted line presents in config file: '{}'", line);
            return Optional.empty();
        }
        return Optional.of(
                AirplaneData.builder()
                        .airplaneName(m.group("airplaneName"))
                        .airplaneType(m.group("airplaneType").equals("Large") ?
                                AirplaneData.AirplaneType.LARGE :
                                AirplaneData.AirplaneType.REGULAR)
                        .landingType(m.group("landingType").equals("Emergency") ?
                                AirplaneData.LandingType.EMERGENCY :
                                AirplaneData.LandingType.NORMAL)
                        .noOfSeconds(Integer.valueOf(m.group("seconds")))
                        .build());
    }
}
