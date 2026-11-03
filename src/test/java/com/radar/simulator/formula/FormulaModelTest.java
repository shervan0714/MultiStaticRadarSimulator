package com.radar.simulator.formula;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.experiment.ModelComparison;
import com.radar.simulator.localization.EstimationResult;
import com.radar.simulator.localization.Status;
import com.radar.simulator.measurement.MeasurementModel;
import com.radar.simulator.measurement.Quantity;
import com.radar.simulator.scenario.Scenario;
import com.radar.simulator.ui.ScenarioConfig;
import com.radar.simulator.util.Vector3D;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for formulas defined in the GUI: they must evaluate their variables
 * correctly, localize exactly when the formula matches the data, and be
 * rejected with a readable message when they are wrong.
 */
public class FormulaModelTest {

    private final Scenario scenario = ScenarioConfig.defaultConfig().toScenario();
    private final Transmitter tx = scenario.getTransmitter();
    private final List<Receiver> receivers = scenario.getReceivers();

    private static FormulaSet set(String measurement, FormulaParameter[] params, FormulaDefinition... formulas) {
        return new FormulaSet(List.of(params), List.of(formulas), measurement);
    }

    private static CustomFormula formula(String expression, FormulaParameter... params) {
        return CustomFormula.compile(new FormulaDefinition("f", expression), List.of(params));
    }

    @Test
    public void testBuiltInVariablesHaveTheRightValues() {
        Vector3D target = new Vector3D(100, 200, 300);
        Receiver rx = receivers.get(3);
        assertEquals(0.0, formula("sqrt((x-rx_x)^2 + (y-rx_y)^2 + (z-rx_z)^2) - d_rx")
            .evaluate(target, tx, rx), 1e-9);
        assertEquals(0.0, formula("sqrt((x-tx_x)^2 + (y-tx_y)^2 + (z-tx_z)^2) - d_tx")
            .evaluate(target, tx, rx), 1e-9);
        assertEquals(3e8, formula("lambda * freq + 0 * x").evaluate(target, tx, rx), 1e-3);
        assertEquals(3e8, formula("c + 0 * x").evaluate(target, tx, rx), 1e-3);
        assertEquals(rx.getGain() + tx.getPower(), formula("gain + pt + 0 * z").evaluate(target, tx, rx), 1e-9);
        assertEquals(330.0, formula("z + k", new FormulaParameter("k", 30)).evaluate(target, tx, rx), 1e-12);
    }

    @Test
    public void testRangeFormulaLocalizesLikeTheBaseline() {
        FormulaLocalizationModel model = new FormulaLocalizationModel(formula("d_tx + d_rx"));
        MeasurementModel measurements = new FormulaMeasurementModel(formula("d_tx + d_rx"), 0, 0);
        for (Vector3D target : List.of(new Vector3D(-3000, -2000, 300), new Vector3D(2000, 1000, 1500))) {
            EstimationResult result = model.estimate(tx, receivers, measurements.generate(tx, receivers, target));
            assertEquals(Status.SUCCESS, result.status());
            assertEquals(0.0, result.position().distance(target), 1e-3);
        }
    }

    @Test
    public void testDifferentMeasurementPrincipleOneWayRange() {
        // A one-way (receiver-only) range is a different formulation entirely
        CustomFormula oneWay = formula("d_rx");
        Vector3D target = new Vector3D(1000, -500, 700);
        EstimationResult result = new FormulaLocalizationModel(oneWay).estimate(tx, receivers,
            new FormulaMeasurementModel(oneWay, 0, 0).generate(tx, receivers, target));
        assertEquals(Status.SUCCESS, result.status());
        assertEquals(0.0, result.position().distance(target), 1e-3);
    }

