package com.radar.simulator.ui;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.model.ExperimentRecord;
import com.radar.simulator.model.Scenario;
import com.radar.simulator.model.Status;
import com.radar.simulator.util.Vector3D;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * 3D view of a scenario and an experiment run.
 *
 * Shows the transmitter (red), receivers (blue, with a mast down to the
 * ground), the ground-truth trajectory (green line), the estimated
 * positions (orange points), and, for the selected time step, the drone,
 * its estimate and the error between them. World coordinates are mapped
 * with {@link SceneTransform}, so +Z (Up) is drawn upwards.
 *
 * Drag to orbit, scroll to zoom. The slider and Play button step through
 * the run.
 */
public class Visualization3D {

    /** Size of the largest scenario dimension in scene units. */
    private static final double SCENE_SIZE = 1000;
    /** Cap on drawn trajectory points; the records keep full resolution. */
    private static final int MAX_DRAWN_POINTS = 400;

    private static final Color TX_COLOR = Color.web("#e53935");
    private static final Color RX_COLOR = Color.web("#1e88e5");
    private static final Color TRUTH_COLOR = Color.web("#43a047");
    private static final Color ESTIMATE_COLOR = Color.web("#fb8c00");
    private static final Color GRID_COLOR = Color.web("#4a4a80");

    private final Group world = new Group();
    private final Group markers = new Group();
    private final SubScene subScene;
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate yaw = new Rotate(-30, Rotate.Y_AXIS);
    private final Rotate pitch = new Rotate(-30, Rotate.X_AXIS);

    private final BorderPane pane = new BorderPane();
    private final Slider stepSlider = new Slider(0, 0, 0);
    private final Label stepLabel = new Label();
    private final Button playButton = new Button("Play");
    private AnimationTimer player;

    private SceneTransform transform = new SceneTransform(new Vector3D(0, 0, 0), 1);
    private List<ExperimentRecord> records = List.of();
    private IntConsumer stepListener = i -> {};

    private double dragX, dragY;

