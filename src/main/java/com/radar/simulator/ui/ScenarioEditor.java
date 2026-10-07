package com.radar.simulator.ui;

import com.radar.simulator.util.Vector3D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Form for editing the scenario: transmitter, a variable-length receiver
 * list, drone trajectory and simulation timing.
 *
 * The form only reads and writes {@link ScenarioConfig}; the controller
 * decides what to do with it.
 */
public class ScenarioEditor {

    private final VBox root = new VBox(8);

    private final TextField txX = numberField();
    private final TextField txY = numberField();
    private final TextField txZ = numberField();
    private final TextField frequency = numberField();
    private final TextField power = numberField();

    private final VBox receiverRows = new VBox(4);
    private final List<ReceiverRow> rows = new ArrayList<>();

    private final TextField startX = numberField();
    private final TextField startY = numberField();
    private final TextField startZ = numberField();
    private final TextField velX = numberField();
    private final TextField velY = numberField();
    private final TextField velZ = numberField();
    private final TextField duration = numberField();
    private final TextField timeStep = numberField();

    /** Editable fields for one receiver. */
    private final class ReceiverRow {
        final TextField id = new TextField();
        final TextField x = numberField();
        final TextField y = numberField();
        final TextField z = numberField();
        final TextField gain = numberField();
        final HBox box;

        ReceiverRow(ScenarioConfig.ReceiverConfig rc) {
            id.setPrefColumnCount(4);
            id.setText(rc.id());
            x.setText(format(rc.x()));
            y.setText(format(rc.y()));
            z.setText(format(rc.z()));
            gain.setText(format(rc.gainDbi()));
            Button remove = new Button("x");
            remove.setMinWidth(Button.USE_PREF_SIZE);
            remove.setTooltip(new javafx.scene.control.Tooltip("Remove receiver"));
            remove.setOnAction(e -> removeRow(this));
            box = new HBox(4, id, x, y, z, gain, remove);
            box.setAlignment(Pos.CENTER_LEFT);
        }

        ScenarioConfig.ReceiverConfig read() {
            String name = id.getText().trim();
            String label = name.isEmpty() ? "receiver" : name;
            return new ScenarioConfig.ReceiverConfig(name,
                parse(x, label + " X"), parse(y, label + " Y"), parse(z, label + " Z"),
                parse(gain, label + " gain"));
        }
    }

    public ScenarioEditor() {
        root.setPadding(new Insets(10));
        root.setPrefWidth(400);
        root.getChildren().addAll(
            section("Transmitter", grid(
                "Position X / Y / Z (m)", new HBox(4, txX, txY, txZ),
                "Frequency (Hz)", frequency,
                "Transmit power (W)", power)),
            section("Receivers", receiverSection()),
            section("Drone (constant velocity)", grid(
                "Start X / Y / Z (m)", new HBox(4, startX, startY, startZ),
                "Velocity X / Y / Z (m/s)", new HBox(4, velX, velY, velZ))),
            section("Simulation", grid(
                "Duration (s)", duration,
                "Time step (s)", timeStep))
        );
        setConfig(ScenarioConfig.defaultConfig());
    }

    public VBox getPane() {
        return root;
    }

    /**
     * Fill the form from a configuration (defaults or a loaded scenario).
     */
    public void setConfig(ScenarioConfig config) {
        setVector(config.transmitterPosition(), txX, txY, txZ);
        frequency.setText(format(config.frequencyHz()));
        power.setText(format(config.transmitPowerW()));

        rows.clear();
        receiverRows.getChildren().clear();
        for (ScenarioConfig.ReceiverConfig rc : config.receivers()) {
            addRow(rc);
        }

        setVector(config.initialPosition(), startX, startY, startZ);
        setVector(config.velocity(), velX, velY, velZ);
        duration.setText(format(config.duration()));
        timeStep.setText(format(config.timeStep()));
    }

    /**
     * Read the form.
     *
     * @throws IllegalArgumentException naming the first field that is not a number
     */
    public ScenarioConfig getConfig() {
        List<ScenarioConfig.ReceiverConfig> receivers = new ArrayList<>();
        for (ReceiverRow row : rows) {
            receivers.add(row.read());
        }
        return new ScenarioConfig(
            readVector(txX, txY, txZ, "Transmitter"),
            parse(frequency, "Frequency"),
            parse(power, "Transmit power"),
            receivers,
            readVector(startX, startY, startZ, "Drone start"),
            readVector(velX, velY, velZ, "Drone velocity"),
            parse(duration, "Duration"),
            parse(timeStep, "Time step")
        );
    }

    private VBox receiverSection() {
        HBox header = new HBox(4,
            headerLabel("ID", 52), headerLabel("X (m)", 70), headerLabel("Y (m)", 70),
            headerLabel("Z (m)", 70), headerLabel("Gain (dBi)", 70));
        Button add = new Button("Add receiver");
        add.setOnAction(e -> addRow(new ScenarioConfig.ReceiverConfig(
            nextReceiverId(), 0, 0, 0, 10)));
        return new VBox(4, header, receiverRows, add);
    }

    private void addRow(ScenarioConfig.ReceiverConfig rc) {
        ReceiverRow row = new ReceiverRow(rc);
        rows.add(row);
        receiverRows.getChildren().add(row.box);
    }

    private void removeRow(ReceiverRow row) {
        rows.remove(row);
        receiverRows.getChildren().remove(row.box);
    }

    private String nextReceiverId() {
        int n = rows.size() + 1;
        while (true) {
            String candidate = "RX" + n;
            if (rows.stream().noneMatch(r -> r.id.getText().trim().equals(candidate))) {
                return candidate;
            }
            n++;
        }
    }

    private static TitledPane section(String title, javafx.scene.Node content) {
        TitledPane pane = new TitledPane(title, content);
        pane.setCollapsible(false);
        return pane;
    }

    private static GridPane grid(Object... labelsAndNodes) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        for (int i = 0; i < labelsAndNodes.length; i += 2) {
            grid.add(new Label((String) labelsAndNodes[i]), 0, i / 2);
            grid.add((javafx.scene.Node) labelsAndNodes[i + 1], 1, i / 2);
        }
        return grid;
    }

    private static Label headerLabel(String text, double width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        return label;
    }

    private static TextField numberField() {
        TextField field = new TextField();
        field.setPrefWidth(70);
        return field;
    }

    private static void setVector(Vector3D v, TextField x, TextField y, TextField z) {
        x.setText(format(v.x));
        y.setText(format(v.y));
        z.setText(format(v.z));
    }

    private static Vector3D readVector(TextField x, TextField y, TextField z, String name) {
        return new Vector3D(parse(x, name + " X"), parse(y, name + " Y"), parse(z, name + " Z"));
    }

    private static double parse(TextField field, String name) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (NumberFormatException e) {
            field.requestFocus();
            throw new IllegalArgumentException(name + " is not a number: \"" + field.getText() + "\"");
        }
    }

    /** Short display form: integers without ".0", large values in scientific notation. */
    static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e7) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