    @Test
    public void testFormulaThatModelsTheDataBeatsTheBuiltInModels() {
        // Measurements carry a 25 m bias; only the formula that knows about it fits exactly
        FormulaSet formulas = FormulaSet.example();
        formulas = new FormulaSet(formulas.parameters(), formulas.formulas(), "Range + bias");

        List<ModelComparison.Entry> entries = ModelComparison.compare(scenario,
            formulas.measurementModel(0, 0), formulas.localizationModels(), 50, 1);

        assertEquals(3, entries.size());
        assertEquals("Formula: Range + bias", entries.get(2).localizationModel().name());
        assertEquals("d_tx + d_rx + bias [bias=25.0]", entries.get(2).localizationModel().version());
        assertEquals("Formula: Range + bias", entries.get(2).records().get(0).measurementModelDescriptor().name());
        assertEquals(0.0, entries.get(2).evaluation().maxError(), 1e-3);
        assertTrue("Baseline should be hurt by the bias",
            entries.get(0).evaluation().meanError() > 5);
    }

    @Test
    public void testFormulaMeasurementKeepsOtherQuantitiesAndAddsSeededNoise() {
        CustomFormula range = formula("d_tx + d_rx");
        Vector3D target = new Vector3D(100, 200, 300);
        var a = new FormulaMeasurementModel(range, 5, 7).generate(tx, receivers, target);
        var b = new FormulaMeasurementModel(range, 5, 7).generate(tx, receivers, target);
        var values = a.perReceiver().get(0).values();
        assertEquals(values.get(Quantity.BISTATIC_RANGE), b.perReceiver().get(0).values().get(Quantity.BISTATIC_RANGE));
        assertNotEquals(range.evaluate(target, tx, receivers.get(0)), values.get(Quantity.BISTATIC_RANGE), 1e-9);
        assertEquals(values.get(Quantity.BISTATIC_RANGE) / 3e8, values.get(Quantity.TIME_DELAY), 1e-15);
        assertTrue(values.containsKey(Quantity.AZIMUTH) && values.containsKey(Quantity.RECEIVED_POWER));
        assertEquals("d_tx + d_rx noise=5.0m seed=7",
            new FormulaMeasurementModel(range, 5, 7).getDescriptor().version());
    }

    @Test
    public void testModelNamesMatchModels() {
        FormulaSet formulas = FormulaSet.example();
        assertEquals(List.of("BaselineLocalization", "ClosedFormLocalization", "Formula: Range + bias"),
            formulas.localizationModelNames());
        assertEquals("Formula: Range + bias",
            formulas.localizationModel("Formula: Range + bias").getDescriptor().name());
        assertEquals("BaselineMeasurement", formulas.measurementModel(0, 0).getDescriptor().name());
    }

    @Test
    public void testInvalidFormulasAreRejectedByName() {
        FormulaParameter[] none = {};
        assertRejected(set(null, none, new FormulaDefinition("Bad", "d_tx + foo")), "Formula \"Bad\"", "foo");
        assertRejected(set(null, none, new FormulaDefinition("Const", "42")), "does not depend on the target");
        assertRejected(set(null, none, new FormulaDefinition("", "x")), "needs a name");
        assertRejected(set(null, none, new FormulaDefinition("A", "x"), new FormulaDefinition("A", "y")),
            "Duplicate formula");
        assertRejected(set("Missing", none, new FormulaDefinition("A", "x")), "does not exist");
        assertRejected(set(null, new FormulaParameter[]{new FormulaParameter("2bad", 1)}), "must start with a letter");
        assertRejected(set(null, new FormulaParameter[]{new FormulaParameter("d_tx", 1)}), "built-in");
        assertRejected(set(null, new FormulaParameter[]{new FormulaParameter("sqrt", 1)}), "built-in");
        assertRejected(set(null, new FormulaParameter[]{new FormulaParameter("k", 1), new FormulaParameter("k", 2)}),
            "Duplicate parameter");
        assertRejected(set(null, new FormulaParameter[]{new FormulaParameter("k", Double.NaN)}), "must be a number");
    }

    private static void assertRejected(FormulaSet formulas, String... expected) {
        try {
            formulas.compile();
            fail("Expected the formulas to be rejected: " + formulas);
        } catch (IllegalArgumentException e) {
            for (String part : expected) {
                assertTrue("Message \"" + e.getMessage() + "\" should contain \"" + part + "\"",
                    e.getMessage().contains(part));
            }
        }
    }
}
