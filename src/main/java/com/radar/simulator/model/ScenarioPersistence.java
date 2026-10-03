package com.radar.simulator.model;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Vector3D;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Persistence module for saving and loading scenario configurations
 * and experiment results.
 *
 * Uses a simple text-based format (not a full JSON library dependency)
 * to keep the project lightweight. Each scenario file stores:
 * <ul>
 *   <li>Transmitter configuration (position, frequency, power, orientation)</li>
 *   <li>Receiver configurations (position, frequency, gain, orientation)</li>
 *   <li>Trajectory parameters (initial position, velocity)</li>
 *   <li>Simulation parameters (duration, time step)</li>
 * </ul>
 *
 * Experiment results can be exported to CSV for downstream analysis.
 */
public class ScenarioPersistence {

    /**
     * Save a scenario configuration to a properties-style text file.
     *
     * @param scenario the scenario to save
     * @param duration simulation duration
     * @param timeStep simulation time step
     * @param filePath path to save to
     * @throws IOException if writing fails
     */
    public void saveScenario(Scenario scenario, double duration, double timeStep,
                             String filePath) throws IOException {
        Properties props = new Properties();

        // Transmitter
        Transmitter tx = scenario.getTransmitter();
        props.setProperty("transmitter.id", tx.getId());
        props.setProperty("transmitter.position.x", String.valueOf(tx.getPosition().x));
        props.setProperty("transmitter.position.y", String.valueOf(tx.getPosition().y));
        props.setProperty("transmitter.position.z", String.valueOf(tx.getPosition().z));
        props.setProperty("transmitter.frequency", String.valueOf(tx.getFrequency()));
        props.setProperty("transmitter.power", String.valueOf(tx.getPower()));
        props.setProperty("transmitter.orientation.x", String.valueOf(tx.getOrientation().x));
        props.setProperty("transmitter.orientation.y", String.valueOf(tx.getOrientation().y));
        props.setProperty("transmitter.orientation.z", String.valueOf(tx.getOrientation().z));

        // Receivers
        List<Receiver> receivers = scenario.getReceivers();
        props.setProperty("receiver.count", String.valueOf(receivers.size()));
        for (int i = 0; i < receivers.size(); i++) {
            Receiver rx = receivers.get(i);
            String prefix = "receiver." + i + ".";
            props.setProperty(prefix + "id", rx.getId());
            props.setProperty(prefix + "position.x", String.valueOf(rx.getPosition().x));
            props.setProperty(prefix + "position.y", String.valueOf(rx.getPosition().y));
            props.setProperty(prefix + "position.z", String.valueOf(rx.getPosition().z));
            props.setProperty(prefix + "frequency", String.valueOf(rx.getFrequency()));
            props.setProperty(prefix + "gain", String.valueOf(rx.getGain()));
            props.setProperty(prefix + "orientation.x", String.valueOf(rx.getOrientation().x));
            props.setProperty(prefix + "orientation.y", String.valueOf(rx.getOrientation().y));
            props.setProperty(prefix + "orientation.z", String.valueOf(rx.getOrientation().z));
        }

        // Trajectory (constant velocity)
        TrajectoryModel trajectory = scenario.getTargetTrajectory();
        if (trajectory instanceof ConstantVelocityTrajectory) {
            props.setProperty("trajectory.type", "ConstantVelocity");
            Vector3D initPos = trajectory.getPosition(0);
            Vector3D velocity = trajectory.getVelocity(0);
            props.setProperty("trajectory.initialPosition.x", String.valueOf(initPos.x));
            props.setProperty("trajectory.initialPosition.y", String.valueOf(initPos.y));
            props.setProperty("trajectory.initialPosition.z", String.valueOf(initPos.z));
            props.setProperty("trajectory.velocity.x", String.valueOf(velocity.x));
            props.setProperty("trajectory.velocity.y", String.valueOf(velocity.y));
            props.setProperty("trajectory.velocity.z", String.valueOf(velocity.z));
        }

        // Simulation parameters
        props.setProperty("simulation.duration", String.valueOf(duration));
        props.setProperty("simulation.timeStep", String.valueOf(timeStep));

        try (OutputStream out = Files.newOutputStream(Paths.get(filePath))) {
            props.store(out, "Multi-Static Radar Simulator - Scenario Configuration");
        }
    }

