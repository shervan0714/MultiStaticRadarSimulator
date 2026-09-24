# Multi-Static Radar Simulator - Prototype Instructions

This guide explains how to run the Command-Line Interface (CLI) Prototype of the Multi-Static Radar Simulator. This prototype allows you to view the core mathematical simulation in action without needing to set up the complex JavaFX 3D user interface environment.

## What This Prototype Does

The CLI prototype (`CLISimulatorApp.java`) executes a 5-step radar simulation where:
1. **Transmitters & Receivers**: A central transmitter (`TX1`) and three receivers (`RX1`, `RX2`, `RX3`) are placed at fixed locations.
2. **Target (Drone)**: A target drone begins at a specific location and moves at a constant velocity over time.
3. **Measurement Generation**: Uses physics-based math (`RadarPhysics`) to generate simulated power values and bistatic ranges received by the three receivers.
4. **Position Estimation**: The localization model triangulates the drone's position based on the received power measurements.
5. **Error Calculation**: It compares the *Estimated* position with the *Actual* position to show the accuracy of the triangulation.

## How to Run the Prototype

Since the project has been facing some issues with Gradle and JavaFX on newer JDKs, we have provided a simple batch script that compiles and runs the prototype using the standard `javac` and `java` commands already installed on your system.

### Steps:

1. Open a terminal (Command Prompt or PowerShell) inside your project directory:
   `c:\Users\sherv\Downloads\paiproject\MultiStaticRadarSimulator`

2. Run the prototype script:
   ```cmd
   run-cli-prototype.bat
   ```

3. You will see output similar to this:

   ```
   ==========================================
    Multi-Static Radar Simulator - Prototype 
   ==========================================

   [SETUP] Initialized 1 Transmitter and 3 Receivers.
   [SETUP] Target Drone configured starting at: Vector3D{x=1000.0, y=2000.0, z=100.0} with velocity: Vector3D{x=50.0, y=0.0, z=0.0}

   --- Step 1 (Time: 0.0s) ---
   Actual Target Position: Vector3D{x=1000.0, y=2000.0, z=100.0}
     RX1 Measured -> Range: 7236.07m, Power: 5.79e-10 W
     RX2 Measured -> Range: 8944.27m, Power: 3.79e-10 W
     RX3 Measured -> Range: 7141.43m, Power: 5.95e-10 W
   Estimated Target Position: Vector3D{x=17.75, y=348.65, z=0.0}
   Estimation Error: 1918.49m (Status: SUCCESS)
   ...
   ```

### Troubleshooting

- If you see `ERROR: Compilation failed!`, ensure that `javac` is on your system's PATH. (Since `java -version` works, this should already be set up).
- Ensure you have not renamed or moved the source files in `src\main\java\com\radar\simulator\`.

Enjoy the prototype!
