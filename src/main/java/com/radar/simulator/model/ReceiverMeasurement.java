package com.radar.simulator.model;

import java.util.Map;

/**
 * Observation from one receiver at one time step.
 *
 * @param receiverId the {@code Receiver.getId()} this observation belongs to.
 *                   Matching by ID rather than list position means reordering
 *                   the receiver list cannot attach it to the wrong receiver.
 * @param values     measured quantities for this receiver
 */
public record ReceiverMeasurement(
    String receiverId,
    Map<Quantity, Double> values
) {}
