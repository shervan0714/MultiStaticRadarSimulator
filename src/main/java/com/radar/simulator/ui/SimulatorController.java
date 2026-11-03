package com.radar.simulator.ui;

import com.radar.simulator.core.ModelDescriptor;
import com.radar.simulator.experiment.EvaluationModule;
import com.radar.simulator.experiment.EvaluationResult;
import com.radar.simulator.experiment.ExperimentRecord;
import com.radar.simulator.experiment.ExperimentRunner;
import com.radar.simulator.experiment.ModelComparison;
import com.radar.simulator.formula.FormulaSet;
import com.radar.simulator.localization.GeometryValidator;
import com.radar.simulator.localization.LocalizationModel;
import com.radar.simulator.localization.LocalizationModels;
import com.radar.simulator.measurement.MeasurementModel;
import com.radar.simulator.persistence.ScenarioPersistence;
import com.radar.simulator.scenario.Scenario;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Application-level coordinator: connects the scenario editor, the formula
 * editor, the experiment pipeline, persistence and the result views.
 *
 * Pipeline per run: editor form → ScenarioConfig → Scenario → geometry
 * validation → ExperimentRunner (trajectory → measurement → localization)
 * → EvaluationModule → 3D view, results table and status line. "Compare
 * models" runs every localization model (built-in and user formulas) on the
 * same measurements. The measurement and localization mathematics live in
 * the model classes, not here.
 */
public class SimulatorController {

    private final BorderPane root = new BorderPane();
    private final ScenarioEditor editor = new ScenarioEditor();
    private final FormulaEditor formulaEditor = new FormulaEditor();
    private final ComboBox<String> modelChoice = new ComboBox<>();
    private final Visualization3D visualization = new Visualization3D();
    private final ResultsPanel results = new ResultsPanel();
    private final ComparisonPanel comparison = new ComparisonPanel();
    private final TabPane resultTabs = new TabPane();
    private final Tab stepsTab = new Tab("Per-step results");
    private final Tab comparisonTab = new Tab("Model comparison");
    private final Label statusLabel = new Label();

    // The measurement and localization models are built from the forms for
    // every run (noise settings, formulas), so a seeded run is reproducible.
    private final GeometryValidator geometryValidator = new GeometryValidator();
    private final ScenarioPersistence persistence = new ScenarioPersistence();

    private Scenario lastScenario;
    private List<ExperimentRecord> lastRecords;
    private EvaluationResult lastEvaluation;
    private File lastDirectory;

    /** Everything a run needs, read from the forms and validated. */
    private record Setup(ScenarioConfig config, Scenario scenario, FormulaSet formulas,
                         MeasurementModel measurementModel,
                         GeometryValidator.ValidationResult geometry) {}

    public SimulatorController() {
        buildUI();
    }

    private void buildUI() {
        Button runButton = new Button("Run experiment");
        runButton.setDefaultButton(true);
        runButton.setOnAction(e -> runExperiment());
        Button compareButton = new Button("Compare models");
        compareButton.setOnAction(e -> compareModels());
        Button saveButton = new Button("Save scenario…");
        saveButton.setOnAction(e -> saveScenario());
        Button loadButton = new Button("Load scenario…");
        loadButton.setOnAction(e -> loadScenario());
        Button exportButton = new Button("Export results…");
        exportButton.setOnAction(e -> exportResults());

        modelChoice.getItems().setAll(LocalizationModels.builtIns().stream()
            .map(m -> m.getDescriptor().name()).toList());
        modelChoice.setValue(modelChoice.getItems().get(0));
        // Formulas can be added or renamed at any time: rebuild the list when it opens
        modelChoice.setOnShowing(e -> refreshModelChoices());
        modelChoice.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(modelChoice, Priority.ALWAYS);
        HBox modelBox = new HBox(6, new Label("Localize with"), modelChoice);
        modelBox.setAlignment(Pos.CENTER_LEFT);

        TitledPane formulaSection = new TitledPane("Custom formulas (no code changes needed)",
            formulaEditor.getPane());
        formulaSection.setCollapsible(false);
        editor.getPane().getChildren().add(formulaSection);

        FlowPane buttons = new FlowPane(8, 8, runButton, compareButton, saveButton, loadButton, exportButton);
        VBox left = new VBox(8, editor.getPane(), modelBox, buttons);
        left.setPadding(new Insets(0, 10, 10, 10));
        ScrollPane scroll = new ScrollPane(left);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(475);
        root.setLeft(scroll);

        stepsTab.setContent(results.getPane());
        comparisonTab.setContent(comparison.getPane());
        stepsTab.setClosable(false);
        comparisonTab.setClosable(false);
        resultTabs.getTabs().addAll(stepsTab, comparisonTab);

        SplitPane center = new SplitPane(visualization.getPane(), resultTabs);
        center.setOrientation(Orientation.VERTICAL);
        center.setDividerPositions(0.62);
        root.setCenter(center);

        // Keep the 3D time step and the selected table row in sync
        visualization.setStepListener(results::select);
        results.setSelectionListener(visualization::showStep);
        comparison.setSelectionListener(entry -> {
            showRun(lastScenario, entry.records(), entry.evaluation());
            showInfo("Showing " + describe(entry.localizationModel()) + " from the comparison");
        });

        statusLabel.setWrapText(true);
        statusLabel.setPadding(new Insets(6, 8, 6, 8));
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setStyle("-fx-border-color: #cccccc transparent transparent transparent;");
        root.setBottom(statusLabel);
    }

