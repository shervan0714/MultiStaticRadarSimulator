# Project Structure Overview

```
MultiStaticRadarSimulator/
│
├── 📄 build.gradle                          ← Gradle build config (JavaFX, JOML, JUnit)
├── 📄 README.md                             ← Project overview & usage
├── 📄 .gitignore                            ← Git ignore rules
│
├── 📁 src/main/java/com/radar/simulator/
│   │
│   ├── 🟢 RadarSimulatorApp.java            ← JavaFX entry point
│   │
│   ├── 📁 core/                             ← PHYSICS LAYER
│   │   ├── 🟡 RadarPhysics.java             ✓ Power calculation & triangulation
│   │   ├── 🟡 Transmitter.java              ✓ Transmitter station model
│   │   ├── 🟡 Receiver.java                 ✓ Receiver station model
│   │   └── 🟡 Drone.java                    ✓ Drone trajectory simulation
│   │
│   ├── 📁 ui/                               ← UI LAYER
│   │   ├── 🔵 SimulatorController.java      ✓ Main simulation loop & orchestration
│   │   ├── 🔵 Visualization3D.java          ✓ JavaFX 3D scene rendering
│   │   └── 🔵 ParameterPanel.java           ✓ Real-time parameter controls
│   │
│   └── 📁 util/                             ← UTILITIES
│       └── ⚪ Vector3D.java                 ✓ 3D math library (add, dot, cross, etc.)
│
├── 📁 src/test/java/com/radar/simulator/
│   │
│   ├── 📁 core/
│   │   ├── 📋 RadarPhysicsTest.java         ✓ 6 unit tests
│   │   ├── 📋 TransmitterTest.java          ✓ 2 unit tests
│   │   └── 📋 DroneTest.java                ✓ 3 unit tests
│   │
│   └── 📁 util/
│       └── 📋 Vector3DTest.java             ✓ 7 unit tests
│
├── 📁 docs/
│   └── 📘 DESIGN.md                         ← Complete design doc (READY TO SUBMIT)
│
├── 📋 WEEKLY-LOG.md                         ← Contribution tracking
├── 📋 GITHUB-SETUP.md                       ← GitHub initialization guide
└── 📋 SUBMISSION-CHECKLIST.md               ← This checklist
```

---

## Module Dependencies & Data Flow

```
┌─────────────────────────────────────────────────────────┐
│                   RadarSimulatorApp                     │
│                  (JavaFX Application)                   │
└──────────────────────────┬──────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌─────────────────┐ ┌──────────────┐ ┌──────────────────┐
│ Parameter Panel │ │Visualization │ │  Simulator       │
│   (UI Input)    │ │    3D        │ │  Controller      │
└─────────────────┘ └──────────────┘ │  (Main Loop)     │
        ▲                  ▲          └────────┬─────────┘
        │                  │                   │
        └──────────────────┼───────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌─────────────────┐ ┌──────────────┐ ┌──────────────────┐
│ Transmitter     │ │  Receiver    │ │    Drone         │
│ (Station Model) │ │ (Station     │ │  (Trajectory)    │
│                 │ │  Model)      │ │                  │
└────────┬────────┘ └──────┬───────┘ └────────┬─────────┘
         │                 │                  │
         └─────────────────┼──────────────────┘
                           │
        ┌──────────────────▼──────────────────┐
        │        RadarPhysics Engine          │
        │                                     │
        │  • calculateReceivedPower()         │
        │  • triangulateDronePosition()       │
        │  • calculateError()                 │
        │  • powerToDBm() / powerFromDBm()    │
        └─────────────────────────────────────┘
                           │
        ┌──────────────────▼──────────────────┐
        │          Vector3D Utilities         │
        │                                     │
        │  • add(), subtract(), scale()       │
        │  • dot(), cross(), distance()       │
        │  • magnitude(), normalize()         │
        └─────────────────────────────────────┘
```

---

## Class Responsibilities Matrix

| Class | Responsibility | Dependencies | Tests |
|-------|-----------------|--------------|-------|
| RadarSimulatorApp | JavaFX app lifecycle | SimulatorController | - |
| SimulatorController | Orchestrate sim loop | All core + UI | - |
| Visualization3D | Render 3D scene | Drone, Transmitter, Receiver | - |
| ParameterPanel | UI controls for input | Transmitter, Receiver, Drone | - |
| RadarPhysics | Power calc & triangulation | Vector3D | ✓ 6 tests |
| Transmitter | Station model + state | Vector3D | ✓ 2 tests |
| Receiver | Station model + state | Vector3D | - |
| Drone | Trajectory simulation | Vector3D | ✓ 3 tests |
| Vector3D | 3D math library | None (utility) | ✓ 7 tests |

---

## Simulation Loop Flow (Per Frame)

