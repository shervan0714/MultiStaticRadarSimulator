package com.radar.simulator.core;

import com.radar.simulator.util.Vector3D;

/**
 * Transmitter station model for multi-static radar system.
 * Represents a fixed radar transmitter with position, frequency, and power.
 */
public class Transmitter {
    private String id;
    private Vector3D position;      // Position in meters (X, Y, Z)
    private double frequency;       // Frequency in Hz
    private double power;           // Transmit power in Watts
    private Vector3D orientation;   // Antenna orientation (azimuth, elevation, roll)

    public Transmitter(String id, Vector3D position, double frequency, double power) {
        this.id = id;
        this.position = new Vector3D(position);
        this.frequency = frequency;
        this.power = power;
        this.orientation = new Vector3D(0, 0, 0);  // Default: pointing straight
    }

    // Getters
    public String getId() { return id; }
    public Vector3D getPosition() { return new Vector3D(position); }
    public double getFrequency() { return frequency; }
    public double getPower() { return power; }
    public Vector3D getOrientation() { return new Vector3D(orientation); }

    // Setters (for real-time parameter adjustment)
    public void setPosition(Vector3D pos) {
        this.position = new Vector3D(pos);
    }

    public void setFrequency(double freq) {
        this.frequency = freq;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public void setOrientation(Vector3D orientation) {
        this.orientation = new Vector3D(orientation);
    }

    @Override
    public String toString() {
        return String.format("Transmitter[id=%s, pos=%s, freq=%.2e Hz, power=%.2f W]", 
            id, position, frequency, power);
    }
}
