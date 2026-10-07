package com.radar.simulator.model;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Matrix3;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Baseline localization model: nonlinear least squares on bistatic ranges.
 *
 * Each receiver i with a BISTATIC_RANGE measurement m_i contributes one
 * equation |p - t| + |p - r_i| = m_i (an ellipsoid with foci at the
 * transmitter t and the receiver r_i). The position p minimising the sum of
 * squared range residuals is found with Levenberg-Marquardt using the
 * analytic Jacobian row u(p - t) + u(p - r_i), where u() is the unit vector.
 *
 * Assumptions:
 * <ul>
 *   <li>At least 3 bistatic ranges are needed for a 3D position.</li>
 *   <li>The solver is started from several points around the sensors. When
 *       two solutions fit equally well (e.g. the mirror image below a
 *       coplanar sensor layout), the one with the larger Z (higher altitude)
 *       is returned, i.e. the target is assumed to be above the sensors.</li>
 * </ul>
 *
 * The result reports the RMS range residual in metres and the condition
 * number of the Jacobian at the solution. A large condition number means
 * the geometry cannot resolve the position reliably.
 */
public class BaselineLocalizationModel implements LocalizationModel {

    /** Condition number above which the estimate is reported as unstable. */
    static final double MAX_CONDITION_NUMBER = 1e8;

    private static final int MAX_ITERATIONS = 200;

