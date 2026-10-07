# Weekly Contribution Log

**Student**: E. Sai Shervan  
**Roll No**: CS24B073  
**Course**: CS5013 - Programming with AI  
**Project**: Multi-Static Radar Simulator  
**Stakeholder**: Dr. Giridhar K, Department of Electrical Engineering  

---

## Week 1: Sep 6 - Sep 12, 2026

### Work Completed
- Created GitHub repository structure with Gradle build system
- Designed and documented complete architecture (4 core modules)
- Created DESIGN.md document with full specification
- Set up project directory structure (src/main, src/test, docs)
- Defined module interfaces and dependencies
- Created initial test plan

### Contributions
- ✓ Analyzed project requirements with Girdhar sir
- ✓ Finalized architecture decisions
- ✓ Wrote comprehensive DESIGN.md (due Sep 11)
- ✓ Pushed all files to GitHub
- ✓ Identified risks and mitigation strategies

### Hours Spent
- Design & Documentation: 6 hours
- Setup & Planning: 2 hours
- **Total: 8 hours**

### Next Week's Plan
- Implement Vector3D utility class
- Create Transmitter, Receiver, Drone skeleton classes
- Begin RadarPhysics module implementation
- Start unit test writing

### Blockers / Notes
- Awaiting final power calculation formula from Girdhar sir
- Using standard radar equation as placeholder until formula confirmed

---

## Week 2: Sep 13 - Sep 19, 2026

### Work Completed
- Implemented scenario and trajectory modules (Scenario, TrajectoryModel, ConstantVelocityTrajectory)
- Defined measurement and localization interfaces (MeasurementModel, LocalizationModel)
- Created data model records: Measurement, ReceiverMeasurement, Quantity, ModelDescriptor, EstimationResult, Status
- Implemented configurable transmitter/receiver geometry and orientation support
- Established constant-velocity target motion with analytical position verification

### Contributions
- ✓ Completed Phase 3 Design 1.3: Scenario and Trajectory modules
- ✓ Completed Phase 3 Design 1.4: Measurement and Localization interfaces
- ✓ Pushed all interface definitions and data models to GitHub

### Hours Spent
- Interface design & implementation: 5 hours
- Scenario and trajectory modules: 3 hours
- **Total: 8 hours**

### Next Week's Plan
- Implement baseline measurement and localization formulations
- Build ExperimentRunner and EvaluationModule
- Create geometry validation for under-constrained configurations
- Begin comprehensive unit testing

### Blockers / Notes
- Stakeholder formulation not yet available; using hand-verifiable baseline
- Decided to keep interfaces simple and version-labelled per design doc

---

## Week 3: Sep 20 - Sep 26, 2026

### Work Completed
- Implemented BaselineMeasurementModel: bistatic range, time delay, and received power calculation
- Implemented BaselineLocalizationModel: power-weighted triangulation with residual computation
- Created ExperimentRunner: full pipeline orchestration (config → trajectory → measurement → localization → evaluation)
- Created EvaluationModule: computes mean, max, and RMS error from experiment records
- Created ExperimentRecord: immutable per-step data capture for reproducibility
- Implemented GeometryValidator: detects under-constrained, collinear, and poorly-conditioned receiver geometry
- Created ScenarioPersistence: scenario save/load (Properties format) and CSV results export
- Added comprehensive unit tests for trajectory, measurement, localization, evaluation, geometry validation, and experiment runner

### Contributions
- ✓ Completed 25 Sep milestone: one baseline measurement/localization formulation validated
- ✓ Integrated experiment runner and evaluation module
- ✓ Added explicit handling of invalid and under-constrained geometry
- ✓ Added 35+ new unit tests across 7 test classes

### Hours Spent
- Measurement/localization implementation: 4 hours
- ExperimentRunner & EvaluationModule: 3 hours
- GeometryValidator & persistence: 3 hours
- Unit tests: 3 hours
- **Total: 13 hours**

### Next Week's Plan
- Complete regression testing across different receiver counts and geometries
- Add persistence round-trip validation
- Upgrade CLI and UI to use ExperimentRunner pipeline
- Update SimulatorController with live error statistics

### Blockers / Notes
- Baseline localization uses simple power-weighted averaging; will need replacement with proper algorithm when stakeholder formula is available
- The replaceable interface pattern is working well — can swap models without touching other modules

---

## Week 4: Sep 27 - Oct 3, 2026

