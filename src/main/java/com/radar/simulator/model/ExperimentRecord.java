package com.radar.simulator.model;

import com.radar.simulator.util.Vector3D;

/**
 * Immutable record of a single simulation time step.
 * Captures ground truth, measurement, localization estimate, model identity,
 * and per-step evaluation data for reproducibility and analysis.
 */
public record ExperimentRecord(
    int stepIndex,
    double time,
    Vector3D groundTruthPosition,
    Vector3D groundTruthVelocity,
    Measurement measurement,
    EstimationResult estimationResult,
    ModelDescriptor measurementModelDescriptor,
    ModelDescriptor localizationModelDescriptor,
    double positionError
) {}
