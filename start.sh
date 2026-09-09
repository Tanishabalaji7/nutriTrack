#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "Building Java NutriTracker..."
javac -d backend/bin -cp "backend/lib/*" $(find backend/src -name "*.java")

if [ $? -eq 0 ]; then
    echo "Starting NutriTracker Server..."
    java -cp "backend/bin:backend/lib/*" com.nutritracker.server.NutritionTrackerServer
else
    echo "Compilation failed."
    exit 1
fi
