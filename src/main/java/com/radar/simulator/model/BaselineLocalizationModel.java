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
        // Break Z-axis symmetry in case all receivers are coplanar
        estimatedPosition = new Vector3D(estimatedPosition.x, estimatedPosition.y, 500.0);

        // Adaptive Gradient Descent to minimize bistatic range residual
        double step = 100.0;
        for (int iter = 0; iter < 1000; iter++) {
            double r0 = computeResidual(estimatedPosition, transmitter, receivers, measurement);
            double eps = 1.0;
            double rX = computeResidual(estimatedPosition.add(new Vector3D(eps, 0, 0)), transmitter, receivers, measurement);
            double rY = computeResidual(estimatedPosition.add(new Vector3D(0, eps, 0)), transmitter, receivers, measurement);
            double rZ = computeResidual(estimatedPosition.add(new Vector3D(0, 0, eps)), transmitter, receivers, measurement);
            
            double dx = (rX - r0) / eps;
            double dy = (rY - r0) / eps;
            double dz = (rZ - r0) / eps;
            Vector3D grad = new Vector3D(dx, dy, dz);
            
            double gradMag = grad.magnitude();
            if (gradMag < 1e-6) break;
            grad = grad.scale(1.0 / gradMag);
            
            Vector3D nextPos = estimatedPosition.subtract(grad.scale(step));
            double nextR = computeResidual(nextPos, transmitter, receivers, measurement);
            
            if (nextR < r0) {
                estimatedPosition = nextPos;
                step *= 1.1;
            } else {
                step *= 0.5;
            }
            if (step < 1e-4) break;
        }

        double finalResidual = computeResidual(estimatedPosition, transmitter, receivers, measurement);
        return new EstimationResult(estimatedPosition, Status.SUCCESS, finalResidual, 1.0);
    }

    private double computeResidual(Vector3D pos, Transmitter transmitter, List<Receiver> receivers, Measurement measurement) {
        double residual = 0.0; 
        for (ReceiverMeasurement rm : measurement.perReceiver()) {
            if (rm.receiverId() >= 0 && rm.receiverId() < receivers.size()) {
                Receiver rx = receivers.get(rm.receiverId());
                Double measuredRange = rm.values().get(Quantity.BISTATIC_RANGE);
                if (measuredRange != null) {
                    double distTx = transmitter.getPosition().distance(pos);
                    double distRx = rx.getPosition().distance(pos);
                    double expectedRange = distTx + distRx;
                    residual += Math.pow(measuredRange - expectedRange, 2);
                }
            }
        }
        return residual;
    }
}
