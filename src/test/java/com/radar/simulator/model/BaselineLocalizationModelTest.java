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
        assertNotNull(result.position());
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
    public void testEstimateReturnsFailureWhenNoPower() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));

        // Create measurement with zero power
        List<ReceiverMeasurement> perRx = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<Quantity, Double> values = new HashMap<>();
            values.put(Quantity.RECEIVED_POWER, 0.0);
            values.put(Quantity.BISTATIC_RANGE, 10000.0);
            perRx.add(new ReceiverMeasurement(i, values));
        }
        Measurement measurement = new Measurement(perRx, 0.0);

        BaselineLocalizationModel locModel = new BaselineLocalizationModel();
        EstimationResult result = locModel.estimate(tx, receivers, measurement);

        assertEquals(Status.NUMERICALLY_UNSTABLE, result.status());
    }

    @Test
    public void testModelDescriptor() {
        BaselineLocalizationModel model = new BaselineLocalizationModel();
        ModelDescriptor descriptor = model.getDescriptor();

        assertEquals("BaselineLocalization", descriptor.name());
        assertEquals("1.0", descriptor.version());
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