```
┌─────────────────────────────────────────────┐
│  1. Read UI Parameter Changes               │
│     (transmitter/receiver/drone positions)  │
└────────────────┬────────────────────────────┘
                 │
┌────────────────▼────────────────────────────┐
│  2. Update Drone Position                   │
│     position += velocity * deltaTime         │
└────────────────┬────────────────────────────┘
                 │
┌────────────────▼────────────────────────────┐
│  3. Calculate Received Power                │
│     For each receiver:                      │
│       power = RadarPhysics.calcPower(...)   │
└────────────────┬────────────────────────────┘
                 │
┌────────────────▼────────────────────────────┐
│  4. Triangulate Drone Position              │
│     calculated_pos = RadarPhysics.          │
│     triangulateDronePosition(receivers)     │
└────────────────┬────────────────────────────┘
                 │
┌────────────────▼────────────────────────────┐
│  5. Calculate Error                         │
│     error = distance(actual, calculated)    │
└────────────────┬────────────────────────────┘
                 │
┌────────────────▼────────────────────────────┐
│  6. Update Visualization                    │
│     • Move drone sphere                     │
│     • Show calculated position              │
│     • Display error metrics                 │
└────────────────┬────────────────────────────┘
                 │
┌────────────────▼────────────────────────────┐
│  7. Update Status Display                   │
│     • Show current time                     │
│     • Show positions (actual/calculated)    │
│     • Show powers at each receiver          │
│     • Show error magnitude                  │
└────────────────┬────────────────────────────┘
                 │
            Loop repeats @ 30 FPS
```

---

## Test Coverage Summary

### Unit Tests (18 Total)

**Vector3D Tests** (7)
- ✓ Distance calculation
- ✓ Magnitude computation
- ✓ Add/subtract vectors
- ✓ Scalar multiplication
- ✓ Normalization
- ✓ Dot/cross products
- ✓ Copy constructor

**Transmitter Tests** (2)
- ✓ Creation and initialization
- ✓ Position real-time update

**Receiver Tests** (included in core tests)
- Placeholder - add tests as needed

**Drone Tests** (3)
- ✓ Creation and initialization
- ✓ Constant velocity trajectory
- ✓ Velocity real-time update

**RadarPhysics Tests** (6)
- ✓ Error calculation (zero difference)
- ✓ Error calculation (known distance)
- ✓ Power conversion (Watts → dBm)
- ✓ Power conversion round-trip
- ✓ Triangulation (minimum receivers)
- ✓ Triangulation (error handling)

**Run with**: `./gradlew test`

---

## Development Roadmap (Sep 6 - Nov 6)

```
Week 1 (Sep 6-12):    ✅ Design + Setup
                      • DESIGN.md complete
                      • GitHub ready
                      • 9 classes + 18 tests scaffolded

Week 2-3 (Sep 13-26): 🔄 Core Physics
                      • Implement real power formula
                      • Refine triangulation algorithm
                      • Add formula validation tests

Week 4 (Sep 27-Oct 3):🔄 UI Integration
                      • Complete Visualization3D 3D rendering
                      • Connect ParameterPanel to live updates
                      • Add error visualization

Week 5 (Oct 4-10):    🔄 Integration & Mid-Demo
                      • One complete simulation cycle
                      • End-to-end testing
                      • Prepare for mid-demo demo

Week 6-7 (Oct 11-24): 🔄 Refinement
                      • Performance optimization
                      • Formula tuning with Girdhar sir
                      • Stakeholder dry-run

Week 8 (Oct 25-31):   🔄 Testing & Polish
                      • Comprehensive testing
                      • Error handling
                      • UI improvements

Week 9 (Nov 1-5):     🔄 Documentation
                      • Final README
                      • User guide
                      • Handoff package

Week 10 (Nov 6):      🎯 Final Submission
                      • Stakeholder demo
                      • Individual viva
                      • Code handoff
```

---

## Key Physics Concepts (To Implement)

### Power Calculation
```
Placeholder (current):
P_r = (P_t * λ²) / (64π² * R_total²)

Expected (from Girdhar sir):
P_r = (P_t * G_t * G_r * λ²) / ((4π)² * R_tx² * R_rx²)

Your job: Replace formula in RadarPhysics.calculateReceivedPower()
```

### Triangulation
```
Goal: Estimate drone position from received powers at 3+ receivers

Current: Weighted average (placeholder)

Better: Least-squares fitting or Kalman filter

Best: Formula-specific algorithm from Girdhar sir
```

### Error Metric
```
Euclidean distance: error = ||actual_pos - calculated_pos||
```

---

## File Checklist for GitHub

Before first commit:
```
✓ build.gradle          - Configured with all dependencies
✓ .gitignore            - Excludes /build, *.class, .idea, etc.
✓ README.md             - Build & run instructions
✓ DESIGN.md             - Architecture & module split
✓ WEEKLY-LOG.md         - Contribution tracking
✓ All 9 Java classes    - Skeleton code with interfaces
✓ All 4 test classes    - 18 unit tests
✓ Stakeholder email     - Appended to DESIGN.md PDF
```

---

**Generated**: September 6, 2026  
**Ready**: YES ✅  
**Next Step**: Create GitHub repo + push code
