package com.radar.simulator;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.model.BaselineLocalizationModel;
import com.radar.simulator.model.BaselineMeasurementModel;
import com.radar.simulator.model.ConstantVelocityTrajectory;
import com.radar.simulator.model.EstimationResult;
import com.radar.simulator.model.Measurement;
import com.radar.simulator.model.Quantity;
import com.radar.simulator.model.ReceiverMeasurement;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Command-line prototype of the Multi-Static Radar Simulator.
 * Runs a simple simulation step and prints the results without a GUI.
 */
public class CLISimulatorApp {
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println(" Multi-Static Radar Simulator - Prototype ");
        System.out.println("==========================================");
        System.out.println();

        // 1. Setup Environment
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));

        System.out.println("[SETUP] Initialized 1 Transmitter and " + receivers.size() + " Receivers.");

        // 2. Setup Target Drone
        Vector3D targetInitialPos = new Vector3D(1000, 2000, 100);
        Vector3D targetVelocity = new Vector3D(50, 0, 0);
        ConstantVelocityTrajectory targetTrajectory = new ConstantVelocityTrajectory(targetInitialPos, targetVelocity);

        System.out.println("[SETUP] Target Drone configured starting at: " + targetInitialPos + " with velocity: " + targetVelocity);
        System.out.println();

        // 3. Run Simulation Loop
        BaselineMeasurementModel measurementModel = new BaselineMeasurementModel();
        BaselineLocalizationModel localizationModel = new BaselineLocalizationModel();

        double simTime = 0.0;
        double timeStep = 1.0;
        int maxSteps = 5;

        for (int step = 1; step <= maxSteps; step++) {
            System.out.println("--- Step " + step + " (Time: " + simTime + "s) ---");
            
            // Move target
            Vector3D currentTargetPos = targetTrajectory.getPosition(simTime);
            System.out.println("Actual Target Position: " + currentTargetPos);

            // Generate Measurements
            Measurement measurement = measurementModel.generate(tx, receivers, currentTargetPos);
            
            // Show measurements
            for (ReceiverMeasurement rm : measurement.perReceiver()) {
                double range = rm.values().get(Quantity.BISTATIC_RANGE);
                double power = rm.values().get(Quantity.RECEIVED_POWER);
                System.out.printf("  RX%d Measured -> Range: %.2fm, Power: %.2e W\n", 
                                  rm.receiverId() + 1, range, power);
            }

            // Estimate Position
            EstimationResult result = localizationModel.estimate(tx, receivers, measurement);
            System.out.println("Estimated Target Position: " + result.position());
            
            // Calculate Error
            double error = currentTargetPos.distance(result.position());
            System.out.printf("Estimation Error: %.2fm (Status: %s)\n", error, result.status());
            System.out.println();

            simTime += timeStep;
        }

        System.out.println("==========================================");
        System.out.println(" Simulation Prototype Completed ");
        System.out.println("==========================================");
    }
}
