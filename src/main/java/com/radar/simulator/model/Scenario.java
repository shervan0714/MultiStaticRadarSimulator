package com.radar.simulator.model;

import com.radar.simulator.core.Transmitter;
import com.radar.simulator.core.Receiver;
import java.util.ArrayList;
import java.util.List;

/**
 * Scenario model containing transmitter, receivers, and target trajectory.
 */
public class Scenario {
    private Transmitter transmitter;
    private List<Receiver> receivers;
    private TrajectoryModel targetTrajectory;

    public Scenario(Transmitter transmitter, List<Receiver> receivers, TrajectoryModel targetTrajectory) {
        this.transmitter = transmitter;
        this.receivers = new ArrayList<>(receivers);
        this.targetTrajectory = targetTrajectory;
    }

    public Transmitter getTransmitter() {
        return transmitter;
    }

    public void setTransmitter(Transmitter transmitter) {
        this.transmitter = transmitter;
    }

    public List<Receiver> getReceivers() {
        return new ArrayList<>(receivers);
    }

    public void setReceivers(List<Receiver> receivers) {
        this.receivers = new ArrayList<>(receivers);
    }

    public void addReceiver(Receiver receiver) {
        this.receivers.add(receiver);
    }

    public TrajectoryModel getTargetTrajectory() {
        return targetTrajectory;
    }

    public void setTargetTrajectory(TrajectoryModel targetTrajectory) {
        this.targetTrajectory = targetTrajectory;
    }
}
