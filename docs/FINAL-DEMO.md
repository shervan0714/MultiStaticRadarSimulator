# Final Demo and Handoff — 6 November 2026

This covers the stakeholder-executed demo, the verification evidence, the requirement status against the Phase 3 design document, and what is still open. The mid-evaluation version is in [MID-DEMO.md](MID-DEMO.md).

## 1. Handoff package

| What | Where | For whom |
|---|---|---|
| `MultiStaticRadarSimulator-windows.zip` | `./gradlew jpackageZip` → `build/dist/` | Dr. Giridhar on Windows: unzip, double-click `MultiStaticRadarSimulator.exe`. Java is bundled; nothing to install. |
| `MultiStaticRadarSimulator.jar` | `./gradlew fatJar` → `build/dist/` | Any machine with Java 17+ (Windows, Linux, Apple-Silicon Mac): `java -jar MultiStaticRadarSimulator.jar` |
| Source + README | GitHub `shervan0714/MultiStaticRadarSimulator`, branch `main` | Build, test and run steps; GUI, formula and CLI usage |
| Contact | E. Sai Shervan, cs24b073@smail.iitm.ac.in | Questions after handoff |

Both packages were checked on the development machine from a separate folder: the JAR's GUI and `--cli` mode run, and the unzipped Windows app opens with no Java on the PATH (it uses the bundled runtime). They have **not yet been tried on Dr. Giridhar's own machine**; the dry run below is that check.

## 2. Stakeholder-executed demo (about 8 minutes)

Dr. Giridhar runs the steps; the student only reads them out. If he cannot attend, record a screen video of him doing the same steps.

1. **Start.** Unzip the package and double-click `MultiStaticRadarSimulator.exe`. The window opens with the default 4-receiver scenario already evaluated: 101 steps, 0 failed, error 0.0000 m (noiseless data, so this is a correctness check).
2. **Inspect.** Drag to orbit the 3D view, press *Play*, click a row in the results table: the view jumps to that step. Yellow arrows show where each receiver points.
3. **Change the geometry.** Edit a receiver position or add a fifth receiver, click *Run experiment*. Then remove receivers until two are left: the red message reads `UNDER_CONSTRAINED` and no estimate is shown.
4. **Add noise.** Set *Range noise σ* to 5 m and run: the mean error becomes about 11.7 m. Run again with the same seed: identical numbers.
5. **Compare the built-in formulations.** Click *Compare models*: the *Model comparison* tab shows the baseline (≈ 11.7 m) and the closed form (≈ 22.5 m) on exactly the same noisy data. Click a row to see that model's run in 3D.
6. **His own formula, no code.** In *Custom formulas*, set *Measurements from* to `Range + bias` (data with a 25 m range bias) and click *Compare models* (set the noise back to 0 first): baseline ≈ 60.6 m, closed form ≈ 68.0 m, the formula `d_tx + d_rx + bias` 0.0000 m. Then let him type a formula of his own (*Add formula*), pick it under *Localize with*, and run.
7. **Reproduce.** *Save scenario…*, change something, *Load scenario…*: the same results, formulas and chosen model come back. *Export results…* writes a CSV whose rows name the exact model and formula used.

Numbers in steps 4–6 are for the default scenario with seed 42.

## 3. Verification evidence

`./gradlew test`: **160 tests, all passing.** Mapped to the design document's test plan:

