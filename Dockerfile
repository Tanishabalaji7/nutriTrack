# ─── NutriTracker AI ── Render‑Ready Multi‑Stage Dockerfile
# Build stage using Maven
FROM maven:3.9.9-eclipse-temurin-21-jammy AS builder

WORKDIR /app

# Copy pom and source code
COPY pom.xml .
COPY backend/src ./backend/src
COPY backend/lib ./backend/lib
COPY frontend ./frontend

# Build the fat JAR (includes dependencies)
RUN mvn -B clean package

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/target/nutritrack-backend-1.0.0-jar-with-dependencies.jar app.jar

# Copy static frontend assets
COPY --from=builder /app/frontend ./frontend

# Create writable data directory for SQLite
RUN mkdir -p backend/data && chmod 777 backend/data

ENV PORT=8080
ENV GEMINI_API_KEY=""

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
  CMD curl -f http://localhost:${PORT}/ || exit 1

CMD ["sh","-c","java -jar app.jar"]
