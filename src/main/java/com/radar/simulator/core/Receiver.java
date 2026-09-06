package com.radar.simulator.core;

import com.radar.simulator.util.Vector3D;

/**
 * Receiver station model for multi-static radar system.
 * Represents a fixed radar receiver with position, frequency, and antenna gain.
 */
public class Receiver {
    private String id;
    private Vector3D position;      // Position in meters (X, Y, Z)
    private double frequency;       // Frequency in Hz
    private double gain;            // Antenna gain in dBi
    private Vector3D orientation;   // Antenna orientation

    public Receiver(String id, Vector3D position, double frequency, double gain) {
        this.id = id;
        this.position = new Vector3D(position);
        this.frequency = frequency;
        this.gain = gain;
        this.orientation = new Vector3D(0, 0, 0);  // Default: pointing straight
    }

    // Getters
    public String getId() { return id; }
    public Vector3D getPosition() { return new Vector3D(position); }
    public double getFrequency() { return frequency; }
    public double getGain() { return gain; }
    public Vector3D getOrientation() { return new Vector3D(orientation); }

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

    public void setOrientation(Vector3D orientation) {
        this.orientation = new Vector3D(orientation);
    }

    @Override
    public String toString() {
        return String.format("Receiver[id=%s, pos=%s, freq=%.2e Hz, gain=%.2f dBi]", 
            id, position, frequency, gain);
    }
}
