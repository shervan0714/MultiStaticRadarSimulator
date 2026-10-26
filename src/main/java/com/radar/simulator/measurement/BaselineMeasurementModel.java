package com.radar.simulator.measurement;

import com.radar.simulator.core.ModelDescriptor;
import com.radar.simulator.core.RadarPhysics;
import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Baseline implementation of a measurement model.
 *
 * For each receiver it produces:
 * <ul>
 *   <li>BISTATIC_RANGE: |target − TX| + |target − RX| (metres)</li>
 *   <li>TIME_DELAY: bistatic range / c (seconds)</li>
 *   <li>RECEIVED_POWER: from {@link RadarPhysics#calculateReceivedPower} (watts)</li>
 *   <li>AZIMUTH / ELEVATION: direction of the target in the receiver's own
 *       antenna frame (degrees). 0/0 means the target is straight along the
 *       receiver's boresight; positive azimuth is anticlockwise (to the left
 *       when looking along the boresight), positive elevation is upwards.</li>
 * </ul>
 *
 * Optional measurement noise: zero-mean Gaussian noise with standard
 * deviation {@code rangeNoiseStd} is added to each bistatic range (and so to
 * the time delay). The random generator is seeded, so the same seed gives
 * exactly the same noisy measurements for the same run. Angles and power
 * stay noiseless.
 */
public class BaselineMeasurementModel implements MeasurementModel {
    private static final double SPEED_OF_LIGHT = 3e8;

    private final double rangeNoiseStd;
    private final long seed;
    private final Random random;

    /** Noiseless measurements. */
    public BaselineMeasurementModel() {
        this(0.0, 0L);
    }

    /**
     * @param rangeNoiseStd standard deviation of the range noise in metres (0 = noiseless)
     * @param seed          random seed, so noisy runs are reproducible
     * @throws IllegalArgumentException if the standard deviation is negative or not finite
     */
    public BaselineMeasurementModel(double rangeNoiseStd, long seed) {
        if (!(rangeNoiseStd >= 0) || !Double.isFinite(rangeNoiseStd)) {
            throw new IllegalArgumentException("Range noise standard deviation must be >= 0");
        }
        this.rangeNoiseStd = rangeNoiseStd;
        this.seed = seed;
        this.random = new Random(seed);
    }

    /**
     * The version includes the noise settings when noise is on, so every
     * record and CSV row says exactly how its measurements were produced.
     */
    @Override
    public ModelDescriptor getDescriptor() {
        String version = rangeNoiseStd > 0
            ? String.format("1.1 noise=%sm seed=%d", rangeNoiseStd, seed)
            : "1.1";
        return new ModelDescriptor("BaselineMeasurement", version);
    }

    public double getRangeNoiseStd() {
        return rangeNoiseStd;
    }

    @Override
    public Measurement generate(Transmitter transmitter, List<Receiver> receivers, Vector3D targetPosition) {
        List<ReceiverMeasurement> perReceiver = new ArrayList<>();

        // Distance from transmitter to target
        double distTxTarget = transmitter.getPosition().distance(targetPosition);

        for (int i = 0; i < receivers.size(); i++) {
            Receiver receiver = receivers.get(i);

            // Distance from target to receiver
            double distTargetRx = targetPosition.distance(receiver.getPosition());
            double bistaticRange = distTxTarget + distTargetRx;
            if (rangeNoiseStd > 0) {
                bistaticRange += random.nextGaussian() * rangeNoiseStd;
            }
            double timeDelay = bistaticRange / SPEED_OF_LIGHT;

            // Calculate received power using the physics model
            com.radar.simulator.core.Drone tempDrone = new com.radar.simulator.core.Drone("temp", targetPosition, new Vector3D(0,0,0));
            double power = RadarPhysics.calculateReceivedPower(transmitter, tempDrone, receiver);

            double[] angles = anglesInReceiverFrame(receiver, targetPosition);

            Map<Quantity, Double> values = new HashMap<>();
            values.put(Quantity.BISTATIC_RANGE, bistaticRange);
            values.put(Quantity.TIME_DELAY, timeDelay);
            values.put(Quantity.RECEIVED_POWER, power);
            values.put(Quantity.AZIMUTH, angles[0]);
            values.put(Quantity.ELEVATION, angles[1]);

            perReceiver.add(new ReceiverMeasurement(receiver.getId(), values));
        }

        // The model does not know simulation time; ExperimentRunner stamps it
        return new Measurement(perReceiver, 0.0);
    }

    /**
     * Direction of the target as seen by the receiver's antenna.
     *
     * The vector from receiver to target is rotated into the antenna frame:
     * first by −azimuth about the vertical axis, then by the elevation about
     * the new sideways axis, so the boresight becomes the local +x axis.
     *
     * @return {azimuth, elevation} in degrees; azimuth in (−180, 180]
     */
    static double[] anglesInReceiverFrame(Receiver receiver, Vector3D target) {
        Vector3D d = target.subtract(receiver.getPosition());
        double az = Math.toRadians(receiver.getAzimuthDeg());
        double el = Math.toRadians(receiver.getElevationDeg());

        // Undo the antenna azimuth (rotate about Z by −az)
        double x1 = Math.cos(az) * d.x + Math.sin(az) * d.y;
        double y1 = -Math.sin(az) * d.x + Math.cos(az) * d.y;
        double z1 = d.z;

        // Undo the antenna elevation (rotate about the local Y axis)
        double x2 = Math.cos(el) * x1 + Math.sin(el) * z1;
        double z2 = -Math.sin(el) * x1 + Math.cos(el) * z1;

        double localAzimuth = Math.toDegrees(Math.atan2(y1, x2));
        double localElevation = Math.toDegrees(Math.atan2(z2, Math.hypot(x2, y1)));
        return new double[]{localAzimuth, localElevation};
    }
}
