package com.radar.simulator.ui;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.model.BaselineLocalizationModel;
import com.radar.simulator.model.BaselineMeasurementModel;
import com.radar.simulator.model.EvaluationResult;
import com.radar.simulator.model.ExperimentRunner;
import com.radar.simulator.model.Scenario;
import com.radar.simulator.util.Vector3D;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Verifies that values entered in the GUI form are transferred correctly
 * into the simulation scenario (design doc test plan: Configuration / UI).
 */
public class ScenarioConfigTest {

    private static final double EPSILON = 1e-9;

    @Test
    public void testToScenarioTransfersEveryField() {
        ScenarioConfig config = new ScenarioConfig(
            new Vector3D(10, 20, 30), 3.0e9, 500.0,
            List.of(
                new ScenarioConfig.ReceiverConfig("A", 1000, 0, 5, 3),
                new ScenarioConfig.ReceiverConfig("B", 0, 1000, 6, 4),
                new ScenarioConfig.ReceiverConfig("C", -1000, -1000, 7, 5)
            ),
            new Vector3D(100, 200, 300), new Vector3D(1, 2, 3),
            20.0, 0.5
        );

        Scenario scenario = config.toScenario();

        assertEquals(new Vector3D(10, 20, 30), scenario.getTransmitter().getPosition());
        assertEquals(3.0e9, scenario.getTransmitter().getFrequency(), EPSILON);
        assertEquals(500.0, scenario.getTransmitter().getPower(), EPSILON);

        List<Receiver> receivers = scenario.getReceivers();
        assertEquals(3, receivers.size());
        assertEquals("B", receivers.get(1).getId());
        assertEquals(new Vector3D(0, 1000, 6), receivers.get(1).getPosition());
        assertEquals(4.0, receivers.get(1).getGain(), EPSILON);
        assertEquals(3.0e9, receivers.get(1).getFrequency(), EPSILON);

        // Trajectory: p(t) = p0 + v t
        assertEquals(new Vector3D(100, 200, 300), scenario.getTargetTrajectory().getPosition(0));
        assertEquals(new Vector3D(110, 220, 330), scenario.getTargetTrajectory().getPosition(10));
    }

    @Test
    public void testRoundTripThroughScenario() {
        ScenarioConfig original = ScenarioConfig.defaultConfig();
        ScenarioConfig copy = ScenarioConfig.fromScenario(
            original.toScenario(), original.duration(), original.timeStep());

        assertEquals(original, copy);
    }

    @Test
    public void testDefaultConfigRunsCleanly() {
        ScenarioConfig config = ScenarioConfig.defaultConfig();
        ExperimentRunner runner = new ExperimentRunner(config.toScenario(),
            new BaselineMeasurementModel(), new BaselineLocalizationModel());

        EvaluationResult evaluation = runner.runAndEvaluate(config.duration(), config.timeStep());

        assertEquals(0, evaluation.failedSteps());
        assertEquals(0.0, evaluation.maxError(), 1e-3);
    }

    @Test
    public void testRejectsDuplicateReceiverIds() {
        assertInvalid(withReceivers(List.of(
            new ScenarioConfig.ReceiverConfig("RX1", 1000, 0, 0, 0),
            new ScenarioConfig.ReceiverConfig("RX1", 0, 1000, 0, 0))), "Duplicate");
    }

    @Test
    public void testRejectsBlankReceiverId() {
        assertInvalid(withReceivers(List.of(
            new ScenarioConfig.ReceiverConfig("  ", 1000, 0, 0, 0))), "ID");
    }

    @Test
    public void testRejectsNonPositiveTiming() {
        ScenarioConfig base = ScenarioConfig.defaultConfig();
        assertInvalid(new ScenarioConfig(base.transmitterPosition(), base.frequencyHz(),
            base.transmitPowerW(), base.receivers(), base.initialPosition(), base.velocity(),
            0.0, 1.0), "Duration");
        assertInvalid(new ScenarioConfig(base.transmitterPosition(), base.frequencyHz(),
            base.transmitPowerW(), base.receivers(), base.initialPosition(), base.velocity(),
            10.0, 20.0), "Time step");
    }

    @Test
    public void testRejectsNonFiniteCoordinates() {
        assertInvalid(withReceivers(List.of(
            new ScenarioConfig.ReceiverConfig("RX1", Double.NaN, 0, 0, 0))), "RX1");
    }

    @Test
    public void testEmptyReceiverListIsLeftToGeometryValidation() {
        // The form allows zero receivers; GeometryValidator reports it explicitly
        Scenario scenario = withReceivers(new ArrayList<>()).toScenario();
        assertTrue(scenario.getReceivers().isEmpty());
    }

    private static ScenarioConfig withReceivers(List<ScenarioConfig.ReceiverConfig> receivers) {
        ScenarioConfig base = ScenarioConfig.defaultConfig();
        return new ScenarioConfig(base.transmitterPosition(), base.frequencyHz(),
            base.transmitPowerW(), receivers, base.initialPosition(), base.velocity(),
            base.duration(), base.timeStep());
    }

    private static void assertInvalid(ScenarioConfig config, String expectedInMessage) {
        try {
            config.toScenario();
            fail("Expected IllegalArgumentException mentioning " + expectedInMessage);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expectedInMessage));
        }
    }
}
