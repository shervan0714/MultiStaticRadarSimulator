package com.radar.simulator.core;

import com.radar.simulator.util.Vector3D;

/**
 * Drone model for multi-static radar simulator.
 * Represents a moving target with position, velocity, and RCS properties.
 */
public class Drone {
    private Vector3D position;          // Current position in meters (X, Y, Z)
    private Vector3D velocity;          // Velocity vector in m/s (Vx, Vy, Vz)
    private Vector3D rcsSignature;      // Radar cross-section properties
    private String id;

    public Drone(String id, Vector3D initialPosition, Vector3D initialVelocity) {
        this.id = id;
        this.position = new Vector3D(initialPosition);
        this.velocity = new Vector3D(initialVelocity);
        this.rcsSignature = new Vector3D(0.1, 0.1, 0.1);  // Default RCS
    }

    /**
     * Update drone position based on constant velocity and time step
     * position(t+dt) = position(t) + velocity * dt
     * 
     * @param deltaTime Time step in seconds
     */
    public void updatePosition(double deltaTime) {
        Vector3D displacement = velocity.scale(deltaTime);
        position = position.add(displacement);
    }

    // Getters
    public String getId() { return id; }
    public Vector3D getPosition() { return new Vector3D(position); }
    public Vector3D getVelocity() { return new Vector3D(velocity); }
    public Vector3D getRCSSignature() { return new Vector3D(rcsSignature); }

    // Setters (for real-time parameter adjustment)
    public void setPosition(Vector3D pos) {
        this.position = new Vector3D(pos);
    }

    public void setVelocity(Vector3D vel) {
        this.velocity = new Vector3D(vel);
    }

    public void setRCSSignature(Vector3D rcs) {
        this.rcsSignature = new Vector3D(rcs);
    }

    @Override
    public String toString() {
        return String.format("Drone[id=%s, pos=%s, vel=%s]", 
            id, position, velocity);
    }
}
