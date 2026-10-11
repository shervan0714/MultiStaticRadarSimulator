# Prompt Log

I use an AI coding assistant (Claude Code, running inside this repository) to help build the simulator. This log keeps the prompts I gave it under version control next to the code they produced, so anyone can see *what I asked*, *what came back*, *how I checked it*, and *which commit it led to*.

**How I use this log**

- Prompts are copied **exactly as I typed them**, typos included, so the log is an honest record.
- *Outcome* is what the assistant actually did. *Checked* is how the result was verified before I accepted it.
- *Better version* is how I would write the same prompt now, after seeing what went wrong or what was unclear. These are rewrites for learning, not prompts I sent.
- New entries are added in the same commit as the change they produced, or in the next one.

Tool: Claude Code (Opus). For the mid-evaluation, only Gemini Pro is allowed, so the same principles will be applied there.

---

## P1 — Understand the project before changing anything
**Date:** 7 Oct 2026 · **Commits:** none (analysis only)

> see my design doc and proposal and understand my project first and then see to how much extent my project is done and then plan what to do divide them into commits

**Goal:** get an honest status check against my own design doc before the mid-demo, instead of jumping into code.
**Outcome:** the assistant read the proposal, design doc, course guide and all source files, built the project and ran the tests and the CLI. It found that the localization was **80–115 m off on noiseless data**, that the tests still passed because they only checked the error was a finite number, that `gradlew` failed on my JDK 26, and that receiver IDs were really list indices. It then proposed 21 commits, ordered by priority.
**Checked:** I compared the status table against my design doc's module list and test plan, and ran the CLI myself to see the error numbers.
**What I learned:** "all tests pass" meant nothing here — the tests could not fail. Asking for understanding *first* exposed this.

**Better version:**
> Read `cs5103.pdf` (proposal) and `desgin_doc.pdf` (Phase 3 design) and the code in `MultiStaticRadarSimulator/`. For each module in design doc §1.3, tell me whether it is done, partial or missing, with evidence (file, test, or command output). Build it and run the tests and the CLI — don't trust the README. Then propose a list of small commits, most important first, with what each one changes and how we'll verify it. Don't modify any files yet.

---

## P2 — Start the work, and record what changes
**Date:** 7 Oct 2026 · **Commits:** `6e250b7`, `85fccdb`, `f19e76b`, `a5ae617`

> yes start and  write the changes if there is file like track.md or somthing whenre previous changes are noted

**Goal:** do commits 1–4 of the plan (build fix, localization solver, real tests, receiver-ID fix) and keep a change record.
**Outcome:** the assistant found no changelog, so it used `WEEKLY-LOG.md` and added one entry per commit. It upgraded the Gradle wrapper, replaced the gradient-descent localizer with a Levenberg–Marquardt least-squares solver, tightened tests to a 1 mm tolerance, and fixed receiver matching and time stamps.
**Checked:** test count went 76 → 90, all passing; CLI error went from ~100 m to 0.0000 m for 3, 4 and 5 receivers. One new test failed on the first try (a target at ground level was expected to be "unstable"); probing showed the solver was right and the *test assumption* was wrong, so the test was corrected, not the code.
**What I learned:** a failing test is information, not something to silence — first decide whether the code or the expectation is wrong.

**Better version:**
> Do commits 1–4 from the plan, one at a time. After each: run `./gradlew test` and the CLI, show me the numbers, then commit with a message that says what changed and why. Record each change as a bullet in the Week 5 section of `WEEKLY-LOG.md`. If a test fails, stop and explain whether the code or the test is wrong before fixing either.

---

## P3 — Make the commit history readable
**Date:** 7 Oct 2026 · **Commits:** pushed `mid-demo-fixes`

> push and commit the changes with good readable commit messaged

**Outcome:** everything was already committed; the branch was pushed. Commit messages follow `type(scope): summary` with a body explaining *why*.
**What I learned:** compare with my own earlier message `rewrote localization math to use gradient descent, accuracy is way better now` — it claimed an improvement that was never measured (the error was still ~100 m). Now messages state measured results.

