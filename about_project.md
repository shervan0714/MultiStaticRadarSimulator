# About the Project — context for code changes

This file explains the whole project in one place, both for me and for an AI assistant (Gemini Pro) that has to read or change the code. **Give this file to the assistant first, then the source file(s) you want changed.**

---

## 0. How to use this file with Gemini

1. Paste this whole file.
2. Paste the source file(s) named in the "Where to change" column of the recipe you need (Section 9), and the related test file.
3. Use a prompt like:

```
Using the project description above, make this change: <task>.
Rules: Java 17, JUnit 4. Change only what the task needs; keep existing behaviour,
interfaces and tests working. If you add a field to a record, list every place that
constructs that record and update them all. Show complete updated methods (not "...").
Explain in 3-4 lines what you changed and why, and tell me which test to add or run.
```

4. Build and test: `gradlew.bat test` (Windows) — all tests must still pass. Run the GUI with `gradlew.bat run`, the CLI with `gradlew.bat runCli`.

---

## 1. What the project is

A Java 17 / JavaFX desktop simulator for **Dr. Giridhar K (Electrical Engineering, IIT Madras)**. A user places **one transmitter (TX)** and **several receivers (RX)**, defines a **drone trajectory**, and the simulator:

1. computes where the drone truly is at each time step (ground truth),
2. simulates what each receiver measures (bistatic range, time delay, received power),
3. estimates the drone's 3D position from those measurements (localization),
4. compares estimate vs truth and reports **mean / max / RMS error**,
5. shows everything in a 3D view and a results table; scenarios can be saved/loaded and results exported to CSV.

The measurement and localization formulas are **replaceable** (interfaces with a name + version), because the stakeholder's own formulation will be plugged in later. The current formulas are my **baseline**.

Repo: `github.com/shervan0714/MultiStaticRadarSimulator` · Author: E. Sai Shervan (CS24B073).

---

## 2. Build, run, test (Windows)

| Command (run in `MultiStaticRadarSimulator/`) | What it does |
|---|---|
| `gradlew.bat test` | Compile + run all 111 JUnit 4 tests |
| `gradlew.bat run` | Open the JavaFX GUI (`RadarSimulatorApp`) |
| `gradlew.bat runCli` | Run the pipeline in the terminal (`CLISimulatorApp`) |
| `gradlew.bat runCli --args="--receivers 4 --duration 10 --timestep 1"` | CLI with options |
| `gradlew.bat clean` | Delete `build/` |

Build file: `build.gradle` (Java 17, JavaFX 21.0.1, JUnit 4.13.2, Gradle wrapper 9.3.0). Test reports: `build/reports/tests/test/index.html`.

---

## 3. Coordinate convention (important)

Right-handed, metres: **+X = East, +Y = North, +Z = Up**, origin at the transmitter frame origin. All positions are `Vector3D`.

JavaFX draws with **+Y pointing down**, so the 3D view converts with `SceneTransform`: scene.x = East, scene.y = −Up, scene.z = North. **Never change radar maths to fix a display problem** — display fixes go in `SceneTransform` / `Visualization3D` only.

---

## 4. Architecture and data flow

```
 ScenarioEditor (GUI form)  ──►  ScenarioConfig ──toScenario()──►  Scenario
 CLISimulatorApp (defaults/--load) ─────────────────────────────►     │
                                                                      ▼
                         GeometryValidator.validate(tx, receivers)  (invalid → explicit Status, stop)
                                                                      ▼
 ExperimentRunner.run(duration, timeStep)   for each step i:  t = i * timeStep
      TrajectoryModel.getPosition(t)         → true position p
      MeasurementModel.generate(tx, rxs, p)  → Measurement (one ReceiverMeasurement per receiver, keyed by receiver ID)
      LocalizationModel.estimate(tx, rxs, m) → EstimationResult (position, status, residual, conditionNumber)
      error = |p − estimate|  (NaN if status ≠ SUCCESS)
      → ExperimentRecord (everything for this step + model names/versions)
                                                                      ▼
 EvaluationModule.evaluate(records) → EvaluationResult (mean, max, RMS over successful steps)
                                                                      ▼
 GUI: Visualization3D (3D) + ResultsPanel (table) + status line      CLI: printed table + summary
 ScenarioPersistence: save/load scenario (.properties), export CSV + summary .txt
```