| Design test plan item | Test classes (tests) | What they show |
|---|---|---|
| Vector / geometry | `Vector3DTest` (7), `Matrix3Test` (5) | Hand-computable vector operations, 3×3 solve and eigenvalues |
| Scenario model | `ReceiverTest` (8), `TransmitterTest` (2), `DroneTest` (3), `ScenarioConfigTest` (12) | Positions, orientation (azimuth/elevation, boresight), invalid configurations rejected |
| Trajectory engine | `ConstantVelocityTrajectoryTest` (5) | Positions equal the analytical solution |
| Measurement model | `BaselineMeasurementModelTest` (16), `RadarPhysicsTest` (6) | Hand-computed range, delay, receiver-frame angles; seeded noise reproducible with the right spread |
| Localization model | `BaselineLocalizationModelTest` (11), `ClosedFormLocalizationModelTest` (7), `LeastSquaresSolverTest` (5), `GeometryValidatorTest` (12) | Noiseless estimates within 1 mm for both formulations; collinear, too few, duplicate, impossible inputs return an explicit status; mirror ambiguity resolved above ground |
| Experiment runner | `ExperimentRunnerTest` (11) | Every step records truth, measurement, estimate, model identity and time, in order |
| Evaluation | `EvaluationModuleTest` (8) | Mean / max / RMS against hand-calculated values; failed steps excluded |
| Persistence | `ScenarioPersistenceTest` (9) | Save → load round trip (including formulas and chosen model) gives identical results; CSV quoting |
| Configuration / UI | `ScenarioConfigTest` (12) | Form values transferred correctly into the scenario |
| Visualization | `SceneTransformTest` (8) | World → scene transform, handedness, downsampling (scene graph checked by off-screen renders, not an automated test) |
| End-to-end | `RegressionTest` (11) | Full runs with 3/4/5 receivers, wide baseline, close range, elevated transmitter within 1 mm; noisy runs never flip to the mirror solution |
| Formulation comparison | `ModelComparisonTest` (3), `FormulaModelTest` (7), `ExpressionTest` (4) | Every compared model sees identical measurements; user formulas evaluate correctly, localize exactly when they match the data, and are rejected with readable messages |

## 4. Requirement status (Phase 3 design document, 6 Nov milestone)

| Requirement | Status | Evidence |
|---|---|---|
| Configurable TX, any number of receivers with position and azimuth/elevation orientation | Done | GUI form, `ScenarioConfig`, `ReceiverTest` |
| Constant-velocity drone trajectory | Done | `ConstantVelocityTrajectory`; other motions can plug into `TrajectoryModel` |
| Measurement generation (range, delay, power, azimuth, elevation) keyed by receiver ID | Done (power is a placeholder) | `BaselineMeasurementModel` |
| Localization with explicit failure statuses, residual and condition number | Done | `BaselineLocalizationModel`, `GeometryValidator` |
| Ground-truth comparison, mean / max / RMS error | Done | `EvaluationModule` |
| 3D visualization of geometry, true and estimated trajectories | Done | `Visualization3D` |
| Model identity recorded for every step | Done | `ExperimentRecord`, CSV `meas_model` / `loc_model` columns |
| Replaceable formulations and **model comparison** | Done | Two built-in localization models + formulas typed in the GUI; *Compare models* / `--compare` |
| Persistence so experiments can be reproduced | Done for scenarios (incl. noise seed and formulas); results export to CSV but cannot be reloaded | `ScenarioPersistence` |
| Standalone packaging (jpackage, fat-JAR fallback) | Done; clean-machine test pending | `fatJar`, `jpackageZip` |
| Stakeholder formulation and MATLAB reference cases | **Open** — not received; he can now enter the formulation himself in the GUI | — |

## 5. Known gaps (honest list)

1. **Dr. Giridhar's own formulation and MATLAB reference cases are not in yet.** The GUI formula editor is how he can add the formulation; reference outputs would become regression tests.
2. **Received power is a placeholder** (ignores antenna gains and RCS). Power is not used for localization.
3. **User formulas describe the measured range only** (not power or angles), and their parameters are fixed values, not estimated.
4. **Angles are generated but no localizer uses them.**
5. **Results cannot be reloaded**; a run is reproduced from the saved scenario and seed instead (identical numbers).
6. **Long runs block the window**: about 1 ms per step, so a 10,000-step run freezes the GUI for roughly 10 s.
7. **Two assumptions need his confirmation**: the drone is never below ground (used to resolve the mirror solution), and a condition number above 10⁸ counts as unstable.
8. **Only constant-velocity motion** is available.
9. **The GUI has no automated scene-graph test**; it is checked through its pure-Java parts and off-screen renders.
