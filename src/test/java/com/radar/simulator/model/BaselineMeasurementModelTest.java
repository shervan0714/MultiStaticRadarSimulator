package com.radar.simulator.model;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Vector3D;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for BaselineMeasurementModel.
 * Uses simple geometries with hand-computable expected measurements.
 */
public class BaselineMeasurementModelTest {

    @Test
    public void testGenerateProducesCorrectReceiverCount() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(1000, 0, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(0, 1000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, 0, 1000), 2.4e9, 10.0));

        BaselineMeasurementModel model = new BaselineMeasurementModel();
        Vector3D targetPos = new Vector3D(500, 500, 500);

        Measurement measurement = model.generate(tx, receivers, targetPos);

        assertEquals(3, measurement.perReceiver().size());
    }

    @Test
    public void testBistaticRangeCalculation() {
        // Tx at origin, target at (3,4,0), Rx at (6,8,0)
        // dist(Tx->target) = 5, dist(target->Rx) = sqrt(9+16) = 5
        // bistatic range = 10
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(6, 8, 0), 2.4e9, 10.0));

        BaselineMeasurementModel model = new BaselineMeasurementModel();
        Measurement measurement = model.generate(tx, receivers, new Vector3D(3, 4, 0));

        ReceiverMeasurement rm = measurement.perReceiver().get(0);
        double bistaticRange = rm.values().get(Quantity.BISTATIC_RANGE);
        assertEquals(10.0, bistaticRange, 0.001);
    }

    @Test
    public void testTimeDelayCalculation() {
        // bistatic range / speed of light
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(3e8, 0, 0), 2.4e9, 10.0));

        BaselineMeasurementModel model = new BaselineMeasurementModel();
        // Target at origin → range = 0 + 3e8 = 3e8 → delay = 1s
        Measurement measurement = model.generate(tx, receivers, new Vector3D(0, 0, 0));

        ReceiverMeasurement rm = measurement.perReceiver().get(0);
        double timeDelay = rm.values().get(Quantity.TIME_DELAY);
        assertEquals(1.0, timeDelay, 0.001);
    }

    @Test
    public void testMeasurementContainsAllQuantities() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(1000, 0, 0), 2.4e9, 10.0));

        BaselineMeasurementModel model = new BaselineMeasurementModel();
        Measurement measurement = model.generate(tx, receivers, new Vector3D(500, 0, 0));

        ReceiverMeasurement rm = measurement.perReceiver().get(0);
        assertTrue(rm.values().containsKey(Quantity.BISTATIC_RANGE));
        assertTrue(rm.values().containsKey(Quantity.TIME_DELAY));
        assertTrue(rm.values().containsKey(Quantity.RECEIVED_POWER));
    }

    @Test
    public void testReceivedPowerPositive() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));

        BaselineMeasurementModel model = new BaselineMeasurementModel();
        Measurement measurement = model.generate(tx, receivers, new Vector3D(1000, 2000, 100));

        ReceiverMeasurement rm = measurement.perReceiver().get(0);
        double power = rm.values().get(Quantity.RECEIVED_POWER);
        assertTrue("Received power should be positive", power > 0);
    }

    @Test
    public void testModelDescriptor() {
        BaselineMeasurementModel model = new BaselineMeasurementModel();
        ModelDescriptor descriptor = model.getDescriptor();

        assertEquals("BaselineMeasurement", descriptor.name());
        assertEquals("1.0", descriptor.version());
    }
}
