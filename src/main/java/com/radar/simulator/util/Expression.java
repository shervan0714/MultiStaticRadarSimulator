package com.radar.simulator.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A small arithmetic expression typed by the user, e.g.
 * {@code d_tx + d_rx + bias} or {@code sqrt((x-rx_x)^2 + (y-rx_y)^2)}.
 *
 * The text is parsed once into a tree and then evaluated many times with
 * different variable values. Only arithmetic is possible: no Java code or
 * script is ever run, so a formula typed in the GUI cannot do anything else.
 *
 * Grammar (usual precedence, ^ is right-associative and binds tighter than
 * unary minus, so -x^2 = -(x^2)):
 * <pre>
 *   expr    = term (('+' | '-') term)*
 *   term    = unary (('*' | '/') unary)*
 *   unary   = ('+' | '-') unary | power
 *   power   = primary ('^' unary)?
 *   primary = number | name | name '(' expr (',' expr)* ')' | '(' expr ')'
 * </pre>
 * Numbers may use exponents (3e8). The constant {@code pi} and the functions
 * in {@link #FUNCTIONS} are built in.
 */
public final class Expression {

    /** Built-in functions and their number of arguments. */
    public static final Map<String, Integer> FUNCTIONS = Map.ofEntries(
        Map.entry("sqrt", 1), Map.entry("abs", 1), Map.entry("exp", 1),
        Map.entry("log", 1), Map.entry("log10", 1),
        Map.entry("sin", 1), Map.entry("cos", 1), Map.entry("tan", 1),
        Map.entry("asin", 1), Map.entry("acos", 1), Map.entry("atan", 1),
        Map.entry("atan2", 2), Map.entry("hypot", 2), Map.entry("pow", 2),
        Map.entry("min", 2), Map.entry("max", 2));

    /** Built-in constants. */
    public static final Set<String> CONSTANTS = Set.of("pi");

    private interface Node {
        double eval(double[] values);
    }

    private final String text;
    private final Node root;
    private final Set<String> usedVariables;

    private Expression(String text, Node root, Set<String> usedVariables) {
        this.text = text;
        this.root = root;
        this.usedVariables = usedVariables;
    }

    /**
     * Parse an expression over the given variables.
     *
     * @param text      the formula
     * @param variables allowed variable names; {@link #evaluate} takes their
     *                  values in this order
     * @throws IllegalArgumentException with a readable message (what is wrong
     *         and at which character) if the text is not a valid formula
     */
    public static Expression parse(String text, List<String> variables) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("The formula is empty");
        }
        Parser parser = new Parser(text, variables);
        Node root = parser.parseExpression();
        parser.skipSpaces();
        if (parser.pos < text.length()) {
            throw parser.error("Unexpected '" + text.charAt(parser.pos) + "'");
        }
        return new Expression(text.trim(), root, Set.copyOf(parser.used));
    }

    /**
     * @param values variable values, in the order given to {@link #parse}
     */
    public double evaluate(double[] values) {
        return root.eval(values);
    }

    /** True if the formula mentions the variable. */
    public boolean uses(String variable) {
        return usedVariables.contains(variable);
    }

    public String text() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }

    private static final class Parser {
        final String s;
        final List<String> variables;
        final Set<String> used = new LinkedHashSet<>();
        int pos;

        Parser(String s, List<String> variables) {
            this.s = s;
            this.variables = variables;
        }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + (pos + 1)
                + " of \"" + s.trim() + "\"");
        }

        void skipSpaces() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        boolean accept(char c) {
            skipSpaces();
            if (pos < s.length() && s.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        Node parseExpression() {
            Node left = parseTerm();
            while (true) {
                if (accept('+')) {
                    Node l = left, r = parseTerm();
                    left = v -> l.eval(v) + r.eval(v);
                } else if (accept('-')) {
                    Node l = left, r = parseTerm();
                    left = v -> l.eval(v) - r.eval(v);
                } else {
                    return left;
                }
            }
        }

        Node parseTerm() {
            Node left = parseUnary();
            while (true) {
                if (accept('*')) {
                    Node l = left, r = parseUnary();
                    left = v -> l.eval(v) * r.eval(v);
                } else if (accept('/')) {
                    Node l = left, r = parseUnary();
                    left = v -> l.eval(v) / r.eval(v);
                } else {
                    return left;
                }
            }
        }

        Node parseUnary() {
            if (accept('-')) {
                Node operand = parseUnary();
                return v -> -operand.eval(v);
            }
            if (accept('+')) {
                return parseUnary();
            }
            return parsePower();
        }

        Node parsePower() {
            Node base = parsePrimary();
            if (accept('^')) {
                Node exponent = parseUnary();
                return v -> Math.pow(base.eval(v), exponent.eval(v));
            }
            return base;
        }

        Node parsePrimary() {
            skipSpaces();
            if (pos >= s.length()) {
                throw error("The formula ends too early");
            }
            char c = s.charAt(pos);
            if (accept('(')) {
                Node inner = parseExpression();
                if (!accept(')')) throw error("Missing ')'");
                return inner;
            }
            if (Character.isDigit(c) || c == '.') {
                return parseNumber();
            }
            if (Character.isLetter(c) || c == '_') {
                return parseName();
            }
            throw error("Unexpected '" + c + "'");
        }

        Node parseNumber() {
            int start = pos;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
            if (pos < s.length() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                int save = pos;
                pos++;
                if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
                if (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                    while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
                } else {
                    pos = save;  // not an exponent, e.g. "2e" is invalid below
                }
            }
            String number = s.substring(start, pos);
            try {
                double value = Double.parseDouble(number);
                return v -> value;
            } catch (NumberFormatException e) {
                pos = start;
                throw error("Bad number \"" + number + "\"");
            }
        }

        Node parseName() {
            int start = pos;
            while (pos < s.length() && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos) == '_')) pos++;
            String name = s.substring(start, pos);

            skipSpaces();
            if (pos < s.length() && s.charAt(pos) == '(') {
                Integer arity = FUNCTIONS.get(name);
                if (arity == null) {
                    pos = start;
                    throw error("Unknown function \"" + name + "\"");
                }
                pos++;
                List<Node> args = new ArrayList<>();
                args.add(parseExpression());
                while (accept(',')) {
                    args.add(parseExpression());
                }
                if (!accept(')')) throw error("Missing ')' after the arguments of " + name);
                if (args.size() != arity) {
                    pos = start;
                    throw error(name + " needs " + arity + " argument" + (arity == 1 ? "" : "s")
                        + ", got " + args.size());
                }
                return function(name, args);
            }

            int index = variables.indexOf(name);
            if (index >= 0) {
                used.add(name);
                return v -> v[index];
            }
            if (name.equals("pi")) {
                return v -> Math.PI;
            }
            pos = start;
            throw error("Unknown name \"" + name + "\" (variables: " + String.join(", ", variables) + ")");
        }

        static Node function(String name, List<Node> args) {
            Node a = args.get(0);
            Node b = args.size() > 1 ? args.get(1) : null;
            return switch (name) {
                case "sqrt" -> v -> Math.sqrt(a.eval(v));
                case "abs" -> v -> Math.abs(a.eval(v));
                case "exp" -> v -> Math.exp(a.eval(v));
                case "log" -> v -> Math.log(a.eval(v));
                case "log10" -> v -> Math.log10(a.eval(v));
                case "sin" -> v -> Math.sin(a.eval(v));
                case "cos" -> v -> Math.cos(a.eval(v));
                case "tan" -> v -> Math.tan(a.eval(v));
                case "asin" -> v -> Math.asin(a.eval(v));
                case "acos" -> v -> Math.acos(a.eval(v));
                case "atan" -> v -> Math.atan(a.eval(v));
                case "atan2" -> v -> Math.atan2(a.eval(v), b.eval(v));
                case "hypot" -> v -> Math.hypot(a.eval(v), b.eval(v));
                case "pow" -> v -> Math.pow(a.eval(v), b.eval(v));
                case "min" -> v -> Math.min(a.eval(v), b.eval(v));
                case "max" -> v -> Math.max(a.eval(v), b.eval(v));
                default -> throw new IllegalStateException(name);
            };
        }
    }
}
