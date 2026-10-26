# Project Structure

What every folder and file in this repository is for. Java packages follow the modules in the Phase 3 design document, so the code reads in the same order as the data flow:

```
scenario → measurement → localization → experiment (runner + evaluation) → ui
                         persistence saves/loads scenarios and exports results
```

All Java sources live under `com.radar.simulator`; the paths below are relative to `src/main/java/com/radar/simulator/` (or `src/test/java/...` for tests).

## Top level

```
MultiStaticRadarSimulator/
├── README.md              Overview, build/run steps, architecture, verification, contact
├── build.gradle           Build: Java 17, JavaFX 21, JUnit 4; `run` (GUI) and `runCli` tasks
├── gradlew, gradlew.bat   Gradle wrapper scripts (Unix / Windows), so no Gradle install is needed
├── gradle/wrapper/        Wrapper jar + properties pinning Gradle 9.3.0
├── .gitignore             Keeps build output, IDE files, logs and exported CSVs out of git
├── .gitattributes         Line endings: LF for gradlew, CRLF for .bat, jar as binary
├── docs/                  Project documentation (below)
└── src/
    ├── main/java/...      Application code
    └── test/java/...      JUnit tests, same package layout as main
```

Generated and not in git: `build/` (compiled classes, test reports — recreated by every build) and `.gradle/` (Gradle's local cache).

## `docs/`

| File | Purpose |
|---|---|
| `PROJECT-STRUCTURE.md` | This file |
| `MID-DEMO.md` | Mid-evaluation demo script and the honest list of known gaps |
| `WEEKLY-LOG.md` | Weekly contribution log required by the course; also records what changed and why |
| `images/default-run.png` | Screenshot: default 4-receiver run (used in the README) |
| `images/coplanar-warning.png` | Screenshot: all sensors at ground level — runs, with the mirror-ambiguity warning |
| `images/invalid-geometry.png` | Screenshot: two receivers — refused with `UNDER_CONSTRAINED` |

## Application code (`src/main/java/com/radar/simulator/`)

### Entry points

| File | Purpose |
|---|---|
| `RadarSimulatorApp.java` | JavaFX entry point (`./gradlew run`). Opens the window and runs the default scenario. |
| `CLISimulatorApp.java` | Command-line entry point (`./gradlew runCli`). Same pipeline without a GUI; options `--receivers`, `--duration`, `--timestep`, `--save`, `--load`, `--export`. |

### `core/` — radar entities and shared types

| File | Purpose |
|---|---|
| `Transmitter.java` | Transmitter: ID, position, frequency, transmit power, orientation vector |
| `Receiver.java` | Receiver: ID (used to match measurements), position, frequency, gain in dBi, orientation as azimuth/elevation in degrees, `getBoresight()` pointing vector |
| `ModelDescriptor.java` | `(name, version)` record that identifies a measurement or localization formulation; stored on every experiment record |
| `RadarPhysics.java` | Received-power formula (placeholder until the stakeholder's equation is available). Only `calculateReceivedPower` is used; `triangulateDronePosition` and the dBm helpers are early-prototype leftovers |
| `Drone.java` | Target entity from the early prototype; now only used as a temporary holder when computing received power |

### `scenario/` — scenario model and trajectory engine

| File | Purpose |
|---|---|
| `Scenario.java` | One experiment setup: transmitter, receiver list and target trajectory |
| `TrajectoryModel.java` | Interface: target position and velocity at time *t* — new motion models plug in here |
| `ConstantVelocityTrajectory.java` | MVP trajectory: `p(t) = p0 + v·t` |

### `measurement/` — measurement model

| File | Purpose |
|---|---|
| `MeasurementModel.java` | Interface: turns transmitter + receivers + true target position into a `Measurement`. Replaceable formulation. |
| `BaselineMeasurementModel.java` | Baseline v1.1: bistatic range, time delay, received power, and azimuth/elevation in each receiver's antenna frame; optional seeded Gaussian range noise |
| `Measurement.java` | All receivers' observations for one time step, plus the time |
| `ReceiverMeasurement.java` | One receiver's observation, keyed by receiver ID so reordering receivers is safe |
| `Quantity.java` | Measurable quantities: `BISTATIC_RANGE`, `TIME_DELAY`, `RECEIVED_POWER`, `AZIMUTH`, `ELEVATION` |

### `localization/` — localization model and geometry checks

| File | Purpose |
|---|---|
| `LocalizationModel.java` | Interface: estimates the target position from a `Measurement`. Replaceable formulation. |
| `BaselineLocalizationModel.java` | Baseline v2.1: least squares on bistatic-range ellipsoids (Levenberg–Marquardt, analytic Jacobian, multi-start, target kept at or above ground); reports residual and condition number |
| `EstimationResult.java` | Estimate + status + RMS range residual + condition number |
| `Status.java` | `SUCCESS`, `UNDER_CONSTRAINED`, `NUMERICALLY_UNSTABLE`, `INVALID_INPUT` — failures are explicit, never a silent wrong position |
| `GeometryValidator.java` | Checks a sensor layout before running: receiver count, duplicate IDs, minimum spacing, collinear sensors (invalid), coplanar sensors (warning) |

### `experiment/` — experiment runner and evaluation

| File | Purpose |
|---|---|
| `ExperimentRunner.java` | Runs the pipeline for every time step: trajectory → measurement → localization → error |
| `ExperimentRecord.java` | Everything recorded for one step: time, true position/velocity, measurement, estimate, model descriptors, error |
| `EvaluationModule.java` | Mean, max and RMS error over the successful steps; counts failed steps |
| `EvaluationResult.java` | The aggregate metrics produced by `EvaluationModule` |

### `persistence/`

| File | Purpose |
|---|---|
| `ScenarioPersistence.java` | Save/load a scenario (+ duration and time step) as a `.properties` file; export per-step results to CSV and an evaluation summary to text |

### `ui/` — JavaFX user interface

| File | Purpose |
|---|---|
| `SimulatorController.java` | Coordinator: reads the form, validates, runs the experiment, updates the views; handles Save/Load/Export |
| `ScenarioEditor.java` | Left-hand form: transmitter, receiver rows (add/remove), drone start and velocity, duration and time step |
| `ScenarioConfig.java` | Plain-Java copy of the form's values; validates them and builds a `Scenario` (unit-testable without JavaFX) |
| `Visualization3D.java` | 3D view: grid, axes, sensors, true trajectory, estimates, error line; orbit/zoom; time slider and Play |
| `SceneTransform.java` | Converts East/North/Up metres to JavaFX scene coordinates (JavaFX's Y axis points down); downsampling helper |
| `ResultsPanel.java` | Per-step results table and mean/max/RMS summary; row selection is linked to the 3D time step |

### `util/`

| File | Purpose |
|---|---|
| `Vector3D.java` | 3D vector maths: add, subtract, scale, dot, cross, distance, normalize |
| `Matrix3.java` | 3×3 linear solve and symmetric eigenvalues, used by the localization solver and the geometry validator |

## Tests (`src/test/java/com/radar/simulator/`)

Each test class sits in the same package as the class it tests. `./gradlew test` runs all of them.

| File | What it verifies |
|---|---|
| `core/TransmitterTest.java`, `core/ReceiverTest.java`, `core/DroneTest.java` | Entity construction and position/velocity/parameter updates; `ReceiverTest` also checks defensive copies |
| `core/RadarPhysicsTest.java` | Error helper, watt↔dBm conversion and the legacy triangulation helper (the received-power formula itself has no test yet) |
| `scenario/ConstantVelocityTrajectoryTest.java` | Positions match the analytical solution at known times |
| `measurement/BaselineMeasurementModelTest.java` | Hand-computable bistatic range, time delay, power; receiver IDs on observations |
| `localization/BaselineLocalizationModelTest.java` | Noiseless estimates within 1 mm; mirror ambiguity; ill-conditioning; missing/impossible ranges; receiver reordering; duplicate IDs |
| `localization/GeometryValidatorTest.java` | Valid, too few, collinear, coplanar, too-close, duplicate-ID layouts; order and scale independence |
| `experiment/ExperimentRunnerTest.java` | Every step recorded in order with ground truth, model identity and time stamps; fractional time steps |
| `experiment/EvaluationModuleTest.java` | Mean/max/RMS against hand-calculated values; failed steps excluded |
| `experiment/RegressionTest.java` | End-to-end reference scenarios (3/4/5 receivers, wide baseline, close range, elevated transmitter) within 1 mm, plus difficult geometries |
| `persistence/ScenarioPersistenceTest.java` | Save → load round trip, identical results after reload, CSV and summary export |
| `ui/ScenarioConfigTest.java` | Form values are transferred correctly into the `Scenario`; invalid input rejected |
| `ui/SceneTransformTest.java` | World → scene mapping, handedness, centring/scaling, downsampling, grid spacing |
| `util/Vector3DTest.java`, `util/Matrix3Test.java` | Vector operations and 3×3 solve/eigenvalues on hand-computable cases |

## Where to change what

| To… | Edit |
|---|---|
| Add a new localization formulation | New class implementing `localization/LocalizationModel`; select it in `ui/SimulatorController` and `CLISimulatorApp` |
| Add a new measurement formulation | New class implementing `measurement/MeasurementModel` |
| Add a new trajectory type | New class implementing `scenario/TrajectoryModel`; extend `ScenarioConfig`, `ScenarioEditor` and `ScenarioPersistence` |
| Change the received-power formula | `core/RadarPhysics.calculateReceivedPower` |
| Change the "numerically unstable" threshold | `BaselineLocalizationModel.MAX_CONDITION_NUMBER` |
| Add a column to the results table | `ui/ResultsPanel` constructor (`column(...)` list) |
| Add a CSV column | `persistence/ScenarioPersistence.exportResultsToCSV` |
| Add a form field | `ui/ScenarioEditor` + `ui/ScenarioConfig` |
| Add a CLI option | `CLISimulatorApp.main` argument loop |
