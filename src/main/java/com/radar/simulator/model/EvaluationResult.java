package com.radar.simulator.model;

/**
 * Aggregate evaluation metrics computed from a sequence of experiment records.
 * Reports mean, maximum, and RMS position error, plus the number of
 * successful vs. failed localization attempts.
 */
public record EvaluationResult(
    double meanError,
    double maxError,
    double rmsError,
    int totalSteps,
    int successfulSteps,
    int failedSteps
) {}
