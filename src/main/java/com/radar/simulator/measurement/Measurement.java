package com.radar.simulator.measurement;

import java.util.List;

public record Measurement(
    List<ReceiverMeasurement> perReceiver,
    double time
) {}
