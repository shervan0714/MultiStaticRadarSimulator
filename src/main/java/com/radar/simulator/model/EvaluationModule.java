package com.radar.simulator.model;

import java.util.List;

/**
 * Evaluation module that computes aggregate error statistics from a
 * sequence of experiment records.
 *
 * Computes mean error, maximum error, and RMS (root-mean-square) error
 * against hand-calculated values, as specified in the design document's
 * test plan.
 */
public class EvaluationModule {

    /**
     * Compute aggregate evaluation metrics from experiment records.
     *
     * @param records list of per-step experiment records
     * @return aggregate evaluation result with mean, max, and RMS error
     * @throws IllegalArgumentException if records list is null or empty
     */
    public EvaluationResult evaluate(List<ExperimentRecord> records) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("Cannot evaluate empty record list");
        }

        double sumError = 0.0;
        double sumSquaredError = 0.0;
        double maxError = 0.0;
        int successfulSteps = 0;
        int failedSteps = 0;

        for (ExperimentRecord record : records) {
            Status status = record.estimationResult().status();

            if (status == Status.SUCCESS) {
                double error = record.positionError();
                sumError += error;
                sumSquaredError += error * error;
                if (error > maxError) {
                    maxError = error;
                }
                successfulSteps++;
            } else {
                failedSteps++;
            }
        }

        if (successfulSteps == 0) {
            return new EvaluationResult(
                Double.NaN, Double.NaN, Double.NaN,
                records.size(), 0, failedSteps
            );
        }

        double meanError = sumError / successfulSteps;
        double rmsError = Math.sqrt(sumSquaredError / successfulSteps);

        return new EvaluationResult(
            meanError, maxError, rmsError,
            records.size(), successfulSteps, failedSteps
        );
    }
}
