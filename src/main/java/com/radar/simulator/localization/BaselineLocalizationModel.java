package com.radar.simulator.localization;

import com.radar.simulator.core.ModelDescriptor;
import com.radar.simulator.measurement.Measurement;
import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Vector3D;

import java.util.List;

/**
 * Baseline localization model: nonlinear least squares on bistatic ranges.
 *
 * Each receiver i with a BISTATIC_RANGE measurement m_i contributes one
 * equation |p - t| + |p - r_i| = m_i (an ellipsoid with foci at the
 * transmitter t and the receiver r_i). The position p minimising the sum of
 * squared range residuals is found with Levenberg-Marquardt using the
 * analytic Jacobian row u(p - t) + u(p - r_i), where u() is the unit vector
 * (see {@link BistaticRanges} and {@link LeastSquaresSolver}).
 *
 * Assumptions:
 * <ul>
 *   <li>At least 3 bistatic ranges are needed for a 3D position.</li>
 *   <li>The solver is started from several points around the sensors.
 *       With (nearly) coplanar sensors a target and its mirror image across
 *       the sensor plane fit the ranges almost equally well; with noisy
 *       ranges the mirror can even fit better. The target is therefore
 *       constrained to be at or above the ground (Z &gt;= 0): if any
 *       candidate is above ground, the best-fitting above-ground candidate
 *       is returned (exact ties go to the higher one). Only if every
 *       candidate is below ground is the best-fitting one returned.</li>
 * </ul>
 *
 * The result reports the RMS range residual in metres and the condition
 * number of the Jacobian at the solution. A large condition number means
 * the geometry cannot resolve the position reliably.
 */
public class BaselineLocalizationModel implements LocalizationModel {

    @Override
    public ModelDescriptor getDescriptor() {
        return new ModelDescriptor("BaselineLocalization", "2.1");
    }

    @Override
    public EstimationResult estimate(Transmitter transmitter, List<Receiver> receivers, Measurement measurement) {
        BistaticRanges problem = BistaticRanges.collect(transmitter, receivers, measurement);
        if (problem.failure() != null) {
            return LeastSquaresSolver.failure(problem.failure());
        }
        return LeastSquaresSolver.solve(startingPoints(problem), problem);
    }

    /**
     * Starting points spread around the sensor centroid at roughly the
     * target distance implied by the measured ranges.
     */
    private static List<Vector3D> startingPoints(BistaticRanges problem) {
        Vector3D centroid = problem.tx;
        double meanRange = 0;
        for (int i = 0; i < problem.size(); i++) {
            centroid = centroid.add(problem.receiverPositions.get(i));
            meanRange += problem.ranges.get(i);
        }
        centroid = centroid.scale(1.0 / (problem.size() + 1));
        double scale = Math.max(meanRange / problem.size() / 2, 1.0);
        return LeastSquaresSolver.startingPoints(centroid, scale);
    }
}
