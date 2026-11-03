package com.radar.simulator.localization;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.measurement.Measurement;
import com.radar.simulator.measurement.Quantity;
import com.radar.simulator.measurement.ReceiverMeasurement;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The bistatic-range equations of one time step, |p - t| + |p - r_i| = m_i,
 * as a least-squares problem with the analytic gradient
 * u(p - t) + u(p - r_i), where u() is the unit vector.
 *
 * {@link #collect} matches measurements to receivers by ID and checks them;
 * if they cannot be used, {@link #failure()} says why.
 */
final class BistaticRanges implements LeastSquaresSolver.Problem {

    final Vector3D tx;
    final List<Vector3D> receiverPositions = new ArrayList<>();
    final List<Double> ranges = new ArrayList<>();
    private Status failure;

    private BistaticRanges(Vector3D tx) {
        this.tx = tx;
    }

    /**
     * Collect the usable BISTATIC_RANGE measurements. Fails with INVALID_INPUT
     * for missing inputs, duplicate receiver IDs, non-finite ranges or a range
     * shorter than its TX-RX baseline, and with UNDER_CONSTRAINED for fewer
     * than 3 ranges.
     */
    static BistaticRanges collect(Transmitter transmitter, List<Receiver> receivers, Measurement measurement) {
        if (transmitter == null || receivers == null || measurement == null) {
            return failed(Status.INVALID_INPUT);
        }
        BistaticRanges problem = new BistaticRanges(transmitter.getPosition());

        Map<String, Receiver> receiversById = new HashMap<>();
        for (Receiver rx : receivers) {
            if (receiversById.put(rx.getId(), rx) != null) {
                return failed(Status.INVALID_INPUT);  // ambiguous receiver ID
            }
        }

        for (ReceiverMeasurement rm : measurement.perReceiver()) {
            Receiver rx = receiversById.get(rm.receiverId());
            Double range = rm.values().get(Quantity.BISTATIC_RANGE);
            if (rx == null || range == null) continue;
            if (!Double.isFinite(range)) {
                return failed(Status.INVALID_INPUT);
            }
            // A bistatic range can never be shorter than the TX-RX baseline.
            double baseline = problem.tx.distance(rx.getPosition());
            if (range < baseline - (1e-6 * baseline + 1e-9)) {
                return failed(Status.INVALID_INPUT);
            }
            problem.receiverPositions.add(rx.getPosition());
            problem.ranges.add(range);
        }

        if (problem.size() < 3) {
            return failed(Status.UNDER_CONSTRAINED);
        }
        return problem;
    }

    private static BistaticRanges failed(Status status) {
        BistaticRanges problem = new BistaticRanges(null);
        problem.failure = status;
        return problem;
    }

    /** Why the measurements cannot be used, or null if they can. */
    Status failure() {
        return failure;
    }

    @Override
    public int size() {
        return ranges.size();
    }

    @Override
    public double residual(int i, Vector3D p) {
        return p.distance(tx) + p.distance(receiverPositions.get(i)) - ranges.get(i);
    }

    @Override
    public double[] gradient(int i, Vector3D p) {
        Vector3D toTx = p.subtract(tx);
        Vector3D toRx = p.subtract(receiverPositions.get(i));
        double dTx = Math.max(toTx.magnitude(), 1e-12);
        double dRx = Math.max(toRx.magnitude(), 1e-12);
        return new double[]{
            toTx.x / dTx + toRx.x / dRx,
            toTx.y / dTx + toRx.y / dRx,
            toTx.z / dTx + toRx.z / dRx
        };
    }
}
