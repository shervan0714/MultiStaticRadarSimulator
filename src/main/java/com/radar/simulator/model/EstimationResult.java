package com.radar.simulator.model;

import com.radar.simulator.util.Vector3D;

public record EstimationResult(
    Vector3D position,
    Status status,
    double residual,
    double conditionNumber
) {}
