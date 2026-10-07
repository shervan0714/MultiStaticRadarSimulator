package com.radar.simulator.ui;

import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps simulator world coordinates to JavaFX scene coordinates.
 *
 * World (design doc 1.1): right-handed, metres, +X East, +Y North, +Z Up.
 * JavaFX 3D: +X right, +Y down, +Z into the screen (also right-handed).
 *
 * Mapping, after centring on the scene and scaling to a fixed scene size:
 * <pre>
 *   scene.x =  East  * scale
 *   scene.y = -Up    * scale
 *   scene.z =  North * scale
 * </pre>
 * East x North = Up maps to (+x) x (+z) = (-y), so handedness is preserved
 * and nothing in the view is mirrored.
 */
public final class SceneTransform {

    private final Vector3D center;
    private final double scale;

    public SceneTransform(Vector3D center, double scale) {
        if (!(scale > 0)) {
            throw new IllegalArgumentException("Scale must be positive");
        }
        this.center = new Vector3D(center);
        this.scale = scale;
    }

    /**
     * A transform that centres the bounding box of the points and scales its
     * largest dimension to {@code sceneSize} scene units.
     */
    public static SceneTransform fitting(List<Vector3D> points, double sceneSize) {
        if (points.isEmpty()) {
            return new SceneTransform(new Vector3D(0, 0, 0), 1.0);
        }
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Vector3D p : points) {
            minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
            minZ = Math.min(minZ, p.z); maxZ = Math.max(maxZ, p.z);
        }
        Vector3D center = new Vector3D((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
        double extent = Math.max(Math.max(maxX - minX, maxY - minY), Math.max(maxZ - minZ, 1.0));
        return new SceneTransform(center, sceneSize / extent);
    }

    /** World position (m) to scene position. */
    public Vector3D toScene(Vector3D world) {
        Vector3D d = world.subtract(center);
        return new Vector3D(d.x * scale, -d.z * scale, d.y * scale);
    }

    /** World length (m) to scene length. */
    public double toSceneLength(double metres) {
        return metres * scale;
    }

    public Vector3D getCenter() {
        return new Vector3D(center);
    }

    public double getScale() {
        return scale;
    }

    /**
     * Evenly spread indices 0..count-1 keeping at most {@code max}, always
     * including the first and last. Used to keep long trajectories
     * responsive in the 3D view while full-resolution results stay in the
     * experiment records.
     */
    public static List<Integer> sampleIndices(int count, int max) {
        List<Integer> indices = new ArrayList<>();
        if (count <= 0) {
            return indices;
        }
        if (count <= max || max < 2) {
            for (int i = 0; i < count; i++) indices.add(i);
            return indices;
        }
        for (int k = 0; k < max; k++) {
            indices.add((int) Math.round(k * (count - 1) / (double) (max - 1)));
        }
        return indices;
    }
}
