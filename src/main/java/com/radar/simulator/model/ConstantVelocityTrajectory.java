package com.radar.simulator.model;

import com.radar.simulator.util.Vector3D;

/**
 * Constant velocity trajectory implementation.
 */
public class ConstantVelocityTrajectory implements TrajectoryModel {
    private final Vector3D initialPosition;
    private final Vector3D velocity;

    public ConstantVelocityTrajectory(Vector3D initialPosition, Vector3D velocity) {
        this.initialPosition = new Vector3D(initialPosition);
        this.velocity = new Vector3D(velocity);
    }

    @Override
    public Vector3D getPosition(double time) {
        return initialPosition.add(velocity.scale(time));
    }

    @Override
    public Vector3D getVelocity(double time) {
        return new Vector3D(velocity);
    }
}
