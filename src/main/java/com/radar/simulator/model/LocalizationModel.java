package com.radar.simulator.model;

import com.radar.simulator.core.Transmitter;
import com.radar.simulator.core.Receiver;
import java.util.List;

public interface LocalizationModel {
    ModelDescriptor getDescriptor();

    EstimationResult estimate(
        Transmitter transmitter,
        List<Receiver> receivers,
        Measurement measurement
    );
}
