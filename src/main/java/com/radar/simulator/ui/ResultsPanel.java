package com.radar.simulator.ui;

import com.radar.simulator.model.EvaluationResult;
import com.radar.simulator.model.ExperimentRecord;
import com.radar.simulator.model.Status;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * Per-step results table and aggregate error metrics for the last run.
 * Failed steps are listed with their status instead of a position.
 */
public class ResultsPanel {

    private final VBox root = new VBox(6);
    private final Label summary = new Label("No experiment run yet");
    private final TableView<ExperimentRecord> table = new TableView<>();
    private IntConsumer selectionListener = i -> {};
    private boolean updatingSelection;

    public ResultsPanel() {
        summary.setStyle("-fx-font-weight: bold;");
        table.setPlaceholder(new Label("Run an experiment to see per-step results"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(List.of(
            column("Step", r -> String.valueOf(r.stepIndex())),
            column("t (s)", r -> String.format("%.2f", r.time())),
            column("True X", r -> coord(r.groundTruthPosition().x)),
            column("True Y", r -> coord(r.groundTruthPosition().y)),
            column("True Z", r -> coord(r.groundTruthPosition().z)),
            column("Est X", r -> estimate(r, r.estimationResult().position().x)),
            column("Est Y", r -> estimate(r, r.estimationResult().position().y)),
            column("Est Z", r -> estimate(r, r.estimationResult().position().z)),
            column("Error (m)", r -> Double.isNaN(r.positionError()) ? "—" : String.format("%.4f", r.positionError())),
            statusColumn(),
            column("Residual (m)", r -> sci(r.estimationResult().residual())),
            column("Cond.", r -> sci(r.estimationResult().conditionNumber()))
        ));
        table.getSelectionModel().selectedIndexProperty().addListener((obs, old, index) -> {
            if (!updatingSelection && index.intValue() >= 0) {
                selectionListener.accept(index.intValue());
            }
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        root.setPadding(new Insets(6, 8, 6, 8));
        root.getChildren().addAll(summary, table);
    }

    public VBox getPane() {
        return root;
    }

    /** Called with the record index when the user selects a row. */
    public void setSelectionListener(IntConsumer listener) {
        this.selectionListener = listener;
    }

    public void show(List<ExperimentRecord> records, EvaluationResult evaluation) {
        summary.setText(SimulatorController.formatMetrics(evaluation));
        table.setItems(FXCollections.observableArrayList(records));
    }

    public void clear(String message) {
        summary.setText(message);
        table.getItems().clear();
    }

    /** Highlight a row without notifying the selection listener. */
    public void select(int index) {
        if (index < 0 || index >= table.getItems().size()
                || table.getSelectionModel().getSelectedIndex() == index) {
            return;
        }
        updatingSelection = true;
        table.getSelectionModel().select(index);
        table.scrollTo(Math.max(index - 3, 0));
        updatingSelection = false;
    }

    private static TableColumn<ExperimentRecord, String> column(
            String title, Function<ExperimentRecord, String> value) {
        TableColumn<ExperimentRecord, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        col.setSortable(false);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }

    private static TableColumn<ExperimentRecord, String> statusColumn() {
        TableColumn<ExperimentRecord, String> col =
            column("Status", r -> r.estimationResult().status().toString());
        col.setStyle("-fx-alignment: CENTER-LEFT;");
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(!empty && !Status.SUCCESS.toString().equals(item)
                    ? "-fx-text-fill: #b00020;" : "");
            }
        });
        return col;
    }

    private static String coord(double value) {
        return String.format("%.2f", value);
    }

    private static String estimate(ExperimentRecord r, double value) {
        return r.estimationResult().status() == Status.SUCCESS ? coord(value) : "—";
    }

    private static String sci(double value) {
        return Double.isFinite(value) ? String.format("%.2e", value) : "—";
    }
}
