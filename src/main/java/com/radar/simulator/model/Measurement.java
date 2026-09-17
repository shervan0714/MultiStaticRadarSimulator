package com.radar.simulator.model;

import java.util.List;

public record Measurement(
    List<ReceiverMeasurement> perReceiver,
    double time
) {}
