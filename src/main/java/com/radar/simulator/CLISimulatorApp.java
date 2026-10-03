package com.radar.simulator;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.model.*;
import com.radar.simulator.util.Vector3D;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line prototype of the Multi-Static Radar Simulator.
 *
 * Demonstrates the complete simulation pipeline:
 *   configuration → trajectory → measurement → localization → evaluation
 *
 * Supports optional scenario save/load and CSV export via command-line arguments:
 *   --save &lt;file&gt;    Save the scenario configuration
 *   --load &lt;file&gt;    Load a scenario configuration instead of defaults
 *   --export &lt;file&gt;  Export results to CSV
 *   --duration &lt;s&gt;   Simulation duration in seconds (default: 10)
 *   --timestep &lt;s&gt;   Time step in seconds (default: 1)
 *   --receivers &lt;n&gt;  Number of receivers: 3, 4, or 5 (default: 3)
 */
public class CLISimulatorApp {

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println(" Multi-Static Radar Simulator v1.0            ");
        System.out.println(" Phase 3 — Experiment Runner + Evaluation     ");
        System.out.println("==============================================");
        System.out.println();

        // Parse command-line arguments
        String saveFile = null;
        String loadFile = null;
        String exportFile = null;
        double duration = 10.0;
        double timeStep = 1.0;
        int receiverCount = 3;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--save":     saveFile = args[++i]; break;
                case "--load":     loadFile = args[++i]; break;
                case "--export":   exportFile = args[++i]; break;
                case "--duration": duration = Double.parseDouble(args[++i]); break;
                case "--timestep": timeStep = Double.parseDouble(args[++i]); break;
                case "--receivers": receiverCount = Integer.parseInt(args[++i]); break;
            }
        }

        // Build or load scenario
        Scenario scenario;
        ScenarioPersistence persistence = new ScenarioPersistence();

        if (loadFile != null) {
            System.out.println("[LOAD] Loading scenario from: " + loadFile);
            try {
                scenario = persistence.loadScenario(loadFile);
                double[] simParams = persistence.loadSimulationParameters(loadFile);
                duration = simParams[0];
                timeStep = simParams[1];
                System.out.println("[LOAD] Loaded successfully.");
            } catch (IOException e) {
                System.err.println("[ERROR] Failed to load scenario: " + e.getMessage());
                return;
            }
        } else {
            scenario = buildDefaultScenario(receiverCount);
        }

        // Print scenario summary
        printScenarioSummary(scenario, duration, timeStep);

        // Validate geometry
        GeometryValidator validator = new GeometryValidator();
        GeometryValidator.ValidationResult geoResult =
            validator.validate(scenario.getTransmitter(), scenario.getReceivers());
        System.out.println("[GEOMETRY] " + geoResult.message());
        if (!geoResult.isValid()) {
            System.err.println("[ERROR] Cannot proceed — geometry is invalid.");
            return;
        }
        System.out.println();

        // Save scenario if requested
        if (saveFile != null) {
            try {
                persistence.saveScenario(scenario, duration, timeStep, saveFile);
                System.out.println("[SAVE] Scenario saved to: " + saveFile);
            } catch (IOException e) {
                System.err.println("[WARN] Failed to save scenario: " + e.getMessage());
            }
        }

        // Run experiment
        MeasurementModel measModel = new BaselineMeasurementModel();
        LocalizationModel locModel = new BaselineLocalizationModel();
        ExperimentRunner runner = new ExperimentRunner(scenario, measModel, locModel);

        System.out.println("[RUN] Starting simulation...");
        System.out.println("  Models: " + measModel.getDescriptor() + " / " + locModel.getDescriptor());
        System.out.println();

        List<ExperimentRecord> records = runner.run(duration, timeStep);

        // Print per-step results
        System.out.println("=== Per-Step Results ===");
        System.out.printf("%-5s %-8s %-30s %-30s %-12s %-15s%n",
            "Step", "Time(s)", "Ground Truth (x,y,z)", "Estimate (x,y,z)", "Error(m)", "Status");
        System.out.println("-".repeat(105));

        for (ExperimentRecord record : records) {
            Vector3D gt = record.groundTruthPosition();
            Vector3D est = record.estimationResult().position();
            String errorStr = Double.isNaN(record.positionError())
                ? "N/A" : String.format("%.2f", record.positionError());

            System.out.printf("%-5d %-8.1f (%-8.1f, %-8.1f, %-8.1f)  (%-8.1f, %-8.1f, %-8.1f)  %-12s %-15s%n",
                record.stepIndex(),
                record.time(),
                gt.x, gt.y, gt.z,
                est.x, est.y, est.z,
                errorStr,
                record.estimationResult().status()
            );
        }

        // Evaluate
        EvaluationModule evalModule = new EvaluationModule();
        EvaluationResult evaluation = evalModule.evaluate(records);

        System.out.println();
        System.out.println("=== Evaluation Summary ===");
        System.out.printf("  Total Steps:      %d%n", evaluation.totalSteps());
        System.out.printf("  Successful Steps: %d%n", evaluation.successfulSteps());
        System.out.printf("  Failed Steps:     %d%n", evaluation.failedSteps());
        if (evaluation.successfulSteps() > 0) {
            System.out.printf("  Mean Error:       %.4f m%n", evaluation.meanError());
            System.out.printf("  Max Error:        %.4f m%n", evaluation.maxError());
            System.out.printf("  RMS Error:        %.4f m%n", evaluation.rmsError());
        }

        // Export results if requested
        if (exportFile != null) {
            try {
                persistence.exportResultsToCSV(records, exportFile);
                String summaryFile = exportFile.replace(".csv", "_summary.txt");
                persistence.exportEvaluationSummary(evaluation, summaryFile);
                System.out.println();
                System.out.println("[EXPORT] Results exported to: " + exportFile);
                System.out.println("[EXPORT] Summary exported to: " + summaryFile);
            } catch (IOException e) {
                System.err.println("[WARN] Failed to export results: " + e.getMessage());
            }
        }

        System.out.println();
        System.out.println("==============================================");
        System.out.println(" Simulation Complete                          ");
        System.out.println("==============================================");
    }

    /**
     * Build a default scenario with configurable receiver count.
     */
    private static Scenario buildDefaultScenario(int receiverCount) {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);

        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));

        if (receiverCount >= 4) {
            receivers.add(new Receiver("RX4", new Vector3D(-5000, -5000, 0), 2.4e9, 10.0));
        }
        if (receiverCount >= 5) {
            receivers.add(new Receiver("RX5", new Vector3D(0, 0, 5000), 2.4e9, 10.0));
        }

        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(
            new Vector3D(1000, 2000, 100),
            new Vector3D(50, 0, 0)
        );

        return new Scenario(tx, receivers, trajectory);
    }

    /**
     * Print a summary of the scenario configuration.
     */
    private static void printScenarioSummary(Scenario scenario, double duration, double timeStep) {
        Transmitter tx = scenario.getTransmitter();
        List<Receiver> receivers = scenario.getReceivers();

        System.out.println("[SETUP] Transmitter: " + tx);
        System.out.println("[SETUP] Receivers (" + receivers.size() + "):");
        for (Receiver rx : receivers) {
            System.out.println("  " + rx);
        }

        TrajectoryModel traj = scenario.getTargetTrajectory();
        System.out.println("[SETUP] Target trajectory:");
        System.out.println("  Initial position: " + traj.getPosition(0));
        System.out.println("  Velocity:         " + traj.getVelocity(0));
        System.out.printf("[SETUP] Duration: %.1fs, Time step: %.2fs%n", duration, timeStep);
    }
}
