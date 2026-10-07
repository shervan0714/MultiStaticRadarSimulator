package com.radar.simulator.model;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Matrix3;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates transmitter/receiver geometry for localization feasibility.
 *
 * Some sensor arrangements do not provide enough independent spatial
 * information for reliable 3D localization. This validator detects
 * under-constrained and degenerate configurations and reports an explicit
 * status rather than silently producing unreliable positions.
 *
 * The transmitter is a focus of every bistatic-range ellipsoid, so it is
 * treated as a sensor alongside the receivers. The spread of the sensor
 * positions is measured by the eigenvalues of their covariance matrix,
 * which does not depend on receiver order or on the coordinate scale.
 *
 * Checks performed:
 * <ul>
 *   <li>Minimum receiver count (at least 3 for 3D)</li>
 *   <li>Unique receiver IDs (measurements are matched by ID)</li>
 *   <li>Minimum baseline: receivers too close together give near-duplicate
 *       equations</li>
 *   <li>Collinear sensors: every position on a circle around the line fits
 *       equally well, so 3D position is undetermined (invalid)</li>
 *   <li>Coplanar sensors: a target and its mirror image across the sensor
 *       plane fit equally well (valid, but reported as a warning)</li>
 * </ul>
 */
public class GeometryValidator {

    /** Minimum number of receivers for 3D localization. */
    private static final int MIN_RECEIVERS = 3;

    /**
     * Minimum baseline distance (metres) between any pair of receivers
     * to avoid poorly conditioned geometry.
     */
    private static final double MIN_BASELINE_METRES = 1.0;

    /**
     * Relative spread below which a sensor direction is treated as flat:
     * sqrt(eigenvalue / largest eigenvalue) of the position covariance.
     */
    private static final double FLATNESS_THRESHOLD = 1e-6;

    /**
     * Immutable result of geometry validation.
     *
     * @param isValid  whether localization can be attempted
     * @param status   SUCCESS, or the failure status to report
     * @param message  human-readable summary
     * @param warnings conditions that do not block localization but limit it
     */
    public record ValidationResult(
        boolean isValid,
        Status status,
        String message,
        List<String> warnings
    ) {
        public ValidationResult(boolean isValid, Status status, String message) {
            this(isValid, status, message, List.of());
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }

    /**
     * Validate the transmitter/receiver geometry.
     *
     * @param transmitter the transmitter
     * @param receivers   the list of receivers
     * @return validation result with status, diagnostic message and warnings
     */
    public ValidationResult validate(Transmitter transmitter, List<Receiver> receivers) {
        if (transmitter == null) {
            return new ValidationResult(false, Status.INVALID_INPUT,
                "Transmitter is null");
        }

        if (receivers == null || receivers.isEmpty()) {
            return new ValidationResult(false, Status.INVALID_INPUT,
                "No receivers provided");
        }

        if (receivers.size() < MIN_RECEIVERS) {
            return new ValidationResult(false, Status.UNDER_CONSTRAINED,
                "Need at least " + MIN_RECEIVERS + " receivers for 3D localization, got " + receivers.size());
        }

        Set<String> ids = new HashSet<>();
        for (Receiver rx : receivers) {
            if (!ids.add(rx.getId())) {
                return new ValidationResult(false, Status.INVALID_INPUT,
                    "Duplicate receiver ID: " + rx.getId());
            }
        }

        // Check minimum baseline
        for (int i = 0; i < receivers.size(); i++) {
            for (int j = i + 1; j < receivers.size(); j++) {
                double distance = receivers.get(i).getPosition()
                    .distance(receivers.get(j).getPosition());
                if (distance < MIN_BASELINE_METRES) {
                    return new ValidationResult(false, Status.NUMERICALLY_UNSTABLE,
                        "Receivers " + receivers.get(i).getId() + " and " +
                        receivers.get(j).getId() + " are too close (" +
                        String.format("%.2f", distance) + " m)");
                }
            }
        }

        // Spread of all sensor positions, largest eigenvalue last
        List<Vector3D> sensors = new ArrayList<>();
        sensors.add(transmitter.getPosition());
        for (Receiver rx : receivers) {
            sensors.add(rx.getPosition());
        }
        double[] eig = Matrix3.symmetricEigenvalues(covariance(sensors));
        double largest = eig[2];

        if (isFlat(eig[1], largest)) {
            return new ValidationResult(false, Status.UNDER_CONSTRAINED,
                "Transmitter and receivers are collinear - cannot determine 3D position");
        }

        List<String> warnings = new ArrayList<>();
        if (isFlat(eig[0], largest)) {
            warnings.add("Transmitter and receivers are coplanar - a target and its mirror "
                + "image across the sensor plane give identical ranges; the solution "
                + "with the higher Z is assumed");
        }

        String message = "Geometry valid: " + receivers.size() + " receivers with sufficient spatial spread";
        if (!warnings.isEmpty()) {
            message += " (" + warnings.size() + " warning" + (warnings.size() > 1 ? "s" : "") + ")";
        }
        return new ValidationResult(true, Status.SUCCESS, message, List.copyOf(warnings));
    }

    private static boolean isFlat(double eigenvalue, double largest) {
        return largest <= 0 || Math.sqrt(Math.max(eigenvalue, 0) / largest) < FLATNESS_THRESHOLD;
    }

    /**
     * Covariance matrix of a set of points about their centroid.
     */
    private static double[][] covariance(List<Vector3D> points) {
        Vector3D centroid = new Vector3D(0, 0, 0);
        for (Vector3D p : points) {
            centroid = centroid.add(p);
        }
        centroid = centroid.scale(1.0 / points.size());

        double[][] cov = new double[3][3];
        for (Vector3D p : points) {
            Vector3D d = p.subtract(centroid);
            double[] v = {d.x, d.y, d.z};
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    cov[i][j] += v[i] * v[j] / points.size();
                }
            }
        }
        return cov;
    }
}
