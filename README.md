# HoneyMCP

HoneyMCP is a Model Context Protocol (MCP) honeypot server designed to detect, log, and alert on malicious activity
targeting AI-enabled systems. It simulates vulnerable network services with synthetic data to attract and analyze both
traditional HTTP attacks and malicious AI agent behavior.

## Table of Contents

- [Overview](#overview)
- [What HoneyMCP Simulates](#what-honeymcp-simulates)
- [Background](#background)
    - [Honeypots in Security](#honeypots-in-security)
    - [Model Context Protocol](#model-context-protocol)
- [Features](#features)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Getting Started](#getting-started)
- [Testing](#testing)
- [License](#license)

## Overview

HoneyMCP operates as a deceptive MCP server that simulates a document archive system. It presents itself as a
network-connected service with:

- **MCP endpoints**: Full implementation of MCP tools, resources, prompts, and completions
- **HTTP APIs**: REST endpoints for data access
- **Authentication forms**: Fake login pages to capture credential attempts
- **Administrative dashboard**: Protected monitoring interface for security operators

All interactions are logged with threat scoring. The system aggregates activity per source IP and generates alerts when
behavior exceeds configurable thresholds.

## What HoneyMCP Simulates

The honeypot presents a fictional document archive service with the following components:

**Archive System**: Exposes tools for browsing and searching document archives. The system intentionally reveals both "
allowed" and "disallowed" archive categories, creating an apparent security misconfiguration that attracts exploitation
attempts.

**Synthetic Documents**: Uses AI-generated content to create realistic file records with plausible names, dates, and
summaries. This provides convincing bait without exposing real data.

**Authentication Portal**: Presents login forms that capture submitted credentials while always returning authentication
failures, encouraging repeated attempts.

**API Services**: Exposes REST endpoints that appear to provide data access, logging all interactions for analysis.

## Background

### Honeypots in Security

A honeypot is a security mechanism that creates a decoy system or service designed to appear as a legitimate target to
attackers. Unlike production systems that try to keep attackers out, honeypots are intentionally exposed to detect,
deflect, and study unauthorized access attempts.

Honeypots serve several purposes in security operations:

- **Detection**: Identify intrusion attempts that might otherwise go unnoticed
- **Intelligence gathering**: Collect information about attacker techniques, tools, and behaviors
- **Diversion**: Draw attackers away from legitimate production systems
- **Research**: Study emerging attack patterns and threat actor methodologies

For more information on honeypot concepts and deployment strategies, see:

- [NIST Special Publication 800-83: Guide to Malware Incident Prevention and Handling](https://csrc.nist.gov/publications/detail/sp/800-83/rev-1/final)
- [The Honeynet Project](https://www.honeynet.org/)

### Model Context Protocol

The Model Context Protocol (MCP) is an open standard that enables AI assistants to interact with external tools, data
sources, and services. MCP provides a structured way for AI systems to:

- Execute tools and functions
- Access resources and data
- Respond to prompts with contextual information
- Provide autocompletion suggestions

As AI systems become more prevalent in enterprise environments, MCP endpoints present a new attack surface. Malicious
actors may attempt to exploit MCP services to:

- Extract sensitive data through crafted tool calls
- Bypass access controls by manipulating AI assistants
- Probe systems for vulnerabilities via automated agents
- Exfiltrate information disguised as legitimate AI interactions

For the official MCP specification and implementation guides, see:

- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [MCP GitHub Organization](https://github.com/modelcontextprotocol)

## Features

### MCP Implementation

- **Tools**: Archive summary and search operations with severity-weighted logging
- **Resources**: File access endpoints that track data retrieval attempts
- **Prompts**: Template prompts that guide AI assistant behavior
- **Completions**: Autocomplete suggestions for archive names

### Threat Detection

- **Pattern Matching**: Regex-based detection of path traversal, deserialization attacks, session fixation, and other
  OWASP categories
- **Event Scoring**: Configurable severity weights for different activity types
- **Alert Generation**: Time-window aggregation with threshold-based alerting
- **IP Tracking**: Per-source activity correlation and cooldown management

### Monitoring

- **Event Logging**: Comprehensive audit trail of all interactions (excludes `/favicon.ico` and `/static/` paths)
- **Dashboard**: Web interface for reviewing events and alerts
- **Push Notifications**: Web Push alerts for real-time security notifications
- **Request Capture**: Full request and response body logging for forensic analysis

### Security Design

- **Token-Protected Dashboard**: Constant-time comparison prevents timing attacks
- **Obscured Resources**: Invalid dashboard access returns 404 to hide existence
- **Proxy Support**: Handles X-Forwarded-For and Cloudflare headers for accurate IP attribution

## Architecture

HoneyMCP is built with Spring Boot and organized into functional modules:

```
honeymcp/
├── config/                 # Application configuration
├── filters/                # Request/response caching
├── interceptors/           # Request interception and detection
└── modules/
    ├── alerts/             # Alert detection and management
    ├── archives/           # MCP tools and synthetic data
    ├── auth/               # Fake authentication endpoints
    ├── dashboard/          # Administrative monitoring
    ├── events/             # Event logging and search
    └── notifications/      # Web Push notification delivery
```

### Technology Stack

- Java 24
- Spring Boot 3.5
- Spring AI with OpenAI integration
- Spring Data JDBC
- HSQLDB embedded database
- Caffeine caching
- Thymeleaf templating

## Configuration

HoneyMCP is configured through environment variables:

### Server Settings

| Variable          | Default               | Description                    |
|-------------------|-----------------------|--------------------------------|
| `SERVER_PORT`     | 8989                  | HTTP listener port             |
| `SERVER_BASEURL`  | http://localhost:8989 | Base URL for generated links   |
| `SERVER_FILE_URL` | /archive/files/       | Path prefix for file downloads |

### Database Settings

| Variable        | Default | Description                                    |
|-----------------|---------|------------------------------------------------|
| `DATABASE_PATH` | (none)  | Directory path for persistent database storage |

By default, HoneyMCP uses an in-memory HSQLDB database. Data is lost when the application stops.

To enable persistent storage, set `DATABASE_PATH` to a directory where the database files should be stored. The
application creates database files named `honeymcp.*` in that directory. If the database already exists, the application
uses the existing data without recreating tables.

For container deployments, mount a volume to the database path:

```bash
docker run -v /host/data:/app/db -e DATABASE_PATH=/app/db honeymcp
```

### Dashboard Settings

| Variable                 | Default     | Description                               |
|--------------------------|-------------|-------------------------------------------|
| `DASHBOARD_ACCESS_TOKEN` | (generated) | Access token for dashboard authentication |

The `DASHBOARD_ACCESS_TOKEN` controls access to the `/dashboard` endpoint where events and alerts are viewed. If not
explicitly set, the application generates a random UUID token at startup and logs it to the console. You must retrieve
this token from the application logs to access the dashboard.

For production deployments, specify the token explicitly to avoid needing to retrieve it from logs on each restart.

### Alert Thresholds

| Variable                     | Default | Description                                    |
|------------------------------|---------|------------------------------------------------|
| `ALERTS_DELAY_MINS`          | 15      | Cooldown period between alerts for the same IP |
| `ALERTS_CHECK_MINS`          | 15      | Time window for score aggregation              |
| `ALERTS_THRESHOLD_FLAGGED`   | 125     | Score threshold for FLAGGED alerts             |
| `ALERTS_THRESHOLD_MALICIOUS` | 175     | Score threshold for MALICIOUS alerts           |

### Event Scoring

| Variable               | Default | Description                                            |
|------------------------|---------|--------------------------------------------------------|
| `EVENTS_WEIGHT_MINOR`  | 10      | Score for minor events (e.g., missing resources, 404s) |
| `EVENTS_WEIGHT_LOW`    | 25      | Score for low-severity events                          |
| `EVENTS_WEIGHT_MEDIUM` | 50      | Score for medium-severity events                       |
| `EVENTS_WEIGHT_HIGH`   | 75      | Score for high-severity events                         |

### AI Integration

| Variable       | Default    | Description                                  |
|----------------|------------|----------------------------------------------|
| `OPENAI_KEY`   | (required) | OpenAI API key for synthetic data generation |
| `OPENAI_MODEL` | gpt-4o     | Model used for content generation            |

## Getting Started

For a short setup guide with both source and container paths, see [QUICKSTART](QUICKSTART.md).

### Prerequisites

- Java 24 or later
- OpenAI API key
- Maven 3.9 or later (wrapper included)

### Deployment Considerations

For production deployment:

1. **Network Isolation**: Deploy the honeypot in an isolated network segment. Do not expose it alongside production
   systems.

2. **TLS Termination**: Configure HTTPS through a reverse proxy or load balancer.

3. **Log Aggregation**: Export event data to a SIEM or log management system for long-term retention and analysis.

4. **Monitoring**: Configure alerting through push notifications or integrate with existing incident response workflows.

### Building

```bash
./mvnw clean package
```

### Running

```bash
export OPENAI_KEY=your-api-key
export DASHBOARD_ACCESS_TOKEN=your-secure-token
./mvnw spring-boot:run
```

Or with a compiled JAR:

```bash
export OPENAI_KEY=your-api-key
export DASHBOARD_ACCESS_TOKEN=your-secure-token
java -jar target/honeymcp-*.jar
```

If you omit `DASHBOARD_ACCESS_TOKEN`, the application generates a random UUID and logs it to the console at startup.
Check the logs for a line containing the generated token to access the dashboard.

For persistent data retention, set both the dashboard token and database path:

```bash
export OPENAI_KEY=your-api-key
export DASHBOARD_ACCESS_TOKEN=your-secure-token
export DATABASE_PATH=/path/to/data
./mvnw spring-boot:run
```

This ensures events, alerts, and push notification subscriptions persist across restarts, and your dashboard URL remains
stable.

### Container Build and Deployment

For security reasons, you should consider running the application in a container. The application is built around
Spring Boot and supports the container build process natively. To build a container image simply run:

```bash
./mvnw spring-boot:build-image
```

This will generate a new docker container image tagged "honeymcp:1.0.0".

Note that this container is built on the Lemuridae Labs base image, which is a slim and secured Java runtime. You
can use the default buildpack base images or create your own custom image using the jar file created in the 
standard build process.

If running in a container, be sure to set the mandatory environment variables such as OPENAPI_KEY and 
we recommend the DASHBOARD_ACCESS_TOKEN as well. Also if you want to retain your data between restarts, set the 
DATABASE_PATH environment variable. Be sure to mount the database path to a persistent volume.

### Accessing the Dashboard

The dashboard is available at `/dashboard` and requires the access token as a query parameter:

```
http://localhost:8989/dashboard?token=<TOKEN>
```

Use the token you specified in `DASHBOARD_ACCESS_TOKEN`, or check the application logs for the auto-generated token if
you did not set one. The token is required for all `/dashboard` endpoints including the events and alerts APIs.

## Testing

### MCP Inspector

The MCP Inspector provides an interactive interface for testing MCP endpoints. Run it with Docker:

```bash
docker run --rm --network host -p 6274:6274 -p 6277:6277 \
  ghcr.io/modelcontextprotocol/inspector:latest
```

Connect the inspector to `http://localhost:8989/mcp` to interact with the honeypot's MCP implementation.

### Manual Testing

Test the fake login endpoint:

```bash
curl -X POST http://localhost:8989/auth/login \
  -d "username=admin&password=password123"
```

Test the archive API (requires dashboard token):

```bash
curl "http://localhost:8989/dashboard/events?token=<TOKEN>"
```

## License

See [LICENSE](LICENSE) for details.
