package com.radar.simulator.model;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Vector3D;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for GeometryValidator.
 * Tests well-conditioned, under-constrained, collinear, and degenerate
 * receiver geometries.
 */
public class GeometryValidatorTest {

    private final GeometryValidator validator = new GeometryValidator();

    @Test
    public void testValidTriangleGeometry() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 10e9, 1.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 10e9, 0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 10e9, 0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 10e9, 0));

        GeometryValidator.ValidationResult result = validator.validate(tx, receivers);

        assertTrue(result.isValid());
        assertEquals(Status.SUCCESS, result.status());
    }

    @Test
    public void testTooFewReceivers() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 10e9, 1.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 10e9, 0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 10e9, 0));

        GeometryValidator.ValidationResult result = validator.validate(tx, receivers);

        assertFalse(result.isValid());
        assertEquals(Status.UNDER_CONSTRAINED, result.status());
    }

    @Test
    public void testNoReceivers() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 10e9, 1.0);
        List<Receiver> receivers = new ArrayList<>();

        GeometryValidator.ValidationResult result = validator.validate(tx, receivers);

        assertFalse(result.isValid());
        assertEquals(Status.INVALID_INPUT, result.status());
    }

    @Test
    public void testNullTransmitter() {
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 10e9, 0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 10e9, 0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 10e9, 0));

        GeometryValidator.ValidationResult result = validator.validate(null, receivers);

        assertFalse(result.isValid());
        assertEquals(Status.INVALID_INPUT, result.status());
    }

    @Test
    public void testCollinearReceivers() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 10e9, 1.0);
        List<Receiver> receivers = new ArrayList<>();
        // All receivers on the X-axis
        receivers.add(new Receiver("RX1", new Vector3D(1000, 0, 0), 10e9, 0));
        receivers.add(new Receiver("RX2", new Vector3D(2000, 0, 0), 10e9, 0));
        receivers.add(new Receiver("RX3", new Vector3D(3000, 0, 0), 10e9, 0));

        GeometryValidator.ValidationResult result = validator.validate(tx, receivers);

        assertFalse(result.isValid());
        assertEquals(Status.UNDER_CONSTRAINED, result.status());
        assertTrue(result.message().contains("collinear"));
    }

    @Test
    public void testReceiversTooClose() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 10e9, 1.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 10e9, 0));
        receivers.add(new Receiver("RX2", new Vector3D(5000, 5000, 0.1), 10e9, 0)); // very close to RX1
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 10e9, 0));

        GeometryValidator.ValidationResult result = validator.validate(tx, receivers);

        assertFalse(result.isValid());
        assertEquals(Status.NUMERICALLY_UNSTABLE, result.status());
    }

    @Test
    public void testFourReceiversNonCoplanar() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 10e9, 1.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 0, 0), 10e9, 0));
        receivers.add(new Receiver("RX2", new Vector3D(0, 5000, 0), 10e9, 0));
        receivers.add(new Receiver("RX3", new Vector3D(0, 0, 5000), 10e9, 0));
        receivers.add(new Receiver("RX4", new Vector3D(-5000, -5000, 0), 10e9, 0));

        GeometryValidator.ValidationResult result = validator.validate(tx, receivers);

        assertTrue(result.isValid());
        assertEquals(Status.SUCCESS, result.status());
    }
}
