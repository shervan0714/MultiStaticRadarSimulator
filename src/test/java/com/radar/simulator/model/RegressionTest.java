package com.radar.simulator.model;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Vector3D;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Regression and reference-case tests.
 *
 * These tests validate the simulator across different receiver counts,
 * receiver orientations, and spatial geometries. Both well-conditioned
 * and difficult geometries are tested to identify ambiguous or
 * numerically unstable configurations.
 *
 * Validated scenarios are retained as regression cases.
 */
public class RegressionTest {

    /** Noiseless synthetic data must reproduce ground truth to within 1 mm. */
    private static final double NOISELESS_TOLERANCE_M = 1e-3;

    // ===== Reference Case 1: Standard triangle, 3 receivers =====

    @Test
    public void testReferenceCase_3Receivers_Triangle() {
        Scenario scenario = createTriangleScenario(3);
        ExperimentRunner runner = new ExperimentRunner(scenario,
            new BaselineMeasurementModel(), new BaselineLocalizationModel());

        List<ExperimentRecord> records = runner.run(5.0, 1.0);

        // All steps should succeed with 3 well-placed receivers
        for (ExperimentRecord record : records) {
            assertEquals("3-receiver triangle should succeed",
                Status.SUCCESS, record.estimationResult().status());
            assertEquals("Noiseless estimate should match ground truth",
                0.0, record.positionError(), NOISELESS_TOLERANCE_M);
        }
    }

    // ===== Reference Case 2: Square layout, 4 receivers =====

    @Test
    public void testReferenceCase_4Receivers_Square() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(-5000, -5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX4", new Vector3D(5000, -5000, 0), 2.4e9, 10.0));

        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(
            new Vector3D(1000, 2000, 100), new Vector3D(50, 0, 0));
        Scenario scenario = new Scenario(tx, receivers, trajectory);

        ExperimentRunner runner = new ExperimentRunner(scenario,
            new BaselineMeasurementModel(), new BaselineLocalizationModel());
        List<ExperimentRecord> records = runner.run(5.0, 1.0);

        for (ExperimentRecord record : records) {
            assertEquals(Status.SUCCESS, record.estimationResult().status());
            assertEquals(0.0, record.positionError(), NOISELESS_TOLERANCE_M);
        }
    }

    // ===== Reference Case 3: 5 receivers with altitude diversity =====