    /**
     * Read the forms, run the experiment with the chosen localization model
     * and update all views. Invalid input, formulas and geometry are
     * reported in the status line.
     */
    public void runExperiment() {
        Setup setup = prepare();
        if (setup == null) return;

        LocalizationModel localizationModel;
        try {
            localizationModel = setup.formulas().localizationModel(modelChoice.getValue());
        } catch (IllegalArgumentException e) {
            showError("Choose a localization model: \"" + modelChoice.getValue() + "\" no longer exists");
            return;
        }

        ExperimentRunner runner = new ExperimentRunner(setup.scenario(), setup.measurementModel(), localizationModel);
        List<ExperimentRecord> records = runner.run(setup.config().duration(), setup.config().timeStep());
        EvaluationResult evaluation = new EvaluationModule().evaluate(records);

        lastScenario = setup.scenario();
        showRun(setup.scenario(), records, evaluation);
        comparison.clear("Click \"Compare models\" to run every model on the same measurements");
        resultTabs.getSelectionModel().select(stepsTab);

        String status = "Models: " + describe(setup.measurementModel().getDescriptor()) + " / "
            + describe(localizationModel.getDescriptor())
            + " | " + setup.geometry().message();
        if (setup.geometry().hasWarnings()) {
            status += " | WARNING: " + String.join("; ", setup.geometry().warnings());
        }
        showInfo(status);
    }

    /**
     * Run every localization model (built-in and formulas) on the same
     * measurements and show the comparison table.
     */
    public void compareModels() {
        Setup setup = prepare();
        if (setup == null) return;

        List<ModelComparison.Entry> entries = ModelComparison.compare(setup.scenario(),
            setup.measurementModel(), setup.formulas().localizationModels(),
            setup.config().duration(), setup.config().timeStep());

        lastScenario = setup.scenario();
        comparison.show(entries, setup.measurementModel().getDescriptor().name());
        ModelComparison.Entry shown = ComparisonPanel.bestEntry(entries);
        if (shown == null) shown = entries.get(0);
        showRun(setup.scenario(), shown.records(), shown.evaluation());
        resultTabs.getSelectionModel().select(comparisonTab);

        String status = "Compared " + entries.size() + " models | Measurements: "
            + describe(setup.measurementModel().getDescriptor())
            + " | 3D view shows " + shown.localizationModel().name();
        if (setup.geometry().hasWarnings()) {
            status += " | WARNING: " + String.join("; ", setup.geometry().warnings());
        }
        showInfo(status);
    }

