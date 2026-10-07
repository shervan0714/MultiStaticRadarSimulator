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
        // TX and all receivers at z=0: valid, but mirror-ambiguous in Z
        assertTrue(result.hasWarnings());
        assertTrue(result.warnings().get(0).contains("coplanar"));
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
        assertFalse(result.hasWarnings());
    }

    @Test
    public void testCollinearReceiversWithOffLineTransmitterAreLocalizable() {
        // The receivers lie on a line, but the transmitter is off it, so the
        // sensors span a plane: only the mirror ambiguity remains.
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 3000, 0), 10e9, 1.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(-4000, 0, 0), 10e9, 0));
        receivers.add(new Receiver("RX2", new Vector3D(0, 0, 0), 10e9, 0));
        receivers.add(new Receiver("RX3", new Vector3D(4000, 0, 0), 10e9, 0));

        GeometryValidator.ValidationResult result = validator.validate(tx, receivers);

        assertTrue(result.isValid());
        assertTrue(result.hasWarnings());

        // And the localizer does recover the target in this layout
        Vector3D target = new Vector3D(1000, 2000, 500);
        EstimationResult estimate = new BaselineLocalizationModel().estimate(tx, receivers,
            new BaselineMeasurementModel().generate(tx, receivers, target));
        assertEquals(Status.SUCCESS, estimate.status());
        assertEquals(0.0, estimate.position().distance(target), 1e-3);
    }

    @Test
    public void testTransmitterOnReceiverLineIsCollinear() {
        // Diagonal line in 3D, kilometre scale
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 10e9, 1.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(1000, 2000, 300), 10e9, 0));
        receivers.add(new Receiver("RX2", new Vector3D(3000, 6000, 900), 10e9, 0));
        receivers.add(new Receiver("RX3", new Vector3D(-2000, -4000, -600), 10e9, 0));

        GeometryValidator.ValidationResult result = validator.validate(tx, receivers);

        assertFalse(result.isValid());
        assertEquals(Status.UNDER_CONSTRAINED, result.status());
    }

    @Test
    public void testResultDoesNotDependOnReceiverOrder() {
        // The first three receivers are collinear; only RX4 leaves the plane.
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 10e9, 1.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(1000, 1000, 0), 10e9, 0));
        receivers.add(new Receiver("RX2", new Vector3D(2000, 2000, 0), 10e9, 0));
        receivers.add(new Receiver("RX3", new Vector3D(3000, 0, 0), 10e9, 0));
        receivers.add(new Receiver("RX4", new Vector3D(0, 2000, 1500), 10e9, 0));

        GeometryValidator.ValidationResult forward = validator.validate(tx, receivers);
        java.util.Collections.reverse(receivers);
        GeometryValidator.ValidationResult reversed = validator.validate(tx, receivers);

        assertTrue(forward.isValid());
        assertFalse(forward.hasWarnings());
        assertEquals(forward.isValid(), reversed.isValid());
        assertEquals(forward.warnings(), reversed.warnings());
    }

    @Test
    public void testCoplanarityCheckIsScaleIndependent() {
        // Same non-coplanar shape at 10 m and 100 km scale
        for (double scale : new double[]{0.01, 100.0}) {
            Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 10e9, 1.0);
            List<Receiver> receivers = new ArrayList<>();
            receivers.add(new Receiver("RX1", new Vector3D(1000 * scale, 0, 0), 10e9, 0));
            receivers.add(new Receiver("RX2", new Vector3D(0, 1000 * scale, 0), 10e9, 0));
            receivers.add(new Receiver("RX3", new Vector3D(0, 0, 1000 * scale), 10e9, 0));

            GeometryValidator.ValidationResult result = validator.validate(tx, receivers);

            assertTrue("valid at scale " + scale, result.isValid());
            assertFalse("no warning at scale " + scale, result.hasWarnings());
        }
    }

    @Test
    public void testDuplicateReceiverIdsAreInvalid() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 10e9, 1.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 10e9, 0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 10e9, 0));
        receivers.add(new Receiver("RX1", new Vector3D(0, -5000, 0), 10e9, 0));

        GeometryValidator.ValidationResult result = validator.validate(tx, receivers);

        assertFalse(result.isValid());
        assertEquals(Status.INVALID_INPUT, result.status());
        assertTrue(result.message().contains("RX1"));
    }
}
