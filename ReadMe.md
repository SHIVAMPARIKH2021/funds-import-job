# Spring Batch Application

A Spring batch application designed with environment-agnostic packaging. Configuration is externalized across profile-specific YAML files, with secrets and profiles supplied at runtime via command-line arguments or environment variables.

---

## Prerequisites

* **Java:** OpenJDK 21 or later
* **Build Tool:** Gradle (wrapper included)
* **Database:** PostgreSQL (for local development and cloud runs)
* **Container Runtime:** Docker (optional, for containerized execution)

---

## Configuration Architecture

Configuration relies on standard Spring Boot profile inheritance:

* `src/main/resources/application.yaml` — Base common configuration (JPA dialect, batch metadata initialization).
* `src/main/resources/application-local.yaml` — Local PostgreSQL connection settings using environment variable overrides with defaults.
* `src/main/resources/application-dev.yaml` — Development/Cloud settings expecting injected credentials and remote endpoints.

### Key Environment Variables

| Variable | Description | Local Default |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`local`, `dev`, `prod`) | `local` |
| `TARGET_DB` | Target database / schema name (`funds`, `emfs`) | `funds` |
| `DB_USER` | PostgreSQL username | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | `postgres` |

---

## Running Locally

### 1. Using Gradle (`bootRun`)

Run with default local settings:
```bash
./gradlew bootRun --args="--spring.profiles.active=local"
```

### 2. Run with custom job parameters:
```bash
./gradlew bootRun --args="--spring.profiles.active=local run.date=2026-09-27 target=funds"
```

### 3. Pass database overrides or specific profiles using system properties:
```bash
./gradlew bootRun -Dspring.profiles.active=dev -DDB_USER=my_user -DDB_PASSWORD=my_password
```

## Building the Executable Artifact
Build the self-contained executable JAR (skipping test execution if a live database is unavailable):
```bash
./gradlew clean bootJar -x test
```
The output JAR will be generated at ```build/libs/funds-import-job-<version>.jar```

## Running the Executable JAR Directly
For the dev environment with cloud credentials:
```bash
java -Dspring.profiles.active=dev \
     -DDB_USER="cloud_db_user" \
     -DDB_PASSWORD="cloud_db_password" \
     -jar build/libs/*.jar \
     run.date=2026-09-27 target=funds
```

## Running with Docker
The application follows the Twelve-Factor App design: the Docker image is built once and contains zero environment-specific secrets. All credentials and profiles are injected at container startup.

### 1. Build the Docker Image
```bash
docker build -t spring-batch-job:latest .
```
### 2. Run the Docker Container
Local Execution (connecting to PostgreSQL on the host)
- macOS / Windows: Use host.docker.internal to point to the host machine's PostgreSQL server.
- Linux: Use --network="host" or your local bridge IP.
```bash
docker run --rm \
  -e SPRING_PROFILES_ACTIVE=local \
  -e TARGET_DB=funds \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  spring-batch-job:latest \
  run.date=2026-09-27
```

## Dev / Cloud Execution
```bash
docker run --rm \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e TARGET_DB=funds \
  -e DB_USER="dev_user" \
  -e DB_PASSWORD="dev_password" \
  spring-batch-job:latest \
  run.date=2026-09-27
```

## CI/CD Pipeline (Jenkins)
When orchestrating builds through Jenkins:

1. Build Step: Run ```./gradlew clean bootJar``` on the controller or agent.
2. Containerization: Execute ```docker build``` using the generated multi-stage ```Dockerfile```.
3. Secret Injection: Fetch credentials securely from Jenkins Credentials Manager or AWS Systems Manager (SSM) and map them directly to container environment variables (```-e DB_USER```, ```-e DB_PASSWORD```) during docker run.