package com.radar.simulator.ui;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.model.ConstantVelocityTrajectory;
import com.radar.simulator.model.Scenario;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Plain-data form of everything the user configures in the GUI.
 *
 * Keeps the UI-to-simulation mapping free of JavaFX so it can be unit
 * tested: the editor reads its fields into a ScenarioConfig, and
 * {@link #toScenario()} checks the values and builds the Scenario.
 *
 * @param transmitterPosition transmitter position (m, ENU)
 * @param frequencyHz         carrier frequency shared by TX and receivers
 * @param transmitPowerW      transmit power in watts
 * @param receivers           receiver positions, IDs and gains
 * @param initialPosition     drone position at t = 0 (m)
 * @param velocity            constant drone velocity (m/s)
 * @param duration            simulated time span (s)
 * @param timeStep            time between samples (s)
 */
public record ScenarioConfig(
    Vector3D transmitterPosition,
    double frequencyHz,
    double transmitPowerW,
    List<ReceiverConfig> receivers,
    Vector3D initialPosition,
    Vector3D velocity,
    double duration,
    double timeStep
) {

    /** One receiver row in the editor. */
    public record ReceiverConfig(String id, double x, double y, double z, double gainDbi) {}

    public ScenarioConfig {
        receivers = List.copyOf(receivers);
    }

    /**
     * The scenario the GUI opens with: a ground-level transmitter, a square
     * of receivers with one raised receiver, and a drone crossing at 300 m.
     */
    public static ScenarioConfig defaultConfig() {
        return new ScenarioConfig(
            new Vector3D(0, 0, 0), 2.4e9, 1000.0,
            List.of(
                new ReceiverConfig("RX1", 5000, 5000, 0, 10),
                new ReceiverConfig("RX2", -5000, 5000, 0, 10),
                new ReceiverConfig("RX3", -5000, -5000, 0, 10),
                new ReceiverConfig("RX4", 5000, -5000, 400, 10)
            ),
            new Vector3D(-3000, -2000, 300),
            new Vector3D(60, 40, 5),
            100.0, 1.0
        );
    }

    /**
     * Validate the values and build the simulation scenario.
     *
     * @throws IllegalArgumentException with a user-readable message if a
     *         value is out of range
     */
    public Scenario toScenario() {
        requireFinite(transmitterPosition, "Transmitter position");
        requireFinite(initialPosition, "Drone initial position");
        requireFinite(velocity, "Drone velocity");
        requirePositive(frequencyHz, "Frequency");
        requirePositive(transmitPowerW, "Transmit power");
        requirePositive(duration, "Duration");
        requirePositive(timeStep, "Time step");
        if (timeStep > duration) {
            throw new IllegalArgumentException("Time step must not exceed the duration");
        }

        Set<String> ids = new HashSet<>();
        List<Receiver> rxList = new ArrayList<>();
        for (ReceiverConfig rc : receivers) {
            String id = rc.id() == null ? "" : rc.id().trim();
            if (id.isEmpty()) {
                throw new IllegalArgumentException("Every receiver needs an ID");
            }
            if (!ids.add(id)) {
                throw new IllegalArgumentException("Duplicate receiver ID: " + id);
            }
            Vector3D position = new Vector3D(rc.x(), rc.y(), rc.z());
            requireFinite(position, "Position of " + id);
            if (!Double.isFinite(rc.gainDbi())) {
                throw new IllegalArgumentException("Gain of " + id + " must be a number");
            }
            rxList.add(new Receiver(id, position, frequencyHz, rc.gainDbi()));
        }

        Transmitter tx = new Transmitter("TX1", transmitterPosition, frequencyHz, transmitPowerW);
        return new Scenario(tx, rxList, new ConstantVelocityTrajectory(initialPosition, velocity));
    }

    /**
     * Build the editor form from an existing scenario, e.g. one loaded from disk.
     */
    public static ScenarioConfig fromScenario(Scenario scenario, double duration, double timeStep) {
        Transmitter tx = scenario.getTransmitter();
        List<ReceiverConfig> rows = new ArrayList<>();
        for (Receiver rx : scenario.getReceivers()) {
            Vector3D p = rx.getPosition();
            rows.add(new ReceiverConfig(rx.getId(), p.x, p.y, p.z, rx.getGain()));
        }
        return new ScenarioConfig(
            tx.getPosition(), tx.getFrequency(), tx.getPower(), rows,
            scenario.getTargetTrajectory().getPosition(0),
            scenario.getTargetTrajectory().getVelocity(0),
            duration, timeStep
        );
    }

    private static void requireFinite(Vector3D v, String name) {
        if (!Double.isFinite(v.x) || !Double.isFinite(v.y) || !Double.isFinite(v.z)) {
            throw new IllegalArgumentException(name + " must be finite numbers");
        }
    }

    private static void requirePositive(double value, String name) {
        if (!(value > 0) || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be a positive number");
        }
    }
}
