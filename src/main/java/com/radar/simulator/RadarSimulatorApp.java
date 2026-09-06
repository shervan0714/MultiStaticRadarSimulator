package com.radar.simulator;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;
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
        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);
        
        // Create scene
        Scene scene = new Scene(controller.getRoot(), 1200, 800);
        primaryStage.setScene(scene);
        
        // Show window
        primaryStage.show();
        
        // Start simulation loop
        controller.startSimulation();
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
