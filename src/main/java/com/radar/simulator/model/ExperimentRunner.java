package com.radar.simulator.model;

import com.radar.simulator.core.Transmitter;
import com.radar.simulator.core.Receiver;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Experiment runner that orchestrates the full simulation pipeline:
 * configuration → trajectory → measurement → localization → evaluation.
 *
 * At each time step it records ground truth, measurements, estimates,
 * model identity, and evaluation data into an ExperimentRecord for
 * reproducibility and downstream analysis.
 */
public class ExperimentRunner {

    private final Scenario scenario;
    private final MeasurementModel measurementModel;
    private final LocalizationModel localizationModel;
    private final GeometryValidator geometryValidator;

    /**
     * Create an experiment runner for the given scenario and models.
     *
     * @param scenario           the scenario containing transmitter, receivers, trajectory
     * @param measurementModel   the measurement generation model
     * @param localizationModel  the localization/estimation model
     */
    public ExperimentRunner(Scenario scenario,
                            MeasurementModel measurementModel,
                            LocalizationModel localizationModel) {
        this.scenario = scenario;
        this.measurementModel = measurementModel;
        this.localizationModel = localizationModel;
        this.geometryValidator = new GeometryValidator();
    }

    /**
     * Run a complete experiment from time 0 to the specified duration.
     *
     * Before running, validates the receiver geometry. If the geometry is
     * insufficient for 3D localization, returns records with an
     * UNDER_CONSTRAINED status rather than silently producing unreliable
     * results.
     *
     * @param duration  total simulation duration in seconds
     * @param timeStep  time step between samples in seconds
     * @return list of experiment records, one per time step
     * @throws IllegalArgumentException if duration or timeStep are invalid
     */
    public List<ExperimentRecord> run(double duration, double timeStep) {
        if (duration <= 0 || timeStep <= 0 || timeStep > duration) {
            throw new IllegalArgumentException(
                "Invalid simulation parameters: duration=" + duration + ", timeStep=" + timeStep);
        }

        Transmitter transmitter = scenario.getTransmitter();
        List<Receiver> receivers = scenario.getReceivers();
        TrajectoryModel trajectory = scenario.getTargetTrajectory();

        // Validate geometry before running
        GeometryValidator.ValidationResult geoResult =
            geometryValidator.validate(transmitter, receivers);

        List<ExperimentRecord> records = new ArrayList<>();

        // Derive time from the step index: accumulating time += timeStep
        // drifts (0.1 + 0.1 + 0.1 > 0.3) and can drop the final step.
        int stepCount = (int) Math.floor(duration / timeStep + 1e-9) + 1;

        for (int stepIndex = 0; stepIndex < stepCount; stepIndex++) {
            double time = stepIndex * timeStep;

            // Get ground truth from trajectory
            Vector3D groundTruthPosition = trajectory.getPosition(time);
            Vector3D groundTruthVelocity = trajectory.getVelocity(time);

            // Generate measurements and stamp them with the simulation time
            Measurement generated = measurementModel.generate(
                transmitter, receivers, groundTruthPosition);
            Measurement measurement = new Measurement(generated.perReceiver(), time);

            EstimationResult estimationResult;
            double positionError;

            if (!geoResult.isValid()) {
                // Report geometry problem explicitly
                estimationResult = new EstimationResult(
                    new Vector3D(0, 0, 0),
                    geoResult.status(),
                    0.0, 0.0
                );
                positionError = Double.NaN;
            } else {
                // Localize
                estimationResult = localizationModel.estimate(
                    transmitter, receivers, measurement);

                // Compute position error
                if (estimationResult.status() == Status.SUCCESS) {
                    positionError = groundTruthPosition.distance(
                        estimationResult.position());
                } else {
                    positionError = Double.NaN;
                }
            }

            ExperimentRecord record = new ExperimentRecord(
                stepIndex,
                time,
                groundTruthPosition,
                groundTruthVelocity,
                measurement,
                estimationResult,
                measurementModel.getDescriptor(),
                localizationModel.getDescriptor(),
                positionError
            );

            records.add(record);
        }

        return records;
    }

    /**
     * Run the experiment and immediately evaluate the results.
     *
     * @param duration  total simulation duration in seconds
     * @param timeStep  time step between samples in seconds
     * @return aggregate evaluation result
     */
    public EvaluationResult runAndEvaluate(double duration, double timeStep) {
        List<ExperimentRecord> records = run(duration, timeStep);
        EvaluationModule evaluationModule = new EvaluationModule();
        return evaluationModule.evaluate(records);
    }
}
