package com.radar.simulator.core;

import org.junit.Test;
import com.radar.simulator.util.Vector3D;
import static org.junit.Assert.*;

/**
 * Unit tests for Receiver class.
 * Verifies creation, position/orientation updates, and defensive copying.
 */
public class ReceiverTest {

    @Test
    public void testReceiverCreation() {
        Vector3D pos = new Vector3D(5000, 5000, 0);
        Receiver rx = new Receiver("RX1", pos, 2.4e9, 10.0);

        assertEquals("RX1", rx.getId());
        assertEquals(5000, rx.getPosition().x, 0.001);
        assertEquals(5000, rx.getPosition().y, 0.001);
        assertEquals(0, rx.getPosition().z, 0.001);
        assertEquals(2.4e9, rx.getFrequency(), 0.001);
        assertEquals(10.0, rx.getGain(), 0.001);
    }

    @Test
    public void testReceiverPositionUpdate() {
        Receiver rx = new Receiver("RX1", new Vector3D(0, 0, 0), 10e9, 0);
        Vector3D newPos = new Vector3D(1000, 2000, 500);
        rx.setPosition(newPos);

        assertEquals(1000, rx.getPosition().x, 0.001);
        assertEquals(2000, rx.getPosition().y, 0.001);
        assertEquals(500, rx.getPosition().z, 0.001);
    }

    @Test
    public void testReceiverOrientationUpdate() {
        Receiver rx = new Receiver("RX1", new Vector3D(0, 0, 0), 10e9, 0);
        assertEquals(0, rx.getOrientation().x, 0.001);

        rx.setOrientation(new Vector3D(45, 30, 0));
        assertEquals(45, rx.getOrientation().x, 0.001);
        assertEquals(30, rx.getOrientation().y, 0.001);
    }

    @Test
    public void testDefensiveCopy() {
        Vector3D pos = new Vector3D(100, 200, 300);
        Receiver rx = new Receiver("RX1", pos, 10e9, 0);

        // Mutating the original should not affect the receiver
        pos.x = 999;
        assertEquals(100, rx.getPosition().x, 0.001);

        // Mutating the returned position should not affect the receiver
        Vector3D returned = rx.getPosition();
        returned.x = 888;
        assertEquals(100, rx.getPosition().x, 0.001);
    }

    @Test
    public void testFrequencyUpdate() {
        Receiver rx = new Receiver("RX1", new Vector3D(0, 0, 0), 10e9, 0);
        rx.setFrequency(5e9);
        assertEquals(5e9, rx.getFrequency(), 0.001);
    }

    @Test
    public void testGainUpdate() {
        Receiver rx = new Receiver("RX1", new Vector3D(0, 0, 0), 10e9, 10.0);
        rx.setGain(25.0);
        assertEquals(25.0, rx.getGain(), 0.001);
    }
}
