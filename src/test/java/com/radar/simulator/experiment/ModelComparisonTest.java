package com.radar.simulator.experiment;

import com.radar.simulator.localization.LocalizationModels;
import com.radar.simulator.measurement.BaselineMeasurementModel;
import com.radar.simulator.measurement.Quantity;
import com.radar.simulator.scenario.Scenario;
import com.radar.simulator.ui.ScenarioConfig;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for running several localization formulations side by side.
 */
public class ModelComparisonTest {

    private final Scenario scenario = ScenarioConfig.defaultConfig().toScenario();

    @Test
    public void testNoiselessComparisonGivesOneEntryPerModel() {
        List<ModelComparison.Entry> entries = ModelComparison.compare(scenario,
            new BaselineMeasurementModel(), LocalizationModels.builtIns(), 100, 1);

        assertEquals(2, entries.size());
        assertEquals("BaselineLocalization", entries.get(0).localizationModel().name());
        assertEquals("ClosedFormLocalization", entries.get(1).localizationModel().name());
        for (ModelComparison.Entry entry : entries) {
            assertEquals(101, entry.records().size());
            assertEquals(0, entry.evaluation().failedSteps());
            assertEquals(0.0, entry.evaluation().maxError(), 1e-3);
            assertEquals(entry.localizationModel(), entry.records().get(0).localizationModelDescriptor());
        }
    }

    @Test
    public void testEveryModelSeesExactlyTheSameNoisyMeasurements() {
        List<ModelComparison.Entry> entries = ModelComparison.compare(scenario,
            new BaselineMeasurementModel(5.0, 42L), LocalizationModels.builtIns(), 20, 1);

        List<ExperimentRecord> a = entries.get(0).records();
        List<ExperimentRecord> b = entries.get(1).records();
        for (int step = 0; step < a.size(); step++) {
            for (int rx = 0; rx < a.get(step).measurement().perReceiver().size(); rx++) {
                assertEquals(
                    a.get(step).measurement().perReceiver().get(rx).values().get(Quantity.BISTATIC_RANGE),
                    b.get(step).measurement().perReceiver().get(rx).values().get(Quantity.BISTATIC_RANGE));
            }
            assertEquals(a.get(step).time(), b.get(step).time(), 0.0);
        }
        // Noise is really there: the models do not reproduce the truth exactly
        assertTrue(entries.get(0).evaluation().meanError() > 0.1);
        assertTrue(entries.get(1).evaluation().meanError() > 0.1);
    }

    @Test
    public void testComparisonMatchesASeparateSingleModelRun() {
        List<ModelComparison.Entry> entries = ModelComparison.compare(scenario,
            new BaselineMeasurementModel(5.0, 42L), LocalizationModels.builtIns(), 20, 1);
        EvaluationResult single = new ExperimentRunner(scenario,
            new BaselineMeasurementModel(5.0, 42L), LocalizationModels.byKey("closed-form"))
            .runAndEvaluate(20, 1);
        assertEquals(single.meanError(), entries.get(1).evaluation().meanError(), 1e-9);
    }
}
