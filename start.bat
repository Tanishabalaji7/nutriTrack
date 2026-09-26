@echo off
REM ==============================================================================
REM NutriTrack AI — Startup Script (Windows)
REM ==============================================================================

echo ====================================================
echo   NutriTrack AI Setup ^& Server (Windows)
echo ====================================================

REM Check if Java and Javac are installed
where javac >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] 'javac' compiler was not found in PATH.
    echo Please install OpenJDK 21 or higher and add it to your system PATH.
    pause
    exit /b 1
)

where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] 'java' runtime was not found in PATH.
    echo Please install OpenJDK 21 or higher and add it to your system PATH.
    pause
    exit /b 1
)

REM Ensure directories exist
if not exist "backend\bin" mkdir "backend\bin"
if not exist "backend\data" mkdir "backend\data"

echo [*] Compiling Java backend classes...
javac -d backend/bin -cp "backend/lib/*" backend/src/com/nutritracker/database/*.java backend/src/com/nutritracker/model/*.java backend/src/com/nutritracker/dao/*.java backend/src/com/nutritracker/service/*.java backend/src/com/nutritracker/controller/*.java backend/src/com/nutritracker/server/*.java

if %ERRORLEVEL% equ 0 (
    echo [OK] Compilation successful!
    echo [*] Launching NutriTrack AI Server on http://localhost:8080 ...
    echo ====================================================
    java -cp "backend/bin;backend/lib/*" com.nutritracker.server.NutritionTrackerServer
) else (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)
