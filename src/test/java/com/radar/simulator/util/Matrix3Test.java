package com.radar.simulator.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for Matrix3 using hand-computable cases.
 */
public class Matrix3Test {

    private static final double EPSILON = 1e-9;

    @Test
    public void testSolveDiagonalSystem() {
        double[][] a = {{2, 0, 0}, {0, 4, 0}, {0, 0, 5}};
        double[] x = Matrix3.solve(a, new double[]{2, 8, -10});
        assertArrayEquals(new double[]{1, 2, -2}, x, EPSILON);
    }

    @Test
    public void testSolveNeedsPivoting() {
        // Zero in the top-left forces a row swap
        double[][] a = {{0, 1, 0}, {1, 0, 0}, {0, 0, 1}};
        double[] x = Matrix3.solve(a, new double[]{3, 7, 1});
        assertArrayEquals(new double[]{7, 3, 1}, x, EPSILON);
    }

    @Test
    public void testSolveSingularReturnsNull() {
        double[][] a = {{1, 2, 3}, {2, 4, 6}, {1, 0, 1}};
        assertNull(Matrix3.solve(a, new double[]{1, 2, 3}));
    }

    @Test
    public void testEigenvaluesOfDiagonalMatrix() {
        double[][] a = {{3, 0, 0}, {0, 1, 0}, {0, 0, 2}};
        assertArrayEquals(new double[]{1, 2, 3}, Matrix3.symmetricEigenvalues(a), EPSILON);
    }

    @Test
    public void testEigenvaluesOfSymmetricMatrix() {
        // [[2,1,0],[1,2,0],[0,0,5]] has eigenvalues 1, 3, 5
        double[][] a = {{2, 1, 0}, {1, 2, 0}, {0, 0, 5}};
        assertArrayEquals(new double[]{1, 3, 5}, Matrix3.symmetricEigenvalues(a), EPSILON);
    }
}