### Work Completed
- Complete regression test suite: validated scenarios across 3, 4, and 5 receiver configurations
- Tested well-conditioned geometries (triangle, square, wide baseline, close-range) and difficult geometries (collinear, under-constrained)
- Persistence round-trip tests: save → load → verify all fields preserved, loaded scenario produces identical results
- CSV export verification tests
- Rewrote CLISimulatorApp to use full ExperimentRunner pipeline with geometry validation, evaluation summary, and CSV export support
- Rewrote SimulatorController to use modular MeasurementModel/LocalizationModel pipeline with live aggregate error statistics (mean/max/RMS)
- Added ReceiverTest class (was previously missing from test suite)
- Model identity and evaluation data recorded in every experiment record

### Contributions
- ✓ Completed 2 Oct milestone: regression testing, persistence, and reference-case validation
- ✓ 10 regression/reference-case tests covering varied receiver counts and geometries
- ✓ CLI now supports --save, --load, --export, --receivers, --duration, --timestep
- ✓ SimulatorController now shows live error statistics and validates geometry before start
- ✓ Total test count: 50+ unit tests across 11 test classes

### Hours Spent
- Regression test suite: 3 hours
- Persistence tests: 2 hours
- CLI rewrite with ExperimentRunner pipeline: 2 hours
- SimulatorController rewrite: 2 hours
- ReceiverTest and cleanup: 1 hour
- **Total: 10 hours**

### Next Week's Plan
- Mid-demo preparation (Oct 9): demonstrate full end-to-end workflow
- Polish visualization to show trajectory trails
- Prepare stakeholder demo scenario
- Review and document known gaps honestly

### Blockers / Notes
- Localization accuracy is limited by the baseline power-weighted averaging algorithm — known gap, will be declared in mid-demo gap list
- Persistence uses Java Properties format; may switch to JSON with Jackson if stakeholder needs more structured export

---

## Week 5: Oct 4 - Oct 10, 2026

### Work Completed
- Upgraded the Gradle wrapper to 9.3.0: the previous 8.x wrapper failed on JDK 26 ("Unsupported class file major version 70"), so `gradlew test` could not run. Added a `gradlew` Unix script and a `runCli` Gradle task for running the pipeline without the GUI.
- Replaced the baseline localization (power-weighted average + finite-difference gradient descent, which left 80-115 m error even on noiseless data) with a Levenberg-Marquardt least-squares solver on the bistatic-range ellipsoids using the analytic Jacobian (BaselineLocalization v2.0). It uses multiple starting points, prefers the higher-altitude solution when a mirror-image solution fits equally well, reports the RMS range residual and the Jacobian condition number, and returns INVALID_INPUT for ranges shorter than the TX-RX baseline and NUMERICALLY_UNSTABLE for ill-conditioned geometry. Noiseless CLI scenarios with 3/4/5 receivers now give 0.0000 m error. Added a small `Matrix3` helper (3x3 solve and symmetric eigenvalues).
- Tightened the verification suite: regression and reference cases previously only asserted that the error was a finite number. Noiseless cases (3/4/5 receivers, wide baseline, close range, elevated transmitter) must now match ground truth within 1 mm. Added localization tests for several target positions, the coplanar mirror ambiguity, an in-plane target (correct answer, but condition number > 1e5 flags it), and missing ranges, plus hand-computed `Matrix3` tests. 86 tests, all passing.
- Fixed the measurement contract (design doc 1.4): the localizer used `receiverId` as a list index, so reordering the receiver list would attach observations to the wrong receiver. `ReceiverMeasurement.receiverId` is now the receiver's string ID (a deliberate change from `int` in the design doc, so it matches `Receiver.getId()`), matched by lookup; duplicate IDs give INVALID_INPUT. ExperimentRunner now stamps each Measurement with its step time (it was always 0.0) and computes time from the step index, so fractional steps no longer drop the last sample (0.1+0.1+0.1 > 0.3). 90 tests, all passing.
- Reworked GeometryValidator: it ignored the transmitter, computed a coplanarity check but never used the result, picked receivers 0-2 as a fixed basis (so the answer depended on list order), and used an absolute threshold that depended on coordinate scale. It now treats TX + receivers as one sensor set and measures their spread with the eigenvalues of the position covariance: collinear sensors are UNDER_CONSTRAINED, while coplanar sensors are valid but carry a mirror-ambiguity warning (shown in the CLI and GUI status). It also rejects duplicate receiver IDs. Collinear receivers with an off-line transmitter are now correctly accepted (verified by localizing a target in that layout). 95 tests, all passing.
- Rebuilt the GUI workflow around the experiment pipeline. The old GUI moved a `Drone` frame-by-frame outside the Scenario/ExperimentRunner pipeline, and its sliders were never connected. The new `ScenarioEditor` panel edits the transmitter, a variable-length receiver list (add/remove, ID, X/Y/Z, gain), the drone start/velocity and duration/time step. "Run experiment" goes form → `ScenarioConfig` → Scenario → geometry validation → ExperimentRunner → EvaluationModule, and shows the model versions, geometry warnings and mean/max/RMS error. Bad input is reported in red naming the field. `ScenarioConfig` is plain Java, so the UI-to-scenario mapping has unit tests (design doc test plan: Configuration/UI). Removed the unused `ParameterPanel`. 103 tests, all passing.
- Rewrote the 3D view. It used metre coordinates directly as JavaFX coordinates, but JavaFX has +Y pointing down, so the scene was not in our East/North/Up convention. `SceneTransform` now maps ENU to JavaFX (East→+x, North→+z, Up→−y; handedness preserved, so nothing is mirrored) and auto-scales to fit the scenario, with unit tests (design doc test plan: Visualization). The view shows a ground grid, E/N/U axes, receiver masts, the true trajectory line and the estimated points (downsampled to 400 for long runs, per the design doc risk list), and the error line for the selected step. Drag to orbit, scroll to zoom; a time slider and Play button step through the run. 111 tests, all passing.
- Added the results panel and file actions to the GUI. Below the 3D view, a per-step table shows time, true and estimated X/Y/Z, error, status (failures in red), residual and condition number, with mean/max/RMS error above it. Selecting a row moves the 3D view to that step, and the slider highlights the matching row. "Save scenario", "Load scenario" and "Export results" connect the existing ScenarioPersistence to the GUI (properties file; CSV with model versions plus an evaluation summary), so a stakeholder can reproduce an experiment without editing code. Checked save → modify → load → export end-to-end: same 101 steps and identical error after reload.
- Mid-demo documentation: rewrote the README (build/run steps, GUI and CLI usage, coordinate convention, architecture, verification, contact) and added docs/MID-DEMO.md with the demo script and an honest gap list. Removed 13 outdated files left over from setup (build-troubleshooting pages, GitHub setup notes, status summaries from 6 Sep, the javac build scripts superseded by gradlew, and the early docs/DESIGN.md draft, which contradicted the submitted Phase 3 design doc). Corrected the name and roll number in this log.

