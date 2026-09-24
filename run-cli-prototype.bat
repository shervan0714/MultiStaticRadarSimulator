@echo off
setlocal enabledelayedexpansion

echo ==================================================
echo Building Multi-Static Radar Simulator CLI Prototype
echo ==================================================

if not exist build mkdir build
if not exist build\classes mkdir build\classes

echo Compiling Java sources...
javac -d build\classes -sourcepath src\main\java src\main\java\com\radar\simulator\CLISimulatorApp.java

if !errorlevel! neq 0 (
    echo ERROR: Compilation failed!
    exit /b 1
)

echo.
echo ==================================================
echo Running Prototype Simulation...
echo ==================================================
echo.

java -cp build\classes com.radar.simulator.CLISimulatorApp

echo.
pause
