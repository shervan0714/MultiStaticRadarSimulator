package com.radar.simulator.formula;

/**
 * A named constant the user can use in formulas, e.g. {@code bias = 25}.
 * Parameters are known values, not estimated: the localizer only solves for
 * the target position.
 */
public record FormulaParameter(String name, double value) {}