### Contributions
- ✓ Core workflow runs end to end in the GUI and CLI: configure → trajectory → measurement → localization → evaluation → visualization
- ✓ Localization verified: noiseless reference cases match ground truth within 1 mm (previously 80-115 m off)
- ✓ Removed the doc clutter left over from setup and rewrote the README; wrote the mid-demo script and an honest gap list (docs/MID-DEMO.md)
- ✓ Test count 76 → 111, all passing

### Hours Spent
- **Total: ____ hours**

### Next Week's Plan
- Incorporate mid-demo feedback
- Receiver orientation as azimuth/elevation (input, drawing, use in measurements)
- Seeded noise model; correct bistatic radar equation for received power
- Ask Dr. Giridhar for his formulation and reference cases

### Blockers / Notes
- Stakeholder formulation still not received; the bistatic-range baseline stands in for it (design doc Plan B)
- Two assumptions need stakeholder confirmation: the target is above a coplanar sensor plane, and a condition number above 1e8 counts as unstable

---

## Week 6: Oct 11 - Oct 17, 2026

### Work Completed
(To be filled in)

### Contributions
(To be filled in)

### Hours Spent
- **Total: ____ hours**

### Next Week's Plan
(To be filled in)

### Blockers / Notes
(To be filled in)

---

## Week 7: Oct 18 - Oct 24, 2026

### Work Completed
(To be filled in)

### Contributions
(To be filled in)

### Hours Spent
- **Total: ____ hours**

### Next Week's Plan
(To be filled in)

### Blockers / Notes
(To be filled in)

---

## Week 8: Oct 25 - Oct 31, 2026

### Work Completed
(To be filled in)

### Contributions
(To be filled in)

### Hours Spent
- **Total: ____ hours**

### Next Week's Plan
(To be filled in)

### Blockers / Notes
(To be filled in)

---

## Week 9: Nov 1 - Nov 5, 2026

### Work Completed
(To be filled in)

### Contributions
(To be filled in)

### Hours Spent
- **Total: ____ hours**

### Next Week's Plan
(To be filled in)

### Blockers / Notes
(To be filled in)

---

## Week 10: Nov 6, 2026

### Work Completed
(To be filled in - FINAL DEMO & SUBMISSION)

### Contributions
(To be filled in)

### Total Project Hours
(To be calculated at end)

### Lessons Learned
(To be filled in at end)

---

## Summary Statistics

| Milestone | Date | Status |
|-----------|------|--------|
| Design Doc | Sep 11 | Submitted |
| Mid-Demo | Oct 9 | Ready |
| Final Demo | Nov 6 | Planned |

**Total Hours (Estimated)**: 80-100 hours over 10 weeks
