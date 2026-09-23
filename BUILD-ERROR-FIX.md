# ⚠️ BUILD ERROR FIX

**Problem**: `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain`

**Cause**: The Gradle wrapper JAR file is missing. The wrapper properties file exists but the actual JAR wasn't downloaded.

---

## 🔧 SOLUTIONS (Try in Order)

### **Solution 1: Use Simple Build Script** ✅ (RECOMMENDED - No Dependencies)

```bash
cd c:\Users\sherv\Downloads\paiproject\MultiStaticRadarSimulator
build.bat
```

This uses `javac` (included with Java) to compile all source files without external tools.

**Result**: All `.class` files compiled to `build\classes\`

---

### **Solution 2: Install Gradle Manually**

**Option A: Chocolatey (Windows Package Manager)**
```powershell
# Run as Administrator
choco install gradle
```

Then run:
```bash
gradle build
gradle test
gradle run
```

**Option B: Download Gradle Directly**
1. Visit: https://gradle.org/releases/
2. Download Gradle 8.5 (binary-only)
3. Extract to `C:\gradle` or similar
4. Add to PATH environment variable
5. Run: `gradle build`

**Option C: Download Gradle Wrapper JAR**
1. Go to: https://services.gradle.org/distributions/gradle-8.5-bin.zip
2. Download and extract
3. Copy `gradle-8.5\lib\gradle-8.5-wrapper.jar` to `gradle\wrapper\`
4. Run: `.\gradlew.bat build`

---

### **Solution 3: Use Docker (If Available)**

```bash
docker run --rm -v "c:\Users\sherv\Downloads\paiproject\MultiStaticRadarSimulator:/app" -w /app gradle:8.5 gradle build
```

---

## ✅ WHAT WORKS RIGHT NOW

### **Compilation Only**
```bash
build.bat          # Uses javac directly - works NOW ✅
```

### **Unit Tests**
JUnit tests need classpath setup. For now:
1. Use Solution 1 or 2 above to get Gradle/Maven
2. Then run: `gradle test`

### **Running Simulator**
Requires JavaFX setup which needs build system. Once you install Gradle:
```bash
gradle run
```

---

## 📋 QUICK FIX CHECKLIST

- [ ] Try `build.bat` first (no dependencies)
- [ ] If tests needed: Install Gradle via Chocolatey
- [ ] If still issues: Download Gradle manually
- [ ] For UI: Need JavaFX libraries (Gradle handles this)

---

## 🎯 BEST PATH FORWARD

**Immediate** (Next 5 minutes):
```bash
build.bat          # Compile everything
```

**For Testing** (Install Gradle):
```bash
choco install gradle    # Windows Package Manager
gradle build            # Full build
gradle test             # Run all 18 unit tests
```

**For Running UI** (After Gradle installed):
```bash
gradle run              # Launches simulator
```

---

## 💡 WHY THIS HAPPENED

Gradle wrapper is designed so developers don't need Gradle installed. But the wrapper itself (the JAR file) must be committed to git, which we didn't do initially. 

**Solution**: We added `gradle\wrapper\gradle-wrapper.properties` but not the actual `gradle\wrapper\gradle-wrapper.jar` file (binary, ~9MB).

---

## ✨ WORKAROUND FOR SEP 11 SUBMISSION

For your design doc submission (no code execution needed):
- ✅ You don't need to run the simulator
- ✅ You don't need to run tests
- ✅ Just submit DESIGN.md as PDF

The compilation/testing is for Oct 9 mid-demo, not Sep 11 deadline.

---

**Next Step**: Try this now:
```bash
cd c:\Users\sherv\Downloads\paiproject\MultiStaticRadarSimulator
build.bat
```

**If you see "Build completed successfully" - you're good!** ✅

---

For help: See this file after you try above steps.
