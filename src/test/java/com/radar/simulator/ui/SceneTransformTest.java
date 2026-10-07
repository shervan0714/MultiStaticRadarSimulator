package com.radar.simulator.ui;

import com.radar.simulator.util.Vector3D;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Verifies the world-to-scene transform used by the 3D view
 * (design doc test plan: Visualization).
 */
public class SceneTransformTest {

    private static final double EPSILON = 1e-9;

    private final SceneTransform unit = new SceneTransform(new Vector3D(0, 0, 0), 1.0);

    @Test
    public void testAxesMapToJavaFxAxes() {
        assertVector(new Vector3D(1, 0, 0), unit.toScene(new Vector3D(1, 0, 0)));   // East -> +x
        assertVector(new Vector3D(0, 0, 1), unit.toScene(new Vector3D(0, 1, 0)));   // North -> +z
        assertVector(new Vector3D(0, -1, 0), unit.toScene(new Vector3D(0, 0, 1)));  // Up -> -y (JavaFX y is down)
    }

    @Test
    public void testHandednessIsPreserved() {
        // East x North = Up must still hold after mapping, otherwise the view is mirrored
        Vector3D east = unit.toScene(new Vector3D(1, 0, 0));
        Vector3D north = unit.toScene(new Vector3D(0, 1, 0));
        Vector3D up = unit.toScene(new Vector3D(0, 0, 1));
        assertVector(up, east.cross(north));
    }

    @Test
    public void testCentreAndScale() {
        SceneTransform t = new SceneTransform(new Vector3D(100, 200, 300), 0.5);
        assertVector(new Vector3D(0, 0, 0), t.toScene(new Vector3D(100, 200, 300)));
        assertVector(new Vector3D(5, -15, 10), t.toScene(new Vector3D(110, 220, 330)));
        assertEquals(50.0, t.toSceneLength(100), EPSILON);
    }

    @Test
    public void testFittingCentresBoundingBoxAndScalesLargestExtent() {
        List<Vector3D> points = List.of(
            new Vector3D(-5000, -1000, 0),
            new Vector3D(5000, 3000, 400));
        SceneTransform t = SceneTransform.fitting(points, 1000);

        assertVector(new Vector3D(0, 1000, 200), t.getCenter());
        assertEquals(0.1, t.getScale(), EPSILON);  // 1000 scene units / 10000 m in X
        assertVector(new Vector3D(500, -20, 200), t.toScene(new Vector3D(5000, 3000, 400)));
    }

    @Test
    public void testFittingSinglePointDoesNotDivideByZero() {
        SceneTransform t = SceneTransform.fitting(List.of(new Vector3D(7, 7, 7)), 1000);
        assertTrue(Double.isFinite(t.getScale()));
        assertVector(new Vector3D(0, 0, 0), t.toScene(new Vector3D(7, 7, 7)));
    }

    @Test
    public void testSampleIndicesKeepsAllWhenUnderLimit() {
        assertEquals(List.of(0, 1, 2, 3), SceneTransform.sampleIndices(4, 10));
        assertTrue(SceneTransform.sampleIndices(0, 10).isEmpty());
    }

    @Test
    public void testSampleIndicesDownsamplesIncludingEndpoints() {
        List<Integer> indices = SceneTransform.sampleIndices(10001, 400);
        assertEquals(400, indices.size());
        assertEquals(Integer.valueOf(0), indices.get(0));
        assertEquals(Integer.valueOf(10000), indices.get(399));
        for (int k = 1; k < indices.size(); k++) {
            assertTrue(indices.get(k) > indices.get(k - 1));
        }
    }

    @Test
    public void testNiceGridStep() {
        assertEquals(1000.0, Visualization3D.niceStep(1000), EPSILON);
        assertEquals(2000.0, Visualization3D.niceStep(1250), EPSILON);
        assertEquals(5.0, Visualization3D.niceStep(3.2), EPSILON);
    }

    private static void assertVector(Vector3D expected, Vector3D actual) {
        assertEquals("x", expected.x, actual.x, EPSILON);
        assertEquals("y", expected.y, actual.y, EPSILON);
        assertEquals("z", expected.z, actual.z, EPSILON);
    }
}
