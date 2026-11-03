package com.radar.simulator.formula;

/**
 * A named formula as typed by the user, e.g.
 * {@code ("Range + bias", "d_tx + d_rx + bias")}.
 * See {@link CustomFormula} for the variables it may use.
 */
public record FormulaDefinition(String name, String expression) {}
