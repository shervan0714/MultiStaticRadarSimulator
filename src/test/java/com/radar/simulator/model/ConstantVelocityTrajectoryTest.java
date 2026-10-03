package com.radar.simulator.model;

import com.radar.simulator.util.Vector3D;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ConstantVelocityTrajectory.
 * Verifies positions against the analytical solution at known time steps.
 */
public class ConstantVelocityTrajectoryTest {

    @Test
    public void testPositionAtTimeZero() {
        Vector3D initPos = new Vector3D(100, 200, 300);
        Vector3D velocity = new Vector3D(10, 20, 30);
        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(initPos, velocity);

        Vector3D pos = trajectory.getPosition(0.0);
        assertEquals(100.0, pos.x, 1e-9);
        assertEquals(200.0, pos.y, 1e-9);
        assertEquals(300.0, pos.z, 1e-9);
    }

    @Test
    public void testPositionAtKnownTime() {
        // position(t) = initPos + velocity * t
        Vector3D initPos = new Vector3D(0, 0, 0);
        Vector3D velocity = new Vector3D(50, 0, 0);
        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(initPos, velocity);

        // After 10 seconds: (500, 0, 0)
        Vector3D pos = trajectory.getPosition(10.0);
        assertEquals(500.0, pos.x, 1e-9);
        assertEquals(0.0, pos.y, 1e-9);
        assertEquals(0.0, pos.z, 1e-9);
    }

    @Test
    public void testPositionWith3DMotion() {
        Vector3D initPos = new Vector3D(1000, 2000, 100);
        Vector3D velocity = new Vector3D(50, -10, 5);
        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(initPos, velocity);

        // After 5 seconds: (1250, 1950, 125)
        Vector3D pos = trajectory.getPosition(5.0);
        assertEquals(1250.0, pos.x, 1e-9);
        assertEquals(1950.0, pos.y, 1e-9);
        assertEquals(125.0, pos.z, 1e-9);
    }

    @Test
    public void testVelocityIsConstant() {
        Vector3D initPos = new Vector3D(0, 0, 0);
        Vector3D velocity = new Vector3D(50, -10, 5);
        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(initPos, velocity);

        Vector3D v0 = trajectory.getVelocity(0.0);
        Vector3D v5 = trajectory.getVelocity(5.0);
        Vector3D v100 = trajectory.getVelocity(100.0);

        assertEquals(v0.x, v5.x, 1e-9);
        assertEquals(v0.y, v5.y, 1e-9);
        assertEquals(v0.z, v5.z, 1e-9);
        assertEquals(v0.x, v100.x, 1e-9);
        assertEquals(v0.y, v100.y, 1e-9);
        assertEquals(v0.z, v100.z, 1e-9);
    }

    @Test
    public void testDefensiveCopy() {
        // Modifying the original vectors after construction should not affect the trajectory
        Vector3D initPos = new Vector3D(100, 200, 300);
        Vector3D velocity = new Vector3D(10, 20, 30);
        ConstantVelocityTrajectory trajectory = new ConstantVelocityTrajectory(initPos, velocity);

        // Mutate originals
        initPos.x = 999;
        velocity.x = 999;

        // Trajectory should be unaffected
        Vector3D pos = trajectory.getPosition(0.0);
        assertEquals(100.0, pos.x, 1e-9);

        Vector3D vel = trajectory.getVelocity(0.0);
        assertEquals(10.0, vel.x, 1e-9);
    }
}