    /**
     * Read and check both forms and the geometry. Shows the problem and
     * returns null if anything is wrong.
     */
    private Setup prepare() {
        ScenarioConfig config;
        Scenario scenario;
        FormulaSet formulas;
        MeasurementModel measurementModel;
        try {
            config = editor.getConfig();
            scenario = config.toScenario();
        } catch (IllegalArgumentException e) {
            showError("Invalid input: " + e.getMessage());
            return null;
        }
        try {
            formulas = formulaEditor.getFormulaSet();
            formulas.compile();
            measurementModel = formulas.measurementModel(config.rangeNoiseStd(), config.noiseSeed());
        } catch (IllegalArgumentException e) {
            showError("Invalid formula: " + e.getMessage());
            return null;
        }
        refreshModelChoices();

        GeometryValidator.ValidationResult geometry =
            geometryValidator.validate(scenario.getTransmitter(), scenario.getReceivers());
        if (!geometry.isValid()) {
            lastRecords = null;
            visualization.showScenario(scenario);
            results.clear("No results: geometry is invalid");
            comparison.clear("No comparison: geometry is invalid");
            showError("Cannot localize: " + geometry.message() + " [" + geometry.status() + "]");
            return null;
        }
        return new Setup(config, scenario, formulas, measurementModel, geometry);
    }

    private void showRun(Scenario scenario, List<ExperimentRecord> records, EvaluationResult evaluation) {
        lastRecords = records;
        lastEvaluation = evaluation;
        results.show(records, evaluation);
        visualization.showExperiment(scenario, records);
    }

    /**
     * Rebuild the "Localize with" list from the built-in models and the
     * current formula names, keeping the selection if it still exists.
     */
    private void refreshModelChoices() {
        List<String> names;
        try {
            names = formulaEditor.getFormulaSet().localizationModelNames();
        } catch (IllegalArgumentException e) {
            names = LocalizationModels.builtIns().stream().map(m -> m.getDescriptor().name()).toList();
        }
        List<String> unique = new ArrayList<>();
        for (String name : names) {
            if (!unique.contains(name)) unique.add(name);
        }
        String selected = modelChoice.getValue();
        modelChoice.getItems().setAll(unique);
        modelChoice.setValue(unique.contains(selected) ? selected : unique.get(0));
    }

    /**
     * "Name v1.0" for numbered versions; "Formula: name (d_tx + d_rx ...)"
     * for formulas, whose version is the formula text.
     */
    static String describe(ModelDescriptor descriptor) {
        String version = descriptor.version();
        return !version.isEmpty() && Character.isDigit(version.charAt(0))
            ? descriptor.name() + " v" + version
            : descriptor.name() + " (" + version + ")";
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
        FormulaSet formulas;
        try {
            config = editor.getConfig();
            config.toScenario();
            formulas = formulaEditor.getFormulaSet();
        } catch (IllegalArgumentException e) {
            showError("Cannot save: " + e.getMessage());
            return;
        }
        File file = chooseFile("Save scenario", "scenario.properties", true,
            new FileChooser.ExtensionFilter("Scenario files", "*.properties"));
        if (file == null) return;
        try {
            persistence.saveScenario(config.toScenario(), config.duration(), config.timeStep(),
                config.rangeNoiseStd(), config.noiseSeed(), file.getPath());
            persistence.saveModelSettings(formulas, modelChoice.getValue(), file.getPath());
            showInfo("Saved scenario, formulas and model choice to " + file.getPath());
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
     * Load a saved scenario (and its formulas and model choice) into the
     * forms and run it.
     */
    public void loadScenario(File file) throws IOException {
        Scenario scenario = persistence.loadScenario(file.getPath());
        double[] timing = persistence.loadSimulationParameters(file.getPath());
        FormulaSet formulas = persistence.loadFormulas(file.getPath());
        String model = persistence.loadLocalizationModelName(file.getPath());
        editor.setConfig(ScenarioConfig.fromScenario(scenario, timing[0], timing[1],
            timing[2], (long) timing[3]));
        formulaEditor.setFormulaSet(formulas);
        refreshModelChoices();
        if (model != null && modelChoice.getItems().contains(model)) {
            modelChoice.setValue(model);
        }
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
     * Write the displayed run's per-step CSV and an evaluation summary next to it.
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
    public FormulaEditor getFormulaEditor() { return formulaEditor; }
    public ComboBox<String> getModelChoice() { return modelChoice; }
    public Scenario getLastScenario() { return lastScenario; }
    public List<ExperimentRecord> getLastRecords() { return lastRecords; }
    public EvaluationResult getLastEvaluation() { return lastEvaluation; }
}
