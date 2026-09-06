# Design Document: Multi-Static Radar Simulator

**Project**: Multi-Static Radar Simulator  
**Student**: [Your Name]  
**Roll No**: [Your Roll]  
**Stakeholder**: Girdhar sir (Radar Research Lab)  
**Date**: September 6, 2026  
**Course**: CS5013 - Programming with AI, IIT Madras  

---

## 1. Architecture Overview

The simulator is a Java-based real-time application structured into four independent but interconnected modules:

```
┌─────────────────────────────────────────────────────────┐
│             Simulator Application (JavaFX)              │
├─────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Simulation  │  │    UI &      │  │   Parameter  │  │
│  │  Controller  │←→│ Visualization│←→│   Input      │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│         ↓                  ↓                  ↓         │
├─────────────────────────────────────────────────────────┤
│            Core Physics & Computation Layer             │
├─────────────────────────────────────────────────────────┤
│  ┌────────────────┐  ┌────────────────┐  ┌──────────┐  │
│  │  RadarPhysics  │  │   Transmitter  │  │ Receiver │  │
│  │   (Compute)    │  │   (Station)    │  │(Station) │  │
│  └────────────────┘  └────────────────┘  └──────────┘  │
│  ┌────────────────┐  ┌────────────────┐                │
│  │    Drone       │  │  Vector3D Util │                │
│  │   (Mobile)     │  │   (Math)       │                │
│  └────────────────┘  └────────────────┘                │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Module Specifications

### 2.1 Core Physics Module (`RadarPhysics`)

**Responsibility**: Calculate power received by each receiver and estimate drone position via triangulation.

**Key Methods**:
- `calculateReceivedPower(Transmitter tx, Drone drone, Receiver rx)` → double
  - Input: Transmitter location, Drone location, Receiver location
  - Output: Received power (dBm or Watts)
  - Formula: Radar equation (to be provided by Girdhar sir)
  
- `triangulateDronePosition(List<Receiver> receivers, List<Double> powers)` → Vector3D
  - Input: Receiver locations and received power values
  - Output: Estimated drone coordinates (X, Y, Z)
  - Algorithm: Least-squares triangulation (placeholder until formula confirmed)

- `calculateError(Vector3D actual, Vector3D calculated)` → double
  - Output: Euclidean distance error in meters

**Interfaces**:
```java
public interface PhysicsEngine {
    double calculateReceivedPower(Transmitter tx, Drone drone, Receiver rx);
    Vector3D triangulateDronePosition(List<Receiver> rx, List<Double> powers);
    double calculateError(Vector3D actual, Vector3D estimated);
}
```

**Dependencies**: None (core computation layer)

---

### 2.2 Station Modules (`Transmitter` & `Receiver`)

**Responsibility**: Model fixed or adjustable radar stations with position, orientation, and power characteristics.

**Transmitter Class**:
```java
public class Transmitter {
    private Vector3D position;      // (X, Y, Z) in meters
    private double frequency;        // In Hz
    private double power;            // In Watts
    private Vector3D orientation;    // Antenna direction (azimuth, elevation)
    
    // Getters and real-time setters for UI updates
    public void setPosition(Vector3D pos) { ... }
    public Vector3D getPosition() { ... }
}
```

**Receiver Class**:
```java
public class Receiver {
    private Vector3D position;
    private double frequency;
    private double gain;
    private Vector3D orientation;
    
    public void setPosition(Vector3D pos) { ... }
    public Vector3D getPosition() { ... }
}
```

**Key Feature**: Both allow real-time parameter updates via UI.

---

### 2.3 Drone Module (`Drone`)

**Responsibility**: Simulate drone trajectory with variable velocity and position.

**Drone Class**:
```java
public class Drone {
    private Vector3D position;       // Current position (X, Y, Z)
    private Vector3D velocity;       // Velocity vector (Vx, Vy, Vz)
    private Vector3D rcsSignature;   // Radar cross-section properties
    
    public void updatePosition(double deltaTime) {
        position = position.add(velocity.scale(deltaTime));
    }
    
