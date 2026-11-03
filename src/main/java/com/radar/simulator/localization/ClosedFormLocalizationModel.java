package com.radar.simulator.localization;

import com.radar.simulator.core.ModelDescriptor;
import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.measurement.Measurement;
import com.radar.simulator.util.Matrix3;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Second localization formulation: a closed-form (non-iterative) solution of
 * the bistatic-range equations, in the style of spherical interpolation.
 *
 * With the transmitter as origin, q = p - t, R = |q| and s_i = r_i - t,
 * squaring |q - s_i| = m_i - R gives an equation that is linear in q once R
 * is treated as known:
 *
 *     s_i · q = (|s_i|² - m_i²) / 2 + m_i R
 *
 * Stacking all receivers, A q = c + m R is solved in the least-squares sense
 * as q = a + b R (a, b from the 3x3 normal equations). Substituting into
 * |q|² = R² gives a quadratic in R; each non-negative root is a candidate,
 * and the same above-ground rule as the baseline picks between them.
 *
 * Differences from the baseline: no starting points and no iteration, but
 * with more than 3 receivers the linearised least squares is not the optimal
 * fit, so noise is amplified more. It needs the receivers (relative to the
 * transmitter) not to lie in one plane through the transmitter; otherwise
 * A^T A is singular and the result is UNDER_CONSTRAINED.
 *
 * The residual and condition number are reported exactly as for the
 * baseline (bistatic-range RMS and Jacobian at the solution), so the two
 * models can be compared directly.
 */
public class ClosedFormLocalizationModel implements LocalizationModel {

    @Override
    public ModelDescriptor getDescriptor() {
        return new ModelDescriptor("ClosedFormLocalization", "1.0");
    }

    @Override
    public EstimationResult estimate(Transmitter transmitter, List<Receiver> receivers, Measurement measurement) {
        BistaticRanges problem = BistaticRanges.collect(transmitter, receivers, measurement);
        if (problem.failure() != null) {
            return LeastSquaresSolver.failure(problem.failure());
        }

        double[][] normal = new double[3][3];
        double[] atc = new double[3];
        double[] atm = new double[3];
        for (int k = 0; k < problem.size(); k++) {
            Vector3D s = problem.receiverPositions.get(k).subtract(problem.tx);
            double m = problem.ranges.get(k);
            double[] row = {s.x, s.y, s.z};
            double c = (s.dot(s) - m * m) / 2;
            for (int i = 0; i < 3; i++) {
                atc[i] += row[i] * c;
                atm[i] += row[i] * m;
                for (int j = 0; j < 3; j++) {
                    normal[i][j] += row[i] * row[j];
                }
            }
        }

        // Receivers in one plane through the transmitter: the linear system
        // cannot determine the out-of-plane coordinate.
        double[] eig = Matrix3.symmetricEigenvalues(normal);
        if (!(eig[0] > 0) || Math.sqrt(eig[2] / eig[0]) > LeastSquaresSolver.MAX_CONDITION_NUMBER) {
            return LeastSquaresSolver.failure(Status.UNDER_CONSTRAINED);
        }
        double[] a = Matrix3.solve(normal, atc);
        double[] b = Matrix3.solve(normal, atm);
        if (a == null || b == null) {
            return LeastSquaresSolver.failure(Status.UNDER_CONSTRAINED);
        }
        Vector3D va = new Vector3D(a[0], a[1], a[2]);
        Vector3D vb = new Vector3D(b[0], b[1], b[2]);

        // |a + b R|² = R²  →  (b·b - 1) R² + 2 (a·b) R + a·a = 0
        List<Double> roots = nonNegativeRoots(vb.dot(vb) - 1, 2 * va.dot(vb), va.dot(va));
        if (roots.isEmpty()) {
            return LeastSquaresSolver.failure(Status.NUMERICALLY_UNSTABLE);
        }

        List<Vector3D> candidates = new ArrayList<>();
        List<Double> residuals = new ArrayList<>();
        for (double r : roots) {
            Vector3D p = problem.tx.add(va.add(vb.scale(r)));
            double rms = LeastSquaresSolver.rmsResidual(p, problem);
            if (!Double.isFinite(rms)) continue;
            candidates.add(p);
            residuals.add(rms);
        }
        if (candidates.isEmpty()) {
            return LeastSquaresSolver.failure(Status.NUMERICALLY_UNSTABLE);
        }
        return LeastSquaresSolver.result(LeastSquaresSolver.selectCandidate(candidates, residuals), problem);
    }

    /**
     * Non-negative real roots of a R² + b R + c = 0. With noisy ranges the
     * discriminant can dip slightly below zero; it is then treated as zero
     * (the closest real solution).
     */
    static List<Double> nonNegativeRoots(double a, double b, double c) {
        List<Double> roots = new ArrayList<>();
        double scale = Math.max(Math.abs(a), Math.max(Math.abs(b), Math.abs(c)));
        if (scale == 0) {
            return roots;
        }
        if (Math.abs(a) < 1e-12 * scale) {
            if (b != 0) roots.add(-c / b);
        } else {
            double disc = Math.max(b * b - 4 * a * c, 0);
            double sqrt = Math.sqrt(disc);
            // Numerically stable form: avoid subtracting nearly equal numbers
            double qq = -0.5 * (b + Math.copySign(sqrt, b));
            roots.add(qq / a);
            if (qq != 0) roots.add(c / qq);
        }
        roots.removeIf(r -> !(r >= 0) || !Double.isFinite(r));
        return roots;
    }
}
