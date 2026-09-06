# 📋 CS5013 Submission Checklist

**Project**: Multi-Static Radar Simulator  
**Student**: [Your Name]  
**Roll No**: [Your Roll]  
**Submission Deadline**: September 11, 2026 (5 days away!)  

---

## ✅ Pre-Submission Checklist (Due Sep 11)

### Documentation
- [ ] **DESIGN.md** - Architecture, modules, tests, milestones (COMPLETE - ready to submit)
- [ ] **README.md** - Project overview, build, run instructions (COMPLETE)
- [ ] **WEEKLY-LOG.md** - Week 1 filled with activities (TODO - fill this week)
- [ ] **Stakeholder acknowledgement email** - from Girdhar sir (TODO - get from prof)

### GitHub Repository
- [ ] Create repository: `MultiStaticRadarSimulator`
- [ ] Add remote: `git remote add origin https://github.com/USERNAME/MultiStaticRadarSimulator`
- [ ] Initial commit: All 18 files
- [ ] Verify branch: `main`
- [ ] Add link to README/DESIGN

### Code Verification
- [ ] [ ] `./gradlew build` runs without errors
- [ ] Unit tests pass: `./gradlew test` (18 tests should run)
- [ ] All 9 skeleton classes compile
- [ ] No missing imports or syntax errors

### Personal Information
- [ ] [ ] Update README.md with your name, roll, email
- [ ] Update DESIGN.md with your name and roll
- [ ] Update WEEKLY-LOG.md with your name and roll
- [ ] Verify all three files have your details

### PDF Submission File
- [ ] Export/print DESIGN.md to PDF (use Google Docs or browser print)
- [ ] Attach Girdhar sir's stakeholder acknowledgement email as appendix
- [ ] Save as: `Proposal_[YourName]_[YourRoll].pdf`
- [ ] File size check (should be < 5MB)

---

## 📦 What's Ready to Submit

| Item | File | Status |
|------|------|--------|
| Architecture Document | DESIGN.md | ✅ COMPLETE (5 pages) |
| Project Overview | README.md | ✅ COMPLETE |
| Build System | build.gradle | ✅ COMPLETE |
| Source Code | 9 files in src/main | ✅ COMPLETE |
| Unit Tests | 4 files in src/test | ✅ COMPLETE (18 tests) |
| Git Ignore | .gitignore | ✅ COMPLETE |
| Setup Guide | GITHUB-SETUP.md | ✅ COMPLETE |

---

## 🚨 Critical: Don't Forget!

1. **Stakeholder Acknowledgement Email**
   - Email Girdhar sir: "I have selected your multi-static radar project for CS5013. I'm building a simulator for formula validation. Would you be willing to be my stakeholder? I'll need a brief email confirmation."
   - Attach his response to the PDF

2. **GitHub URL**
   - Once you create the repo, share the link in your README
   - Instructor will check git history at mid-demo

3. **Weekly Log Entry**
   - Fill WEEKLY-LOG.md Week 1 section:
     - What you did (design, setup, created 9 classes, 18 tests)
     - Hours spent (~8 hours)
     - Next week's plan
     - Any blockers

---

## 🎬 Quick Start (Step-by-Step)

### Step 1: Prepare Your Files (Today)
```bash
# Navigate to project
cd c:\Users\sherv\Downloads\paiproject\MultiStaticRadarSimulator

# Fill in your name
# Edit: README.md (line 23)
# Edit: DESIGN.md (line 3)
# Edit: WEEKLY-LOG.md (line 3)
```

### Step 2: Get Stakeholder Email (This Week)
- Email or meet Girdhar sir
- Request formal acknowledgement
- Save his reply email

### Step 3: Test Build (This Week)
```bash
cd c:\Users\sherv\Downloads\paiproject\MultiStaticRadarSimulator
gradlew.bat build
gradlew.bat test
```

### Step 4: Create GitHub Repo (This Week)
- Go to https://github.com/new
- Follow steps in GITHUB-SETUP.md
- Push your code

### Step 5: Create PDF (By Sep 10)
- Copy DESIGN.md content
- Paste into Google Docs or Word
- Export as PDF
- Attach stakeholder email as appendix
- Save as: `CS5013_Design_[YourName]_[YourRoll].pdf`

### Step 6: Submit (By Sep 11, 23:59 IST)
- Upload PDF to course submission portal
- Include GitHub repo link in comments

---

## 📝 Current Files Status

### Ready ✅
```
✓ build.gradle
✓ README.md
✓ DESIGN.md
✓ WEEKLY-LOG.md
✓ GITHUB-SETUP.md
✓ .gitignore
✓ RadarSimulatorApp.java
✓ Vector3D.java
✓ Transmitter.java
✓ Receiver.java
✓ Drone.java
✓ RadarPhysics.java
✓ SimulatorController.java
✓ Visualization3D.java
✓ ParameterPanel.java
✓ Vector3DTest.java
✓ TransmitterTest.java
✓ DroneTest.java
✓ RadarPhysicsTest.java

TOTAL: 19 files ready
```

### Needs Your Input ⏳
```
? Stakeholder acknowledgement email (from Girdhar sir)
? Your name in all docs
? Your roll number in all docs
? Your email address
? GitHub username/URL
? WEEKLY-LOG.md Week 1 completion
```

---

## 🛠️ Build Commands Quick Reference

```bash
# Clean build
./gradlew clean build

# Run tests only
./gradlew test

# Run specific test
./gradlew test --tests RadarPhysicsTest

# Run application (when complete)
./gradlew run

# View dependencies
./gradlew dependencies

# Generate IDE files
./gradlew idea  # for IntelliJ
```

---

## 📞 Need Help?

### Common Issues

**Q: Gradle build fails with "JavaFX not found"**
- A: Edit build.gradle, ensure JavaFX dependencies match your Java version (currently 21.0.1)

**Q: Tests won't run**
- A: Ensure JUnit dependency is in build.gradle (it is), then `./gradlew test --info`

**Q: Can't push to GitHub**
- A: Check remote: `git remote -v` should show https://github.com/username/repo.git

**Q: Missing stakeholder email**
- A: The project won't be accepted without it. Email Girdhar sir immediately.

---

## 🎯 Success Criteria for Sep 11 Submission

✅ **Realism of scope** - Multi-module simulator fits one semester  
✅ **Quality prior-work** - DESIGN.md lists alternatives (Rust radar, Python sim, etc.)  
✅ **Stakeholder acknowledgement** - Email from Girdhar sir attached  
✅ **Verification plan** - Error comparison: actual vs. calculated  
✅ **Milestone plan** - Detailed in DESIGN.md (design → mid-demo → final)  

---

## 📅 Timeline Reminder

| Date | Deadline | What's Due |
|------|----------|-----------|
| Sep 11 | TODAY (5 days) | Design Doc + GitHub Setup |
| Sep 12 | - | Receive feedback from instructor |
| Oct 9 | Mid-Demo | Working simulator + git history |
| Nov 6 | Final Demo | Production-ready simulator |

---

## 💪 You're All Set!

Everything is scaffolded. Your job now:
1. Add your details
2. Get stakeholder email
3. Test build
4. Push to GitHub
5. Submit PDF + link

**Total time to Sep 11**: ~3-4 hours of administrative work  
**You're 90% done already!**

---

**Document Generated**: September 6, 2026  
**Last Updated**: September 6, 2026  
**Ready**: YES ✅