    private static final double[][] START_DIRECTIONS = {
        {0, 0, 1}, {0, 0, -1},
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0},
        {0.577, 0.577, 0.577}, {-0.577, -0.577, 0.577}
    };

    /** One bistatic-range equation: receiver position and measured range. */
    private record RangeEquation(Vector3D receiverPosition, double range) {}

    @Override
    public ModelDescriptor getDescriptor() {
        return new ModelDescriptor("BaselineLocalization", "2.0");
    }

    @Override
    public EstimationResult estimate(Transmitter transmitter, List<Receiver> receivers, Measurement measurement) {
        if (transmitter == null || receivers == null || measurement == null) {
            return failure(Status.INVALID_INPUT);
        }

        Map<String, Receiver> receiversById = new HashMap<>();
        for (Receiver rx : receivers) {
            if (receiversById.put(rx.getId(), rx) != null) {
                return failure(Status.INVALID_INPUT);  // ambiguous receiver ID
            }
        }

        Vector3D tx = transmitter.getPosition();
        List<RangeEquation> equations = new ArrayList<>();
        for (ReceiverMeasurement rm : measurement.perReceiver()) {
            Receiver rx = receiversById.get(rm.receiverId());
            Double range = rm.values().get(Quantity.BISTATIC_RANGE);
            if (rx == null || range == null) continue;
            if (!Double.isFinite(range)) {
                return failure(Status.INVALID_INPUT);
            }
            // A bistatic range can never be shorter than the TX-RX baseline.
            double baseline = tx.distance(rx.getPosition());
            if (range < baseline - (1e-6 * baseline + 1e-9)) {
                return failure(Status.INVALID_INPUT);
            }
            equations.add(new RangeEquation(rx.getPosition(), range));
        }

        if (equations.size() < 3) {
            return failure(Status.UNDER_CONSTRAINED);
        }

        Vector3D best = null;
        double bestRms = Double.POSITIVE_INFINITY;
        for (Vector3D start : startingPoints(tx, equations)) {
            Vector3D candidate = solveFrom(start, tx, equations);
            double rms = rmsResidual(candidate, tx, equations);
            if (!Double.isFinite(rms)) continue;

            boolean clearlyBetter = rms < bestRms * (1 - 1e-6) - 1e-6;
            boolean tieButHigher = Math.abs(rms - bestRms) <= bestRms * 1e-6 + 1e-6
                && best != null && candidate.z > best.z;
            if (best == null || clearlyBetter || tieButHigher) {
                best = candidate;
                bestRms = rms;
            }
        }

        if (best == null) {
            return failure(Status.NUMERICALLY_UNSTABLE);
        }

        double conditionNumber = conditionNumber(best, tx, equations);
        Status status = conditionNumber > MAX_CONDITION_NUMBER
            ? Status.NUMERICALLY_UNSTABLE
            : Status.SUCCESS;
        return new EstimationResult(best, status, bestRms, conditionNumber);
    }

    /**
     * Starting points spread around the sensor centroid at roughly the
     * target distance implied by the measured ranges.
     */
    private List<Vector3D> startingPoints(Vector3D tx, List<RangeEquation> equations) {
        Vector3D centroid = tx;
        double meanRange = 0;
        for (RangeEquation eq : equations) {
            centroid = centroid.add(eq.receiverPosition());
            meanRange += eq.range();
        }
        centroid = centroid.scale(1.0 / (equations.size() + 1));
        double scale = Math.max(meanRange / equations.size() / 2, 1.0);

        List<Vector3D> starts = new ArrayList<>();
        for (double[] d : START_DIRECTIONS) {
            starts.add(centroid.add(new Vector3D(d[0], d[1], d[2]).scale(scale)));
        }
        return starts;
    }

    /**
     * Levenberg-Marquardt iteration from a single starting point.
     */
    private Vector3D solveFrom(Vector3D start, Vector3D tx, List<RangeEquation> equations) {
        Vector3D p = start;
        double cost = cost(p, tx, equations);
        double lambda = 1e-3;

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            double[][] jtj = new double[3][3];
            double[] jtf = new double[3];
            for (RangeEquation eq : equations) {
                double[] row = jacobianRow(p, tx, eq.receiverPosition());
                double f = predictedRange(p, tx, eq.receiverPosition()) - eq.range();
                for (int i = 0; i < 3; i++) {
                    jtf[i] += row[i] * f;
                    for (int j = 0; j < 3; j++) {
                        jtj[i][j] += row[i] * row[j];
                    }
                }
            }

            boolean improved = false;
            while (lambda < 1e12) {
                double[][] damped = new double[3][3];
                for (int i = 0; i < 3; i++) {
                    damped[i] = jtj[i].clone();
                    damped[i][i] += lambda * (jtj[i][i] + 1e-12);
                }
                double[] delta = Matrix3.solve(damped, new double[]{-jtf[0], -jtf[1], -jtf[2]});
                if (delta == null) {
                    lambda *= 10;
                    continue;
                }

                Vector3D step = new Vector3D(delta[0], delta[1], delta[2]);
                Vector3D next = p.add(step);
                double nextCost = cost(next, tx, equations);
                if (nextCost < cost) {
                    p = next;
                    lambda = Math.max(lambda / 10, 1e-12);
                    improved = true;
                    if (step.magnitude() < 1e-9 * (1 + p.magnitude())) {
                        return p;
                    }
                    cost = nextCost;
                    break;
                }
                lambda *= 10;
            }

            if (!improved || cost < 1e-24) {
                break;
            }
        }
        return p;
    }

    private static double predictedRange(Vector3D p, Vector3D tx, Vector3D rx) {
        return p.distance(tx) + p.distance(rx);
    }

    /**
     * Gradient of |p - tx| + |p - rx| with respect to p.
     */
    private static double[] jacobianRow(Vector3D p, Vector3D tx, Vector3D rx) {
        Vector3D toTx = p.subtract(tx);
        Vector3D toRx = p.subtract(rx);
        double dTx = Math.max(toTx.magnitude(), 1e-12);
        double dRx = Math.max(toRx.magnitude(), 1e-12);
        return new double[]{
            toTx.x / dTx + toRx.x / dRx,
            toTx.y / dTx + toRx.y / dRx,
            toTx.z / dTx + toRx.z / dRx
        };
    }

    private static double cost(Vector3D p, Vector3D tx, List<RangeEquation> equations) {
        double sum = 0;
        for (RangeEquation eq : equations) {
            double f = predictedRange(p, tx, eq.receiverPosition()) - eq.range();
            sum += f * f;
        }
        return sum;
    }

    private static double rmsResidual(Vector3D p, Vector3D tx, List<RangeEquation> equations) {
        return Math.sqrt(cost(p, tx, equations) / equations.size());
    }

    /**
     * Condition number of the Jacobian at p: sqrt(max/min eigenvalue of J^T J).
     */
    private static double conditionNumber(Vector3D p, Vector3D tx, List<RangeEquation> equations) {
        double[][] jtj = new double[3][3];
        for (RangeEquation eq : equations) {
            double[] row = jacobianRow(p, tx, eq.receiverPosition());
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    jtj[i][j] += row[i] * row[j];
                }
            }
        }
        double[] eig = Matrix3.symmetricEigenvalues(jtj);
        if (eig[0] <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.sqrt(eig[2] / eig[0]);
    }

    private static EstimationResult failure(Status status) {
        return new EstimationResult(new Vector3D(0, 0, 0), status, Double.NaN, Double.NaN);
    }
}
