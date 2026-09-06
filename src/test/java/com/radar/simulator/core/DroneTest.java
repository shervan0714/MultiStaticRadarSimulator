package com.radar.simulator.core;

import org.junit.Test;
import com.radar.simulator.util.Vector3D;
import static org.junit.Assert.*;

/**
 * Unit tests for Drone class
 */
public class DroneTest {

    @Test
    public void testDroneCreation() {
        Vector3D pos = new Vector3D(0, 0, 0);
        Vector3D vel = new Vector3D(10, 0, 0);
        Drone drone = new Drone("DRONE1", pos, vel);
        
        assertEquals("DRONE1", drone.getId());
        assertEquals(0, drone.getPosition().x, 0.001);
        assertEquals(10, drone.getVelocity().x, 0.001);
    }

    @Test
    public void testDroneTrajectory_constantVelocity() {
        Vector3D pos = new Vector3D(0, 0, 0);
        Vector3D vel = new Vector3D(10, 0, 0);  // 10 m/s in X direction
        Drone drone = new Drone("DRONE1", pos, vel);
        
        // After 1 second
        drone.updatePosition(1.0);
        assertEquals(10.0, drone.getPosition().x, 0.001);
        assertEquals(0, drone.getPosition().y, 0.001);
    }

    @Test
    public void testDroneVelocityUpdate() {
        Drone drone = new Drone("DRONE1", new Vector3D(0, 0, 0), new Vector3D(10, 0, 0));
        Vector3D newVel = new Vector3D(5, 5, 0);
        drone.setVelocity(newVel);
        
        assertEquals(5, drone.getVelocity().x, 0.001);
        assertEquals(5, drone.getVelocity().y, 0.001);
    }
}