    /**
     * Load a scenario configuration from a properties-style text file.
     *
     * @param filePath path to load from
     * @return the loaded scenario
     * @throws IOException if reading fails or format is invalid
     */
    public Scenario loadScenario(String filePath) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(Paths.get(filePath))) {
            props.load(in);
        }

        // Transmitter
        Transmitter tx = new Transmitter(
            props.getProperty("transmitter.id", "TX1"),
            new Vector3D(
                Double.parseDouble(props.getProperty("transmitter.position.x", "0")),
                Double.parseDouble(props.getProperty("transmitter.position.y", "0")),
                Double.parseDouble(props.getProperty("transmitter.position.z", "0"))
            ),
            Double.parseDouble(props.getProperty("transmitter.frequency", "10e9")),
            Double.parseDouble(props.getProperty("transmitter.power", "1.0"))
        );

        String oxStr = props.getProperty("transmitter.orientation.x");
        if (oxStr != null) {
            tx.setOrientation(new Vector3D(
                Double.parseDouble(oxStr),
                Double.parseDouble(props.getProperty("transmitter.orientation.y", "0")),
                Double.parseDouble(props.getProperty("transmitter.orientation.z", "0"))
            ));
        }

        // Receivers
        int rxCount = Integer.parseInt(props.getProperty("receiver.count", "0"));
        List<Receiver> receivers = new ArrayList<>();
        for (int i = 0; i < rxCount; i++) {
            String prefix = "receiver." + i + ".";
            Receiver rx = new Receiver(
                props.getProperty(prefix + "id", "RX" + (i + 1)),
                new Vector3D(
                    Double.parseDouble(props.getProperty(prefix + "position.x", "0")),
                    Double.parseDouble(props.getProperty(prefix + "position.y", "0")),
                    Double.parseDouble(props.getProperty(prefix + "position.z", "0"))
                ),
                Double.parseDouble(props.getProperty(prefix + "frequency", "10e9")),
                Double.parseDouble(props.getProperty(prefix + "gain", "0"))
            );

            String roxStr = props.getProperty(prefix + "orientation.x");
            if (roxStr != null) {
                rx.setOrientation(new Vector3D(
                    Double.parseDouble(roxStr),
                    Double.parseDouble(props.getProperty(prefix + "orientation.y", "0")),
                    Double.parseDouble(props.getProperty(prefix + "orientation.z", "0"))
                ));
            }

            receivers.add(rx);
        }

        // Trajectory
        TrajectoryModel trajectory;
        String trajectoryType = props.getProperty("trajectory.type", "ConstantVelocity");
        if ("ConstantVelocity".equals(trajectoryType)) {
            Vector3D initPos = new Vector3D(
                Double.parseDouble(props.getProperty("trajectory.initialPosition.x", "0")),
                Double.parseDouble(props.getProperty("trajectory.initialPosition.y", "0")),
                Double.parseDouble(props.getProperty("trajectory.initialPosition.z", "0"))
            );
            Vector3D velocity = new Vector3D(
                Double.parseDouble(props.getProperty("trajectory.velocity.x", "0")),
                Double.parseDouble(props.getProperty("trajectory.velocity.y", "0")),
                Double.parseDouble(props.getProperty("trajectory.velocity.z", "0"))
            );
            trajectory = new ConstantVelocityTrajectory(initPos, velocity);
        } else {
            throw new IOException("Unknown trajectory type: " + trajectoryType);
        }

        return new Scenario(tx, receivers, trajectory);
    }

    /**
     * Read simulation parameters from a saved scenario file.
     *
     * @param filePath path to the scenario file
     * @return array of [duration, timeStep]
     * @throws IOException if reading fails
     */
    public double[] loadSimulationParameters(String filePath) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(Paths.get(filePath))) {
            props.load(in);
        }
        double duration = Double.parseDouble(props.getProperty("simulation.duration", "10.0"));
        double timeStep = Double.parseDouble(props.getProperty("simulation.timeStep", "1.0"));
        return new double[]{duration, timeStep};
    }

    /**
     * Export experiment results to CSV for downstream analysis.
     * Columns: step, time, gt_x, gt_y, gt_z, est_x, est_y, est_z, error, status
     *
     * @param records   list of experiment records
     * @param filePath  path to the output CSV file
     * @throws IOException if writing fails
     */
    public void exportResultsToCSV(List<ExperimentRecord> records, String filePath)
            throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(filePath)))) {
            // Header
            writer.println("step,time,gt_x,gt_y,gt_z,est_x,est_y,est_z,error,status,"
                + "meas_model,loc_model");

            for (ExperimentRecord record : records) {
                Vector3D gt = record.groundTruthPosition();
                Vector3D est = record.estimationResult().position();
                writer.printf("%d,%.6f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%s,%s,%s%n",
                    record.stepIndex(),
                    record.time(),
                    gt.x, gt.y, gt.z,
                    est.x, est.y, est.z,
                    Double.isNaN(record.positionError()) ? -1.0 : record.positionError(),
                    record.estimationResult().status(),
                    record.measurementModelDescriptor().name() + "_" +
                        record.measurementModelDescriptor().version(),
                    record.localizationModelDescriptor().name() + "_" +
                        record.localizationModelDescriptor().version()
                );
            }
        }
    }

    /**
     * Export an evaluation summary alongside experiment results.
     *
     * @param evaluation evaluation result
     * @param filePath   path to the output file
     * @throws IOException if writing fails
     */
    public void exportEvaluationSummary(EvaluationResult evaluation, String filePath)
            throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(filePath)))) {
            writer.println("=== Evaluation Summary ===");
            writer.printf("Total Steps:      %d%n", evaluation.totalSteps());
            writer.printf("Successful Steps: %d%n", evaluation.successfulSteps());
            writer.printf("Failed Steps:     %d%n", evaluation.failedSteps());
            writer.printf("Mean Error:       %.4f m%n", evaluation.meanError());
            writer.printf("Max Error:        %.4f m%n", evaluation.maxError());
            writer.printf("RMS Error:        %.4f m%n", evaluation.rmsError());
        }
    }
}
