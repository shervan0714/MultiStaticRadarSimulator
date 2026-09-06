# Multi-Static Radar Simulator

A real-time 3D simulator for multi-static radar systems built for Girdhar sir's research in radar signal processing and drone tracking.

## Overview

This simulator allows researchers to:
- Place transmitter and receiver stations in 3D space with configurable orientations
- Simulate drone trajectories with variable velocity
- Calculate drone position from received power signals using radar equations
- Compare actual vs. calculated positions for formula validation and error analysis
- Adjust all parameters in real-time during simulation

## Quick Start

### Prerequisites
- Java 17+
- Gradle 7.0+

### Build & Run

```bash
# Clone the repository
git clone https://github.com/yourusername/MultiStaticRadarSimulator.git
cd MultiStaticRadarSimulator

# Build the project
./gradlew build

# Run the simulator
./gradlew run
```

## Project Structure

```
MultiStaticRadarSimulator/
├── src/
│   ├── main/java/com/radar/simulator/
│   │   ├── RadarSimulatorApp.java           # JavaFX entry point
│   │   ├── core/
│   │   │   ├── Transmitter.java             # Transmitter model
│   │   │   ├── Receiver.java                # Receiver model
│   │   │   ├── Drone.java                   # Drone trajectory model
│   │   │   └── RadarPhysics.java            # Power calculation & triangulation
│   │   ├── ui/
│   │   │   ├── SimulatorController.java     # Main UI controller
│   │   │   ├── Visualization3D.java         # JavaFX 3D rendering
│   │   │   └── ParameterPanel.java          # Real-time parameter adjustment
│   │   └── util/
│   │       └── Vector3D.java                # 3D vector utilities
│   └── test/java/com/radar/simulator/
│       ├── core/
│       │   ├── TransmitterTest.java
│       │   ├── ReceiverTest.java
│       │   ├── DroneTest.java
│       │   └── RadarPhysicsTest.java
├── docs/
│   ├── DESIGN.md                            # Architecture & design document
├── build.gradle
├── .gitignore
└── README.md
```

## Architecture

### Core Modules

1. **RadarPhysics Module** (Physics Engine)
   - Power calculation from transmitter to drone to receiver
   - Triangulation algorithm for drone position estimation
   - Error calculation (actual vs. calculated)

2. **Transmitter/Receiver Module**
   - Station positioning (X, Y, Z coordinates)
   - Orientation/antenna characteristics
   - Frequency and power parameters

3. **Drone Trajectory Module**
   - 3D position tracking
   - Velocity control (constant or variable)
   - Real-time position updates

4. **UI & Visualization Module**
   - Real-time 3D rendering of stations and drone
   - Parameter adjustment panel
   - Error display (actual vs. calculated positions)

## Features

### Current (Mid-Demo Target)
- ✓ 3D visualization of transmitter, receivers, and drone
- ✓ Real-time parameter adjustment (positions, velocity)
- ✓ Basic power calculation
- ✓ Position estimation via triangulation

### Final Demo
- ✓ Improved triangulation algorithm
- ✓ Error metrics and visualization
- ✓ Support for multiple receiver configurations
- ✓ Data export capabilities

## Usage Example

1. Launch the simulator
2. Place transmitter at (0, 0, 1000m)
3. Place receivers at (10km, 0, 500m), (-10km, 0, 500m), (0, 10km, 500m)
4. Launch drone at (5km, 5km, 2km) with velocity 50 m/s
5. Observe calculated position vs. actual position in real-time
6. Adjust formula parameters to minimize error

## Technology Stack

- **Language**: Java 17
- **Build**: Gradle
- **3D Visualization**: JavaFX 3D
- **Math**: JOML (Java OpenGL Math Library)
- **Testing**: JUnit 4
- **Logging**: SLF4J

## Contribution

This is a solo student project for CS5013 (Programming with AI) at IIT Madras.

**Student**: Your Name  
**Roll No**: Your Roll  
**Stakeholder**: Girdhar sir (Research)  
**Course**: CS5013, IIT Madras  

## License

Internal use only (IIT Madras)
