package com.radar.simulator.measurement;

import com.radar.simulator.core.ModelDescriptor;
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
        assertEquals("RX1", rm.receiverId());
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
        assertEquals("1.1", descriptor.version());
    }

    // ----- AZIMUTH / ELEVATION in the receiver's antenna frame -----

    private static double[] angles(Receiver rx, Vector3D target) {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        ReceiverMeasurement rm = new BaselineMeasurementModel()
            .generate(tx, List.of(rx), target).perReceiver().get(0);
        return new double[]{rm.values().get(Quantity.AZIMUTH), rm.values().get(Quantity.ELEVATION)};
    }

    @Test
    public void testTargetOnBoresightReadsZeroAngles() {
        Receiver rx = new Receiver("RX1", new Vector3D(1000, 1000, 0), 2.4e9, 10.0);
        rx.setOrientation(225, 0);  // facing the origin (south-west)
        double[] a = angles(rx, new Vector3D(0, 0, 0));
        assertEquals(0.0, a[0], 1e-9);
        assertEquals(0.0, a[1], 1e-9);
    }

    @Test
    public void testAzimuthIsAnticlockwiseFromBoresight() {
        Receiver rx = new Receiver("RX1", new Vector3D(0, 0, 0), 2.4e9, 10.0);  // faces East
        assertEquals(90.0, angles(rx, new Vector3D(0, 500, 0))[0], 1e-9);    // North = left
        assertEquals(-90.0, angles(rx, new Vector3D(0, -500, 0))[0], 1e-9);  // South = right
        assertEquals(45.0, angles(rx, new Vector3D(100, 100, 0))[0], 1e-9);
    }

    @Test
    public void testElevationIsRelativeToAntennaTilt() {
        Receiver rx = new Receiver("RX1", new Vector3D(0, 0, 0), 2.4e9, 10.0);
        // Antenna horizontal: target 45 degrees up
        assertEquals(45.0, angles(rx, new Vector3D(100, 0, 100))[1], 1e-9);
        // Antenna tilted 30 degrees up: horizontal target reads -30, target 30 degrees up reads 0
        rx.setOrientation(0, 30);
        assertEquals(-30.0, angles(rx, new Vector3D(100, 0, 0))[1], 1e-9);
        double[] onAxis = angles(rx, new Vector3D(Math.cos(Math.toRadians(30)), 0, 0.5));
        assertEquals(0.0, onAxis[0], 1e-9);
        assertEquals(0.0, onAxis[1], 1e-9);
    }

    @Test
    public void testOrientationChangesAnglesButNotRange() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        Receiver rx = new Receiver("RX1", new Vector3D(2000, 0, 0), 2.4e9, 10.0);
        Vector3D target = new Vector3D(1000, 1500, 300);
        BaselineMeasurementModel model = new BaselineMeasurementModel();

        ReceiverMeasurement before = model.generate(tx, List.of(rx), target).perReceiver().get(0);
        rx.setOrientation(120, 10);
        ReceiverMeasurement after = model.generate(tx, List.of(rx), target).perReceiver().get(0);

        assertEquals(before.values().get(Quantity.BISTATIC_RANGE),
            after.values().get(Quantity.BISTATIC_RANGE), 1e-9);
        assertNotEquals(before.values().get(Quantity.AZIMUTH),
            after.values().get(Quantity.AZIMUTH), 1.0);
    }
}