**2-minute explanation:** The user configures a scenario in the form (or the CLI). It is validated for geometry problems first. The `ExperimentRunner` then loops over time: the trajectory gives the true drone position, the measurement model turns it into per-receiver bistatic ranges, and the localization model solves for the position from those ranges. Each step is stored as an `ExperimentRecord`; `EvaluationModule` turns all records into mean/max/RMS error. The GUI shows the true path (green), estimates (orange), sensors and per-step errors; the same pipeline runs in the CLI. Formulations sit behind interfaces so the stakeholder's model can replace the baseline without touching the rest.

---

## 5. Packages and classes (`src/main/java/com/radar/simulator/`)

### Entry points
- **`RadarSimulatorApp`** — JavaFX `Application`. `start()` creates `SimulatorController`, a 1400×900 scene, calls `controller.runExperiment()` (so the default scenario is shown), shows the window. `stop()` calls `System.exit(0)`.
- **`CLISimulatorApp`** — `main(String[] args)`. Defaults: `duration = 10.0`, `timeStep = 1.0`, `receiverCount = 3`. Parses args in a `for` loop with `switch (args[i])`: `--save`, `--load`, `--export`, `--duration`, `--timestep`, `--receivers` (each reads the value with `args[++i]`; unknown args are ignored). Then: build/load scenario → print summary → validate geometry (prints `[GEOMETRY]`, each warning as `[WARN]`, stops if invalid) → optional save → `ExperimentRunner.run` with `BaselineMeasurementModel` + `BaselineLocalizationModel` → prints per-step table and `=== Evaluation Summary ===` → optional CSV export (+ `_summary.txt`). `buildDefaultScenario(int receiverCount)`: TX at (0,0,0), 2.4 GHz, 1000 W; RX1 (5000,5000,0), RX2 (−5000,5000,0), RX3 (0,−5000,0), RX4 (−5000,−5000,0) if ≥4, RX5 (0,0,5000) if ≥5; drone starts (1000,2000,100), velocity (50,0,0).

### `core/` — entities and shared types
- **`Transmitter(String id, Vector3D position, double frequency, double power)`** — getters/setters; `orientation` (Vector3D, default 0,0,0; not used in maths). Getters return **copies** of vectors.
- **`Receiver(String id, Vector3D position, double frequency, double gain)`** — gain in dBi; `id` is how measurements are matched to receivers. `orientation` stored but not used yet.
- **`ModelDescriptor(String name, String version)`** — record identifying a formulation.
- **`RadarPhysics`** — `calculateReceivedPower(Transmitter, Drone, Receiver)`: **placeholder** `P = Pt·λ² / (64·π²·(Rtx+Rrx)²)`, λ = c/f (ignores gains and RCS). Also `triangulateDronePosition`, `calculateError`, `powerToDBm`, `powerFromDBm` — legacy helpers from the early prototype, not used by the app (only by `RadarPhysicsTest`).
- **`Drone`** — early-prototype target; now only used as a temporary holder inside `BaselineMeasurementModel` to call `calculateReceivedPower`.

### `scenario/`
- **`Scenario(Transmitter, List<Receiver>, TrajectoryModel)`** — `getReceivers()` returns a **copy** of the list; setters and `addReceiver`.
- **`TrajectoryModel`** (interface) — `Vector3D getPosition(double time)`, `Vector3D getVelocity(double time)`.
- **`ConstantVelocityTrajectory(Vector3D initialPosition, Vector3D velocity)`** — `p(t) = p0 + v·t`, velocity constant.

### `measurement/`
- **`MeasurementModel`** (interface) — `ModelDescriptor getDescriptor()`, `Measurement generate(Transmitter, List<Receiver>, Vector3D targetPosition)`.
- **`BaselineMeasurementModel`** — descriptor `("BaselineMeasurement", "1.0")`. For each receiver: `bistaticRange = |p − tx| + |p − rx|`, `timeDelay = range / 3e8`, `power = RadarPhysics.calculateReceivedPower(...)`. Puts them in a `Map<Quantity, Double>`; returns `Measurement(perReceiver, 0.0)` (the runner stamps the real time). **No noise.**
- **`Measurement(List<ReceiverMeasurement> perReceiver, double time)`** — record.
- **`ReceiverMeasurement(String receiverId, Map<Quantity, Double> values)`** — record; `receiverId` = `Receiver.getId()`.
- **`Quantity`** — enum `BISTATIC_RANGE, TIME_DELAY, RECEIVED_POWER, AZIMUTH, ELEVATION` (AZIMUTH/ELEVATION are never generated yet).

