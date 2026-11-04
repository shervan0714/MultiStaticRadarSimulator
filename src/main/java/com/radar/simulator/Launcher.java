package com.radar.simulator;

/**
 * Entry point of the standalone JAR and the packaged app.
 *
 * With no arguments it opens the GUI; with {@code --cli} it runs the
 * command-line pipeline with the remaining arguments, e.g.
 * {@code java -jar MultiStaticRadarSimulator.jar --cli --receivers 5 --compare}.
 *
 * It must not extend {@code javafx.application.Application}: when JavaFX is
 * on the class path (as in the single JAR) rather than the module path, the
 * Java launcher refuses to start an Application subclass directly.
 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--cli")) {
            String[] rest = new String[args.length - 1];
            System.arraycopy(args, 1, rest, 0, rest.length);
            CLISimulatorApp.main(rest);
        } else {
            RadarSimulatorApp.main(args);
        }
    }
}