---

## P4 — Account and authorship
**Date:** 7 Oct 2026 · **Commits:** none (configuration)

> i want you to use shervan0714 github  not karthiks

**Outcome:** the assistant checked the push credentials, remote and commit authors — all were already `shervan0714`. But the GitHub API showed my commits were **not linked to my account**, because they used the email `shervan0714@github.com`, which isn't on my account. It switched this repo to my GitHub no-reply email so new commits link to me.
**What I learned:** my prompt named a symptom without saying where I saw it, so the assistant had to investigate everything. It still found a real problem I didn't know about.

**Better version:**
> On GitHub I see "karthiks" next to my commits in the commit list (screenshot attached). Check which account the pushes and commit authors use and make sure everything in this repo is attributed to my GitHub account shervan0714.

---

## P5 — Finish the mid-demo work
**Date:** 7 Oct 2026 · **Commits:** `9c2660b`, `b369519`, `39455f1`, `c4b0567`, `64e3836`

> do all commits before mid demo and push it to main

**Outcome:** geometry validator fix, the GUI rebuilt around the experiment pipeline, a 3D view in the correct East/North/Up frame, the results table with save/load/export, and mid-demo docs with an honest gap list. Each commit was tested and pushed, then `main` was fast-forwarded.
**Checked:** 111 tests passing; GUI checked from off-screen renders (default run, bad input, two receivers, save → load → export round trip).

---

## P6 — See it running
**Date:** 7–8 Oct 2026

> i want to see the simulation can u run and show me how the application is right now

> once run the project

**Outcome:** the assistant launched the app and sent screenshots of three states (now in `docs/images/`).
**What I learned:** looking at the running app is a different check from the tests — tests cannot tell whether the 3D view looks right. (The upside-down Y axis fixed in `39455f1` was caught the same way, from a rendered screenshot.)

---

## P7 — Plan before acting (mid-evaluation guidelines)
**Date:** 8 Oct 2026 · **Commits:** none

> *(pasted the TA's mid-evaluation announcement)* do necessary work first say me what is there to do then i will say to do or not

**Outcome:** a checklist mapping each deliverable to what already exists in the repo and what is missing (e.g. no prompts were tracked in git — which is why this file exists).
**What I learned:** asking for a plan and approving it myself keeps me in control of the project and makes me read what is being proposed.

---

## P8 — Clean up the repository
**Date:** 11 Oct 2026 · **Commits:** `ef46a4f`, `1412818`

> why are they many empty folders in the project remove not needed

> format the folder and file strucutre properly if it isn not in a clean way , ,create a file and say what each file is for

**Outcome:** the empty folders were all generated Gradle output (not in git); the assistant cleaned them. It then split the 21-class `model` package into the design doc's modules using `git mv` (history kept), and wrote `docs/PROJECT-STRUCTURE.md`.
**Checked:** 111 tests still passing after the move; git recorded 27 renames, not deletions.
**What I learned:** "why" questions are worth asking — the answer (generated build output) meant nothing in git needed changing.

**Better version:**
> Reorganise the source packages to match the modules in my design doc §1.3. Use `git mv` so history is kept, change nothing except packages and imports, and run all tests before committing. Then write `docs/PROJECT-STRUCTURE.md` explaining every file, so I can find my way around during the live code task.

---

## P9 — This document
**Date:** 11 Oct 2026

> do 1+2  but right them as  a student who is understanding the project and using help of ai to do the project ,write prompts also in that way

**Outcome:** this prompt log (tracked in git) and the Principles & Practices document for the mid-evaluation.

---

## Prompt checklist I now follow

1. **Context first** — point to the design doc / files; say what the project is for.
2. **Ask for understanding or a plan before code** — "don't modify files yet".
3. **One concern per prompt** — small enough to become one commit.
4. **State how it will be checked** — tests, tolerance, command output, screenshot.
5. **Say what not to touch** — e.g. "only packages and imports".
6. **Ask "why" when I don't understand** — I must be able to explain every change in the viva.
7. **Log the prompt** here with the commit it produced.
