package com.radar.simulator.model;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Vector3D;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for ScenarioPersistence.
 * Verifies save/load round-trip and CSV export.
 */
public class ScenarioPersistenceTest {

    private ScenarioPersistence persistence;
    private String tempScenarioFile;
    private String tempCsvFile;
    private String tempSummaryFile;

    @Before
    public void setUp() {
        persistence = new ScenarioPersistence();
        tempScenarioFile = System.getProperty("java.io.tmpdir") + File.separator + 
            "test_scenario_" + System.nanoTime() + ".properties";
        tempCsvFile = System.getProperty("java.io.tmpdir") + File.separator +
            "test_results_" + System.nanoTime() + ".csv";
        tempSummaryFile = System.getProperty("java.io.tmpdir") + File.separator +
            "test_summary_" + System.nanoTime() + ".txt";
    }

    @After
    public void tearDown() {
        new File(tempScenarioFile).delete();
        new File(tempCsvFile).delete();
        new File(tempSummaryFile).delete();
    }

    @Test
    public void testSaveAndLoadRoundTrip() throws IOException {
        // Create a scenario
        Transmitter tx = new Transmitter("TX1", new Vector3D(100, 200, 300), 5e9, 500.0);
        tx.setOrientation(new Vector3D(10, 20, 30));

        List<Receiver> receivers = new ArrayList<>();
        Receiver rx1 = new Receiver("RX1", new Vector3D(1000, 2000, 50), 5e9, 15.0);
        rx1.setOrientation(new Vector3D(5, 10, 0));
        receivers.add(rx1);
        receivers.add(new Receiver("RX2", new Vector3D(-1000, 3000, 100), 5e9, 12.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -2000, 75), 5e9, 18.0));

        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(
            new Vector3D(500, 1000, 200), new Vector3D(30, -10, 5));

        Scenario original = new Scenario(tx, receivers, trajectory);

        // Save
        persistence.saveScenario(original, 20.0, 0.5, tempScenarioFile);

        // Load
        Scenario loaded = persistence.loadScenario(tempScenarioFile);
        double[] simParams = persistence.loadSimulationParameters(tempScenarioFile);

        // Verify transmitter
        assertEquals("TX1", loaded.getTransmitter().getId());
        assertEquals(100.0, loaded.getTransmitter().getPosition().x, 1e-9);
        assertEquals(200.0, loaded.getTransmitter().getPosition().y, 1e-9);
        assertEquals(300.0, loaded.getTransmitter().getPosition().z, 1e-9);
        assertEquals(5e9, loaded.getTransmitter().getFrequency(), 1e-3);
        assertEquals(500.0, loaded.getTransmitter().getPower(), 1e-9);

        // Verify receivers
        assertEquals(3, loaded.getReceivers().size());
        assertEquals("RX1", loaded.getReceivers().get(0).getId());
        assertEquals(1000.0, loaded.getReceivers().get(0).getPosition().x, 1e-9);
        assertEquals(15.0, loaded.getReceivers().get(0).getGain(), 1e-9);

        // Verify trajectory
        Vector3D pos0 = loaded.getTargetTrajectory().getPosition(0);
        assertEquals(500.0, pos0.x, 1e-9);
        assertEquals(1000.0, pos0.y, 1e-9);
        assertEquals(200.0, pos0.z, 1e-9);

        Vector3D vel = loaded.getTargetTrajectory().getVelocity(0);
        assertEquals(30.0, vel.x, 1e-9);
        assertEquals(-10.0, vel.y, 1e-9);
        assertEquals(5.0, vel.z, 1e-9);

        // Verify simulation parameters
        assertEquals(20.0, simParams[0], 1e-9);
        assertEquals(0.5, simParams[1], 1e-9);
    }

    @Test
    public void testLoadedScenarioProducesSameResults() throws IOException {
        // Create, save, load, and run — results should be identical
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));

        ConstantVelocityTrajectory traj = new ConstantVelocityTrajectory(
            new Vector3D(1000, 2000, 100), new Vector3D(50, 0, 0));
        Scenario original = new Scenario(tx, receivers, traj);

        // Run with original
        ExperimentRunner runner1 = new ExperimentRunner(original,
            new BaselineMeasurementModel(), new BaselineLocalizationModel());
        List<ExperimentRecord> results1 = runner1.run(5.0, 1.0);

        // Save and reload
        persistence.saveScenario(original, 5.0, 1.0, tempScenarioFile);
        Scenario loaded = persistence.loadScenario(tempScenarioFile);

        // Run with loaded
        ExperimentRunner runner2 = new ExperimentRunner(loaded,
            new BaselineMeasurementModel(), new BaselineLocalizationModel());
        List<ExperimentRecord> results2 = runner2.run(5.0, 1.0);

        // Compare results
        assertEquals(results1.size(), results2.size());
        for (int i = 0; i < results1.size(); i++) {
            assertEquals(results1.get(i).positionError(),
                results2.get(i).positionError(), 1e-6);
            assertEquals(results1.get(i).groundTruthPosition().x,
                results2.get(i).groundTruthPosition().x, 1e-6);
        }
    }

    @Test
    public void testCSVExport() throws IOException {
        // Run a small experiment and export
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 2.4e9, 1000.0);
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX2", new Vector3D(-5000, 5000, 0), 2.4e9, 10.0));
        receivers.add(new Receiver("RX3", new Vector3D(0, -5000, 0), 2.4e9, 10.0));

        ConstantVelocityTrajectory traj = new ConstantVelocityTrajectory(
            new Vector3D(1000, 2000, 100), new Vector3D(50, 0, 0));
        Scenario scenario = new Scenario(tx, receivers, traj);

        ExperimentRunner runner = new ExperimentRunner(scenario,
            new BaselineMeasurementModel(), new BaselineLocalizationModel());
        List<ExperimentRecord> records = runner.run(3.0, 1.0);

        // Export
        persistence.exportResultsToCSV(records, tempCsvFile);

        // Verify file exists and has content
        File csvFile = new File(tempCsvFile);
        assertTrue("CSV file should exist", csvFile.exists());
        assertTrue("CSV file should not be empty", csvFile.length() > 0);
    }

    @Test
    public void testEvaluationSummaryExport() throws IOException {
        EvaluationResult evaluation = new EvaluationResult(
            150.5, 300.2, 180.7, 10, 9, 1);

        persistence.exportEvaluationSummary(evaluation, tempSummaryFile);

        File summaryFile = new File(tempSummaryFile);
        assertTrue("Summary file should exist", summaryFile.exists());
        assertTrue("Summary file should not be empty", summaryFile.length() > 0);
    }
}