    public Visualization3D() {
        camera.setNearClip(1);
        camera.setFarClip(20 * SCENE_SIZE);
        camera.setTranslateZ(-1.8 * SCENE_SIZE);
        camera.setFieldOfView(40);

        Group cameraRig = new Group(camera);
        cameraRig.getTransforms().addAll(yaw, pitch);

        Group root3D = new Group(world, markers, cameraRig);
        subScene = new SubScene(root3D, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#1a1a2e"));
        subScene.setCamera(camera);
        installMouseControls();

        Pane viewport = new Pane(subScene);
        subScene.widthProperty().bind(viewport.widthProperty());
        subScene.heightProperty().bind(viewport.heightProperty());
        viewport.setMinSize(200, 200);

        StackPane stack = new StackPane(viewport, legend());
        StackPane.setAlignment(stack.getChildren().get(1), Pos.TOP_LEFT);
        pane.setCenter(stack);
        pane.setBottom(playbackControls());
        stepSlider.setDisable(true);
        playButton.setDisable(true);
    }

    public Pane getPane() {
        return pane;
    }

    /** Called with the record index whenever the selected time step changes. */
    public void setStepListener(IntConsumer listener) {
        this.stepListener = listener;
    }

    /**
     * Show only the sensor layout (e.g. when the geometry is invalid).
     */
    public void showScenario(Scenario scenario) {
        showExperiment(scenario, List.of());
    }

    /**
     * Show the sensor layout plus the ground-truth and estimated trajectories.
     */
    public void showExperiment(Scenario scenario, List<ExperimentRecord> records) {
        stopPlayback();
        this.records = List.copyOf(records);

        List<Vector3D> extentPoints = new ArrayList<>();
        extentPoints.add(scenario.getTransmitter().getPosition());
        for (Receiver rx : scenario.getReceivers()) {
            extentPoints.add(rx.getPosition());
            extentPoints.add(new Vector3D(rx.getPosition().x, rx.getPosition().y, 0));
        }
        for (ExperimentRecord r : records) {
            extentPoints.add(r.groundTruthPosition());
        }
        transform = SceneTransform.fitting(extentPoints, SCENE_SIZE);

        world.getChildren().clear();
        drawGroundGrid(extentPoints);
        drawAxes();

        double sensorRadius = 0.012 * SCENE_SIZE;
        world.getChildren().add(sphere(scenario.getTransmitter().getPosition(), sensorRadius, TX_COLOR));
        for (Receiver rx : scenario.getReceivers()) {
            Vector3D p = rx.getPosition();
            world.getChildren().add(sphere(p, sensorRadius, RX_COLOR));
            if (Math.abs(p.z) > 1e-9) {
                world.getChildren().add(line(new Vector3D(p.x, p.y, 0), p, 1.0, RX_COLOR.darker()));
            }
        }

        List<Integer> drawn = SceneTransform.sampleIndices(records.size(), MAX_DRAWN_POINTS);
        for (int k = 1; k < drawn.size(); k++) {
            world.getChildren().add(line(
                records.get(drawn.get(k - 1)).groundTruthPosition(),
                records.get(drawn.get(k)).groundTruthPosition(), 1.5, TRUTH_COLOR));
        }
        for (int i : drawn) {
            ExperimentRecord r = records.get(i);
            if (r.estimationResult().status() == Status.SUCCESS) {
                world.getChildren().add(sphere(r.estimationResult().position(), 0.004 * SCENE_SIZE, ESTIMATE_COLOR));
            }
        }

        boolean hasSteps = !records.isEmpty();
        stepSlider.setDisable(!hasSteps);
        playButton.setDisable(records.size() < 2);
        stepSlider.setMax(Math.max(records.size() - 1, 0));
        stepSlider.setValue(0);
        showStep(0);
    }

    /**
     * Move the drone/estimate markers to the given record index.
     */
    public void showStep(int index) {
        markers.getChildren().clear();
        if (records.isEmpty()) {
            stepLabel.setText("No run");
            return;
        }
        int i = Math.max(0, Math.min(index, records.size() - 1));
        if ((int) Math.round(stepSlider.getValue()) != i) {
            stepSlider.setValue(i);
        }
        ExperimentRecord r = records.get(i);

        Vector3D truth = r.groundTruthPosition();
        markers.getChildren().add(sphere(truth, 0.01 * SCENE_SIZE, TRUTH_COLOR.brighter()));
        markers.getChildren().add(line(new Vector3D(truth.x, truth.y, 0), truth, 0.6, TRUTH_COLOR.darker()));

        String detail;
        if (r.estimationResult().status() == Status.SUCCESS) {
            Vector3D estimate = r.estimationResult().position();
            markers.getChildren().add(sphere(estimate, 0.007 * SCENE_SIZE, ESTIMATE_COLOR.brighter()));
            if (r.positionError() > 0) {
                markers.getChildren().add(line(truth, estimate, 0.8, Color.WHITE));
            }
            detail = String.format("error %.3f m", r.positionError());
        } else {
            detail = r.estimationResult().status().toString();
        }
        stepLabel.setText(String.format("Step %d / %d   t = %.2f s   %s",
            r.stepIndex(), records.size() - 1, r.time(), detail));
        stepListener.accept(i);
    }

    // ----- scene construction -----

    private void drawGroundGrid(List<Vector3D> points) {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Vector3D p : points) {
            minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
        }
        double step = niceStep(Math.max(maxX - minX, maxY - minY) / 8);
        double x0 = Math.floor(minX / step) * step, x1 = Math.ceil(maxX / step) * step;
        double y0 = Math.floor(minY / step) * step, y1 = Math.ceil(maxY / step) * step;
        for (double x = x0; x <= x1 + step / 2; x += step) {
            world.getChildren().add(line(new Vector3D(x, y0, 0), new Vector3D(x, y1, 0), 0.5, GRID_COLOR));
        }
        for (double y = y0; y <= y1 + step / 2; y += step) {
            world.getChildren().add(line(new Vector3D(x0, y, 0), new Vector3D(x1, y, 0), 0.5, GRID_COLOR));
        }
    }

    /** East (red), North (green), Up (blue) axes at the transmitter-frame origin. */
    private void drawAxes() {
        double length = 0.15 * SCENE_SIZE / transform.getScale();
        Vector3D origin = new Vector3D(0, 0, 0);
        world.getChildren().addAll(
            line(origin, new Vector3D(length, 0, 0), 2, Color.web("#ef5350")),
            line(origin, new Vector3D(0, length, 0), 2, Color.web("#66bb6a")),
            line(origin, new Vector3D(0, 0, length), 2, Color.web("#42a5f5")));
    }

    /** 1, 2 or 5 times a power of ten, at least {@code raw}. */
    static double niceStep(double raw) {
        if (!(raw > 0)) return 1;
        double magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
        for (double m : new double[]{1, 2, 5, 10}) {
            if (m * magnitude >= raw) return m * magnitude;
        }
        return 10 * magnitude;
    }

    private Sphere sphere(Vector3D world, double sceneRadius, Color color) {
        Sphere sphere = new Sphere(sceneRadius);
        sphere.setMaterial(new PhongMaterial(color));
        Vector3D p = transform.toScene(world);
        sphere.setTranslateX(p.x);
        sphere.setTranslateY(p.y);
        sphere.setTranslateZ(p.z);
        return sphere;
    }

