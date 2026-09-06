package com.radar.simulator.ui;

import javafx.scene.layout.Pane;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.Line;
import javafx.scene.transform.Rotate;
import com.radar.simulator.core.*;
import com.radar.simulator.util.Vector3D;
import java.util.ArrayList;
import java.util.List;

/**
 * 3D visualization of the radar simulator using JavaFX.
 * Displays transmitter, receivers, drone, and signal paths in 3D space.
 */
public class Visualization3D {
    private SubScene subScene;
    private Group root3D;
    
    private Sphere transmitterSphere;
    private List<Sphere> receiverSpheres;
    private Sphere droneSphere;
    private Sphere calculatedDroneSphere;
    private List<Line> signalLines;
    
    private Transmitter transmitter;
    private List<Receiver> receivers;
    private Drone drone;

    public Visualization3D(Transmitter tx, List<Receiver> rxList, Drone drn) {
        this.transmitter = tx;
        this.receivers = rxList;
        this.drone = drn;
        
        root3D = new Group();
        receiverSpheres = new ArrayList<>();
        signalLines = new ArrayList<>();
        
        buildScene();
    }

    /**
     * Build the initial 3D scene
     */
    private void buildScene() {
        // Create camera
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-50000);
        
        // Create transmitter (red sphere)
        transmitterSphere = createSphere(300, Color.RED);
        Vector3D txPos = transmitter.getPosition();
        transmitterSphere.setTranslateX(txPos.x);
        transmitterSphere.setTranslateY(txPos.y);
        transmitterSphere.setTranslateZ(txPos.z);
        root3D.getChildren().add(transmitterSphere);

        // Create receivers (blue spheres)
        for (Receiver rx : receivers) {
            Sphere rxSphere = createSphere(300, Color.BLUE);
            Vector3D rxPos = rx.getPosition();
            rxSphere.setTranslateX(rxPos.x);
            rxSphere.setTranslateY(rxPos.y);
            rxSphere.setTranslateZ(rxPos.z);
            root3D.getChildren().add(rxSphere);
            receiverSpheres.add(rxSphere);
        }

        // Create drone (green sphere)
        droneSphere = createSphere(200, Color.GREEN);
        Vector3D dronePos = drone.getPosition();
        droneSphere.setTranslateX(dronePos.x);
        droneSphere.setTranslateY(dronePos.y);
        droneSphere.setTranslateZ(dronePos.z);
        root3D.getChildren().add(droneSphere);

        // Create calculated position indicator (semi-transparent green)
        calculatedDroneSphere = createSphere(150, Color.web("00FF00", 0.5));
        root3D.getChildren().add(calculatedDroneSphere);

        // Create SubScene
        subScene = new SubScene(root3D, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#1a1a2e"));
        subScene.setCamera(camera);
    }

    /**
     * Create a sphere with specified radius and color
     */
    private Sphere createSphere(double radius, Color color) {
        Sphere sphere = new Sphere(radius);
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(color);
        sphere.setMaterial(material);
        return sphere;
    }

    /**
     * Update visualization with current simulation state
     * 
     * @param drn Current drone position
     * @param calculatedPos Calculated drone position from triangulation
     * @param powers Received power at each receiver
     * @param error Position error magnitude
     */
    public void updateSimulation(Drone drn, Vector3D calculatedPos, 
                                List<Double> powers, double error) {
        // Update drone position (actual)
        Vector3D dronePos = drn.getPosition();
        droneSphere.setTranslateX(dronePos.x);
        droneSphere.setTranslateY(dronePos.y);
        droneSphere.setTranslateZ(dronePos.z);

        // Update calculated drone position
        calculatedDroneSphere.setTranslateX(calculatedPos.x);
        calculatedDroneSphere.setTranslateY(calculatedPos.y);
        calculatedDroneSphere.setTranslateZ(calculatedPos.z);
    }

    /**
     * Get the JavaFX Pane for integration into UI
     */
    public Pane getPane() {
        Pane pane = new Pane();
        pane.getChildren().add(subScene);
        subScene.widthProperty().bind(pane.widthProperty());
        subScene.heightProperty().bind(pane.heightProperty());
        return pane;
    }
}
