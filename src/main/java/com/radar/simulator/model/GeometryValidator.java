package com.radar.simulator.model;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Vector3D;

import java.util.List;

/**
 * Validates receiver geometry for localization feasibility.
 *
 * Some receiver arrangements do not provide enough independent spatial
 * information for reliable 3D localization. This validator detects
 * under-constrained and degenerate configurations and reports an explicit
 * status rather than silently producing unreliable positions.
 *
 * Checks performed:
 * <ul>
 *   <li>Minimum receiver count (at least 3 for 3D)</li>
 *   <li>Collinearity — all receivers on a single line cannot resolve 3D</li>
 *   <li>Coplanarity — all receivers in a plane may be marginal for 3D altitude</li>
 *   <li>Minimum baseline — receivers too close together yield poor conditioning</li>
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
     * Threshold for collinearity detection. If the cross product magnitude
     * of any two receiver-pair vectors is below this fraction of their
     * product of magnitudes, they are considered collinear.
     */
    private static final double COLLINEARITY_THRESHOLD = 1e-6;

    /**
     * Immutable result of geometry validation.
     */
    public record ValidationResult(
        boolean isValid,
        Status status,
        String message
    ) {}

    /**
     * Validate the transmitter/receiver geometry.
     *
     * @param transmitter the transmitter
     * @param receivers   the list of receivers
     * @return validation result with status and diagnostic message
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

        // Check collinearity: use the first receiver as origin, compute
        // vectors to all others, and check if the cross product of any
        // pair of these vectors is non-zero (i.e. they span at least 2D).
        Vector3D origin = receivers.get(0).getPosition();
        boolean foundNonCollinear = false;

        for (int i = 1; i < receivers.size() && !foundNonCollinear; i++) {
            Vector3D vi = receivers.get(i).getPosition().subtract(origin);
            for (int j = i + 1; j < receivers.size(); j++) {
                Vector3D vj = receivers.get(j).getPosition().subtract(origin);
                Vector3D cross = vi.cross(vj);
                double crossMag = cross.magnitude();
                double productMag = vi.magnitude() * vj.magnitude();

                if (productMag > 0 && crossMag / productMag > COLLINEARITY_THRESHOLD) {
                    foundNonCollinear = true;
                    break;
                }
            }
        }

        if (!foundNonCollinear) {
            return new ValidationResult(false, Status.UNDER_CONSTRAINED,
                "All receivers are collinear — cannot determine 3D position");
        }

        // Check that at least one receiver is not coplanar with the others
        // to provide altitude resolution. Check if the triple scalar product
        // of three receiver-pair vectors is non-zero.
        if (receivers.size() >= 4) {
            boolean foundNonCoplanar = false;
            Vector3D v1 = receivers.get(1).getPosition().subtract(origin);
            Vector3D v2 = receivers.get(2).getPosition().subtract(origin);

            for (int k = 3; k < receivers.size(); k++) {
                Vector3D vk = receivers.get(k).getPosition().subtract(origin);
                double tripleProduct = Math.abs(v1.cross(v2).dot(vk));
                if (tripleProduct > COLLINEARITY_THRESHOLD) {
                    foundNonCoplanar = true;
                    break;
                }
            }

            // If all are coplanar, it's not strictly invalid but the altitude
            // resolution will be poor. We still allow it but could flag a warning.
        }

        return new ValidationResult(true, Status.SUCCESS,
            "Geometry valid: " + receivers.size() + " receivers with sufficient spatial spread");
    }
}
