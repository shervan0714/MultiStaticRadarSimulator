package com.radar.simulator.localization;

import com.radar.simulator.core.ModelDescriptor;
import com.radar.simulator.measurement.Measurement;
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
