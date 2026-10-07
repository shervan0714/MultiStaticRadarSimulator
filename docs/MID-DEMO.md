# Mid-Demo — 9 October 2026

## Demo script (about 5 minutes)

1. **Tests first.** Run `./gradlew test`: 111 tests pass. Point out that noiseless reference scenarios must match ground truth within 1 mm, and that degenerate layouts must return an explicit status.
2. **Launch** with `./gradlew run`. The default 4-receiver scenario is already evaluated: 101 steps, 0 failed, mean/max/RMS error 0.0000 m.
3. **Show the 3D view.** Orbit the camera, point out the East/North/Up axes and RX4 raised 400 m on its mast, then press *Play*. Click a row in the results table and the view jumps to that step.
4. **Change the geometry.** Edit a receiver position or add a fifth receiver, then click *Run experiment*. The status line shows the model versions and any geometry warning.
5. **Coplanar warning.** Set RX4's Z to 0 and run again. The run still succeeds, and the status line warns that the target and its mirror image below the sensor plane fit equally well.
6. **Explicit failure.** Remove receivers until two are left and run. The red message reads `UNDER_CONSTRAINED`, and no estimate is shown.
7. **Reproducibility.** Save the scenario, change a value, then load the file again: the same results come back. Export the results to CSV and open it, showing the model name and version recorded on every row.
8. **Command line.** `./gradlew runCli --args="--receivers 5"` runs the same pipeline without the GUI.

## What works

- Configuration of one transmitter and any number of receivers (positions and gains), the drone's start position and constant velocity, and the duration and time step.
- The full pipeline: trajectory → measurement → localization → evaluation, with model name and version recorded for every step.
- Geometry validation with explicit statuses: too few receivers, collinear sensors, duplicate IDs, impossible ranges, and ill-conditioning.
- A 3D view in the design doc coordinate convention, with true and estimated trajectories, playback, and the error at each step.
- Mean, max and RMS error, plus a per-step table that shows residual and condition number.
- Scenario save/load, and CSV export with an evaluation summary.

## Known gaps (honest list)

| # | Gap | Impact | Plan |
|---|---|---|---|
| 1 | **The stakeholder's measurement/localization formulation is not implemented yet.** The current models are my own baseline (bistatic range + least squares), as planned in the design doc. | Results show what the baseline can do, not what Dr. Giridhar's model can do. | Add it as a second model behind the same interfaces once received. |
| 2 | **Measurements are noiseless.** | 0.0000 m error is expected for noiseless data and is a correctness check, not a measure of real-world accuracy. | Seeded Gaussian noise model, with the seed recorded per run. |
| 3 | **Received power uses a placeholder formula** (uses (R_tx+R_rx)², and ignores antenna gains and RCS). Power is not used for localization. | Power values in the CSV are not physically meaningful yet. | Proper bistatic radar equation, confirmed with the stakeholder. |
| 4 | **Receiver orientation is not modelled.** It is stored as a vector, not as azimuth/elevation degrees as the design doc specifies. It doesn't affect measurements, the AZIMUTH/ELEVATION quantities are never generated, and orientation is not drawn. | Orientation acceptance criterion not met yet. | Add azimuth/elevation input, drawing, and use in the measurement model. |
| 5 | **Only one formulation exists**, so the "run two formulations and compare" acceptance criterion is not demonstrated. | — | Model registry plus a side-by-side comparison in the GUI and CLI. |
| 6 | **Persistence covers the scenario, not the full experiment.** Results are exported to CSV but cannot be reloaded, and the format is Java properties, not JSON/Jackson as the proposal said. | Results can be reproduced only by re-running the experiment. | Save/load a full experiment (scenario, model versions, seed, results). |
| 7 | **Mirror ambiguity is resolved by assumption.** When all sensors lie in one plane, the solver returns the solution above that plane. The "numerically unstable" cut-off (condition number > 1e8) is my choice, not validated. | A target below a coplanar sensor plane is reported at its mirror position. | Confirm both choices with the stakeholder. |
| 8 | **Constant-velocity trajectory only.** | — | The trajectory interface allows more models; add one if the stakeholder needs it. |
| 9 | **The GUI runs the experiment on the UI thread** (about 1 ms per step). | A 10,000-step run freezes the window for roughly 10 s. | Move the run to a background task with a progress indicator. |
| 10 | **No packaging.** Running requires a JDK and `gradlew`; not yet tried on the stakeholder's machine. | Stakeholder cannot run it standalone yet. | Fat JAR + jpackage, tested on a clean machine. |
| 11 | **No stakeholder/MATLAB reference cases yet.** All reference cases are synthetic. | Correctness is checked against ground truth, not against the stakeholder's model. | Add the stakeholder's reference cases as regression fixtures when provided. |
| 12 | **The GUI is verified through its pure-Java parts** (form mapping, coordinate transform) and manual/off-screen checks. There is no automated scene-graph test. | — | Add a headless JavaFX test if time allows. |
