package com.radar.simulator.model;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Vector3D;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for BaselineLocalizationModel.
 * Verifies localization estimates against known synthetic cases.
 */
public class BaselineLocalizationModelTest {

    @Test
    public void testEstimateWithValidMeasurements() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));

        // Generate measurements from a known position
        BaselineMeasurementModel measModel = new BaselineMeasurementModel();
        Measurement measurement = measModel.generate(tx, receivers, new Vector3D(1000, 2000, 100));

        BaselineLocalizationModel locModel = new BaselineLocalizationModel();
        EstimationResult result = locModel.estimate(tx, receivers, measurement);

        assertEquals(Status.SUCCESS, result.status());
        assertEquals(0.0, result.position().distance(new Vector3D(1000, 2000, 100)), 1e-3);
    }

    @Test
    public void testNoiselessEstimateMatchesGroundTruthAcrossPositions() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = squareWithMast();
        BaselineMeasurementModel measModel = new BaselineMeasurementModel();
        BaselineLocalizationModel locModel = new BaselineLocalizationModel();

        Vector3D[] targets = {
            new Vector3D(1000, 2000, 100),
            new Vector3D(-3000, 1500, 800),
            new Vector3D(4000, -4000, 2500),
            new Vector3D(200, -100, 50)
        };
        for (Vector3D target : targets) {
            EstimationResult result = locModel.estimate(tx, receivers,
                measModel.generate(tx, receivers, target));
            assertEquals("Status for " + target, Status.SUCCESS, result.status());
            assertEquals("Error for " + target, 0.0, result.position().distance(target), 1e-3);
            assertEquals(0.0, result.residual(), 1e-6);
            assertTrue(result.conditionNumber() >= 1.0);
        }
    }

    @Test
    public void testCoplanarSensorsReturnSolutionAboveSensorPlane() {
        // TX and RX all at z=0: the target and its mirror at -z give identical ranges
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));

        Vector3D target = new Vector3D(-2000, 1000, 600);
        EstimationResult result = new BaselineLocalizationModel().estimate(tx, receivers,
            new BaselineMeasurementModel().generate(tx, receivers, target));

        assertEquals(Status.SUCCESS, result.status());
        assertEquals(600.0, result.position().z, 1e-3);
    }

    @Test
    public void testTargetInSensorPlaneReportsLargeConditionNumber() {
        // With every sensor and the target at z=0, the ranges are nearly
        // insensitive to Z: noiseless data still localizes, but the condition
        // number warns that any range noise would be hugely amplified in Z.
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));
        BaselineMeasurementModel measModel = new BaselineMeasurementModel();
        BaselineLocalizationModel locModel = new BaselineLocalizationModel();

        Vector3D inPlane = new Vector3D(1000, 2000, 0);
        EstimationResult flat = locModel.estimate(tx, receivers,
            measModel.generate(tx, receivers, inPlane));
        EstimationResult elevated = locModel.estimate(tx, receivers,
            measModel.generate(tx, receivers, new Vector3D(1000, 2000, 100)));

        assertEquals(0.0, flat.position().distance(inPlane), 1e-3);
        assertTrue("In-plane target should be ill-conditioned", flat.conditionNumber() > 1e5);
        assertTrue("Elevated target should be well-conditioned", elevated.conditionNumber() < 1e2);
    }

    @Test
    public void testMissingRangesAreUnderConstrained() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = squareWithMast();

        // Only power is reported, no bistatic ranges
        List<ReceiverMeasurement> perRx = new ArrayList<>();
        for (int i = 0; i < receivers.size(); i++) {
            Map<Quantity, Double> values = new HashMap<>();
            values.put(Quantity.RECEIVED_POWER, 1e-9);
            perRx.add(new ReceiverMeasurement(i, values));
        }

        EstimationResult result = new BaselineLocalizationModel().estimate(
            tx, receivers, new Measurement(perRx, 0.0));

        assertEquals(Status.UNDER_CONSTRAINED, result.status());
    }

    private List<Receiver> squareWithMast() {
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(-5000, -5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX4", new Vector3D(5000, -5000, 300), 2.4e9, 10.0));
        return receivers;
    }

    @Test
    public void testEstimateWithTooFewReceivers() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));

        // Create minimal measurement with 2 receivers
        List<ReceiverMeasurement> perRx = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Map<Quantity, Double> values = new HashMap<>();
            values.put(Quantity.RECEIVED_POWER, 0.001);
            values.put(Quantity.BISTATIC_RANGE, 10000.0);
            perRx.add(new ReceiverMeasurement(i, values));
        }
        Measurement measurement = new Measurement(perRx, 0.0);

        BaselineLocalizationModel locModel = new BaselineLocalizationModel();
        EstimationResult result = locModel.estimate(tx, receivers, measurement);

        assertEquals(Status.UNDER_CONSTRAINED, result.status());
    }

    @Test
    public void testRangeShorterThanBaselineIsInvalid() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));

        // RX1 is 7071 m from TX, so a 5000 m bistatic range is physically impossible
        List<ReceiverMeasurement> perRx = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<Quantity, Double> values = new HashMap<>();
            values.put(Quantity.BISTATIC_RANGE, 5000.0);
            perRx.add(new ReceiverMeasurement(i, values));
        }
        Measurement measurement = new Measurement(perRx, 0.0);

        BaselineLocalizationModel locModel = new BaselineLocalizationModel();
        EstimationResult result = locModel.estimate(tx, receivers, measurement);

        assertEquals(Status.INVALID_INPUT, result.status());
    }

    @Test
    public void testModelDescriptor() {
        BaselineLocalizationModel model = new BaselineLocalizationModel();
        ModelDescriptor descriptor = model.getDescriptor();

        assertEquals("BaselineLocalization", descriptor.name());
        assertEquals("2.0", descriptor.version());
    }

    @Test
    public void testResidualIsComputed() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));

        BaselineMeasurementModel measModel = new BaselineMeasurementModel();
        Measurement measurement = measModel.generate(tx, receivers, new Vector3D(1000, 2000, 100));

        BaselineLocalizationModel locModel = new BaselineLocalizationModel();
        EstimationResult result = locModel.estimate(tx, receivers, measurement);

        // Residual should be a finite non-negative number
        assertTrue(Double.isFinite(result.residual()));
        assertTrue(result.residual() >= 0);
    }
}
