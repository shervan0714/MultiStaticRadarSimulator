package com.radar.simulator.model;

import java.util.Map;

public record ReceiverMeasurement(
    int receiverId,
    Map<Quantity, Double> values
) {}
