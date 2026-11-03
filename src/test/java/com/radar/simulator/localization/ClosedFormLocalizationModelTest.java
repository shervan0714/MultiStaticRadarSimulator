package com.radar.simulator.localization;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.measurement.BaselineMeasurementModel;
import com.radar.simulator.measurement.Measurement;
import com.radar.simulator.util.Vector3D;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for the closed-form (spherical-interpolation) localization model and
 * the built-in model registry.
 */
public class ClosedFormLocalizationModelTest {

    private final Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);

    private static List<Receiver> squareWithMast() {
        return List.of(
            new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0),
            new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0),
            new Receiver("RX3", new Vector3D(-5000, -5000, 0), 2.4e9, 10.0),
            new Receiver("RX4", new Vector3D(5000, -5000, 400), 2.4e9, 10.0));
    }

    @Test
    public void testNoiselessEstimateMatchesGroundTruth() {
        List<Receiver> receivers = squareWithMast();
        ClosedFormLocalizationModel model = new ClosedFormLocalizationModel();
        Vector3D[] targets = {
            new Vector3D(1000, 2000, 100),
            new Vector3D(-3000, 1500, 800),
            new Vector3D(4000, -4000, 2500),
            new Vector3D(-3000, -2000, 300)
        };
        for (Vector3D target : targets) {
            Measurement m = new BaselineMeasurementModel().generate(tx, receivers, target);
            EstimationResult result = model.estimate(tx, receivers, m);
            assertEquals("Status for " + target, Status.SUCCESS, result.status());
            assertEquals("Error for " + target, 0.0, result.position().distance(target), 1e-3);
        }
    }

    @Test
    public void testExactlyThreeNonCoplanarReceivers() {
        List<Receiver> receivers = List.of(
            new Receiver("RX1", new Vector3D(6000, 0, 0), 2.4e9, 10.0),
            new Receiver("RX2", new Vector3D(0, 6000, 200), 2.4e9, 10.0),
            new Receiver("RX3", new Vector3D(-4000, -4000, 600), 2.4e9, 10.0));
        Vector3D target = new Vector3D(800, 1200, 500);
        EstimationResult result = new ClosedFormLocalizationModel().estimate(tx, receivers,
            new BaselineMeasurementModel().generate(tx, receivers, target));
        assertEquals(Status.SUCCESS, result.status());
        assertEquals(0.0, result.position().distance(target), 1e-3);
    }

    @Test
    public void testFlatLayoutIsUnderConstrainedForClosedForm() {
        // TX and all receivers at z = 0: the baseline still works (with the
        // above-ground rule) but the linear system cannot recover z.
        List<Receiver> receivers = List.of(
            new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0),
            new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0),
            new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));
        Measurement m = new BaselineMeasurementModel().generate(tx, receivers, new Vector3D(1000, 2000, 100));
        assertEquals(Status.UNDER_CONSTRAINED,
            new ClosedFormLocalizationModel().estimate(tx, receivers, m).status());
        assertEquals(Status.SUCCESS,
            new BaselineLocalizationModel().estimate(tx, receivers, m).status());
    }

    @Test
    public void testTooFewRangesAndBadRanges() {
        List<Receiver> receivers = squareWithMast().subList(0, 2);
        Measurement m = new BaselineMeasurementModel().generate(tx, receivers, new Vector3D(100, 100, 100));
        assertEquals(Status.UNDER_CONSTRAINED,
            new ClosedFormLocalizationModel().estimate(tx, receivers, m).status());
        assertEquals(Status.INVALID_INPUT,
            new ClosedFormLocalizationModel().estimate(null, receivers, m).status());
    }

    @Test
    public void testNoisyEstimateStaysAboveGroundAndClose() {
        List<Receiver> receivers = squareWithMast();
        BaselineMeasurementModel noisy = new BaselineMeasurementModel(5.0, 42L);
        ClosedFormLocalizationModel model = new ClosedFormLocalizationModel();
        Vector3D target = new Vector3D(-1000, 500, 400);
        for (int i = 0; i < 50; i++) {
            EstimationResult result = model.estimate(tx, receivers, noisy.generate(tx, receivers, target));
            assertEquals(Status.SUCCESS, result.status());
            assertTrue("Below ground: " + result.position(), result.position().z >= 0);
            assertTrue("Error too large: " + result.position(), result.position().distance(target) < 1000);
        }
    }

    @Test
    public void testQuadraticRoots() {
        // (R - 2)(R - 5) = R² - 7R + 10
        List<Double> roots = ClosedFormLocalizationModel.nonNegativeRoots(1, -7, 10);
        assertEquals(2, roots.size());
        assertTrue(roots.contains(2.0) && roots.contains(5.0));
        // (R + 1)(R - 3): only the non-negative root
        assertEquals(List.of(3.0), ClosedFormLocalizationModel.nonNegativeRoots(1, -2, -3));
        // Linear case: 2R - 8 = 0
        assertEquals(List.of(4.0), ClosedFormLocalizationModel.nonNegativeRoots(0, 2, -8));
    }

    @Test
    public void testRegistryListsBothModels() {
        assertEquals(List.of("baseline", "closed-form"), LocalizationModels.keys());
        assertEquals("BaselineLocalization", LocalizationModels.byKey("baseline").getDescriptor().name());
        assertEquals("ClosedFormLocalization", LocalizationModels.byKey("closed-form").getDescriptor().name());
        assertEquals(2, LocalizationModels.builtIns().size());
        try {
            LocalizationModels.byKey("nope");
            fail("Expected an exception for an unknown key");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("baseline"));
        }
    }
}
