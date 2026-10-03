package com.radar.simulator.model;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Vector3D;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for ExperimentRunner.
 * Tests the full simulation pipeline: configuration → trajectory →
 * measurement → localization → evaluation.
 */
public class ExperimentRunnerTest {

    @Test
    public void testEndToEndSimulation() {
        Scenario scenario = createValidScenario();
        ExperimentRunner runner = new ExperimentRunner(
            scenario,
            new BaselineMeasurementModel(),
            new BaselineLocalizationModel()
        );

        List<ExperimentRecord> records = runner.run(5.0, 1.0);

        // Should have 6 records (t=0,1,2,3,4,5)
        assertEquals(6, records.size());
    }

    @Test
    public void testRecordsContainGroundTruth() {
        Scenario scenario = createValidScenario();
        ExperimentRunner runner = new ExperimentRunner(
            scenario,
            new BaselineMeasurementModel(),
            new BaselineLocalizationModel()
        );

        List<ExperimentRecord> records = runner.run(2.0, 1.0);

        // At t=0, ground truth should be initial position
        ExperimentRecord first = records.get(0);
        assertEquals(0, first.stepIndex());
        assertEquals(0.0, first.time(), 1e-9);
        assertEquals(1000.0, first.groundTruthPosition().x, 1e-9);
        assertEquals(2000.0, first.groundTruthPosition().y, 1e-9);
        assertEquals(100.0, first.groundTruthPosition().z, 1e-9);

        // At t=1, position = (1050, 2000, 100) with velocity (50,0,0)
        ExperimentRecord second = records.get(1);
        assertEquals(1050.0, second.groundTruthPosition().x, 1e-9);
    }

    @Test
    public void testRecordsContainModelIdentity() {
        Scenario scenario = createValidScenario();
        ExperimentRunner runner = new ExperimentRunner(
            scenario,
            new BaselineMeasurementModel(),
            new BaselineLocalizationModel()
        );

        List<ExperimentRecord> records = runner.run(1.0, 1.0);

        ExperimentRecord record = records.get(0);
        assertEquals("BaselineMeasurement", record.measurementModelDescriptor().name());
        assertEquals("BaselineLocalization", record.localizationModelDescriptor().name());
    }

    @Test
    public void testRecordsContainPositionError() {
        Scenario scenario = createValidScenario();
        ExperimentRunner runner = new ExperimentRunner(
            scenario,
            new BaselineMeasurementModel(),
            new BaselineLocalizationModel()
        );

        List<ExperimentRecord> records = runner.run(1.0, 1.0);

        for (ExperimentRecord record : records) {
            if (record.estimationResult().status() == Status.SUCCESS) {
                assertTrue("Error should be finite", Double.isFinite(record.positionError()));
                assertTrue("Error should be non-negative", record.positionError() >= 0);
            }
        }
    }

    @Test
    public void testRunAndEvaluate() {
        Scenario scenario = createValidScenario();
        ExperimentRunner runner = new ExperimentRunner(
            scenario,
            new BaselineMeasurementModel(),
            new BaselineLocalizationModel()
        );

        EvaluationResult evaluation = runner.runAndEvaluate(5.0, 1.0);

        assertEquals(6, evaluation.totalSteps());
        assertTrue(evaluation.successfulSteps() > 0);
    }

    @Test
    public void testWithUnderConstrainedGeometry() {
        // Only 2 receivers → should report UNDER_CONSTRAINED
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 0, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 0, 0), 2.4e9, 10.0));

        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(
            new Vector3D(0, 1000, 100), new Vector3D(50, 0, 0));
        Scenario scenario = new Scenario(tx, receivers, trajectory);

        ExperimentRunner runner = new ExperimentRunner(
            scenario,
            new BaselineMeasurementModel(),
            new BaselineLocalizationModel()
        );

        List<ExperimentRecord> records = runner.run(2.0, 1.0);

        // All records should have UNDER_CONSTRAINED status
        for (ExperimentRecord record : records) {
            assertEquals(Status.UNDER_CONSTRAINED, record.estimationResult().status());
            assertTrue(Double.isNaN(record.positionError()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDuration() {
        Scenario scenario = createValidScenario();
        ExperimentRunner runner = new ExperimentRunner(
            scenario,
            new BaselineMeasurementModel(),
            new BaselineLocalizationModel()
        );

        runner.run(-1.0, 1.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTimeStep() {
        Scenario scenario = createValidScenario();
        ExperimentRunner runner = new ExperimentRunner(
            scenario,
            new BaselineMeasurementModel(),
            new BaselineLocalizationModel()
        );

        runner.run(5.0, 0.0);
    }

    // --- Helper ---

    private Scenario createValidScenario() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));

        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(
            new Vector3D(1000, 2000, 100), new Vector3D(50, 0, 0));

        return new Scenario(tx, receivers, trajectory);
    }
}
