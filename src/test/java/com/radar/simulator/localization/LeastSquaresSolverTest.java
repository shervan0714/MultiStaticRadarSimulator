package com.radar.simulator.localization;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.measurement.BaselineMeasurementModel;
import com.radar.simulator.util.Vector3D;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for the shared Levenberg-Marquardt solver.
 */
public class LeastSquaresSolverTest {

    private static BistaticRanges rangesFor(Vector3D target) {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = List.of(
            new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0),
            new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0),
            new Receiver("RX3", new Vector3D(-5000, -5000, 0), 2.4e9, 10.0),
            new Receiver("RX4", new Vector3D(5000, -5000, 400), 2.4e9, 10.0));
        return BistaticRanges.collect(tx, receivers,
            new BaselineMeasurementModel().generate(tx, receivers, target));
    }

    @Test
    public void testNumericalGradientMatchesAnalyticGradient() {
        BistaticRanges analytic = rangesFor(new Vector3D(1000, 2000, 300));
        // Same residuals, but falling back to the interface's central difference
        LeastSquaresSolver.Problem numeric = new LeastSquaresSolver.Problem() {
            public int size() { return analytic.size(); }
            public double residual(int i, Vector3D p) { return analytic.residual(i, p); }
        };
        Vector3D p = new Vector3D(-1500, 700, 250);
        for (int i = 0; i < analytic.size(); i++) {
            assertArrayEquals(analytic.gradient(i, p), numeric.gradient(i, p), 1e-6);
        }
    }

    @Test
    public void testNumericalGradientProblemConvergesToTruth() {
        Vector3D target = new Vector3D(1000, 2000, 300);
        BistaticRanges analytic = rangesFor(target);
        LeastSquaresSolver.Problem numeric = new LeastSquaresSolver.Problem() {
            public int size() { return analytic.size(); }
            public double residual(int i, Vector3D p) { return analytic.residual(i, p); }
        };
        EstimationResult result = LeastSquaresSolver.solve(
            LeastSquaresSolver.startingPoints(new Vector3D(0, 0, 80), 7000), numeric);
        assertEquals(Status.SUCCESS, result.status());
        assertEquals(0.0, result.position().distance(target), 1e-3);
    }

    @Test
    public void testAboveGroundCandidateBeatsBetterFittingMirror() {
        Vector3D above = new Vector3D(0, 0, 100);
        Vector3D below = new Vector3D(0, 0, -100);
        assertEquals(above, LeastSquaresSolver.selectCandidate(List.of(below, above), List.of(0.1, 5.0)));
    }

    @Test
    public void testExactTieGoesToHigherCandidate() {
        Vector3D low = new Vector3D(0, 0, 10);
        Vector3D high = new Vector3D(0, 0, 50);
        assertEquals(high, LeastSquaresSolver.selectCandidate(List.of(low, high), List.of(1.0, 1.0)));
    }

    @Test
    public void testUnevaluableProblemIsReportedUnstable() {
        LeastSquaresSolver.Problem nan = new LeastSquaresSolver.Problem() {
            public int size() { return 3; }
            public double residual(int i, Vector3D p) { return Double.NaN; }
        };
        EstimationResult result = LeastSquaresSolver.solve(
            LeastSquaresSolver.startingPoints(new Vector3D(0, 0, 0), 100), nan);
        assertEquals(Status.NUMERICALLY_UNSTABLE, result.status());
    }
}
