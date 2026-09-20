package com.radar.simulator.model;

import com.radar.simulator.core.RadarPhysics;
import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Baseline implementation of a measurement model.
 * Calculates bistatic range and simple received power.
 */
public class BaselineMeasurementModel implements MeasurementModel {
    private static final double SPEED_OF_LIGHT = 3e8;

    @Override
    public ModelDescriptor getDescriptor() {
        return new ModelDescriptor("BaselineMeasurement", "1.0");
    }

    @Override
    public Measurement generate(Transmitter transmitter, List<Receiver> receivers, Vector3D targetPosition) {
        List<ReceiverMeasurement> perReceiver = new ArrayList<>();

        // Distance from transmitter to target
        double distTxTarget = transmitter.getPosition().distance(targetPosition);

        for (int i = 0; i < receivers.size(); i++) {
            Receiver receiver = receivers.get(i);
            
            // Distance from target to receiver
            double distTargetRx = targetPosition.distance(receiver.getPosition());
            double bistaticRange = distTxTarget + distTargetRx;
            double timeDelay = bistaticRange / SPEED_OF_LIGHT;
            
            // Calculate a simplified received power for now using placeholder logic
            // Assuming power is simply proportional to inverse square of distance for now
            // To be replaced with actual radar equation
            double power = transmitter.getPower() / (bistaticRange * bistaticRange);

            Map<Quantity, Double> values = new HashMap<>();
            values.put(Quantity.BISTATIC_RANGE, bistaticRange);
            values.put(Quantity.TIME_DELAY, timeDelay);
            values.put(Quantity.RECEIVED_POWER, power);

            perReceiver.add(new ReceiverMeasurement(i, values));
        }

        // Time would typically come from the scenario or experiment runner
        return new Measurement(perReceiver, 0.0);
    }
}
