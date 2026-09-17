package com.radar.simulator.model;

import com.radar.simulator.core.Transmitter;
import com.radar.simulator.core.Receiver;
import com.radar.simulator.util.Vector3D;
import java.util.List;

public interface MeasurementModel {
    ModelDescriptor getDescriptor();

    Measurement generate(
        Transmitter transmitter,
        List<Receiver> receivers,
        Vector3D targetPosition
    );
}
