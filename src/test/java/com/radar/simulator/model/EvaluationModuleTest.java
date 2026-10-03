package com.radar.simulator.model;

import com.radar.simulator.util.Vector3D;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for EvaluationModule.
 * Verifies mean, max, and RMS error against hand-calculated values.
 */
public class EvaluationModuleTest {

    private final EvaluationModule evaluationModule = new EvaluationModule();

    @Test
    public void testMeanErrorHandCalculated() {
        // Errors: 3, 4, 5. Mean = 4.0
        List<ExperimentRecord> records = createRecordsWithErrors(3.0, 4.0, 5.0);

        EvaluationResult result = evaluationModule.evaluate(records);

        assertEquals(4.0, result.meanError(), 1e-9);
        assertEquals(3, result.totalSteps());
        assertEquals(3, result.successfulSteps());
        assertEquals(0, result.failedSteps());
    }

    @Test
    public void testMaxError() {
        // Errors: 1, 7, 3, 2. Max = 7.0
        List<ExperimentRecord> records = createRecordsWithErrors(1.0, 7.0, 3.0, 2.0);

        EvaluationResult result = evaluationModule.evaluate(records);

        assertEquals(7.0, result.maxError(), 1e-9);
    }

    @Test
    public void testRmsError() {
        // Errors: 3, 4. RMS = sqrt((9 + 16)/2) = sqrt(12.5) ≈ 3.5355
        List<ExperimentRecord> records = createRecordsWithErrors(3.0, 4.0);

        EvaluationResult result = evaluationModule.evaluate(records);

        double expectedRMS = Math.sqrt((9.0 + 16.0) / 2.0);
        assertEquals(expectedRMS, result.rmsError(), 1e-6);
    }

    @Test
    public void testSingleStep() {
        List<ExperimentRecord> records = createRecordsWithErrors(5.0);

        EvaluationResult result = evaluationModule.evaluate(records);

        assertEquals(5.0, result.meanError(), 1e-9);
        assertEquals(5.0, result.maxError(), 1e-9);
        assertEquals(5.0, result.rmsError(), 1e-9);
        assertEquals(1, result.totalSteps());
        assertEquals(1, result.successfulSteps());
    }

    @Test
    public void testWithFailedSteps() {
        List<ExperimentRecord> records = new ArrayList<>();

        // Two successful records with error 3 and 4
        records.add(createRecordWithError(0, 0.0, 3.0, Status.SUCCESS));
        records.add(createRecordWithError(1, 1.0, 4.0, Status.SUCCESS));

        // One failed record
        records.add(createRecordWithError(2, 2.0, Double.NaN, Status.UNDER_CONSTRAINED));

        EvaluationResult result = evaluationModule.evaluate(records);

        assertEquals(3.5, result.meanError(), 1e-9);
        assertEquals(4.0, result.maxError(), 1e-9);
        assertEquals(3, result.totalSteps());
        assertEquals(2, result.successfulSteps());
        assertEquals(1, result.failedSteps());
    }

    @Test
    public void testAllFailedSteps() {
        List<ExperimentRecord> records = new ArrayList<>();
        records.add(createRecordWithError(0, 0.0, Double.NaN, Status.UNDER_CONSTRAINED));
        records.add(createRecordWithError(1, 1.0, Double.NaN, Status.NUMERICALLY_UNSTABLE));

        EvaluationResult result = evaluationModule.evaluate(records);

        assertTrue(Double.isNaN(result.meanError()));
        assertTrue(Double.isNaN(result.rmsError()));
        assertEquals(2, result.totalSteps());
        assertEquals(0, result.successfulSteps());
        assertEquals(2, result.failedSteps());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyRecordListThrows() {
        evaluationModule.evaluate(new ArrayList<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullRecordListThrows() {
        evaluationModule.evaluate(null);
    }

    // --- Helper methods ---

    private List<ExperimentRecord> createRecordsWithErrors(double... errors) {
        List<ExperimentRecord> records = new ArrayList<>();
        for (int i = 0; i < errors.length; i++) {
            records.add(createRecordWithError(i, i * 1.0, errors[i], Status.SUCCESS));
        }
        return records;
    }

    private ExperimentRecord createRecordWithError(int step, double time,
                                                    double error, Status status) {
        Vector3D pos = new Vector3D(0, 0, 0);
        Vector3D vel = new Vector3D(0, 0, 0);
        EstimationResult estResult = new EstimationResult(pos, status, 0.0, 1.0);
        Measurement measurement = new Measurement(new ArrayList<>(), time);
        ModelDescriptor measDesc = new ModelDescriptor("TestMeas", "1.0");
        ModelDescriptor locDesc = new ModelDescriptor("TestLoc", "1.0");

        return new ExperimentRecord(step, time, pos, vel, measurement,
            estResult, measDesc, locDesc, error);
    }
}
