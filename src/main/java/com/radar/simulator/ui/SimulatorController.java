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
import com.radar.simulator.model.ScenarioPersistence;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Application-level coordinator: connects the scenario editor, the
 * experiment pipeline, persistence and the result views.
 *
 * Pipeline per run: editor form → ScenarioConfig → Scenario → geometry
 * validation → ExperimentRunner (trajectory → measurement → localization)
 * → EvaluationModule → 3D view, results table and status line. The
 * measurement and localization mathematics live in the model classes,
 * not here.
 */
public class SimulatorController {

    private final BorderPane root = new BorderPane();
    private final ScenarioEditor editor = new ScenarioEditor();
    private final Visualization3D visualization = new Visualization3D();
    private final ResultsPanel results = new ResultsPanel();
    private final Label statusLabel = new Label();

    // Replaceable formulations
    private final MeasurementModel measurementModel = new BaselineMeasurementModel();
    private final LocalizationModel localizationModel = new BaselineLocalizationModel();
    private final GeometryValidator geometryValidator = new GeometryValidator();
    private final ScenarioPersistence persistence = new ScenarioPersistence();

    private Scenario lastScenario;
    private List<ExperimentRecord> lastRecords;
    private EvaluationResult lastEvaluation;
    private File lastDirectory;

    public SimulatorController() {
        buildUI();
    }

    private void buildUI() {
        Button runButton = new Button("Run experiment");
        runButton.setDefaultButton(true);
        runButton.setOnAction(e -> runExperiment());
        Button saveButton = new Button("Save scenario…");
        saveButton.setOnAction(e -> saveScenario());
        Button loadButton = new Button("Load scenario…");
        loadButton.setOnAction(e -> loadScenario());
        Button exportButton = new Button("Export results…");
        exportButton.setOnAction(e -> exportResults());

        FlowPane buttons = new FlowPane(8, 8, runButton, saveButton, loadButton, exportButton);
        VBox left = new VBox(8, editor.getPane(), buttons);
        left.setPadding(new Insets(0, 10, 10, 10));
        ScrollPane scroll = new ScrollPane(left);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(440);
        root.setLeft(scroll);

        SplitPane center = new SplitPane(visualization.getPane(), results.getPane());
        center.setOrientation(Orientation.VERTICAL);
        center.setDividerPositions(0.66);
        root.setCenter(center);

        // Keep the 3D time step and the selected table row in sync
        visualization.setStepListener(results::select);
        results.setSelectionListener(visualization::showStep);

        statusLabel.setWrapText(true);
        statusLabel.setPadding(new Insets(6, 8, 6, 8));
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setStyle("-fx-border-color: #cccccc transparent transparent transparent;");
        root.setBottom(statusLabel);
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
            lastRecords = null;
            visualization.showScenario(scenario);
            results.clear("No results: geometry is invalid");
            showError("Cannot localize: " + geometry.message() + " [" + geometry.status() + "]");
            return;
        }

        ExperimentRunner runner = new ExperimentRunner(scenario, measurementModel, localizationModel);
        List<ExperimentRecord> records = runner.run(config.duration(), config.timeStep());
        EvaluationResult evaluation = new EvaluationModule().evaluate(records);

        lastScenario = scenario;
        lastRecords = records;
        lastEvaluation = evaluation;

        results.show(records, evaluation);
        visualization.showExperiment(scenario, records);

        String status = "Models: " + measurementModel.getDescriptor().name() + " v"
            + measurementModel.getDescriptor().version() + " / "
            + localizationModel.getDescriptor().name() + " v"
            + localizationModel.getDescriptor().version()
            + " | " + geometry.message();
        if (geometry.hasWarnings()) {
            status += " | WARNING: " + String.join("; ", geometry.warnings());
        }
        showInfo(status);
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

    // ----- persistence -----

    private void saveScenario() {
        ScenarioConfig config;
        try {
            config = editor.getConfig();
            config.toScenario();
        } catch (IllegalArgumentException e) {
            showError("Cannot save: " + e.getMessage());
            return;
        }
        File file = chooseFile("Save scenario", "scenario.properties", true,
            new FileChooser.ExtensionFilter("Scenario files", "*.properties"));
        if (file == null) return;
        try {
            persistence.saveScenario(config.toScenario(), config.duration(), config.timeStep(), file.getPath());
            showInfo("Saved scenario to " + file.getPath());
        } catch (IOException e) {
            showError("Could not save scenario: " + e.getMessage());
        }
    }

    private void loadScenario() {
        File file = chooseFile("Load scenario", null, false,
            new FileChooser.ExtensionFilter("Scenario files", "*.properties"));
        if (file == null) return;
        try {
            loadScenario(file);
        } catch (IOException | RuntimeException e) {
            showError("Could not load scenario: " + e.getMessage());
        }
    }

    /**
     * Load a saved scenario into the form and run it.
     */
    public void loadScenario(File file) throws IOException {
        Scenario scenario = persistence.loadScenario(file.getPath());
        double[] timing = persistence.loadSimulationParameters(file.getPath());
        editor.setConfig(ScenarioConfig.fromScenario(scenario, timing[0], timing[1]));
        runExperiment();
        if (lastRecords != null) {
            statusLabel.setText("Loaded " + file.getName() + " | " + statusLabel.getText());
        }
    }

    private void exportResults() {
        if (lastRecords == null) {
            showError("Nothing to export: run an experiment first");
            return;
        }
        File file = chooseFile("Export results", "results.csv", true,
            new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        if (file == null) return;
        try {
            exportResults(file);
        } catch (IOException e) {
            showError("Could not export results: " + e.getMessage());
        }
    }

    /**
     * Write the last run's per-step CSV and an evaluation summary next to it.
     */
    public void exportResults(File csvFile) throws IOException {
        String csvPath = csvFile.getPath();
        String summaryPath = csvPath.toLowerCase().endsWith(".csv")
            ? csvPath.substring(0, csvPath.length() - 4) + "_summary.txt"
            : csvPath + "_summary.txt";
        persistence.exportResultsToCSV(lastRecords, csvPath);
        persistence.exportEvaluationSummary(lastEvaluation, summaryPath);
        showInfo("Exported " + lastRecords.size() + " steps to " + csvPath + " and summary to " + summaryPath);
    }

    private File chooseFile(String title, String initialName, boolean save, FileChooser.ExtensionFilter filter) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(filter);
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        if (initialName != null) {
            chooser.setInitialFileName(initialName);
        }
        Window window = root.getScene() == null ? null : root.getScene().getWindow();
        File file = save ? chooser.showSaveDialog(window) : chooser.showOpenDialog(window);
        if (file != null) {
            lastDirectory = file.getParentFile();
        }
        return file;
    }

    private void showInfo(String message) {
        statusLabel.setStyle(statusLabel.getStyle().replace("-fx-text-fill: #b00020;", ""));
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setStyle(statusLabel.getStyle().replace("-fx-text-fill: #b00020;", "")
            + "-fx-text-fill: #b00020;");
        statusLabel.setText(message);
    }

    public BorderPane getRoot() { return root; }
    public ScenarioEditor getEditor() { return editor; }
    public Scenario getLastScenario() { return lastScenario; }
    public List<ExperimentRecord> getLastRecords() { return lastRecords; }
    public EvaluationResult getLastEvaluation() { return lastEvaluation; }
}
