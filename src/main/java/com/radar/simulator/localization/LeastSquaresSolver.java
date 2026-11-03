package com.radar.simulator.localization;

import com.radar.simulator.util.Matrix3;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Levenberg-Marquardt least squares for a 3D position, shared by the
 * localization models.
 *
 * A {@link Problem} supplies one residual per measurement (predicted minus
 * measured value) and its gradient with respect to the position. The solver
 * minimises the sum of squared residuals from several starting points and
 * keeps the best candidate, with the target constrained to be at or above the
 * ground (see {@link #selectCandidate}).
 */
public final class LeastSquaresSolver {

    /** Condition number above which an estimate is reported as unstable. */
    public static final double MAX_CONDITION_NUMBER = 1e8;

    static final int MAX_ITERATIONS = 200;

    private static final double[][] START_DIRECTIONS = {
        {0, 0, 1}, {0, 0, -1},
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0},
        {0.577, 0.577, 0.577}, {-0.577, -0.577, 0.577}
    };

    /** Step for the numerical gradient, in metres. */
    private static final double GRADIENT_STEP = 1e-3;

    private LeastSquaresSolver() {}

    /** A least-squares problem in the target position p. */
    public interface Problem {
        /** Number of residuals (measurements). */
        int size();

        /** Residual i at p: predicted minus measured value. */
        double residual(int i, Vector3D p);

        /**
         * Gradient of residual i with respect to p. The default is a central
         * difference, so a model that only knows its formula still works.
         */
        default double[] gradient(int i, Vector3D p) {
            double h = GRADIENT_STEP;
            return new double[]{
                (residual(i, p.add(new Vector3D(h, 0, 0))) - residual(i, p.add(new Vector3D(-h, 0, 0)))) / (2 * h),
                (residual(i, p.add(new Vector3D(0, h, 0))) - residual(i, p.add(new Vector3D(0, -h, 0)))) / (2 * h),
                (residual(i, p.add(new Vector3D(0, 0, h))) - residual(i, p.add(new Vector3D(0, 0, -h)))) / (2 * h)
            };
        }
    }

    /**
     * Starting points spread in fixed directions around a centre point.
     *
     * @param centre where the target is expected (e.g. the sensor centroid)
     * @param scale  roughly how far from the centre the target may be (m)
     */
    public static List<Vector3D> startingPoints(Vector3D centre, double scale) {
        List<Vector3D> starts = new ArrayList<>();
        for (double[] d : START_DIRECTIONS) {
            starts.add(centre.add(new Vector3D(d[0], d[1], d[2]).scale(scale)));
        }
        return starts;
    }

    /**
     * Solve from every starting point and return the selected candidate with
     * its RMS residual and condition number.
     */
    public static EstimationResult solve(List<Vector3D> starts, Problem problem) {
        List<Vector3D> candidates = new ArrayList<>();
        List<Double> residuals = new ArrayList<>();
        for (Vector3D start : starts) {
            Vector3D candidate = solveFrom(start, problem);
            double rms = rmsResidual(candidate, problem);
            if (!Double.isFinite(rms)) continue;
            candidates.add(candidate);
            residuals.add(rms);
        }

        if (candidates.isEmpty()) {
            return failure(Status.NUMERICALLY_UNSTABLE);
        }
        return result(selectCandidate(candidates, residuals), problem);
    }

    /**
     * Wrap a position as a result: RMS residual, condition number, and
     * NUMERICALLY_UNSTABLE if the condition number is too large.
     */
    public static EstimationResult result(Vector3D position, Problem problem) {
        double conditionNumber = conditionNumber(position, problem);
        Status status = conditionNumber > MAX_CONDITION_NUMBER
            ? Status.NUMERICALLY_UNSTABLE
            : Status.SUCCESS;
        return new EstimationResult(position, status, rmsResidual(position, problem), conditionNumber);
    }

    /** A failed estimate with the given status and no position. */
    public static EstimationResult failure(Status status) {
        return new EstimationResult(new Vector3D(0, 0, 0), status, Double.NaN, Double.NaN);
    }

    /**
     * Pick the solution among the multi-start candidates: any candidate at or
     * above the ground beats any candidate below it; then the lower residual
     * wins, and on an exact tie the higher candidate.
     */
    public static Vector3D selectCandidate(List<Vector3D> candidates, List<Double> residuals) {
        Vector3D best = null;
        double bestRms = Double.POSITIVE_INFINITY;
        boolean bestAboveGround = false;
        for (int i = 0; i < candidates.size(); i++) {
            double rms = residuals.get(i);
            Vector3D candidate = candidates.get(i);
            boolean aboveGround = candidate.z >= 0;

            boolean better;
            if (best == null || aboveGround != bestAboveGround) {
                better = best == null || aboveGround;
            } else if (Math.abs(rms - bestRms) <= bestRms * 1e-6 + 1e-6) {
                better = candidate.z > best.z;
            } else {
                better = rms < bestRms;
            }
            if (better) {
                best = candidate;
                bestRms = rms;
                bestAboveGround = aboveGround;
            }
        }
        return best;
    }

    /**
     * Levenberg-Marquardt iteration from a single starting point.
     */
    static Vector3D solveFrom(Vector3D start, Problem problem) {
        Vector3D p = start;
        double cost = cost(p, problem);
        double lambda = 1e-3;

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            double[][] jtj = new double[3][3];
            double[] jtf = new double[3];
            for (int k = 0; k < problem.size(); k++) {
                double[] row = problem.gradient(k, p);
                double f = problem.residual(k, p);
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
                double nextCost = cost(next, problem);
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

    private static double cost(Vector3D p, Problem problem) {
        double sum = 0;
        for (int i = 0; i < problem.size(); i++) {
            double f = problem.residual(i, p);
            sum += f * f;
        }
        return sum;
    }

    /** RMS of the residuals at p. */
    public static double rmsResidual(Vector3D p, Problem problem) {
        return Math.sqrt(cost(p, problem) / problem.size());
    }

    /**
     * Condition number of the Jacobian at p: sqrt(max/min eigenvalue of J^T J).
     */
    public static double conditionNumber(Vector3D p, Problem problem) {
        double[][] jtj = new double[3][3];
        for (int k = 0; k < problem.size(); k++) {
            double[] row = problem.gradient(k, p);
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    jtj[i][j] += row[i] * row[j];
                }
            }
        }
        double[] eig = Matrix3.symmetricEigenvalues(jtj);
        if (!(eig[0] > 0)) {  // also catches NaN from a formula that cannot be evaluated
            return Double.POSITIVE_INFINITY;
        }
        return Math.sqrt(eig[2] / eig[0]);
    }
}
