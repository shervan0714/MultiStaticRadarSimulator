package com.radar.simulator.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Vector3D utility class
 */
public class Vector3DTest {

    @Test
    public void testDistance_zeroDistance() {
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(0, 0, 0);
        assertEquals(0.0, v1.distance(v2), 0.001);
    }

    @Test
    public void testDistance_knownPoints() {
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(3, 4, 0);
        assertEquals(5.0, v1.distance(v2), 0.001);
    }

    @Test
    public void testMagnitude() {
        Vector3D v = new Vector3D(3, 4, 0);
        assertEquals(5.0, v.magnitude(), 0.001);
    }

    @Test
    public void testAdd() {
        Vector3D v1 = new Vector3D(1, 2, 3);
        Vector3D v2 = new Vector3D(4, 5, 6);
        Vector3D result = v1.add(v2);
        assertEquals(5.0, result.x, 0.001);
        assertEquals(7.0, result.y, 0.001);
        assertEquals(9.0, result.z, 0.001);
    }

    @Test
    public void testScale() {
        Vector3D v = new Vector3D(1, 2, 3);
        Vector3D result = v.scale(2);
        assertEquals(2.0, result.x, 0.001);
        assertEquals(4.0, result.y, 0.001);
        assertEquals(6.0, result.z, 0.001);
    }

    @Test
    public void testNormalize() {
        Vector3D v = new Vector3D(3, 4, 0);
        Vector3D normalized = v.normalize();
        assertEquals(1.0, normalized.magnitude(), 0.001);
    }

    @Test
    public void testDot() {
        Vector3D v1 = new Vector3D(1, 0, 0);
        Vector3D v2 = new Vector3D(0, 1, 0);
        assertEquals(0.0, v1.dot(v2), 0.001);
    }
}