### `localization/`
- **`LocalizationModel`** (interface) — `ModelDescriptor getDescriptor()`, `EstimationResult estimate(Transmitter, List<Receiver>, Measurement)`.
- **`BaselineLocalizationModel`** — descriptor `("BaselineLocalization", "2.0")`. See Section 7. Constants: `MAX_CONDITION_NUMBER = 1e8` (package-private), `MAX_ITERATIONS = 200`, `START_DIRECTIONS` (8 directions).
- **`EstimationResult(Vector3D position, Status status, double residual, double conditionNumber)`** — record. Failures use position (0,0,0) and NaN residual/condition number.
- **`Status`** — enum `SUCCESS, UNDER_CONSTRAINED, NUMERICALLY_UNSTABLE, INVALID_INPUT`.
- **`GeometryValidator`** — `ValidationResult validate(Transmitter, List<Receiver>)`. Nested record `ValidationResult(boolean isValid, Status status, String message, List<String> warnings)` with a 3-arg constructor (no warnings) and `hasWarnings()`. Constants: `MIN_RECEIVERS = 3`, `MIN_BASELINE_METRES = 1.0`, `FLATNESS_THRESHOLD = 1e-6`. Check order: null TX → INVALID_INPUT; no receivers → INVALID_INPUT; < 3 → UNDER_CONSTRAINED; duplicate IDs → INVALID_INPUT; two receivers closer than 1 m → NUMERICALLY_UNSTABLE; collinear sensors → UNDER_CONSTRAINED; coplanar sensors → valid + warning.

### `experiment/`
- **`ExperimentRunner(Scenario, MeasurementModel, LocalizationModel)`** — `List<ExperimentRecord> run(double duration, double timeStep)` (throws `IllegalArgumentException` if duration ≤ 0, timeStep ≤ 0 or timeStep > duration); `EvaluationResult runAndEvaluate(duration, timeStep)`. Step count = `floor(duration/timeStep + 1e-9) + 1`; `time = stepIndex * timeStep`. If geometry is invalid, every record gets the validator's status and error NaN.
- **`ExperimentRecord(int stepIndex, double time, Vector3D groundTruthPosition, Vector3D groundTruthVelocity, Measurement measurement, EstimationResult estimationResult, ModelDescriptor measurementModelDescriptor, ModelDescriptor localizationModelDescriptor, double positionError)`** — record.
- **`EvaluationModule`** — `EvaluationResult evaluate(List<ExperimentRecord>)`; throws on null/empty; only `SUCCESS` steps count toward the errors; all failed → NaN metrics.
- **`EvaluationResult(double meanError, double maxError, double rmsError, int totalSteps, int successfulSteps, int failedSteps)`** — record.

### `persistence/`
- **`ScenarioPersistence`** — `saveScenario(Scenario, double duration, double timeStep, String filePath)` and `loadScenario(String)` using Java `Properties` keys: `transmitter.id/.position.x|y|z/.frequency/.power/.orientation.x|y|z`, `receiver.count`, `receiver.<i>.id/.position.x|y|z/.frequency/.gain/.orientation.x|y|z`, `trajectory.type` (only `ConstantVelocity`), `trajectory.initialPosition.x|y|z`, `trajectory.velocity.x|y|z`, `simulation.duration`, `simulation.timeStep`. `loadSimulationParameters(path)` → `double[]{duration, timeStep}`. `exportResultsToCSV(records, path)` — header `step,time,gt_x,gt_y,gt_z,est_x,est_y,est_z,error,status,meas_model,loc_model` (error −1 if NaN). `exportEvaluationSummary(evaluation, path)` — text summary.

