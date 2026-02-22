# HoneyMCP Quickstart

This is a short, practical guide for getting HoneyMCP running. Choose one of the two paths below.

## Path A: Build and Run From Source

### Prerequisites
- Java 24+
- OpenAI API key
- Maven 3.9+ (wrapper included)

### Build
```bash
./mvnw clean package
```

### Run
```bash
export OPENAI_KEY=your-api-key
export DASHBOARD_ACCESS_TOKEN=your-secure-token
# Optional: persist data across restarts
# export DATABASE_PATH=/path/to/data

./mvnw spring-boot:run
```

If you do not set `DASHBOARD_ACCESS_TOKEN`, the application will generate one at startup and print it to the console.

Dashboard URL:
```
http://localhost:8989/dashboard?token=<TOKEN>
```

### Run on Port 80 (Source)
Port 80 is a privileged port on most systems.

Option A: set the app to listen on port 80 (may require elevated privileges):
```bash
export SERVER_PORT=80
./mvnw spring-boot:run
```

Option B: keep the app on 8989 and use a reverse proxy to expose port 80.

## Path B: Run the Container

### Pull and Run (On Port 80)
```bash
docker run --rm -p 80:8989 \
  -e OPENAI_KEY=your-api-key \
  -e DASHBOARD_ACCESS_TOKEN=your-secure-token \
  lemuridaelabs/honeymcp
```

### Optional: Persistent Storage
```bash
docker run --rm -p 80:8989 \
  -e OPENAI_KEY=your-api-key \
  -e DASHBOARD_ACCESS_TOKEN=your-secure-token \
  -e DATABASE_PATH=/app/db \
  -v /host/data:/app/db \
  lemuridaelabs/honeymcp
```

If you omit `DASHBOARD_ACCESS_TOKEN`, the container will generate one and print it to logs.

## Notes
- This is a honeypot. Deploy in a network segment isolated from production services.
- The service listens on `8989` by default.

## Advanced: OpenTelemetry (OTLP) Logging

HoneyMCP supports optional OTLP logging/metrics export via the `otlp` Spring profile.
Recommended: enable the profile and set the OTLP environment variables directly.

### Enable the OTLP Profile
Set the active profile to include `otlp`:
```bash
export SPRING_PROFILES_ACTIVE=otlp
```

### Configure Endpoints and Headers
These environment variables are used by the `otlp` profile:
- `OTLP_LOGS_ENDPOINT`
- `OTLP_LOGS_HEADERS`
- `OTLP_METRICS_ENDPOINT`
- `OTLP_METRICS_HEADERS`

### Enable Logs and Metrics Export
You can explicitly enable or disable OTLP export with:
- `OTLP_LOGS_ENABLED=true|false`
- `OTLP_METRICS_ENABLED=true|false`

### Local Honeycomb Example (Optional)
For internal development, you can create `src/main/resources/application-local.yml`
and set:
- `HONEYCOMB_TEAM`

Then enable both profiles:
```bash
export SPRING_PROFILES_ACTIVE=otlp,local
```

The `application-local.yml` file is git-ignored by default.