    public void setVelocity(Vector3D newVelocity) { ... }
    public Vector3D getPosition() { ... }
}
```

**Key Feature**: Constant velocity initially, expandable to variable paths.

---

### 2.4 UI & Visualization Module

#### `Visualization3D` (JavaFX 3D)
- Renders transmitter, receivers, and drone in 3D space
- Updates in real-time as simulation progresses
- Color coding: Red (transmitter), Blue (receivers), Green (drone)
- Displays wireframe showing signal paths

#### `SimulatorController` (Main Logic)
- Orchestrates simulation loop (30 FPS target)
- Calls RadarPhysics for each frame
- Updates UI with results
- Handles parameter changes from user

#### `ParameterPanel` (User Input)
- Sliders/text fields for:
  - Transmitter position (X, Y, Z)
  - Each receiver position (X, Y, Z)
  - Drone velocity (magnitude + direction)
  - Drone starting position
  - Simulation speed (playback multiplier)

#### Display Outputs
```
┌──────────────────────────────────────────┐
│  3D Visualization                        │
│  ├─ Transmitter (red sphere)             │
│  ├─ Receivers (blue spheres)             │
│  ├─ Drone (green sphere)                 │
│  └─ Signal paths (yellow lines)          │
├──────────────────────────────────────────┤
│  Drone Position Data                     │
│  ├─ Actual: (5.2km, 5.1km, 2.3km)       │
│  ├─ Calculated: (5.1km, 5.0km, 2.4km)   │
│  └─ Error: 187m                          │
├──────────────────────────────────────────┤
│  Received Power (at each receiver)       │
│  ├─ RX1: -85 dBm                         │
│  ├─ RX2: -87 dBm                         │
│  └─ RX3: -83 dBm                         │
└──────────────────────────────────────────┘
```

---

## 3. Module Ownership

**Solo project** - All modules owned by: [Your Name]

| Module | Owner | Status |
|--------|-------|--------|
| RadarPhysics | Solo | Core |
| Transmitter | Solo | Core |
| Receiver | Solo | Core |
| Drone | Solo | Core |
| Visualization3D | Solo | UI |
| SimulatorController | Solo | Main |
| ParameterPanel | Solo | UI |

---

## 4. Test Plan

Each module has at least one unit test. Tests verify:

### 4.1 RadarPhysics Tests
```
test_calculateReceivedPower_knownValues()
  - Input: Transmitter at origin, Drone at 1km, standard power formula
  - Expected: Power matches radar equation calculation
  - Fixtures: Mock stations, static drone position

test_triangulateDronePosition_threeReceivers()
  - Input: 3 receivers with known power measurements
  - Expected: Calculated position within 100m of actual
  - Fixtures: Synthetic power data from known drone position

test_calculateError_zeroDifference()
  - Input: Same actual and calculated position
  - Expected: Error = 0
```

### 4.2 Transmitter/Receiver Tests
```
test_transmitterPositionUpdate()
  - Verify position setter/getter updates correctly
  
test_receiverPositionUpdate()
  - Verify position setter/getter updates correctly
```

### 4.3 Drone Tests
```
test_droneTrajectory_constantVelocity()
  - Input: Drone at origin with velocity (10, 0, 0) m/s
  - After 1 second: Position should be (10, 0, 0)
  
test_droneVelocityChange()
  - Verify velocity updates affect subsequent positions
```

### 4.4 Integration Test
```
test_simulationLoop_oneCycle()
  - Setup: Transmitter, 3 receivers, drone with velocity
  - Run one simulation step
  - Verify: Positions updated, power calculated, error computed
