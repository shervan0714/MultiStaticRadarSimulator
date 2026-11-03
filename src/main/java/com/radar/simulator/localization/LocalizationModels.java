package com.radar.simulator.localization;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry of the built-in localization formulations, so the GUI and CLI can
 * offer a choice and compare them. Formulas typed in the GUI are added on top
 * of these (see {@code formula.FormulaSet}).
 *
 * To add a new built-in formulation, implement {@link LocalizationModel} and
 * add one line to {@link #BUILT_IN}.
 */
public final class LocalizationModels {

    private LocalizationModels() {}

    /** Short key (used by the CLI {@code --model} option) → new model instance. */
    private static final Map<String, Supplier<LocalizationModel>> BUILT_IN = new LinkedHashMap<>();
    static {
        BUILT_IN.put("baseline", BaselineLocalizationModel::new);
        BUILT_IN.put("closed-form", ClosedFormLocalizationModel::new);
    }

    /** The keys of the built-in models, in display order. */
    public static List<String> keys() {
        return List.copyOf(BUILT_IN.keySet());
    }

    /** One new instance of every built-in model, in display order. */
    public static List<LocalizationModel> builtIns() {
        return BUILT_IN.values().stream().map(Supplier::get).toList();
    }

    /**
     * @throws IllegalArgumentException listing the valid keys if the key is unknown
     */
    public static LocalizationModel byKey(String key) {
        Supplier<LocalizationModel> supplier = BUILT_IN.get(key);
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown localization model \"" + key
                + "\"; choose one of " + String.join(", ", BUILT_IN.keySet()));
        }
        return supplier.get();
    }
}
