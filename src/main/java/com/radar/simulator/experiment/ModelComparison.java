package com.radar.simulator.experiment;

import com.radar.simulator.core.ModelDescriptor;
import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.localization.LocalizationModel;
import com.radar.simulator.measurement.Measurement;
import com.radar.simulator.measurement.MeasurementModel;
import com.radar.simulator.scenario.Scenario;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs several localization formulations on the same scenario and the same
 * measurements, so their errors can be compared side by side (proposal
 * acceptance criterion: run two formulations and compare).
 *
 * The measurements are generated once, by the first run, and replayed to
 * every later model. A noisy measurement model therefore gives every model
 * exactly the same noisy data.
 */
public final class ModelComparison {

    private ModelComparison() {}

    /** One model's run: its records and aggregate metrics. */
    public record Entry(ModelDescriptor localizationModel,
                        List<ExperimentRecord> records,
                        EvaluationResult evaluation) {}

    /**
     * @return one entry per localization model, in the given order
     */
    public static List<Entry> compare(Scenario scenario, MeasurementModel measurementModel,
                                      List<LocalizationModel> localizationModels,
                                      double duration, double timeStep) {
        ReplayingMeasurementModel replay = new ReplayingMeasurementModel(measurementModel);
        EvaluationModule evaluation = new EvaluationModule();
        List<Entry> entries = new ArrayList<>();
        for (LocalizationModel model : localizationModels) {
            List<ExperimentRecord> records =
                new ExperimentRunner(scenario, replay, model).run(duration, timeStep);
            entries.add(new Entry(model.getDescriptor(), records, evaluation.evaluate(records)));
            replay.rewind();
        }
        return entries;
    }

    /**
     * Records the measurements of the first pass and replays them, in order,
     * on every later pass.
     */
    private static final class ReplayingMeasurementModel implements MeasurementModel {
        private final MeasurementModel source;
        private final List<Measurement> recorded = new ArrayList<>();
        private int next;
        private boolean replaying;

        ReplayingMeasurementModel(MeasurementModel source) {
            this.source = source;
        }

        void rewind() {
            replaying = true;
            next = 0;
        }

        @Override
        public ModelDescriptor getDescriptor() {
            return source.getDescriptor();
        }

        @Override
        public Measurement generate(Transmitter transmitter, List<Receiver> receivers, Vector3D targetPosition) {
            if (replaying) {
                return recorded.get(next++);
            }
            Measurement measurement = source.generate(transmitter, receivers, targetPosition);
            recorded.add(measurement);
            return measurement;
        }
    }
}
