#!/usr/bin/env bash
# ==============================================================================
# NutriTrack AI — Startup Script (macOS / Linux)
# ==============================================================================

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "===================================================="
echo "  🥗 Starting NutriTrack AI Setup & Server..."
echo "===================================================="

# Verify Java is installed
if ! command -v javac &> /dev/null || ! command -v java &> /dev/null; then
    echo "❌ Error: Java (JDK 21 or higher) is required but not found in PATH."
    echo "Please install OpenJDK 21+ and ensure 'java' and 'javac' are available."
    exit 1
fi

# Ensure required directories exist
mkdir -p backend/bin backend/data

# Compile Java backend
echo "📦 Compiling Java backend classes..."
javac -d backend/bin -cp "backend/lib/*" $(find backend/src -name "*.java")

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo "🚀 Launching NutriTrack AI Server on http://localhost:8080 ..."
    echo "===================================================="
    java -cp "backend/bin:backend/lib/*" com.nutritracker.server.NutritionTrackerServer
else
    echo "❌ Compilation failed."
    exit 1
fi