### `ui/` (JavaFX)
- **`SimulatorController`** — builds the window: left = `ScenarioEditor` + buttons (*Run experiment*, *Save scenario…*, *Load scenario…*, *Export results…*); centre = vertical `SplitPane` (`Visualization3D` top, `ResultsPanel` bottom); bottom = status label. Fields `measurementModel` / `localizationModel` (the formulations used by the GUI). `runExperiment()`: form → `ScenarioConfig.toScenario()` (errors shown red as "Invalid input: …") → validator (invalid → red "Cannot localize: …") → runner → evaluation → updates views and status line (model versions + geometry warnings). `static formatMetrics(EvaluationResult)` builds the "Steps: … | Mean error …" text (used by `ResultsPanel`). `loadScenario(File)`, `exportResults(File)`.
- **`ScenarioEditor`** — the form. Text fields for TX position/frequency/power, one row per receiver (ID, X, Y, Z, gain, `x` remove button) + *Add receiver*, drone start + velocity, duration + time step. `getConfig()` parses fields (throws `IllegalArgumentException` naming the bad field); `setConfig(ScenarioConfig)` fills them.
- **`ScenarioConfig`** — record `(Vector3D transmitterPosition, double frequencyHz, double transmitPowerW, List<ReceiverConfig> receivers, Vector3D initialPosition, Vector3D velocity, double duration, double timeStep)`; nested `ReceiverConfig(String id, double x, double y, double z, double gainDbi)`. `defaultConfig()` (GUI default: TX origin, 2.4 GHz, 1000 W; RX1 (5000,5000,0), RX2 (−5000,5000,0), RX3 (−5000,−5000,0), RX4 (5000,−5000,400); drone (−3000,−2000,300) at (60,40,5) m/s; 100 s, step 1 s). `toScenario()` validates (positive timing, step ≤ duration, non-blank unique IDs, finite numbers). `fromScenario(Scenario, duration, timeStep)`. Pure Java → unit-testable.
- **`Visualization3D`** — 3D `SubScene`: ground grid, E/N/U axes (red/green/blue), TX (red), RX (blue + mast), true trajectory line (green), estimate points (orange), selected-step drone + estimate + white error line. Mouse drag = orbit, scroll = zoom; slider + *Play* (whole run plays in ~10 s). Constants: `SCENE_SIZE = 1000`, `MAX_DRAWN_POINTS = 400`, colours `TX_COLOR`, `RX_COLOR`, `TRUTH_COLOR`, `ESTIMATE_COLOR`, `GRID_COLOR`. Methods `showScenario`, `showExperiment(scenario, records)`, `showStep(index)`, `setStepListener`.
- **`SceneTransform`** — world→scene mapping (Section 3), `fitting(points, sceneSize)` auto-centres/scales, `sampleIndices(count, max)` downsampling.
- **`ResultsPanel`** — summary label + `TableView<ExperimentRecord>` with columns Step, t (s), True X/Y/Z, Est X/Y/Z, Error (m), Status (red if not SUCCESS), Residual (m), Cond. Columns are created in the constructor with `column("Title", r -> String)`. Row selection ↔ 3D step are linked.

