package com.radar.simulator.ui;

import com.radar.simulator.model.BaselineLocalizationModel;
import com.radar.simulator.model.BaselineMeasurementModel;
import com.radar.simulator.model.EvaluationModule;
import com.radar.simulator.model.EvaluationResult;
import com.radar.simulator.model.ExperimentRecord;
import com.radar.simulator.model.ExperimentRunner;
import com.radar.simulator.model.GeometryValidator;
import com.radar.simulator.model.LocalizationModel;
import com.radar.simulator.model.MeasurementModel;
import com.radar.simulator.model.Scenario;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Application-level coordinator: connects the scenario editor, the
 * experiment pipeline and the result views.
 *
 * Pipeline per run: editor form → ScenarioConfig → Scenario → geometry
 * validation → ExperimentRunner (trajectory → measurement → localization)
 * → EvaluationModule → visualization and status display. The measurement
 * and localization mathematics live in the model classes, not here.
 */
public class SimulatorController {

    private final BorderPane root = new BorderPane();
    private final ScenarioEditor editor = new ScenarioEditor();
    private final Visualization3D visualization = new Visualization3D();
    private final Label statusLabel = new Label();
    private final Label metricsLabel = new Label();

    // Replaceable formulations
    private final MeasurementModel measurementModel = new BaselineMeasurementModel();
    private final LocalizationModel localizationModel = new BaselineLocalizationModel();
    private final GeometryValidator geometryValidator = new GeometryValidator();

    private Scenario lastScenario;
    private List<ExperimentRecord> lastRecords;
    private EvaluationResult lastEvaluation;

    public SimulatorController() {
        buildUI();
    }

    private void buildUI() {
        Button runButton = new Button("Run experiment");
        runButton.setDefaultButton(true);
        runButton.setOnAction(e -> runExperiment());

        VBox left = new VBox(8, editor.getPane(), new HBox(8, runButton));
        left.setPadding(new Insets(0, 0, 10, 10));
        ScrollPane scroll = new ScrollPane(left);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(430);
        root.setLeft(scroll);

        root.setCenter(visualization.getPane());

        statusLabel.setWrapText(true);
        VBox statusPanel = new VBox(4, statusLabel, metricsLabel);
        statusPanel.setPadding(new Insets(8));
        statusPanel.setStyle("-fx-border-color: #cccccc;");
        root.setBottom(statusPanel);
    }

    /**
     * Read the form, run the experiment and update all views.
     * Invalid input and invalid geometry are reported in the status line.
     */
    public void runExperiment() {
        ScenarioConfig config;
        Scenario scenario;
        try {
            config = editor.getConfig();
            scenario = config.toScenario();
        } catch (IllegalArgumentException e) {
            showError("Invalid input: " + e.getMessage());
            return;
        }

        GeometryValidator.ValidationResult geometry =
            geometryValidator.validate(scenario.getTransmitter(), scenario.getReceivers());
        if (!geometry.isValid()) {
            showError("Cannot localize: " + geometry.message() + " [" + geometry.status() + "]");
            visualization.showScenario(scenario);
            return;
        }

        ExperimentRunner runner = new ExperimentRunner(scenario, measurementModel, localizationModel);
        List<ExperimentRecord> records = runner.run(config.duration(), config.timeStep());
        EvaluationResult evaluation = new EvaluationModule().evaluate(records);

        lastScenario = scenario;
        lastRecords = records;
        lastEvaluation = evaluation;

        visualization.showExperiment(scenario, records);

        String status = "Models: " + measurementModel.getDescriptor().name() + " v"
            + measurementModel.getDescriptor().version() + " / "
            + localizationModel.getDescriptor().name() + " v"
            + localizationModel.getDescriptor().version()
            + " | " + geometry.message();
        if (geometry.hasWarnings()) {
            status += " | WARNING: " + String.join("; ", geometry.warnings());
        }
        statusLabel.setStyle("");
        statusLabel.setText(status);
        metricsLabel.setText(formatMetrics(evaluation));
    }

    static String formatMetrics(EvaluationResult evaluation) {
        String steps = String.format("Steps: %d (%d localized, %d failed)",
            evaluation.totalSteps(), evaluation.successfulSteps(), evaluation.failedSteps());
        if (evaluation.successfulSteps() == 0) {
            return steps + " | no successful estimates";
        }
        return steps + String.format(" | Mean error: %.4f m | Max error: %.4f m | RMS error: %.4f m",
            evaluation.meanError(), evaluation.maxError(), evaluation.rmsError());
    }

    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #b00020;");
        statusLabel.setText(message);
        metricsLabel.setText("");
    }

    public BorderPane getRoot() { return root; }
    public ScenarioEditor getEditor() { return editor; }
    public Scenario getLastScenario() { return lastScenario; }
    public List<ExperimentRecord> getLastRecords() { return lastRecords; }
    public EvaluationResult getLastEvaluation() { return lastEvaluation; }
}
