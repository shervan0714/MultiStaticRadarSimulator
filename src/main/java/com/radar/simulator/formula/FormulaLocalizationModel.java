package com.radar.simulator.formula;

import com.radar.simulator.core.ModelDescriptor;
import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.localization.EstimationResult;
import com.radar.simulator.localization.LeastSquaresSolver;
import com.radar.simulator.localization.LocalizationModel;
import com.radar.simulator.localization.Status;
import com.radar.simulator.measurement.Measurement;
import com.radar.simulator.measurement.Quantity;
import com.radar.simulator.measurement.ReceiverMeasurement;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Localization model that inverts a user formula: it finds the target
 * position p for which formula(p, receiver i) best matches each receiver's
 * measured BISTATIC_RANGE, in the least-squares sense.
 *
 * Uses the shared {@link LeastSquaresSolver} (Levenberg-Marquardt, several
 * starting points, target kept at or above the ground) with a numerical
 * gradient, since the formula is only known as text. Parameters are fixed
 * values; only the 3 position coordinates are estimated, so at least 3
 * measurements are needed.
 */
public class FormulaLocalizationModel implements LocalizationModel {

    private final CustomFormula formula;

    public FormulaLocalizationModel(CustomFormula formula) {
        this.formula = formula;
    }

    @Override
    public ModelDescriptor getDescriptor() {
        return new ModelDescriptor("Formula: " + formula.name(), formula.describe());
    }

    @Override
    public EstimationResult estimate(Transmitter transmitter, List<Receiver> receivers, Measurement measurement) {
        if (transmitter == null || receivers == null || measurement == null) {
            return LeastSquaresSolver.failure(Status.INVALID_INPUT);
        }
        Map<String, Receiver> receiversById = new HashMap<>();
        for (Receiver rx : receivers) {
            if (receiversById.put(rx.getId(), rx) != null) {
                return LeastSquaresSolver.failure(Status.INVALID_INPUT);
            }
        }

        List<Receiver> used = new ArrayList<>();
        List<Double> measured = new ArrayList<>();
        for (ReceiverMeasurement rm : measurement.perReceiver()) {
            Receiver rx = receiversById.get(rm.receiverId());
            Double value = rm.values().get(Quantity.BISTATIC_RANGE);
            if (rx == null || value == null) continue;
            if (!Double.isFinite(value)) {
                return LeastSquaresSolver.failure(Status.INVALID_INPUT);
            }
            used.add(rx);
            measured.add(value);
        }
        if (used.size() < 3) {
            return LeastSquaresSolver.failure(Status.UNDER_CONSTRAINED);
        }

        LeastSquaresSolver.Problem problem = new LeastSquaresSolver.Problem() {
            @Override
            public int size() {
                return used.size();
            }

            @Override
            public double residual(int i, Vector3D p) {
                return formula.evaluate(p, transmitter, used.get(i)) - measured.get(i);
            }
        };
        return LeastSquaresSolver.solve(startingPoints(transmitter, used, measured), problem);
    }

    /**
     * Starting points around the sensor centroid, at the larger of the sensor
     * spread and half the mean measured value (the target distance, when the
     * formula is range-like).
     */
    private static List<Vector3D> startingPoints(Transmitter transmitter, List<Receiver> receivers,
                                                 List<Double> measured) {
        Vector3D centroid = transmitter.getPosition();
        for (Receiver rx : receivers) {
            centroid = centroid.add(rx.getPosition());
        }
        centroid = centroid.scale(1.0 / (receivers.size() + 1));

        double spread = centroid.distance(transmitter.getPosition());
        for (Receiver rx : receivers) {
            spread = Math.max(spread, centroid.distance(rx.getPosition()));
        }
        double mean = 0;
        for (double m : measured) {
            mean += m;
        }
        mean /= measured.size();
        double scale = Math.max(Math.max(spread, Math.abs(mean) / 2), 1.0);
        return LeastSquaresSolver.startingPoints(centroid, scale);
    }
}
