package com.radar.simulator.ui;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.model.ExperimentRecord;
import com.radar.simulator.model.Scenario;
import com.radar.simulator.model.Status;
import com.radar.simulator.util.Vector3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;

import java.util.List;

/**
 * 3D view of a scenario and the result of an experiment run:
 * transmitter (red), receivers (blue), ground-truth trajectory (green)
 * and estimated positions (orange).
 */
public class Visualization3D {
    private final Group root3D = new Group();
    private final SubScene subScene;

    public Visualization3D() {
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFarClip(200000);
        camera.setTranslateZ(-50000);

        subScene = new SubScene(root3D, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#1a1a2e"));
        subScene.setCamera(camera);
    }

    /**
     * Show only the sensor layout (e.g. when the geometry is invalid).
     */
    public void showScenario(Scenario scenario) {
        root3D.getChildren().clear();
        root3D.getChildren().add(sphere(scenario.getTransmitter().getPosition(), 300, Color.RED));
        for (Receiver rx : scenario.getReceivers()) {
            root3D.getChildren().add(sphere(rx.getPosition(), 300, Color.DODGERBLUE));
        }
    }

    /**
     * Show the sensor layout plus the ground-truth and estimated trajectories.
     */
    public void showExperiment(Scenario scenario, List<ExperimentRecord> records) {
        showScenario(scenario);
        for (ExperimentRecord record : records) {
            root3D.getChildren().add(sphere(record.groundTruthPosition(), 80, Color.LIMEGREEN));
            if (record.estimationResult().status() == Status.SUCCESS) {
                root3D.getChildren().add(sphere(record.estimationResult().position(), 50, Color.ORANGE));
            }
        }
    }

    private static Sphere sphere(Vector3D position, double radius, Color color) {
        Sphere sphere = new Sphere(radius);
        sphere.setMaterial(new PhongMaterial(color));
        sphere.setTranslateX(position.x);
        sphere.setTranslateY(position.y);
        sphere.setTranslateZ(position.z);
        return sphere;
    }

    /**
     * Get the JavaFX Pane for integration into the UI.
     */
    public Pane getPane() {
        Pane pane = new Pane();
        pane.getChildren().add(subScene);
        subScene.widthProperty().bind(pane.widthProperty());
        subScene.heightProperty().bind(pane.heightProperty());
        return pane;
    }
}