    /** Thin cylinder between two world points (radius in scene units). */
    private Node line(Vector3D fromWorld, Vector3D toWorld, double sceneRadius, Color color) {
        Vector3D a = transform.toScene(fromWorld);
        Vector3D b = transform.toScene(toWorld);
        Point3D from = new Point3D(a.x, a.y, a.z);
        Point3D diff = new Point3D(b.x, b.y, b.z).subtract(from);
        double length = diff.magnitude();

        Cylinder cylinder = new Cylinder(sceneRadius, Math.max(length, 1e-6));
        cylinder.setMaterial(new PhongMaterial(color));
        Point3D mid = from.midpoint(b.x, b.y, b.z);
        cylinder.setTranslateX(mid.getX());
        cylinder.setTranslateY(mid.getY());
        cylinder.setTranslateZ(mid.getZ());

        // A Cylinder is built along +Y; rotate it onto the segment direction
        if (length > 1e-9) {
            Point3D yAxis = new Point3D(0, 1, 0);
            Point3D axis = yAxis.crossProduct(diff);
            double angle = yAxis.angle(diff);
            if (axis.magnitude() > 1e-9) {
                cylinder.getTransforms().add(new Rotate(angle, axis));
            }
        }
        return cylinder;
    }

    // ----- interaction -----

    private void installMouseControls() {
        subScene.setOnMousePressed(e -> {
            dragX = e.getSceneX();
            dragY = e.getSceneY();
        });
        subScene.setOnMouseDragged(e -> {
            yaw.setAngle(yaw.getAngle() + (e.getSceneX() - dragX) * 0.3);
            pitch.setAngle(Math.max(-89, Math.min(89, pitch.getAngle() - (e.getSceneY() - dragY) * 0.3)));
            dragX = e.getSceneX();
            dragY = e.getSceneY();
        });
        subScene.setOnScroll(e -> {
            double z = camera.getTranslateZ() * Math.pow(1.001, e.getDeltaY());
            camera.setTranslateZ(Math.max(-15 * SCENE_SIZE, Math.min(-0.2 * SCENE_SIZE, z)));
        });
    }

    private HBox playbackControls() {
        stepSlider.setMajorTickUnit(1);
        stepSlider.setBlockIncrement(1);
        stepSlider.valueProperty().addListener((obs, old, value) -> {
            int i = (int) Math.round(value.doubleValue());
            if (i != (int) Math.round(old.doubleValue())) {
                showStep(i);
            }
        });
        playButton.setOnAction(e -> {
            if (player != null) {
                stopPlayback();
            } else {
                startPlayback();
            }
        });
        HBox.setHgrow(stepSlider, Priority.ALWAYS);
        stepLabel.setMinWidth(320);
        HBox controls = new HBox(8, playButton, stepSlider, stepLabel);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(6, 8, 6, 8));
        return controls;
    }

    private void startPlayback() {
        if (records.size() < 2) return;
        if (stepSlider.getValue() >= stepSlider.getMax()) {
            stepSlider.setValue(0);
        }
        playButton.setText("Pause");
        // Whole run plays in about 10 s regardless of the number of steps
        double stepsPerSecond = Math.max(records.size() / 10.0, 1.0);
        player = new AnimationTimer() {
            private long start = -1;
            private double startStep;

            @Override
            public void handle(long now) {
                if (start < 0) {
                    start = now;
                    startStep = stepSlider.getValue();
                }
                double step = startStep + (now - start) / 1e9 * stepsPerSecond;
                if (step >= stepSlider.getMax()) {
                    stepSlider.setValue(stepSlider.getMax());
                    stopPlayback();
                } else {
                    stepSlider.setValue(Math.floor(step));
                }
            }
        };
        player.start();
    }

    private void stopPlayback() {
        if (player != null) {
            player.stop();
            player = null;
        }
        playButton.setText("Play");
    }

    private static Node legend() {
        TextFlow legend = new TextFlow(
            swatch(TX_COLOR), legendText(" Transmitter    "),
            swatch(RX_COLOR), legendText(" Receiver    "),
            swatch(TRUTH_COLOR), legendText(" True trajectory    "),
            swatch(ESTIMATE_COLOR), legendText(" Estimate\n"),
            legendText("Axes: red = East (+X), green = North (+Y), blue = Up (+Z)\n"
                + "Drag to rotate, scroll to zoom"));
        legend.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-padding: 6;");
        legend.setMouseTransparent(true);
        legend.setMaxSize(TextFlow.USE_PREF_SIZE, TextFlow.USE_PREF_SIZE);
        StackPane.setMargin(legend, new Insets(8));
        return legend;
    }

    private static Text swatch(Color color) {
        Text text = new Text("■");
        text.setFill(color);
        return text;
    }

    private static Text legendText(String content) {
        Text text = new Text(content);
        text.setFill(Color.web("#d0d0e0"));
        text.setStyle("-fx-font-size: 11px;");
        return text;
    }
}
