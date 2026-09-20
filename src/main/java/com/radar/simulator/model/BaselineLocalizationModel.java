package com.radar.simulator.model;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Vector3D;

import java.util.List;

/**
 * Baseline implementation of a localization model.
 * Performs a very simplified weighted average triangulation based on power.
 */
public class BaselineLocalizationModel implements LocalizationModel {

    @Override
    public ModelDescriptor getDescriptor() {
        return new ModelDescriptor("BaselineLocalization", "1.0");
    }

    @Override
    public EstimationResult estimate(Transmitter transmitter, List<Receiver> receivers, Measurement measurement) {
        if (receivers.size() < 3 || measurement.perReceiver().size() < 3) {
            return new EstimationResult(new Vector3D(0, 0, 0), Status.UNDER_CONSTRAINED, 0.0, 0.0);
        }

        Vector3D estimatedPosition = new Vector3D(0, 0, 0);
        double totalWeight = 0;

        for (ReceiverMeasurement rm : measurement.perReceiver()) {
            if (rm.receiverId() >= 0 && rm.receiverId() < receivers.size()) {
                Receiver rx = receivers.get(rm.receiverId());
                Double power = rm.values().get(Quantity.RECEIVED_POWER);
                
                if (power != null && power > 0) {
                    // Simple placeholder logic: closer receivers have higher power
                    double weight = power;
                    totalWeight += weight;
                    estimatedPosition = estimatedPosition.add(rx.getPosition().scale(weight));
                }
            }
        }

        if (totalWeight <= 0) {
            return new EstimationResult(new Vector3D(0, 0, 0), Status.NUMERICALLY_UNSTABLE, 0.0, 0.0);
        }

        estimatedPosition = estimatedPosition.scale(1.0 / totalWeight);

        // Calculate a simple residual (sum of squared errors from expected ranges) - placeholder
        double residual = 0.0; 

        return new EstimationResult(estimatedPosition, Status.SUCCESS, residual, 1.0);
    }
}
