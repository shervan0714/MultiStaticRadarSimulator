package com.radar.simulator.formula;

import com.radar.simulator.core.Receiver;
import com.radar.simulator.core.Transmitter;
import com.radar.simulator.util.Expression;
import com.radar.simulator.util.Vector3D;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * A user-defined formula for the value one receiver measures, compiled with
 * the current parameter values.
 *
 * The formula may use these built-in variables (all SI units):
 * <pre>
 *   x, y, z            target position (m)
 *   tx_x, tx_y, tx_z   transmitter position (m)
 *   rx_x, rx_y, rx_z   this receiver's position (m)
 *   d_tx, d_rx         target distance to the transmitter / this receiver (m)
 *   freq, lambda       carrier frequency (Hz) and wavelength (m)
 *   gain               this receiver's gain (dBi)
 *   pt                 transmit power (W)
 *   c                  speed of light, 3e8 m/s
 * </pre>
 * plus every parameter the user defines (e.g. {@code bias = 25}). The
 * built-in bistatic range is {@code d_tx + d_rx}.
 */
public final class CustomFormula {

    public static final double SPEED_OF_LIGHT = 3e8;

    /** Variables every formula can use, in evaluation order. */
    public static final List<String> BUILT_IN_VARIABLES = List.of(
        "x", "y", "z", "tx_x", "tx_y", "tx_z", "rx_x", "rx_y", "rx_z",
        "d_tx", "d_rx", "freq", "lambda", "gain", "pt", "c");

    /** Variables that depend on the target; a formula must use at least one. */
    static final List<String> TARGET_VARIABLES = List.of("x", "y", "z", "d_tx", "d_rx");

    private final FormulaDefinition definition;
    private final List<FormulaParameter> parameters;
    private final Expression expression;

    private CustomFormula(FormulaDefinition definition, List<FormulaParameter> parameters,
                          Expression expression) {
        this.definition = definition;
        this.parameters = parameters;
        this.expression = expression;
    }

    /**
     * Parse the formula with the given parameters.
     *
     * @throws IllegalArgumentException naming the formula if the text is
     *         invalid or does not depend on the target position
     */
    public static CustomFormula compile(FormulaDefinition definition, List<FormulaParameter> parameters) {
        List<String> variables = new ArrayList<>(BUILT_IN_VARIABLES);
        for (FormulaParameter p : parameters) {
            variables.add(p.name());
        }
        Expression expression;
        try {
            expression = Expression.parse(definition.expression(), variables);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Formula \"" + definition.name() + "\": " + e.getMessage());
        }
        if (TARGET_VARIABLES.stream().noneMatch(expression::uses)) {
            throw new IllegalArgumentException("Formula \"" + definition.name()
                + "\" does not depend on the target: use at least one of "
                + String.join(", ", TARGET_VARIABLES));
        }
        return new CustomFormula(definition, List.copyOf(parameters), expression);
    }

    /** The value this formula predicts for one receiver and target position. */
    public double evaluate(Vector3D target, Transmitter transmitter, Receiver receiver) {
        Vector3D t = transmitter.getPosition();
        Vector3D r = receiver.getPosition();
        double[] values = new double[BUILT_IN_VARIABLES.size() + parameters.size()];
        values[0] = target.x;
        values[1] = target.y;
        values[2] = target.z;
        values[3] = t.x;
        values[4] = t.y;
        values[5] = t.z;
        values[6] = r.x;
        values[7] = r.y;
        values[8] = r.z;
        values[9] = target.distance(t);
        values[10] = target.distance(r);
        values[11] = receiver.getFrequency();
        values[12] = SPEED_OF_LIGHT / receiver.getFrequency();
        values[13] = receiver.getGain();
        values[14] = transmitter.getPower();
        values[15] = SPEED_OF_LIGHT;
        for (int i = 0; i < parameters.size(); i++) {
            values[BUILT_IN_VARIABLES.size() + i] = parameters.get(i).value();
        }
        return expression.evaluate(values);
    }

    public String name() {
        return definition.name();
    }

    /**
     * The formula text and the values of the parameters it uses, e.g.
     * {@code d_tx + d_rx + bias [bias=25.0]}; used as the model version so
     * every record says exactly which formula produced it.
     */
    public String describe() {
        StringJoiner used = new StringJoiner(" ", " [", "]").setEmptyValue("");
        for (FormulaParameter p : parameters) {
            if (expression.uses(p.name())) {
                used.add(p.name() + "=" + p.value());
            }
        }
        return expression.text() + used;
    }
}
