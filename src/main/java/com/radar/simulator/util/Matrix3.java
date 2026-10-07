package com.radar.simulator.util;

/**
 * Small 3x3 linear-algebra helpers used by the localization solver.
 * Matrices are row-major double[3][3] arrays.
 */
public final class Matrix3 {

    private Matrix3() {}

    /**
     * Solve A x = b by Gaussian elimination with partial pivoting.
     *
     * @return the solution, or null if A is singular
     */
    public static double[] solve(double[][] a, double[] b) {
        double[][] m = new double[3][4];
        double maxAbs = 0;
        for (int i = 0; i < 3; i++) {
            System.arraycopy(a[i], 0, m[i], 0, 3);
            m[i][3] = b[i];
            for (int j = 0; j < 3; j++) {
                maxAbs = Math.max(maxAbs, Math.abs(a[i][j]));
            }
        }
        double singularThreshold = Math.max(maxAbs * 1e-14, 1e-300);

        for (int col = 0; col < 3; col++) {
            int pivot = col;
            for (int row = col + 1; row < 3; row++) {
                if (Math.abs(m[row][col]) > Math.abs(m[pivot][col])) {
                    pivot = row;
                }
            }
            if (Math.abs(m[pivot][col]) < singularThreshold) {
                return null;
            }
            double[] tmp = m[col];
            m[col] = m[pivot];
            m[pivot] = tmp;

            for (int row = col + 1; row < 3; row++) {
                double factor = m[row][col] / m[col][col];
                for (int k = col; k < 4; k++) {
                    m[row][k] -= factor * m[col][k];
                }
            }
        }

        double[] x = new double[3];
        for (int row = 2; row >= 0; row--) {
            double sum = m[row][3];
            for (int k = row + 1; k < 3; k++) {
                sum -= m[row][k] * x[k];
            }
            x[row] = sum / m[row][row];
        }
        return x;
    }

    /**
     * Eigenvalues of a symmetric 3x3 matrix using cyclic Jacobi rotations.
     *
     * @return the three eigenvalues in ascending order
     */
    public static double[] symmetricEigenvalues(double[][] a) {
        double[][] m = new double[3][3];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(a[i], 0, m[i], 0, 3);
        }

        for (int sweep = 0; sweep < 50; sweep++) {
            double offDiagonal = m[0][1] * m[0][1] + m[0][2] * m[0][2] + m[1][2] * m[1][2];
            double diagonal = m[0][0] * m[0][0] + m[1][1] * m[1][1] + m[2][2] * m[2][2];
            if (offDiagonal <= 1e-30 * diagonal || offDiagonal == 0) break;

            for (int p = 0; p < 2; p++) {
                for (int q = p + 1; q < 3; q++) {
                    if (Math.abs(m[p][q]) < 1e-300) continue;
                    double theta = (m[q][q] - m[p][p]) / (2 * m[p][q]);
                    double t = Math.signum(theta) / (Math.abs(theta) + Math.sqrt(theta * theta + 1));
                    if (theta == 0) t = 1;
                    double c = 1 / Math.sqrt(t * t + 1);
                    double s = t * c;

                    for (int k = 0; k < 3; k++) {
                        double mkp = m[k][p];
                        double mkq = m[k][q];
                        m[k][p] = c * mkp - s * mkq;
                        m[k][q] = s * mkp + c * mkq;
                    }
                    for (int k = 0; k < 3; k++) {
                        double mpk = m[p][k];
                        double mqk = m[q][k];
                        m[p][k] = c * mpk - s * mqk;
                        m[q][k] = s * mpk + c * mqk;
                    }
                }
            }
        }

        double[] eig = {m[0][0], m[1][1], m[2][2]};
        java.util.Arrays.sort(eig);
        return eig;
    }
}
