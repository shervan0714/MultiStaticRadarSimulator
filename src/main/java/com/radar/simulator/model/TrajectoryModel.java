package com.radar.simulator.model;

import com.radar.simulator.util.Vector3D;

/**
 * Trajectory Model interface for target motion generation.
 */
public interface TrajectoryModel {
    /**
     * Get target position at a given time.
     * @param time time in seconds
     * @return target position
     */
    Vector3D getPosition(double time);

    /**
     * Get target velocity at a given time.
     * @param time time in seconds
     * @return target velocity
     */
    Vector3D getVelocity(double time);
}
