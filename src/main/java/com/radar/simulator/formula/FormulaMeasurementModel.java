package com.radar.simulator.formula;

import com.radar.simulator.core.ModelDescriptor;
import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.measurement.BaselineMeasurementModel;
import com.radar.simulator.measurement.Measurement;
import com.radar.simulator.measurement.MeasurementModel;
import com.radar.simulator.measurement.Quantity;
import com.radar.simulator.measurement.ReceiverMeasurement;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Measurement model whose measured range comes from a user formula.
 *
 * For each receiver, BISTATIC_RANGE = formula value (+ seeded Gaussian noise,
 * as in the baseline) and TIME_DELAY = that value / c. Received power and
 * the angles are taken from the noiseless baseline model, so they stay
 * available.
 */
public class FormulaMeasurementModel implements MeasurementModel {

    private final CustomFormula formula;
    private final double rangeNoiseStd;
    private final long seed;
    private final Random random;
    private final BaselineMeasurementModel others = new BaselineMeasurementModel();

    /**
     * @throws IllegalArgumentException if the noise standard deviation is negative or not finite
     */
    public FormulaMeasurementModel(CustomFormula formula, double rangeNoiseStd, long seed) {
        if (!(rangeNoiseStd >= 0) || !Double.isFinite(rangeNoiseStd)) {
            throw new IllegalArgumentException("Range noise standard deviation must be >= 0");
        }
        this.formula = formula;
        this.rangeNoiseStd = rangeNoiseStd;
        this.seed = seed;
        this.random = new Random(seed);
    }

    @Override
    public ModelDescriptor getDescriptor() {
        String version = formula.describe();
        if (rangeNoiseStd > 0) {
            version += String.format(" noise=%sm seed=%d", rangeNoiseStd, seed);
        }
        return new ModelDescriptor("Formula: " + formula.name(), version);
    }

    @Override
    public Measurement generate(Transmitter transmitter, List<Receiver> receivers, Vector3D targetPosition) {
        Measurement base = others.generate(transmitter, receivers, targetPosition);
        List<ReceiverMeasurement> perReceiver = new ArrayList<>();
        for (int i = 0; i < receivers.size(); i++) {
            double value = formula.evaluate(targetPosition, transmitter, receivers.get(i));
            if (rangeNoiseStd > 0) {
                value += random.nextGaussian() * rangeNoiseStd;
            }
            Map<Quantity, Double> values = new HashMap<>(base.perReceiver().get(i).values());
            values.put(Quantity.BISTATIC_RANGE, value);
            values.put(Quantity.TIME_DELAY, value / CustomFormula.SPEED_OF_LIGHT);
            perReceiver.add(new ReceiverMeasurement(receivers.get(i).getId(), values));
        }
        return new Measurement(perReceiver, 0.0);
    }
}
