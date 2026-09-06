package com.radar.simulator.core;

import com.radar.simulator.util.Vector3D;
import java.util.List;

/**
 * Physics engine for radar signal calculations.
 * Implements power calculations and drone position triangulation.
 */
public class RadarPhysics {
    
    // Physical constants
    private static final double SPEED_OF_LIGHT = 3e8;  // m/s
    private static final double PI = Math.PI;

    /**
     * Calculate received power using Friis transmission equation (placeholder).
     * P_r = (P_t * G_t * G_r * λ^2) / (4π)^2 / R_tx^2 / R_rx^2
     * 
     * This is a placeholder using simplified radar equation.
     * To be replaced with actual power calculation formula from Girdhar sir.
     * 
     * @param transmitter Transmitter station
     * @param drone Moving drone target
     * @param receiver Receiver station
     * @return Received power in Watts
     */
    public static double calculateReceivedPower(Transmitter transmitter, 
                                               Drone drone, 
                                               Receiver receiver) {
        // Distance from transmitter to drone
        double distTxDrone = transmitter.getPosition().distance(drone.getPosition());
        
        // Distance from drone to receiver
        double distDroneRx = drone.getPosition().distance(receiver.getPosition());
        
        // Total distance
        double totalDistance = distTxDrone + distDroneRx;
        
        // Wavelength λ = c / f
        double wavelength = SPEED_OF_LIGHT / transmitter.getFrequency();
        
        // Simplified Friis transmission equation (placeholder)
        // This MUST be updated with actual radar equation when available
        double numerator = transmitter.getPower() * wavelength * wavelength;
        double denominator = 64 * PI * PI * totalDistance * totalDistance;
        
        double receivedPower = numerator / denominator;
        
        return receivedPower;
    }

    /**
     * Triangulate drone position from received power at multiple receivers.
     * Uses least-squares fitting (placeholder algorithm).
     * 
     * @param receivers List of receiver stations
     * @param powers List of received power values (one per receiver)
     * @return Estimated drone position
     */
    public static Vector3D triangulateDronePosition(List<Receiver> receivers, 
                                                    List<Double> powers) {
        if (receivers.size() != powers.size() || receivers.size() < 3) {
            throw new IllegalArgumentException(
                "Need at least 3 receivers with corresponding power measurements");
        }

        // Placeholder: use average of receiver positions weighted by inverse power
        // This MUST be replaced with proper triangulation algorithm
        Vector3D estimatedPosition = new Vector3D(0, 0, 0);
        double totalWeight = 0;

        for (int i = 0; i < receivers.size(); i++) {
            Receiver rx = receivers.get(i);
            double power = powers.get(i);
            
            // Avoid division by zero
            double weight = power > 0 ? 1.0 / power : 0;
            totalWeight += weight;
            
            Vector3D weighted = rx.getPosition().scale(weight);
            estimatedPosition = estimatedPosition.add(weighted);
        }

        if (totalWeight > 0) {
            estimatedPosition = estimatedPosition.scale(1.0 / totalWeight);
        }

        return estimatedPosition;
    }

    /**
     * Calculate error between actual and calculated drone position.
     * 
     * @param actual Actual drone position
     * @param calculated Calculated drone position from triangulation
     * @return Euclidean distance error in meters
     */
    public static double calculateError(Vector3D actual, Vector3D calculated) {
        return actual.distance(calculated);
    }

    /**
     * Convert power from Watts to dBm (decibel-milliwatts)
     * 
     * @param powerWatts Power in Watts
     * @return Power in dBm
     */
    public static double powerToDBm(double powerWatts) {
        return 10 * Math.log10(powerWatts * 1000);
    }

    /**
     * Convert power from dBm to Watts
     * 
     * @param powerDBm Power in dBm
     * @return Power in Watts
     */
    public static double powerFromDBm(double powerDBm) {
        return Math.pow(10, powerDBm / 10) / 1000;
    }
}
