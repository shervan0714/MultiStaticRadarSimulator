# Prompt Log — Interactive Multistatic Radar Simulator

**Course:** CS5013  
**Project:** Interactive Multistatic Radar Simulator for 3D Drone Localization and Model Evaluation


## 1. Understanding the project

### Prompt 1 — Understand the requirements

I am working on an Interactive Multistatic Radar Simulator for 3D Drone Localization. First read my project proposal and Phase 3 design document. Help me understand what exactly we are supposed to build and what the stakeholder is expecting. Explain the main features in simple terms. Dont make any code changes yet.

### Prompt 2 — Compare the implementation with the design

Now compare the actual code with the module responsibilities in section 1.3 of my design document. For each module, tell me whether it is complete, partially implemented or missing. Give the relevant file or test as evidence. Please dont assume a feature is complete just because a class or interface exists.

### Prompt 3 — Plan the work and commits

Based on the code review, help me decide what should be fixed first before the mid-demo. Divide the work into small commits in priority order. For each one, mention the files likely to change, the expected behaviour and how I can verify it. Dont execute the changes until I review the plan.

### Prompt 4 — Understand the data flow

Explain how my actual code moves data from scenario configuration to trajectory generation, measurement generation, localization, evaluation and visualization. Use the real class names from the repository and point out any difference between the intended architecture and what is implemented.

## 2. Localization accuracy and solver debugging

### Prompt 5 — Investigate the large localization error

I ran the CLI on noiseless data and the localization error was around 80 to 115 metres, even though the tests were passing. This is a serious issue because the actual drone position is known in the simulation. Inspect the solver and existing tests first. Help me find why such a large error is not being caught. Dont change the code before explaining the likely root cause.

### Prompt 6 — Improve the accuracy test

The current tests seem to allow a finite localization error without checking if the estimated position is actually correct. Help me improve the test for a valid noiseless synthetic scenario, where the estimate should match the known target position within 1 mm. Use deterministic inputs and explain why this tolerance is suitable for this test. Dont claim the test passes until it has been run.

### Prompt 7 — Check the gradient descent implementation

Inspect my existing gradient descent localization implementation. Check the objective function, gradient, step size and convergence condition. Since the error is very large even without measurement noise, help me determine whether the problem is in the optimization method or in how measurements are passed to it. Compare possible approaches before suggesting a replacement.

### Prompt 8 — Replace the solver carefully

We have identified that the existing gradient descent approach is not giving reliable localization results. Help me replace it with a Levenberg–Marquardt least-squares solver for the bistatic-range measurements. Keep the existing `LocalizationModel` interface if possible. Explain the residual equation, Jacobian and convergence checks before proposing the code change.

### Prompt 9 — Verify the new solver

After changing the solver, run the same noiseless scenarios as before, including cases with 3, 4 and 5 receivers. Show the actual localization errors and compare them with the earlier results. Check the tests as well. Dont say the new method is better just because it uses a different algorithm; I want numerical evidence.

## 3. Debugging incorrect test assumptions

### Prompt 10 — Investigate the ground-level target test

One localization test expects a target at ground level to return a numerically unstable status. Before changing the solver, check whether this expectation is actually correct. Work through the geometry and measurements and explain whether the code is wrong or the test assumption is wrong. Dont modify anything until we understand the case.

### Prompt 11 — Correct the test only if needed

After investigating the ground-level target case, update the test only if the expected behaviour is incorrect. Explain why the revised expectation makes sense and ensure the test still checks useful behaviour. Run the focused test and the complete test suite if the environment allows it.

## 4. Receiver IDs and measurement timestamps

### Prompt 12 — Match measurements by receiver ID

I suspect measurements may be matched to receivers using their list positions instead of their actual receiver IDs. Inspect `BaselineMeasurementModel`, `Measurement`, `ReceiverMeasurement` and the localization code. Explain what can go wrong if the receiver list is reordered, and suggest a fix that keeps every observation associated with the correct receiver.

### Prompt 13 — Add receiver-order and timestamp tests

Add regression tests for receiver matching. Create a scenario where the receiver list is reordered but receiver IDs remain the same, and check that the observations are still associated correctly. Also verify that measurement time corresponds to the correct simulation step. Run the tests and report the actual result.

## 5. Geometry validation

### Prompt 14 — Review the coplanarity check

Inspect `GeometryValidator`. The simulator needs to detect layouts that may cause ambiguous 3D localization. Check whether the coplanarity check considers the transmitter as well as the receivers, and whether the validator is actually used by the localization pipeline. Explain the current logic before proposing changes.

### Prompt 15 — Test mirror ambiguity

I want to test a case where the sensors lie in the same horizontal plane. A target and its mirror position across that plane may produce the same bistatic ranges. Help me build a deterministic test and inspect what the current solver returns. Explain why getting a finite position does not prove that the geometry is reliable.

### Prompt 16 — Check under-constrained status handling

Inspect `GeometryValidator`, `Status` and the code which displays localization results. Check how the project handles invalid input, under-constrained geometry and numerical instability. Make sure the code uses the status values that actually exist in the enum. Suggest tests so an unreliable estimate is not silently shown as a normal successful result.

## 6. Gradle and JDK compatibility

### Prompt 17 — Investigate the Gradle/JDK failure

My Gradle wrapper is failing when I try to build the project with JDK 26. Check the exact error, Gradle wrapper version, `build.gradle` and the Java version being used. Help me determine whether this is a Gradle compatibility problem or an application-code problem. Dont change the build files before explaining the cause.

