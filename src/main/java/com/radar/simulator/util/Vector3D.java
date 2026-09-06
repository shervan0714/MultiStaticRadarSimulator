package com.radar.simulator.util;

/**
 * 3D Vector utility class for position and velocity calculations.
 * Supports vector operations needed for radar simulator physics.
 */
public class Vector3D {
    public double x, y, z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Copy constructor
     */
    public Vector3D(Vector3D v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    /**
     * Add two vectors: this + v
     */
    public Vector3D add(Vector3D v) {
        return new Vector3D(this.x + v.x, this.y + v.y, this.z + v.z);
    }

    /**
     * Subtract two vectors: this - v
     */
    public Vector3D subtract(Vector3D v) {
        return new Vector3D(this.x - v.x, this.y - v.y, this.z - v.z);
    }

    /**
     * Scale vector by scalar: this * scalar
     */
    public Vector3D scale(double scalar) {
        return new Vector3D(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    /**
     * Dot product: this · v
     */
    public double dot(Vector3D v) {
        return this.x * v.x + this.y * v.y + this.z * v.z;
    }

    /**
     * Cross product: this × v
     */
    public Vector3D cross(Vector3D v) {
        return new Vector3D(
            this.y * v.z - this.z * v.y,
            this.z * v.x - this.x * v.z,
            this.x * v.y - this.y * v.x
        );
    }

    /**
     * Euclidean distance from this point to another point
     */
    public double distance(Vector3D v) {
        double dx = this.x - v.x;
        double dy = this.y - v.y;
        double dz = this.z - v.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Magnitude (length) of this vector
     */
    public double magnitude() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    /**
     * Normalized vector (unit vector in same direction)
     */
    public Vector3D normalize() {
        double mag = magnitude();
        if (mag == 0) return new Vector3D(0, 0, 0);
        return new Vector3D(this.x / mag, this.y / mag, this.z / mag);
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Vector3D)) return false;
        Vector3D v = (Vector3D) obj;
        return this.x == v.x && this.y == v.y && this.z == v.z;
    }
}
