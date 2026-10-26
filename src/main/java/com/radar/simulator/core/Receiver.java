package com.radar.simulator.core;

import com.radar.simulator.util.Vector3D;

/**
 * Receiver station model for multi-static radar system.
 * Represents a fixed radar receiver with position, frequency, antenna gain
 * and antenna orientation.
 *
 * Orientation follows the design document convention: azimuth in degrees,
 * measured counter-clockwise from +X (East) towards +Y (North); elevation
 * in degrees, measured upwards from the horizontal plane.
 */
public class Receiver {
    private String id;
    private Vector3D position;      // Position in meters (X, Y, Z)
    private double frequency;       // Frequency in Hz
    private double gain;            // Antenna gain in dBi
    private double azimuthDeg;      // Boresight azimuth, CCW from East
    private double elevationDeg;    // Boresight elevation above horizontal, [-90, 90]

    public Receiver(String id, Vector3D position, double frequency, double gain) {
        this.id = id;
        this.position = new Vector3D(position);
        this.frequency = frequency;
        this.gain = gain;
        this.azimuthDeg = 0.0;       // Default: pointing East, horizontal
        this.elevationDeg = 0.0;
    }

    // Getters
    public String getId() { return id; }
    public Vector3D getPosition() { return new Vector3D(position); }
    public double getFrequency() { return frequency; }
    public double getGain() { return gain; }
    public double getAzimuthDeg() { return azimuthDeg; }
    public double getElevationDeg() { return elevationDeg; }

    /**
     * Unit vector the antenna points along (its boresight), in world ENU coordinates.
     */
    public Vector3D getBoresight() {
        double az = Math.toRadians(azimuthDeg);
        double el = Math.toRadians(elevationDeg);
        return new Vector3D(Math.cos(el) * Math.cos(az), Math.cos(el) * Math.sin(az), Math.sin(el));
    }

    // Setters (for real-time parameter adjustment)
    public void setPosition(Vector3D pos) {
        this.position = new Vector3D(pos);
    }

    public void setFrequency(double freq) {
        this.frequency = freq;
    }

    public void setGain(double gain) {
        this.gain = gain;
    }

    /**
     * Set the antenna orientation.
     *
     * @param azimuthDeg   degrees counter-clockwise from East (any finite value)
     * @param elevationDeg degrees above the horizontal, between -90 and 90
     * @throws IllegalArgumentException if a value is not finite or elevation is out of range
     */
    public void setOrientation(double azimuthDeg, double elevationDeg) {
        if (!Double.isFinite(azimuthDeg) || !Double.isFinite(elevationDeg)) {
            throw new IllegalArgumentException("Orientation angles must be finite");
        }
        if (elevationDeg < -90 || elevationDeg > 90) {
            throw new IllegalArgumentException("Elevation must be between -90 and 90 degrees");
        }
        this.azimuthDeg = azimuthDeg;
        this.elevationDeg = elevationDeg;
    }

    @Override
    public String toString() {
        return String.format("Receiver[id=%s, pos=%s, freq=%.2e Hz, gain=%.2f dBi, az=%.1f°, el=%.1f°]",
            id, position, frequency, gain, azimuthDeg, elevationDeg);
    }
}