### `util/`
- **`Vector3D`** — **public mutable fields** `x, y, z`; methods return new vectors: `add, subtract, scale, dot, cross, distance, magnitude, normalize`; `equals` is exact (no `hashCode` override — don't use as a map key).
- **`Matrix3`** — `solve(double[][] a, double[] b)` (3×3 Gaussian elimination, returns `null` if singular) and `symmetricEigenvalues(double[][] a)` (Jacobi, ascending order).

---

## 6. Tests (`src/test/java/...`, JUnit 4)

Style: `import org.junit.Test; import static org.junit.Assert.*;`, methods `@Test public void testSomething()`, doubles compared with `assertEquals(expected, actual, delta)`. One test class per main class, same package. Key ones:

| Test class | Checks |
|---|---|
| `localization/BaselineLocalizationModelTest` | noiseless estimate within 1e-3 m, mirror ambiguity (higher Z chosen), ground-level target has huge condition number, missing ranges → UNDER_CONSTRAINED, impossible range → INVALID_INPUT, reordered receivers, duplicate IDs |
| `localization/GeometryValidatorTest` | valid / too few / collinear / coplanar warning / too close / duplicate IDs / order & scale independence |
| `experiment/RegressionTest` | full runs (3/4/5 receivers, wide baseline, close range, elevated TX) within `NOISELESS_TOLERANCE_M = 1e-3`; asserts descriptor versions `"1.0"` (measurement) and `"2.0"` (localization) |
| `experiment/ExperimentRunnerTest` | records in order, ground truth, time stamps, fractional steps (0.3 s / 0.1 s → 4 records) |
| `experiment/EvaluationModuleTest` | mean/max/RMS by hand, failed steps excluded |
| `persistence/ScenarioPersistenceTest` | save→load round trip, same results after reload, CSV/summary files written (non-empty) |
| `ui/ScenarioConfigTest`, `ui/SceneTransformTest` | form→scenario mapping and validation; world→scene mapping, downsampling, grid step |

**If you change a version string or a default, these tests will tell you** — update the test only if the new behaviour is intended.

---

## 7. The maths, explained simply

**Bistatic range.** The signal goes TX → drone → RX. Its path length is `m = |p − t| + |p − r|`. All points with the same `m` form an **ellipsoid** with foci at TX and RX. Each receiver gives one ellipsoid; the drone is where they intersect.

**Localization (BaselineLocalizationModel v2.0).** Unknown `p = (x, y, z)`. Residual for receiver *i*: `f_i(p) = |p − t| + |p − r_i| − m_i`. Minimise `Σ f_i²` (least squares) with **Levenberg–Marquardt (LM)**:
- Jacobian row (derivative of `f_i` w.r.t. p) = unit vector from TX to p + unit vector from r_i to p.
- Each iteration solves `(JᵀJ + λ·diag(JᵀJ)) δ = −Jᵀf` with `Matrix3.solve`; if the cost drops, accept the step and decrease λ (behaves like Gauss–Newton, fast); otherwise increase λ (behaves like gradient descent, safe).
- Stops when the step is tiny or after 200 iterations.
- **Multi-start:** runs from 8 starting points around the sensor centroid, keeps the lowest residual. If two solutions are equally good (mirror image below a flat sensor layout), it keeps the one with **higher Z** (assumes the drone is above the sensors).
- **Residual** reported = RMS of `f_i` in metres (≈0 for noiseless data).
- **Condition number** = sqrt(largest / smallest eigenvalue of JᵀJ). Large = geometry can't pin the position down well (e.g. drone in the same plane as all sensors). Above `1e8` → `NUMERICALLY_UNSTABLE`.
- Fewer than 3 range measurements → `UNDER_CONSTRAINED`; a range shorter than the TX–RX distance (physically impossible) → `INVALID_INPUT`; duplicate receiver IDs → `INVALID_INPUT`.
- Why the old method failed: it guessed from received power, forced z = 500 m, and used finite-difference gradient descent → 80–115 m error even without noise.

**Geometry check.** Put TX and all RX positions into a covariance matrix; its 3 eigenvalues say how spread out the sensors are in each direction. Middle eigenvalue ≈ 0 → all on a line (can't localize). Smallest ≈ 0 → all in a plane (works, but mirror ambiguity → warning).

**Error metrics** (successful steps only): mean = Σe/n, max = max e, RMS = sqrt(Σe²/n). RMS ≥ mean always.

**Why error is 0.0000 m:** measurements are noiseless, so a correct solver recovers the exact position. It proves correctness, not real-world accuracy.

---

## 8. Rules — things a change must not break

1. **Match measurements by receiver ID** (`ReceiverMeasurement.receiverId` ↔ `Receiver.getId()`), never by list index.
2. **Time comes from the step index** (`t = i * timeStep`), never `t += timeStep`.
3. **Failures are explicit:** return a `Status` other than `SUCCESS`; never return a made-up position as success. Error for failed steps is `NaN` and is excluded from metrics.
4. **Formulation changes → bump the version** in `getDescriptor()` (and update the version asserts in `RegressionTest`).
5. **Records are immutable.** Adding a field to a record means updating **every** constructor call:
   - `EvaluationResult`: `EvaluationModule` (2 places) + `persistence/ScenarioPersistenceTest`
   - `ExperimentRecord`: `ExperimentRunner` + `experiment/EvaluationModuleTest`
   - `EstimationResult`: `ExperimentRunner`, `BaselineLocalizationModel` (`failure()` + end of `estimate`) + `EvaluationModuleTest`
   - `Measurement`: `ExperimentRunner`, `BaselineMeasurementModel`, `EvaluationModuleTest`, `BaselineLocalizationModelTest`
   - `ReceiverMeasurement`: `BaselineMeasurementModel`, `BaselineLocalizationModelTest`
   - `ScenarioConfig`: `ScenarioConfig.defaultConfig()`, `ScenarioConfig.fromScenario()`, `ScenarioEditor.getConfig()`, `ScenarioConfigTest` (4 places)
6. **Display ≠ maths:** visual fixes stay in `ui/`.
7. **GUI and CLI share the pipeline:** put logic in model classes, not in `SimulatorController` or `CLISimulatorApp`.
8. `SPEED_OF_LIGHT = 3e8` is defined in **two** places (`BaselineMeasurementModel`, `RadarPhysics`) — change both if needed.

---

## 9. Recipes for likely changes

### 9.1 Add a CLI option (e.g. `--velocity vx vy vz`)
File: `CLISimulatorApp.java`.
1. Declare a variable with a default before the loop, e.g. `Vector3D velocity = new Vector3D(50, 0, 0);`
2. Add a case: `case "--velocity": velocity = new Vector3D(Double.parseDouble(args[++i]), Double.parseDouble(args[++i]), Double.parseDouble(args[++i])); break;`
3. Use it where needed (e.g. pass it into `buildDefaultScenario(receiverCount, velocity)` and use it in `new ConstantVelocityTrajectory(...)`).
4. Update the Javadoc option list at the top of the class and the README table.

### 9.2 Add a column to the GUI results table
File: `ui/ResultsPanel.java`, constructor, inside `table.getColumns().addAll(List.of(...))`:
```java
column("Speed (m/s)", r -> String.format("%.2f", r.groundTruthVelocity().magnitude())),
```
Any value from `ExperimentRecord` can be shown this way.

### 9.3 Add a CSV column
File: `persistence/ScenarioPersistence.java`, `exportResultsToCSV`: add the name to the header string, a `%…` to the `printf` format, and the value to the argument list (same position in all three).

### 9.4 Add an evaluation metric (e.g. standard deviation)
1. `experiment/EvaluationResult.java`: add `double stdError` to the record.
2. `experiment/EvaluationModule.java`: compute it (`sqrt(Σ(e−mean)²/n)` over successful steps); update **both** `new EvaluationResult(...)` calls (use `Double.NaN` in the all-failed branch).
3. `persistence/ScenarioPersistenceTest.java`: update its `new EvaluationResult(...)`.
4. Show it: `SimulatorController.formatMetrics`, `CLISimulatorApp` summary `printf`, `ScenarioPersistence.exportEvaluationSummary`.
5. Test in `EvaluationModuleTest` with hand-calculated values.

### 9.5 Add measurement noise
File: `measurement/BaselineMeasurementModel.java`.
```java
private final double rangeNoiseStd;   // metres
private final java.util.Random random;

public BaselineMeasurementModel() { this(0.0, 0L); }      // keeps current noiseless behaviour
public BaselineMeasurementModel(double rangeNoiseStd, long seed) {
    this.rangeNoiseStd = rangeNoiseStd;
    this.random = new java.util.Random(seed);              // seeded → reproducible
}
// in generate(), after computing bistaticRange:
bistaticRange += random.nextGaussian() * rangeNoiseStd;
```
Recompute `timeDelay` from the noisy range. Bump the version only when noise is on (e.g. return `"1.1"` if `rangeNoiseStd > 0`) so existing tests still expect `"1.0"`. Noisy runs give non-zero error — don't use them in the 1 mm tests.

### 9.6 Add a new trajectory (e.g. circular)
New file `scenario/CircularTrajectory.java`:
```java
public class CircularTrajectory implements TrajectoryModel {
    private final Vector3D center; private final double radius, omega; // omega in rad/s
    public CircularTrajectory(Vector3D center, double radius, double omega) {
        this.center = new Vector3D(center); this.radius = radius; this.omega = omega;
    }
    @Override public Vector3D getPosition(double t) {
        return center.add(new Vector3D(radius * Math.cos(omega * t), radius * Math.sin(omega * t), 0));
    }
    @Override public Vector3D getVelocity(double t) {
        return new Vector3D(-radius * omega * Math.sin(omega * t), radius * omega * Math.cos(omega * t), 0);
    }
}
```
Use it in `CLISimulatorApp.buildDefaultScenario` or a test. **Caution:** `ScenarioPersistence.saveScenario` only writes `ConstantVelocity` trajectories, and `ScenarioConfig.fromScenario` / the GUI form assume constant velocity — supporting it in the GUI/persistence needs a `trajectory.type` branch and new form fields.

### 9.7 Add a geometry rule (e.g. "receivers must be at or above ground")
File: `localization/GeometryValidator.java`, in `validate()` before the baseline check:
```java
for (Receiver rx : receivers) {
    if (rx.getPosition().z < 0) {
        return new ValidationResult(false, Status.INVALID_INPUT,
            "Receiver " + rx.getId() + " is below ground (z < 0)");
    }
}
```
Add a test in `GeometryValidatorTest`. The GUI and CLI show the message automatically.

### 9.8 Change a threshold / default
| What | Where |
|---|---|
| "Unstable" condition-number limit (1e8) | `BaselineLocalizationModel.MAX_CONDITION_NUMBER` |
| LM iterations (200), start points | `BaselineLocalizationModel.MAX_ITERATIONS`, `START_DIRECTIONS` |
| Min receivers (3), min spacing (1 m), flatness (1e-6) | `GeometryValidator` constants |
| GUI default scenario | `ScenarioConfig.defaultConfig()` |
| CLI defaults / built-in layout | `CLISimulatorApp.main` variables, `buildDefaultScenario` |
| Test tolerance (1 mm) | `RegressionTest.NOISELESS_TOLERANCE_M` |

### 9.9 Add a new localization (or measurement) model and use it
1. New class implementing `LocalizationModel` with its own `getDescriptor()` name/version.
2. GUI: change the `localizationModel` field initialiser in `SimulatorController`. CLI: change `LocalizationModel locModel = new BaselineLocalizationModel();` in `CLISimulatorApp`.
3. The status line, CSV and records then show the new name/version automatically.

### 9.10 Add a field to the GUI form (e.g. drone RCS)
1. `ScenarioConfig`: add the record component; update `defaultConfig()`, `fromScenario()`, and validate it in `toScenario()`.
2. `ScenarioEditor`: add a `TextField`, put it in a `grid(...)` row in the constructor, fill it in `setConfig`, read it with `parse(...)` in `getConfig()`.
3. Update the 4 `new ScenarioConfig(` calls in `ScenarioConfigTest`.
4. If it must be saved: add keys in `ScenarioPersistence.saveScenario` / `loadScenario`.

### 9.11 Change how the 3D view looks
`ui/Visualization3D.java`: colours = the `*_COLOR` constants; sensor size `0.012 * SCENE_SIZE`, estimate points `0.004 * SCENE_SIZE`, trajectory line radius `1.5` (in `showExperiment`); playback length `records.size() / 10.0` (≈10 s) in `startPlayback`; max drawn points `MAX_DRAWN_POINTS`. Initial camera angle: `yaw` / `pitch` `Rotate` fields (−30°).

### 9.12 Generate a new quantity (e.g. AZIMUTH at each receiver)
In `BaselineMeasurementModel.generate`, per receiver: `Vector3D d = targetPosition.subtract(receiver.getPosition()); values.put(Quantity.AZIMUTH, Math.toDegrees(Math.atan2(d.y, d.x)));` (azimuth counter-clockwise from East, as in the design doc; elevation = `Math.toDegrees(Math.atan2(d.z, Math.hypot(d.x, d.y)))`). The localizer ignores quantities it doesn't use.

---

## 10. Known gaps (be honest about these)

Measurements are noiseless; received power is a placeholder formula; receiver orientation is stored but unused (no azimuth/elevation input yet); only one formulation per model; full experiment reload not implemented (scenario only, CSV export); only constant-velocity trajectory in GUI/persistence; GUI runs the experiment on the UI thread (~1 ms/step); no packaging (fat JAR/jpackage); stakeholder formulation and MATLAB reference cases not received yet. Details: `docs/MID-DEMO.md`.

---

## 11. Quick answers for the viva

- **Why bistatic range?** It's what a TX–RX pair measures from the echo delay: total path TX→target→RX = c × delay.
- **Why at least 3 receivers?** 3 unknowns (x, y, z), one equation per receiver.
- **Why multiple start points?** Least squares on ellipsoids can have several local minima (e.g. the mirror solution); trying several starts and keeping the best avoids getting stuck.
- **Why LM instead of plain gradient descent?** LM uses the Jacobian to take Gauss–Newton steps (fast, precise near the answer) and falls back to small gradient-like steps when a step fails (robust).
- **What does a high condition number mean?** Small measurement errors would cause large position errors — the geometry is weak in some direction.
- **Why are collinear sensors rejected but coplanar ones allowed?** Collinear: infinitely many solutions on a circle. Coplanar: only two (mirror images), resolved by assuming the drone is above the sensors.
- **Why match by receiver ID?** So reordering the receiver list can never attach a measurement to the wrong receiver.
- **Why 0 m error?** Noiseless data; it checks correctness. Real accuracy needs the noise model.
- **How are formulations swapped?** Implement `MeasurementModel` / `LocalizationModel`, give a new name/version, change one line in the controller/CLI.