    @Test
    public void testReferenceCase_5Receivers_WithAltitude() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX4", new Vector3D(-5000, -5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX5", new Vector3D(0, 0, 5000), 2.4e9, 10.0));

        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(
            new Vector3D(1000, 2000, 100), new Vector3D(50, 0, 0));
        Scenario scenario = new Scenario(tx, receivers, trajectory);

        ExperimentRunner runner = new ExperimentRunner(scenario,
            new BaselineMeasurementModel(), new BaselineLocalizationModel());
        EvaluationResult evaluation = runner.runAndEvaluate(5.0, 1.0);

        assertEquals(6, evaluation.totalSteps());
        assertEquals(6, evaluation.successfulSteps());
        assertEquals(0, evaluation.failedSteps());
        assertEquals(0.0, evaluation.maxError(), NOISELESS_TOLERANCE_M);
    }

    // ===== Difficult Geometry: 2 receivers (under-constrained) =====

    @Test
    public void testDifficultGeometry_2Receivers() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 0, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 0, 0), 2.4e9, 10.0));

        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(
            new Vector3D(0, 1000, 100), new Vector3D(50, 0, 0));
        Scenario scenario = new Scenario(tx, receivers, trajectory);

        ExperimentRunner runner = new ExperimentRunner(scenario,
            new BaselineMeasurementModel(), new BaselineLocalizationModel());
        List<ExperimentRecord> records = runner.run(3.0, 1.0);

        // All records should report under-constrained
        for (ExperimentRecord record : records) {
            assertEquals("2 receivers should be under-constrained",
                Status.UNDER_CONSTRAINED, record.estimationResult().status());
        }
    }

    // ===== Difficult Geometry: collinear receivers =====

    @Test
    public void testDifficultGeometry_CollinearReceivers() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(1000, 0, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(2000, 0, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(3000, 0, 0), 2.4e9, 10.0));

        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(
            new Vector3D(0, 1000, 100), new Vector3D(50, 0, 0));
        Scenario scenario = new Scenario(tx, receivers, trajectory);

        ExperimentRunner runner = new ExperimentRunner(scenario,
            new BaselineMeasurementModel(), new BaselineLocalizationModel());
        List<ExperimentRecord> records = runner.run(3.0, 1.0);

        // Collinear receivers should be flagged
        for (ExperimentRecord record : records) {
            assertNotEquals("Collinear receivers should not succeed silently",
                Status.SUCCESS, record.estimationResult().status());
        }
    }

    // ===== Reference Case: wide baseline =====

    @Test
    public void testReferenceCase_WideBaseline() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(50000, 50000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-50000, 50000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -50000, 0), 2.4e9, 10.0));

        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(
            new Vector3D(1000, 2000, 100), new Vector3D(50, 0, 0));
        Scenario scenario = new Scenario(tx, receivers, trajectory);

        ExperimentRunner runner = new ExperimentRunner(scenario,
            new BaselineMeasurementModel(), new BaselineLocalizationModel());
        EvaluationResult evaluation = runner.runAndEvaluate(5.0, 1.0);

        assertEquals(6, evaluation.successfulSteps());
        assertEquals(0.0, evaluation.maxError(), NOISELESS_TOLERANCE_M);
    }

    // ===== Reference Case: close-range target =====

    @Test
    public void testReferenceCase_CloseRangeTarget() {
        // Target very close to receivers (inside the triangle)
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(100, 100, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-100, 100, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -100, 0), 2.4e9, 10.0));

        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(
            new Vector3D(0, 0, 50), new Vector3D(1, 0, 0));
        Scenario scenario = new Scenario(tx, receivers, trajectory);

        ExperimentRunner runner = new ExperimentRunner(scenario,
            new BaselineMeasurementModel(), new BaselineLocalizationModel());
        EvaluationResult evaluation = runner.runAndEvaluate(5.0, 1.0);

        assertEquals(6, evaluation.successfulSteps());
        assertEquals(0.0, evaluation.maxError(), NOISELESS_TOLERANCE_M);
    }

    // ===== Reference Case: GUI default layout (TX and RX at different heights) =====

    @Test
    public void testReferenceCase_ElevatedTransmitter() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 1000), 10e9, 1.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(10000, 0, 500), 10e9, 0));
        receivers.add(new Receiver("RX2", new Vector3D(-10000, 0, 500), 10e9, 0));
        receivers.add(new Receiver("RX3", new Vector3D(0, 10000, 500), 10e9, 0));

        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(
            new Vector3D(5000, 5000, 2000), new Vector3D(50, 0, 0));
        Scenario scenario = new Scenario(tx, receivers, trajectory);

        ExperimentRunner runner = new ExperimentRunner(scenario,
            new BaselineMeasurementModel(), new BaselineLocalizationModel());
        EvaluationResult evaluation = runner.runAndEvaluate(10.0, 1.0);

        assertEquals(11, evaluation.successfulSteps());
        assertEquals(0.0, evaluation.maxError(), NOISELESS_TOLERANCE_M);
    }

    // ===== Regression: model identity is recorded =====

    @Test
    public void testModelIdentityRecorded() {
        Scenario scenario = createTriangleScenario(3);
        BaselineMeasurementModel measModel = new BaselineMeasurementModel();
        BaselineLocalizationModel locModel = new BaselineLocalizationModel();

        ExperimentRunner runner = new ExperimentRunner(scenario, measModel, locModel);
        List<ExperimentRecord> records = runner.run(2.0, 1.0);

        for (ExperimentRecord record : records) {
            assertEquals("BaselineMeasurement", record.measurementModelDescriptor().name());
            assertEquals("1.0", record.measurementModelDescriptor().version());
            assertEquals("BaselineLocalization", record.localizationModelDescriptor().name());
            assertEquals("2.0", record.localizationModelDescriptor().version());
        }
    }

    // ===== Regression: evaluation metrics are consistent =====

    @Test
    public void testEvaluationMetricsConsistency() {
        Scenario scenario = createTriangleScenario(3);
        ExperimentRunner runner = new ExperimentRunner(scenario,
            new BaselineMeasurementModel(), new BaselineLocalizationModel());
        EvaluationResult evaluation = runner.runAndEvaluate(5.0, 1.0);

        // RMS should be >= mean (by Jensen's inequality for convex sqrt)
        assertTrue("RMS >= mean error",
            evaluation.rmsError() >= evaluation.meanError() - 1e-9);

        // Max should be >= mean
        assertTrue("Max >= mean error",
            evaluation.maxError() >= evaluation.meanError() - 1e-9);

        // Total = successful + failed
        assertEquals(evaluation.totalSteps(),
            evaluation.successfulSteps() + evaluation.failedSteps());
    }

    // ===== Helper =====

    private Scenario createTriangleScenario(int receiverCount) {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));

        if (receiverCount >= 4) {
            receivers.add(new Receiver("RX4", new Vector3D(-5000, -5000, 0), 2.4e9, 10.0));
        }

        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(
            new Vector3D(1000, 2000, 100), new Vector3D(50, 0, 0));

        return new Scenario(tx, receivers, trajectory);
    }
}
