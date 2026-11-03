package com.radar.simulator.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for the user-formula parser: hand-computable values, precedence and
 * readable error messages.
 */
public class ExpressionTest {

    private static final List<String> VARS = List.of("x", "y", "d_tx");

    private static double eval(String text, double x, double y, double dTx) {
        return Expression.parse(text, VARS).evaluate(new double[]{x, y, dTx});
    }

    @Test
    public void testArithmeticAndPrecedence() {
        assertEquals(7.0, eval("1 + 2 * 3", 0, 0, 0), 1e-12);
        assertEquals(9.0, eval("(1 + 2) * 3", 0, 0, 0), 1e-12);
        assertEquals(2.0, eval("8 / 2 / 2", 0, 0, 0), 1e-12);
        assertEquals(1.0, eval("5 - 3 - 1", 0, 0, 0), 1e-12);
        assertEquals(512.0, eval("2 ^ 3 ^ 2", 0, 0, 0), 1e-9);   // right-associative
        assertEquals(-4.0, eval("-2 ^ 2", 0, 0, 0), 1e-12);     // -(2^2)
        assertEquals(0.5, eval("2 ^ -1", 0, 0, 0), 1e-12);
        assertEquals(3e8, eval("3e8", 0, 0, 0), 1e-3);
        assertEquals(0.025, eval("2.5E-2", 0, 0, 0), 1e-15);
    }

    @Test
    public void testVariablesAndFunctions() {
        assertEquals(5.0, eval("sqrt(x^2 + y^2)", 3, 4, 0), 1e-12);
        assertEquals(5.0, eval("hypot(x, y)", 3, 4, 0), 1e-12);
        assertEquals(1030.0, eval("d_tx + 30", 0, 0, 1000), 1e-12);
        assertEquals(Math.PI / 4, eval("atan2(y, x)", 1, 1, 0), 1e-12);
        assertEquals(4.0, eval("max(x, y)", 3, 4, 0), 1e-12);
        assertEquals(2.0, eval("log10(100)", 0, 0, 0), 1e-12);
        assertEquals(-1.0, eval("cos(pi)", 0, 0, 0), 1e-12);
    }

    @Test
    public void testUsesReportsOnlyMentionedVariables() {
        Expression e = Expression.parse("x + 2*x", VARS);
        assertTrue(e.uses("x"));
        assertFalse(e.uses("y"));
        assertEquals("x + 2*x", e.text());
    }

    @Test
    public void testErrorsAreReadable() {
        assertError("x + foo", "Unknown name \"foo\"");
        assertError("bar(x)", "Unknown function \"bar\"");
        assertError("(x + 1", "Missing ')'");
        assertError("x +", "ends too early");
        assertError("x $ 2", "Unexpected '$'");
        assertError("atan2(x)", "atan2 needs 2 arguments");
        assertError("  ", "empty");
        assertError("1.2.3", "Bad number");
    }

    private static void assertError(String text, String expected) {
        try {
            Expression.parse(text, VARS);
            fail("Expected an error for: " + text);
        } catch (IllegalArgumentException e) {
            assertTrue("Message \"" + e.getMessage() + "\" should contain \"" + expected + "\"",
                e.getMessage().contains(expected));
        }
    }
}
