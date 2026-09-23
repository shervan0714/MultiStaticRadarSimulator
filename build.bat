@echo off
REM Simple build script for Multi-Static Radar Simulator
REM Uses javac (included with Java) to compile all files

setlocal enabledelayedexpansion

REM Create output directories
if not exist build mkdir build
if not exist build\classes mkdir build\classes
if not exist build\test-classes mkdir build\test-classes

REM Set classpath
set CLASSPATH=build\classes

echo.
echo ========================================
echo Building Multi-Static Radar Simulator
echo ========================================
echo.

REM Compile main source files
echo Compiling main source files...
javac -d build\classes ^
    -sourcepath src\main\java ^
    src\main\java\com\radar\simulator\RadarSimulatorApp.java ^
    src\main\java\com\radar\simulator\core\RadarPhysics.java ^
    src\main\java\com\radar\simulator\core\Transmitter.java ^
    src\main\java\com\radar\simulator\core\Receiver.java ^
    src\main\java\com\radar\simulator\core\Drone.java ^
    src\main\java\com\radar\simulator\ui\SimulatorController.java ^
    src\main\java\com\radar\simulator\ui\Visualization3D.java ^
    src\main\java\com\radar\simulator\ui\ParameterPanel.java ^
    src\main\java\com\radar\simulator\util\Vector3D.java

if !errorlevel! neq 0 (
    echo ERROR: Compilation failed!
    exit /b 1
)

echo.
echo ✓ Build completed successfully!
echo.
echo To run unit tests, you need JUnit installed.
echo For now, the main classes are compiled in build\classes
echo.
