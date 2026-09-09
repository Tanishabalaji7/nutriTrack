# ─── NutriTracker AI — Render-Ready Dockerfile ───────────────────────────────
# Render automatically sets the PORT env variable.
# SQLite data is stored in /app/backend/data (use Render Disk for persistence).
# ─────────────────────────────────────────────────────────────────────────────

FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy only what's needed for compilation
COPY backend/src ./backend/src
COPY backend/lib ./backend/lib

# Compile all Java source files
RUN mkdir -p backend/bin && \
    javac -d backend/bin -cp "backend/lib/*" \
    $(find backend/src -name "*.java")

# ─── Runtime Stage ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy compiled classes and libs from builder
COPY --from=builder /app/backend/bin ./backend/bin
COPY --from=builder /app/backend/lib ./backend/lib

# Copy frontend static files
COPY frontend ./frontend

# Create data directory for SQLite database
RUN mkdir -p backend/data && chmod 777 backend/data

# Render sets PORT automatically — default to 8080 for local dev
ENV PORT=8080
ENV GEMINI_API_KEY=""

# Expose the port
EXPOSE 8080

# Health check so Render knows the app is ready
HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
    CMD curl -f http://localhost:${PORT}/ || exit 1

# Start the server — reads PORT from environment
CMD ["sh", "-c", "java -cp backend/bin:backend/lib/* com.nutritracker.server.NutritionTrackerServer"]
