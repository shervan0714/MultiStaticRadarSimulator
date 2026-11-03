package com.radar.simulator.ui;

import com.radar.simulator.formula.FormulaDefinition;
import com.radar.simulator.formula.FormulaParameter;
import com.radar.simulator.formula.FormulaSet;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Form for user-defined parameters and formulas, so a new formulation can be
 * tried without changing code: add parameters (name = value), type formulas
 * in terms of the built-in variables and the parameters, and choose which
 * formula (if any) generates the measurements. Every formula also appears in
 * the controller's "Localize with" list and in model comparisons.
 *
 * The form only reads and writes {@link FormulaSet}; checking the formulas is
 * done by {@link FormulaSet#compile()}.
 */
public class FormulaEditor {

    /** Measurement choice meaning "use the built-in bistatic range". */
    static final String BUILT_IN_MEASUREMENT = "Built-in bistatic range";

    private final VBox root = new VBox(6);
    private final VBox parameterRows = new VBox(4);
    private final VBox formulaRows = new VBox(4);
    private final List<ParameterRow> parameters = new ArrayList<>();
    private final List<FormulaRow> formulas = new ArrayList<>();
    private final ComboBox<String> measurementChoice = new ComboBox<>();

    private final class ParameterRow {
        final TextField name = new TextField();
        final TextField value = new TextField();
        final HBox box;

        ParameterRow(FormulaParameter p) {
            name.setPrefWidth(110);
            name.setText(p.name());
            value.setPrefWidth(110);
            value.setText(ScenarioEditor.format(p.value()));
            box = new HBox(4, name, new Label("="), value, removeButton(() -> {
                parameters.remove(this);
                parameterRows.getChildren().remove(ParameterRow.this.box);
            }));
            box.setAlignment(Pos.CENTER_LEFT);
        }

        FormulaParameter read() {
            String n = name.getText().trim();
            try {
                return new FormulaParameter(n, Double.parseDouble(value.getText().trim()));
            } catch (NumberFormatException e) {
                value.requestFocus();
                throw new IllegalArgumentException("Parameter " + (n.isEmpty() ? "" : n + " ")
                    + "value is not a number: \"" + value.getText() + "\"");
            }
        }
    }

    private final class FormulaRow {
        final TextField name = new TextField();
        final TextField expression = new TextField();
        final HBox box;

        FormulaRow(FormulaDefinition f) {
            name.setPrefWidth(110);
            name.setText(f.name());
            expression.setText(f.expression());
            expression.setPromptText("e.g. d_tx + d_rx + bias");
            HBox.setHgrow(expression, Priority.ALWAYS);
            box = new HBox(4, name, expression, removeButton(() -> {
                formulas.remove(this);
                formulaRows.getChildren().remove(FormulaRow.this.box);
            }));
            box.setAlignment(Pos.CENTER_LEFT);
        }

        FormulaDefinition read() {
            return new FormulaDefinition(name.getText().trim(), expression.getText().trim());
        }
    }

    public FormulaEditor() {
        Button addParameter = new Button("Add parameter");
        addParameter.setOnAction(e -> addParameter(new FormulaParameter(nextName("p", parameters.size(),
            parameters.stream().map(r -> r.name.getText().trim()).toList()), 0)));
        Button addFormula = new Button("Add formula");
        addFormula.setOnAction(e -> addFormula(new FormulaDefinition(nextName("Formula ", formulas.size(),
            formulas.stream().map(r -> r.name.getText().trim()).toList()), "d_tx + d_rx")));

        // Rebuild the choices whenever the list opens, so renamed formulas show up
        measurementChoice.setOnShowing(e -> refreshMeasurementChoices());
        measurementChoice.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(measurementChoice, Priority.ALWAYS);
        HBox measurementBox = new HBox(6, new Label("Measurements from"), measurementChoice);
        measurementBox.setAlignment(Pos.CENTER_LEFT);

        Label help = new Label("Variables: x y z (target), tx_x tx_y tx_z, rx_x rx_y rx_z, "
            + "d_tx d_rx (target to TX / this RX), freq, lambda, gain (dBi), pt (W), c, pi, "
            + "and your parameters. Operators + - * / ^ and sqrt abs exp log log10 sin cos tan "
            + "asin acos atan atan2 hypot pow min max. A formula gives the value each receiver "
            + "measures; the built-in bistatic range is d_tx + d_rx. Every formula can be chosen "
            + "under \"Localize with\" and is included in Compare models.");
        help.setWrapText(true);
        help.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #666;");

        root.setPadding(new Insets(4, 0, 0, 0));
        root.getChildren().addAll(
            sectionLabel("Parameters (name = value)"), parameterRows, addParameter,
            sectionLabel("Formulas (name, formula)"), formulaRows, addFormula,
            measurementBox, help);
        setFormulaSet(FormulaSet.example());
    }

    public VBox getPane() {
        return root;
    }

    /** Fill the form (defaults or a loaded scenario). */
    public void setFormulaSet(FormulaSet set) {
        parameters.clear();
        parameterRows.getChildren().clear();
        for (FormulaParameter p : set.parameters()) {
            addParameter(p);
        }
        formulas.clear();
        formulaRows.getChildren().clear();
        for (FormulaDefinition f : set.formulas()) {
            addFormula(f);
        }
        refreshMeasurementChoices();
        measurementChoice.setValue(set.measurementFormula() == null
            ? BUILT_IN_MEASUREMENT : set.measurementFormula());
    }

    /**
     * Read the form. Only parameter values are checked here; names and
     * formulas are checked by {@link FormulaSet#compile()}.
     *
     * @throws IllegalArgumentException if a parameter value is not a number
     */
    public FormulaSet getFormulaSet() {
        List<FormulaParameter> ps = new ArrayList<>();
        for (ParameterRow row : parameters) {
            ps.add(row.read());
        }
        List<FormulaDefinition> fs = new ArrayList<>();
        for (FormulaRow row : formulas) {
            fs.add(row.read());
        }
        String measurement = measurementChoice.getValue();
        return new FormulaSet(ps, fs, BUILT_IN_MEASUREMENT.equals(measurement) ? null : measurement);
    }

    private void refreshMeasurementChoices() {
        String selected = measurementChoice.getValue();
        List<String> choices = new ArrayList<>();
        choices.add(BUILT_IN_MEASUREMENT);
        for (FormulaRow row : formulas) {
            String name = row.name.getText().trim();
            if (!name.isEmpty() && !choices.contains(name)) choices.add(name);
        }
        measurementChoice.getItems().setAll(choices);
        measurementChoice.setValue(choices.contains(selected) ? selected : BUILT_IN_MEASUREMENT);
    }

    private void addParameter(FormulaParameter p) {
        ParameterRow row = new ParameterRow(p);
        parameters.add(row);
        parameterRows.getChildren().add(row.box);
    }

    private void addFormula(FormulaDefinition f) {
        FormulaRow row = new FormulaRow(f);
        formulas.add(row);
        formulaRows.getChildren().add(row.box);
    }

    private static String nextName(String prefix, int count, List<String> existing) {
        int n = count + 1;
        while (existing.contains(prefix + n)) n++;
        return prefix + n;
    }

    private static Button removeButton(Runnable action) {
        Button remove = new Button("x");
        remove.setMinWidth(Button.USE_PREF_SIZE);
        remove.setTooltip(new Tooltip("Remove"));
        remove.setOnAction(e -> action.run());
        return remove;
    }

    private static Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        return label;
    }
}
