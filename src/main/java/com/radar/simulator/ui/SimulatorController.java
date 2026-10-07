package com.radar.simulator.ui;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.animation.AnimationTimer;
import com.radar.simulator.core.*;
import com.radar.simulator.model.*;
import com.radar.simulator.util.Vector3D;
import java.util.ArrayList;
import java.util.List;

/**
 * Main simulation controller that orchestrates the radar simulator.
 * Manages simulation loop, parameter updates, and UI synchronization.
 *
 * Uses the modular model pipeline (MeasurementModel → LocalizationModel)
 * so that different formulations can be swapped without changing the
 * controller logic.
 */
public class SimulatorController {
    private BorderPane root;
    
    // Simulation components
    private Transmitter transmitter;
    private List<Receiver> receivers;
    private Drone drone;
    
    // Model pipeline (replaceable formulations)
    private MeasurementModel measurementModel;
    private LocalizationModel localizationModel;
    private GeometryValidator geometryValidator;
    
    // UI components
    private Visualization3D visualization;
    private ParameterPanel parameterPanel;
    private VBox statusPanel;
    
    // Status labels (kept as fields so simulationStep can update them)
    private Label timeLabel;
    private Label positionLabel;
    private Label estimateLabel;
    private Label errorLabel;
    private Label powerLabel;
    private Label statusLabel;
    
    // Simulation state
    private boolean simulationRunning = false;
    private double simulationTime = 0.0;
    private double deltaTime = 0.033;  // ~30 FPS
    
    // Tracking for evaluation
    private List<Double> errorHistory;
    private int stepCount;
    
    private AnimationTimer animationTimer;

    public SimulatorController() {
        initializeSimulation();
        buildUI();
    }

    /**
     * Initialize simulation with default transmitter, receivers, drone,
     * and baseline models.
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
        
        // Baseline models (replaceable)
        measurementModel = new BaselineMeasurementModel();
        localizationModel = new BaselineLocalizationModel();
        geometryValidator = new GeometryValidator();
        
        // Evaluation tracking
        errorHistory = new ArrayList<>();
        stepCount = 0;
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
     * Create status information display with real-time updating labels.
     */
    private VBox createStatusPanel() {
        VBox panel = new VBox(5);
        panel.setStyle("-fx-border-color: #cccccc; -fx-padding: 10;");
        
        timeLabel = new Label("Time: 0.0s");
        positionLabel = new Label("Actual Position: (0, 0, 0)");
        estimateLabel = new Label("Estimated Position: (0, 0, 0)");
        errorLabel = new Label("Error: N/A | Mean: N/A | Max: N/A | RMS: N/A");
        powerLabel = new Label("Power: N/A");
        statusLabel = new Label("Status: Ready | Model: " + 
            measurementModel.getDescriptor().name() + "/" +
            localizationModel.getDescriptor().name());
        
        panel.getChildren().addAll(
            timeLabel, positionLabel, estimateLabel, 
            errorLabel, powerLabel, statusLabel
        );
        
        return panel;
    }

    /**
     * Start the simulation loop.
     * Validates geometry before starting.
     */
    public void startSimulation() {
        // Validate geometry before starting
        GeometryValidator.ValidationResult geoResult = 
            geometryValidator.validate(transmitter, receivers);
        if (!geoResult.isValid()) {
            statusLabel.setText("Status: CANNOT START — " + geoResult.message());
            return;
        }
        
        simulationRunning = true;
        statusLabel.setText("Status: Running | Model: " +
            measurementModel.getDescriptor().name() + "/" +
            localizationModel.getDescriptor().name() +
            (geoResult.hasWarnings() ? " | WARNING: " + String.join("; ", geoResult.warnings()) : ""));
        
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
        statusLabel.setText("Status: Stopped at t=" + 
            String.format("%.1f", simulationTime) + "s | Steps: " + stepCount);
    }

    /**
     * Execute one simulation step using the modular model pipeline.
     *
     * Pipeline: position update → measurement generation → localization →
     *           error computation → visualization update → status update
     */
    private void simulationStep() {
        if (!simulationRunning) return;

        // Update drone position
        drone.updatePosition(deltaTime);
        simulationTime += deltaTime;
        stepCount++;

        Vector3D actualPosition = drone.getPosition();

        // Generate measurements using the measurement model
        Measurement measurement = measurementModel.generate(
            transmitter, receivers, actualPosition);

        // Estimate position using the localization model
        EstimationResult estimation = localizationModel.estimate(
            transmitter, receivers, measurement);

        Vector3D calculatedPosition = estimation.position();

        // Calculate error
        double error = 0;
        if (estimation.status() == Status.SUCCESS) {
            error = actualPosition.distance(calculatedPosition);
            errorHistory.add(error);
        }

        // Extract power values for visualization
        List<Double> powers = new ArrayList<>();
        for (ReceiverMeasurement rm : measurement.perReceiver()) {
            Double power = rm.values().get(Quantity.RECEIVED_POWER);
            powers.add(power != null ? power : 0.0);
        }

        // Update visualization
        visualization.updateSimulation(drone, calculatedPosition, powers, error);

        // Update status display with live statistics
        updateStatusPanel(actualPosition, calculatedPosition, estimation, powers, error);
    }

    /**
     * Update the status panel with current simulation data including
     * live aggregate error statistics.
     */
    private void updateStatusPanel(Vector3D actualPos, Vector3D estimatedPos,
                                    EstimationResult estimation,
                                    List<Double> powers, double error) {
        timeLabel.setText(String.format("Time: %.1fs | Step: %d", simulationTime, stepCount));
        
        positionLabel.setText(String.format("Actual Position: (%.1f, %.1f, %.1f)",
            actualPos.x, actualPos.y, actualPos.z));
        
        estimateLabel.setText(String.format("Estimated Position: (%.1f, %.1f, %.1f) [%s]",
            estimatedPos.x, estimatedPos.y, estimatedPos.z, estimation.status()));

        // Compute live aggregate stats
        if (!errorHistory.isEmpty()) {
            double sum = 0, sumSq = 0, max = 0;
            for (double e : errorHistory) {
                sum += e;
                sumSq += e * e;
                if (e > max) max = e;
            }
            double mean = sum / errorHistory.size();
            double rms = Math.sqrt(sumSq / errorHistory.size());
            
            errorLabel.setText(String.format(
                "Error: %.1fm | Mean: %.1fm | Max: %.1fm | RMS: %.1fm",
                error, mean, max, rms));
        } else {
            errorLabel.setText(String.format("Error: %.1fm | Mean: N/A | Max: N/A | RMS: N/A", error));
        }

        // Power display
        StringBuilder powerStr = new StringBuilder("Power: ");
        for (int i = 0; i < powers.size(); i++) {
            double dbm = RadarPhysics.powerToDBm(powers.get(i));
            powerStr.append(String.format("RX%d: %.1f dBm  ", i + 1, dbm));
        }
        powerLabel.setText(powerStr.toString());
    }

    // Getters
    public BorderPane getRoot() { return root; }
    public Transmitter getTransmitter() { return transmitter; }
    public List<Receiver> getReceivers() { return receivers; }
    public Drone getDrone() { return drone; }
    public Visualization3D getVisualization() { return visualization; }
}
