package com.radar.simulator;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.radar.simulator.ui.SimulatorController;

/**
 * Main application entry point for the Multi-Static Radar Simulator.
 * Initializes JavaFX and launches the simulator UI.
 */
public class RadarSimulatorApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Create main controller
        SimulatorController controller = new SimulatorController();
        
        // Set up the main window
        primaryStage.setTitle("Multi-Static Radar Simulator");

        // Create scene
        Scene scene = new Scene(controller.getRoot(), 1400, 900);
        primaryStage.setScene(scene);

        // Open with the default scenario already evaluated
        controller.runExperiment();

        // Show window
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