### Prompt 18 — Fix the build with minimal changes

Based on the compatibility issue, help me choose a suitable Gradle version while keeping the existing JavaFX and other dependencies working. Dont upgrade unrelated libraries unnecessarily. After the change, run the build and tests if possible, and tell me which errors remain instead of assuming the issue is solved.

## 7. GUI integration and 3D visualization

### Prompt 19 — Connect the GUI to the experiment pipeline

Inspect `SimulatorController`, `ScenarioEditor` and `ExperimentRunner`. Help me connect the UI to the actual scenario and experiment pipeline so that clicking Run uses the same trajectory, measurement, localization and evaluation logic as the backend. Preserve the existing module boundaries and avoid duplicating numerical calculations inside the UI.

### Prompt 20 — Debug the inverted 3D axis

I noticed that the 3D visualization does not seem to match the coordinate convention in my design document: +X is East, +Y is North and +Z is upward. Inspect `SceneTransform` and `Visualization3D`. Check how world coordinates are mapped to the JavaFX scene and help me find any inverted axis or wrong sign. Dont change the radar calculations just to fix a display issue.

### Prompt 21 — Verify the rendered scene

After the coordinate fix, launch the application and inspect the rendered output if the environment supports it. Check transmitter and receiver locations, drone trajectory and the displayed axes. Unit tests alone may not catch a visual inversion. If you cannot launch the GUI or inspect a screenshot, clearly tell me which checks you could not perform.

## 8. Results, persistence and CSV export

### Prompt 22 — Improve the results table

Inspect `EvaluationModule`, `EvaluationResult` and `ResultsPanel`. Help me display the error at each time step along with mean, maximum and RMS localization errors. Show residual, condition number and model version only where these values are available in the actual result objects. Keep the UI separate from the numerical calculations.

### Prompt 23 — Test scenario save and load

Inspect `ScenarioPersistence` and the scenario classes. Check whether transmitter, receiver positions, orientations and drone parameters are preserved when a scenario is saved and loaded. Identify fields that may be lost. Add a round-trip test using non-default values and verify the result instead of assuming persistence works.

### Prompt 24 — Check CSV export

Review the CSV export in my project. I want each row to contain the relevant simulation time, ground-truth position, estimated position, localization error and model identity where available. Compare the exported values with `ExperimentRecord` and `EvaluationResult` rather than recalculating them independently in the UI. Test a small experiment and inspect the actual CSV output.

## 9. Git and package refactoring

### Prompt 25 — Review the Git history

I have made several changes before the mid-demo. Check the existing Git history and working tree, and help me understand which changes are already committed. Suggest clear commit messages that explain the purpose of each change. Dont create duplicate commits or claim something is committed unless Git confirms it.

### Prompt 26 — Check GitHub attribution

I want future commits to be attributed correctly to my GitHub account, `shervan0714`. Check the configured Git author name, email and remote URL. Explain the difference between the account used for pushing and the email attached to a commit. Help me correct the configuration for future commits without rewriting existing history unnecessarily.

### Prompt 27 — Investigate empty folders

There are empty folders in my project and I am not sure if they are needed. Check whether they are tracked source folders, generated Gradle output or cache directories. Explain which ones are safe to clean and make sure no source files or important configuration are removed.

### Prompt 28 — Reorganize the old model package

My old `model` package contains classes with different responsibilities. Compare the package structure with section 1.3 of my design document. Help me split the classes into scenario, measurement, localization, experiment and persistence packages, while keeping shared entities in `core` and utilities in `util`. Use `git mv` where appropriate and avoid changing application behaviour during the refactor.

### Prompt 29 — Verify the package refactor

After moving the classes, check package declarations, imports, test locations and references to the old package. Run the complete test suite and inspect the Git diff to confirm that the change is mainly a package reorganization. Help me update `docs/PROJECT-STRUCTURE.md` so I can locate files quickly during live coding.

## 10. Prompt logging and evaluation preparation

### Prompt 30 — Record the development interaction

Help me update `prompts/PROMPT-LOG.md` for this development step. Record the actual prompt, the issue I was trying to solve, what the AI suggested, what I actually changed, how I verified it and the related commit if there is one. Keep original prompts unchanged and put improved versions separately. Dont invent test results or say I checked something if I did not.

### Prompt 31 — Practise a live coding task

I have my CS5013 mid-evaluation soon and the TA may ask me to make a small modification in the existing code. Give me a task based on one of my actual modules, such as trajectory generation, measurement matching, geometry validation or evaluation. Dont show the solution immediately. Let me explain my approach first, then point out missing edge cases and ask how I would test it.

### Prompt 32 — Understand AI-generated code before using it

I received this code from an AI assistant and want to make sure I understand it before using it. Explain the purpose of each important method and the reasoning behind the implementation. Point out assumptions, edge cases and parts that need independent verification. Then ask me a few questions to check whether I have understood the code.

### Prompt 33 — Final project review

Review my radar simulator against the project proposal and design document. Make a table with each requirement, relevant source files, current implementation status and available test evidence. Separate completed features from partial and pending work. Give me the five most important things I should fix or be prepared to explain during the mid-evaluation.

## Verification checklist for each real AI-assisted change

- [ ] I understand the problem and can explain it in my own words.
- [ ] I checked the relevant source code and design requirements.
- [ ] I reviewed the proposed diff before accepting the change.
- [ ] I ran the focused test and recorded the real result.
- [ ] I ran the full test suite when feasible.
- [ ] I updated the prompt log with what actually happened.
- [ ] I did not describe a planned or suggested change as completed.
