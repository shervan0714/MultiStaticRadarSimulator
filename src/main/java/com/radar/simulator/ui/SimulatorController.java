package com.radar.simulator.ui;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.animation.AnimationTimer;
import com.radar.simulator.core.*;
import com.radar.simulator.util.Vector3D;
import java.util.ArrayList;
import java.util.List;

/**
 * Main simulation controller that orchestrates the radar simulator.
 * Manages simulation loop, parameter updates, and UI synchronization.
 */
public class SimulatorController {
    private BorderPane root;
    
    // Simulation components
    private Transmitter transmitter;
    private List<Receiver> receivers;
    private Drone drone;
    
    // UI components
    private Visualization3D visualization;
    private ParameterPanel parameterPanel;
    private VBox statusPanel;
    
    // Simulation state
    private boolean simulationRunning = false;
    private double simulationTime = 0.0;
    private double deltaTime = 0.033;  // ~30 FPS
    
    private AnimationTimer animationTimer;

    public SimulatorController() {
        initializeSimulation();
        buildUI();
    }

    /**
     * Initialize simulation with default transmitter, receivers, and drone
     */
    private void initializeSimulation() {
        // Transmitter at origin
        transmitter = new Transmitter("TX1", 
            new Vector3D(0, 0, 1000),      // 1 km altitude
            10e9,                           // 10 GHz
            1.0                             // 1 Watt
        );

        // Three receivers in triangle pattern
        receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", 
            new Vector3D(10000, 0, 500), 10e9, 0));
        receivers.add(new Receiver("RX2", 
            new Vector3D(-10000, 0, 500), 10e9, 0));
        receivers.add(new Receiver("RX3", 
            new Vector3D(0, 10000, 500), 10e9, 0));

        // Drone moving with constant velocity
        drone = new Drone("DRONE1",
            new Vector3D(5000, 5000, 2000),   // Initial position (5km, 5km, 2km)
            new Vector3D(50, 0, 0)            // Velocity: 50 m/s in X direction
        );
    }

    /**
     * Build the UI layout
     */
    private void buildUI() {
        root = new BorderPane();
        
        // Create 3D visualization
        visualization = new Visualization3D(transmitter, receivers, drone);
        root.setCenter(visualization.getPane());
        
        // Create parameter control panel
        parameterPanel = new ParameterPanel(transmitter, receivers, drone, this);
        root.setRight(parameterPanel.getPane());
        
        // Create status display panel
        statusPanel = createStatusPanel();
        root.setBottom(statusPanel);
    }

    /**
     * Create status information display
     */
    private VBox createStatusPanel() {
        VBox panel = new VBox(5);
        panel.setStyle("-fx-border-color: #cccccc; -fx-padding: 10;");
        
        Label timeLabel = new Label("Time: 0.0s");
        Label positionLabel = new Label("Drone Position: (0, 0, 0)");
        Label errorLabel = new Label("Error: N/A");
        
        panel.getChildren().addAll(timeLabel, positionLabel, errorLabel);
        
        return panel;
    }

    /**
     * Start the simulation loop
     */
    public void startSimulation() {
        simulationRunning = true;
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                simulationStep();
            }
        };
        animationTimer.start();
    }

    /**
     * Stop the simulation
     */
    public void stopSimulation() {
        simulationRunning = false;
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    /**
     * Execute one simulation step
     */
    private void simulationStep() {
        if (!simulationRunning) return;

        // Update drone position
        drone.updatePosition(deltaTime);
        simulationTime += deltaTime;

        // Calculate received power at each receiver
        List<Double> powers = new ArrayList<>();
        for (Receiver rx : receivers) {
            double power = RadarPhysics.calculateReceivedPower(transmitter, drone, rx);
            powers.add(power);
        }

        // Triangulate drone position from received powers
        Vector3D calculatedPosition = RadarPhysics.triangulateDronePosition(receivers, powers);

        // Calculate error
        double error = RadarPhysics.calculateError(drone.getPosition(), calculatedPosition);

        // Update visualization
        visualization.updateSimulation(drone, calculatedPosition, powers, error);

        // Update status display
        updateStatusPanel(calculatedPosition, error);
    }

    /**
     * Update the status panel with current simulation data
     */
    private void updateStatusPanel(Vector3D calculatedPos, double error) {
        // Update labels with current values
        // This would typically update the UI components created in createStatusPanel()
    }

    // Getters
    public BorderPane getRoot() { return root; }
    public Transmitter getTransmitter() { return transmitter; }
    public List<Receiver> getReceivers() { return receivers; }
    public Drone getDrone() { return drone; }
    public Visualization3D getVisualization() { return visualization; }
}
