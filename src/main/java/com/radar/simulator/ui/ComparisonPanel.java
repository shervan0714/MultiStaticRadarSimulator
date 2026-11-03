package com.radar.simulator.ui;

import com.radar.simulator.experiment.EvaluationResult;
import com.radar.simulator.experiment.ModelComparison;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Side-by-side metrics of several localization models run on the same
 * measurements. The model with the lowest mean error is shown in bold;
 * selecting a row shows that model's run in the 3D view and step table.
 */
public class ComparisonPanel {

    private final VBox root = new VBox(6);
    private final Label summary = new Label("Click \"Compare models\" to run every model on the same measurements");
    private final TableView<ModelComparison.Entry> table = new TableView<>();
    private Consumer<ModelComparison.Entry> selectionListener = e -> {};
    private ModelComparison.Entry best;

    public ComparisonPanel() {
        summary.setWrapText(true);
        table.setPlaceholder(new Label("No comparison yet"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<ModelComparison.Entry, String> model = column("Model", e -> e.localizationModel().name());
        model.setStyle("-fx-alignment: CENTER-LEFT;");
        model.setPrefWidth(200);
        TableColumn<ModelComparison.Entry, String> version = column("Version", e -> e.localizationModel().version());
        version.setStyle("-fx-alignment: CENTER-LEFT;");
        version.setPrefWidth(220);
        table.getColumns().addAll(List.of(
            model, version,
            column("Localized", e -> String.valueOf(e.evaluation().successfulSteps())),
            column("Failed", e -> String.valueOf(e.evaluation().failedSteps())),
            column("Mean error (m)", e -> metric(e.evaluation(), e.evaluation().meanError())),
            column("Max error (m)", e -> metric(e.evaluation(), e.evaluation().maxError())),
            column("RMS error (m)", e -> metric(e.evaluation(), e.evaluation().rmsError()))
        ));
        table.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(ModelComparison.Entry item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(!empty && item == best ? "-fx-font-weight: bold;" : "");
            }
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry != null) selectionListener.accept(entry);
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        root.setPadding(new Insets(6, 8, 6, 8));
        root.getChildren().addAll(summary, table);
    }

    public VBox getPane() {
        return root;
    }

    /** Called when the user selects a model's row. */
    public void setSelectionListener(Consumer<ModelComparison.Entry> listener) {
        this.selectionListener = listener;
    }

    public void show(List<ModelComparison.Entry> entries, String measurementModel) {
        best = bestEntry(entries);
        summary.setText(entries.size() + " models, same measurements (" + measurementModel + ")."
            + (best == null ? " No model localized any step."
                : " Lowest mean error: " + best.localizationModel().name() + ".")
            + " Select a row to show that model's run.");
        table.setItems(FXCollections.observableArrayList(entries));
    }

    public void clear(String message) {
        best = null;
        summary.setText(message);
        table.getItems().clear();
    }

    /** The entry with the lowest mean error among models that localized at least one step. */
    static ModelComparison.Entry bestEntry(List<ModelComparison.Entry> entries) {
        ModelComparison.Entry best = null;
        for (ModelComparison.Entry e : entries) {
            if (e.evaluation().successfulSteps() == 0) continue;
            if (best == null || e.evaluation().meanError() < best.evaluation().meanError()) {
                best = e;
            }
        }
        return best;
    }

    private static String metric(EvaluationResult evaluation, double value) {
        return evaluation.successfulSteps() == 0 ? "—" : String.format("%.4f", value);
    }

    private static TableColumn<ModelComparison.Entry, String> column(
            String title, Function<ModelComparison.Entry, String> value) {
        TableColumn<ModelComparison.Entry, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        col.setSortable(false);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }
}
