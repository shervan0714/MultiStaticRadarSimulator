package com.radar.simulator.formula;

import com.radar.simulator.localization.LocalizationModel;
import com.radar.simulator.localization.LocalizationModels;
import com.radar.simulator.measurement.BaselineMeasurementModel;
import com.radar.simulator.measurement.MeasurementModel;
import com.radar.simulator.util.Expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The parameters and formulas the user defined in the GUI, and which formula
 * (if any) generates the measurements.
 *
 * Plain Java (no JavaFX) so it can be unit tested. {@link #compile()} checks
 * everything and reports the first problem by name; the model builders turn
 * the formulas into measurement and localization models that run through the
 * normal pipeline and can be compared with the built-in models.
 *
 * @param parameters         named constants usable in every formula
 * @param formulas           the user's formulas
 * @param measurementFormula name of the formula that generates the
 *                           measurements, or null/blank for the built-in
 *                           bistatic range
 */
public record FormulaSet(List<FormulaParameter> parameters,
                         List<FormulaDefinition> formulas,
                         String measurementFormula) {

    /** Display prefix that marks a formula model in model lists. */
    public static final String PREFIX = "Formula: ";

    public FormulaSet {
        parameters = List.copyOf(parameters);
        formulas = List.copyOf(formulas);
        measurementFormula = measurementFormula == null || measurementFormula.isBlank()
            ? null : measurementFormula.trim();
    }

    /** No parameters, no formulas, built-in measurements. */
    public static FormulaSet empty() {
        return new FormulaSet(List.of(), List.of(), null);
    }

    /**
     * The example shown when the GUI opens: a range bias parameter and a
     * formula that models it. Measurements stay built-in, so the default run
     * is unchanged.
     */
    public static FormulaSet example() {
        return new FormulaSet(
            List.of(new FormulaParameter("bias", 25)),
            List.of(new FormulaDefinition("Range + bias", "d_tx + d_rx + bias")),
            null);
    }

    /**
     * Check every parameter and formula and compile the formulas.
     *
     * @throws IllegalArgumentException naming the first invalid parameter or formula
     */
    public List<CustomFormula> compile() {
        Set<String> parameterNames = new HashSet<>();
        for (FormulaParameter p : parameters) {
            String name = p.name() == null ? "" : p.name().trim();
            if (!name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException("Parameter name \"" + name
                    + "\" must start with a letter and use only letters, digits and _");
            }
            if (CustomFormula.BUILT_IN_VARIABLES.contains(name) || Expression.CONSTANTS.contains(name)
                    || Expression.FUNCTIONS.containsKey(name)) {
                throw new IllegalArgumentException("Parameter name \"" + name
                    + "\" is already a built-in variable or function");
            }
            if (!parameterNames.add(name)) {
                throw new IllegalArgumentException("Duplicate parameter name: " + name);
            }
            if (!Double.isFinite(p.value())) {
                throw new IllegalArgumentException("Parameter " + name + " must be a number");
            }
        }
        List<FormulaParameter> cleaned = parameters.stream()
            .map(p -> new FormulaParameter(p.name().trim(), p.value())).toList();

        Set<String> formulaNames = new HashSet<>();
        List<CustomFormula> compiled = new ArrayList<>();
        for (FormulaDefinition f : formulas) {
            String name = f.name() == null ? "" : f.name().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Every formula needs a name");
            }
            if (!formulaNames.add(name)) {
                throw new IllegalArgumentException("Duplicate formula name: " + name);
            }
            compiled.add(CustomFormula.compile(new FormulaDefinition(name, f.expression()), cleaned));
        }
        if (measurementFormula != null && !formulaNames.contains(measurementFormula)) {
            throw new IllegalArgumentException("Measurements use formula \"" + measurementFormula
                + "\", which does not exist");
        }
        return compiled;
    }

    /**
     * The model that generates the measurements: the chosen formula, or the
     * built-in bistatic range.
     */
    public MeasurementModel measurementModel(double rangeNoiseStd, long seed) {
        if (measurementFormula == null) {
            return new BaselineMeasurementModel(rangeNoiseStd, seed);
        }
        for (CustomFormula formula : compile()) {
            if (formula.name().equals(measurementFormula)) {
                return new FormulaMeasurementModel(formula, rangeNoiseStd, seed);
            }
        }
        throw new IllegalStateException("unreachable: compile() checked the name");
    }

    /** Every localization model: the built-in ones, then one per formula. */
    public List<LocalizationModel> localizationModels() {
        List<LocalizationModel> models = new ArrayList<>(LocalizationModels.builtIns());
        for (CustomFormula formula : compile()) {
            models.add(new FormulaLocalizationModel(formula));
        }
        return models;
    }

    /** The names shown in the GUI's model list, in the same order as {@link #localizationModels()}. */
    public List<String> localizationModelNames() {
        List<String> names = new ArrayList<>();
        for (LocalizationModel model : LocalizationModels.builtIns()) {
            names.add(model.getDescriptor().name());
        }
        for (FormulaDefinition f : formulas) {
            names.add(PREFIX + (f.name() == null ? "" : f.name().trim()));
        }
        return names;
    }

    /**
     * The localization model with the given display name.
     *
     * @throws IllegalArgumentException if there is no such model
     */
    public LocalizationModel localizationModel(String displayName) {
        for (LocalizationModel model : localizationModels()) {
            if (model.getDescriptor().name().equals(displayName)) {
                return model;
            }
        }
        throw new IllegalArgumentException("No localization model called \"" + displayName + "\"");
    }
}
