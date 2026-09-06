package com.radar.simulator.core;

import org.junit.Test;
import com.radar.simulator.util.Vector3D;
import static org.junit.Assert.*;

/**
 * Unit tests for Transmitter class
 */
public class TransmitterTest {

    @Test
    public void testTransmitterCreation() {
        Vector3D pos = new Vector3D(0, 0, 1000);
        Transmitter tx = new Transmitter("TX1", pos, 10e9, 1.0);
        
        assertEquals("TX1", tx.getId());
        assertEquals(1000, tx.getPosition().z, 0.001);
        assertEquals(10e9, tx.getFrequency(), 0.001);
        assertEquals(1.0, tx.getPower(), 0.001);
    }

    @Test
    public void testTransmitterPositionUpdate() {
        Transmitter tx = new Transmitter("TX1", new Vector3D(0, 0, 0), 10e9, 1.0);
        Vector3D newPos = new Vector3D(100, 200, 300);
        tx.setPosition(newPos);
        
        assertEquals(100, tx.getPosition().x, 0.001);
        assertEquals(200, tx.getPosition().y, 0.001);
        assertEquals(300, tx.getPosition().z, 0.001);
    }
}
