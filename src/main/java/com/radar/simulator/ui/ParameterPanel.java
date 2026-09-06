package com.radar.simulator.ui;

import javafx.scene.layout.VBox;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import com.radar.simulator.core.*;

/**
 * Parameter adjustment panel for real-time simulator control.
 * Allows users to modify transmitter, receiver, and drone parameters during simulation.
 */
public class ParameterPanel {
    private VBox root;
    private Transmitter transmitter;
    private java.util.List<Receiver> receivers;
    private Drone drone;
    private SimulatorController controller;

    public ParameterPanel(Transmitter tx, java.util.List<Receiver> rxList, 
                         Drone drn, SimulatorController ctrl) {
        this.transmitter = tx;
        this.receivers = rxList;
        this.drone = drn;
        this.controller = ctrl;
        
        root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-border-color: #cccccc;");
        
        buildPanel();
    }

    /**
     * Build the parameter control panel
     */
    private void buildPanel() {
        // Transmitter controls
        TitledPane txPane = createTransmitterControls();
        root.getChildren().add(txPane);

        // Receiver controls
        TitledPane rxPane = createReceiverControls();
        root.getChildren().add(rxPane);

        // Drone controls
        TitledPane dronePane = createDroneControls();
        root.getChildren().add(dronePane);

        // Simulation controls
        TitledPane simPane = createSimulationControls();
        root.getChildren().add(simPane);
    }

    /**
     * Create transmitter parameter controls
     */
    private TitledPane createTransmitterControls() {
        VBox txBox = new VBox(5);
        txBox.setPadding(new Insets(5));

        Label posLabel = new Label("Transmitter Position (m)");
        Slider txXSlider = new Slider(-20000, 20000, transmitter.getPosition().x);
        Slider txYSlider = new Slider(-20000, 20000, transmitter.getPosition().y);
        Slider txZSlider = new Slider(0, 5000, transmitter.getPosition().z);

        txBox.getChildren().addAll(
            posLabel,
            new Label("X:"), txXSlider,
            new Label("Y:"), txYSlider,
            new Label("Z:"), txZSlider
        );

        TitledPane pane = new TitledPane("Transmitter", txBox);
        pane.setCollapsible(false);
        return pane;
    }

    /**
     * Create receiver parameter controls
     */
    private TitledPane createReceiverControls() {
        VBox rxBox = new VBox(5);
        rxBox.setPadding(new Insets(5));
        rxBox.getChildren().add(new Label("Receiver controls would go here"));
        
        TitledPane pane = new TitledPane("Receivers", rxBox);
        pane.setCollapsible(false);
        return pane;
    }

    /**
     * Create drone parameter controls
     */
    private TitledPane createDroneControls() {
        VBox droneBox = new VBox(5);
        droneBox.setPadding(new Insets(5));

        Label velLabel = new Label("Drone Velocity (m/s)");
        Slider velSlider = new Slider(0, 200, drone.getVelocity().magnitude());

        droneBox.getChildren().addAll(
            velLabel,
            velSlider,
            new Label("Initial position controls would go here")
        );

        TitledPane pane = new TitledPane("Drone", droneBox);
        pane.setCollapsible(false);
        return pane;
    }

    /**
     * Create simulation control buttons
     */
    private TitledPane createSimulationControls() {
        VBox simBox = new VBox(5);
        simBox.setPadding(new Insets(5));

        Button startBtn = new Button("Start");
        Button stopBtn = new Button("Stop");
        Button resetBtn = new Button("Reset");

        startBtn.setPrefWidth(100);
        stopBtn.setPrefWidth(100);
        resetBtn.setPrefWidth(100);

        startBtn.setOnAction(e -> controller.startSimulation());
        stopBtn.setOnAction(e -> controller.stopSimulation());
        resetBtn.setOnAction(e -> resetSimulation());

        simBox.getChildren().addAll(startBtn, stopBtn, resetBtn);

        TitledPane pane = new TitledPane("Simulation", simBox);
        pane.setCollapsible(false);
        return pane;
    }

    /**
     * Reset simulation to initial state
     */
    private void resetSimulation() {
        // Implementation would reset drone position and time
        System.out.println("Simulation reset");
    }

    /**
     * Get the root VBox for integration into main UI
     */
    public VBox getPane() {
        return root;
    }
}
