import com.atd.simulation.TrafficSimulationExecutor;
import com.atd.simulation.data.AirplaneLandingReport;
import com.google.common.collect.ImmutableList;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TrafficSimulationExecutorTest {

    private static final List<AirplaneLandingReport> LANDING_REPORTS_FOR_TEST_COFIG_1 =
            ImmutableList.of(
                    AirplaneLandingReport
                            .builder()
                            .landed(true)
                            .airplaneName("Plane-1")
                            .startOffsetInSecs(3L)
                            .chosenRunwayIndex(0)
                            .build(),
                    AirplaneLandingReport
                            .builder()
                            .landed(true)
                            .airplaneName("Plane-2")
                            .startOffsetInSecs(4L)
                            .chosenRunwayIndex(0)
                            .build(),
                    AirplaneLandingReport
                            .builder()
                            .landed(true)
                            .airplaneName("Plane-3")
                            .startOffsetInSecs(4L)
                            .chosenRunwayIndex(1)
                            .build(),
                    AirplaneLandingReport
                            .builder()
                            .landed(true)
                            .airplaneName("Plane-4")
                            .startOffsetInSecs(4L)
                            .chosenRunwayIndex(1)
                            .executionTime(7L)
                            .build(),
                    AirplaneLandingReport
                            .builder()
                            .landed(true)
                            .airplaneName("Plane-5")
                            .startOffsetInSecs(5L)
                            .chosenRunwayIndex(0)
                            .build());

    @DataProvider
    public static Object[][] multiSource() {
        return new Object[][] {
                {"test/test-config1.txt", LANDING_REPORTS_FOR_TEST_COFIG_1}
        };
    }

    @Test(dataProvider = "multiSource")
    public void simulate(String configPath, List<AirplaneLandingReport> expectedReports) throws Exception {
        TrafficSimulationExecutor simulationExecutor = new TrafficSimulationExecutor();
        List<AirplaneLandingReport> reports = simulationExecutor.simulate(configPath);
        assertEquals(reports, expectedReports);
        // If in expected reports is defined execution time, then evaluate it as a maximum limit.
        // If in expected reports is defined chosenRunwayIndex, then check it also.
        for (int index = 0; index < reports.size(); index++) {
            AirplaneLandingReport expectedReportData = expectedReports.get(index);
            AirplaneLandingReport landingReport = reports.get(index);
            if (expectedReportData.getChosenRunwayIndex() != null) {
                assertEquals(landingReport.getChosenRunwayIndex(), expectedReportData.getChosenRunwayIndex());
            }
            Long expectedTime = expectedReportData.getExecutionTime();
            if (expectedTime == null) {
                continue;
            }
            assertTrue(landingReport.getExecutionTime() <= expectedTime);
        }
    }
}
