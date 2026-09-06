package com.radar.simulator.core;

import org.junit.Test;
import com.radar.simulator.util.Vector3D;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Unit tests for RadarPhysics class
 */
public class RadarPhysicsTest {

    @Test
    public void testCalculateError_zeroDifference() {
        Vector3D pos1 = new Vector3D(0, 0, 0);
        Vector3D pos2 = new Vector3D(0, 0, 0);
        double error = RadarPhysics.calculateError(pos1, pos2);
        
        assertEquals(0.0, error, 0.001);
    }

    @Test
    public void testCalculateError_knownDistance() {
        Vector3D actual = new Vector3D(0, 0, 0);
        Vector3D calculated = new Vector3D(3, 4, 0);
        double error = RadarPhysics.calculateError(actual, calculated);
        
        assertEquals(5.0, error, 0.001);
    }

    @Test
    public void testPowerConversion_wattsToDBm() {
        // 1 Watt = 30 dBm
        double dbm = RadarPhysics.powerToDBm(1.0);
        assertEquals(30.0, dbm, 0.1);
    }

    @Test
    public void testPowerConversion_roundTrip() {
        double original = 0.1;  // Watts
        double dbm = RadarPhysics.powerToDBm(original);
        double recovered = RadarPhysics.powerFromDBm(dbm);
        
        assertEquals(original, recovered, 0.0001);
    }

    @Test
    public void testTriangulateDronePosition_minimumReceivers() {
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(10000, 0, 0), 10e9, 0));
        receivers.add(new Receiver("RX2", new Vector3D(-10000, 0, 0), 10e9, 0));
        receivers.add(new Receiver("RX3", new Vector3D(0, 10000, 0), 10e9, 0));
        
        List<Double> powers = new ArrayList<>();
        powers.add(0.001);
        powers.add(0.001);
        powers.add(0.001);
        
        Vector3D result = RadarPhysics.triangulateDronePosition(receivers, powers);
        assertNotNull(result);
    }

    @Test
    public void testTriangulateDronePosition_insufficientReceivers() {
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(new Receiver("RX1", new Vector3D(10000, 0, 0), 10e9, 0));
        
        List<Double> powers = new ArrayList<>();
        powers.add(0.001);
        
        assertThrows(IllegalArgumentException.class, () -> {
            RadarPhysics.triangulateDronePosition(receivers, powers);
        });
    }
}