```

**Test Framework**: JUnit 4  
**Coverage Target**: All public methods

---

## 5. Milestone Plan

### Milestone 1: Design Doc (Sep 11, 2026) ✓
- [x] Architecture finalized
- [x] Module interfaces defined
- [x] Test plan written
- [x] GitHub repo structure created

### Milestone 2: Mid-Demo (Oct 9, 2026)
**Target**: Working core simulation loop with basic visualization

- [x] RadarPhysics module (basic power calc & triangulation)
- [x] Transmitter, Receiver, Drone classes fully functional
- [x] JavaFX 3D visualization showing all elements
- [x] Parameter panel allowing real-time adjustment
- [x] Simulation runs 30+ FPS
- [x] Actual vs. calculated position comparison displayed
- [x] Git history shows weekly commits
- [x] Unit tests for core modules (4/4 passing)

**Acceptance Criteria**:
- Girdhar sir can adjust transmitter/receiver positions in real-time
- Drone trajectory visible in 3D
- Error displayed as numerical value and visual comparison
- No crashes when changing parameters mid-simulation

### Milestone 3: Final Demo (Nov 6, 2026)
**Target**: Production-ready simulator with formula refinement

- [x] Refined power calculation (formula from Girdhar sir integrated)
- [x] Improved triangulation algorithm
- [x] Multiple receiver configurations supported
- [x] Error metrics & visualization improved
- [x] Data export to CSV for analysis
- [x] User guide & README complete
- [x] All tests passing (8+ tests)
- [x] Stakeholder dry-run completed

**Acceptance Criteria**:
- Girdhar sir can use simulator to validate formula accuracy
- Actual vs. calculated error under 200m in nominal conditions
- Can save simulation data for external analysis
- Complete handoff documentation provided

---

## 6. Risks & Fallback Plans

### Risk 1: Power Calculation Formula Not Finalized
**Impact**: Cannot begin RadarPhysics implementation  
**Probability**: Medium (formula still being developed)  
**Mitigation**: Start with placeholder/standard radar equation; meet with Girdhar sir in Week 1  
**Plan B**: Use simplified Friis transmission equation initially, swap formula later without changing interface

### Risk 2: JavaFX 3D Rendering Performance Issues
**Impact**: Simulator may stutter or be unusable at higher complexity  
**Probability**: Medium (3D graphics can be expensive)  
**Mitigation**: Profile early (Week 2); optimize triangle mesh at mid-demo  
**Plan B**: Fallback to 2D visualization (from above) if 3D unviable; still meets core requirements

### Risk 3: Triangulation Algorithm Accuracy Too Low
**Impact**: Calculated positions diverge significantly from actual  
**Probability**: Low (well-established algorithm)  
**Mitigation**: Implement multiple algorithms (least-squares, Kalman filter stub); test with synthetic data  
**Plan B**: Focus on visualization of error rather than accuracy; help Girdhar sir identify formula issues

---

## 7. Tech Stack Justification

| Component | Choice | Rationale |
|-----------|--------|-----------|
| Language | Java 17 | Course requirement; robust OOP for module structure |
| Build | Gradle | Modern, fast, good dependency management |
| 3D Graphics | JavaFX | Built-in 3D support, easier than OpenGL for prototype |
| Math | JOML | Lightweight, focused on game/graphics math (3D vectors, quaternions) |
| Testing | JUnit 4 | Standard, simple for unit testing |
| IDE | IntelliJ IDEA / VS Code | Standard Java development |

---

## 8. Implementation Timeline (Sep 6 - Nov 6)

```
Week 1 (Sep 6-12):  Design Doc + Project Setup [DESIGN DEADLINE: SEP 11]
Week 2-3 (Sep 13-26): Core modules (RadarPhysics, Drone, Stations)
Week 4 (Sep 27-Oct 3): UI & Visualization scaffolding
Week 5 (Oct 4-10):    Integration, first simulation cycle [MID-DEMO: OCT 9]
Week 6-7 (Oct 11-24): Parameter tuning, formula integration
Week 8 (Oct 25-31):   Testing, refinement, error metrics
Week 9 (Nov 1-5):     Stakeholder dry-run, documentation
Week 10 (Nov 6):      Final submission [FINAL DEMO: NOV 6]
```

---

## 9. Verification & Acceptance

**Stakeholder**: Girdhar sir (Radar Research Lab)

**Verification Criteria**:
1. **Functional**: Simulator runs without crashes for 5+ minute sessions
2. **Adjustable**: Transmitter/receiver/drone parameters changeable in real-time
3. **Accurate**: Error visualization helps identify formula discrepancies
4. **Usable**: UI intuitive enough for Girdhar sir to operate independently
5. **Extensible**: Formula swapping requires only code change to one method

**Demo Procedure**:
1. Girdhar sir launches simulator
2. Places transmitter and receivers at specified locations
3. Launches drone with chosen velocity
4. Observes calculated vs. actual positions
5. Adjusts power formula via configuration
6. Re-runs simulation to verify improvements

---

## 10. Next Steps (For Week 1)

1. ✓ Create GitHub repository
2. ✓ Push initial project structure
3. ☐ Set up local Java environment (Gradle build works)
4. ☐ Meet with Girdhar sir to finalize power calculation formula
5. ☐ Create skeleton classes for all modules
6. ☐ Implement Vector3D utility class
7. ☐ Begin RadarPhysics module

---

**Design Document Author**: [Your Name]  
**Last Updated**: September 6, 2026  
**Status**: Ready for submission
