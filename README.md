# CodeTrail Learning Platform

CodeTrail is a Java 21 desktop learning platform built with JavaFX, FXML, Spring Boot, Spring Security, Spring Data
JPA, and PostgreSQL. The JavaFX client communicates with its Spring Boot backend over HTTP using JWT authentication.
No external API or internet connection is required after Maven dependencies and the PostgreSQL image are available.

This repository implements the project plan through **Phase 2**:

- Student and Admin registration/login with BCrypt passwords, JWT, and role-based authorization
- Full Languages tree for Python, Java, JavaScript, C#, C++, and C
- Full DSA / Competitive Programming tree
- 133 original seeded lessons rendered from Markdown
- Completion, enrollment, quiz attempts, and per-topic progress stored in PostgreSQL
- Visualization-first simulations for all 54 DSA lessons, including arrays, trees, graphs, sorting, searching,
  range queries, dynamic programming, strings, mathematics, and geometry
- Editable, validated inputs for every simulation family, with Apply Inputs and Restore Example actions
- An editable weighted-graph lab for Dijkstra, Prim, and Kruskal with pseudocode playback and edge-state tracing
- Admin user table showing each student's saved completion and quiz activity
- Light and dark themes

Later topic families, certificates, streaks, and advanced analytics are intentionally deferred.

## Requirements

- Java 21
- Maven 3.6.3 or newer
- An existing PostgreSQL 16-compatible server, or Docker with Compose
- A graphical desktop with GTK 3 when running Linux

Spring Boot 3.5.16 is compatible with Java 21. Maven selects the correct JavaFX native libraries for Linux, macOS,
or Windows; the repository contains no user-specific SDK path.

## Run

Use an existing PostgreSQL server by creating the local database and account once:

```sql
CREATE USER codetrail WITH PASSWORD 'codetrail';
CREATE DATABASE codetrail OWNER codetrail;
```

For example, open `psql` as your PostgreSQL administrator, run those two statements, then leave the server running.
Docker is only an optional shortcut:

```bash
docker compose up -d postgres
```

Then launch the complete desktop application:

```bash
mvn clean javafx:run
```

`application.Main` starts Spring Boot on an available loopback port and then opens JavaFX. The client discovers that
port automatically and all application data still travels through the REST API.

Demo accounts created by the Java seeder:

```text
Student: student / student123
Admin:   admin / admin123
```

## Eclipse

1. Import the repository with **File > Import > Maven > Existing Maven Projects**.
2. Start the PostgreSQL server configured above. Docker is not required.
3. Open `src/application/Main.java`.
4. Choose **Run As > Java Application**.

No JavaFX VM arguments are required. The included Maven classpath and Java 21 launch metadata are platform-neutral.

## Configuration

The defaults match `docker-compose.yml`. Override them with environment variables when needed:

| Variable | Default |
| --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/codetrail` |
| `DATABASE_USER` | `codetrail` |
| `DATABASE_PASSWORD` | `codetrail` |
| `JWT_SECRET` | local development key; replace before deployment |
| `SERVER_PORT` | `8080` for standalone backend runs |

The desktop launcher intentionally overrides the backend port with an available local port. See `.env.example` and
[`docs/phase-0-design.md`](docs/phase-0-design.md) for the schema, API contract, wireframes, and request flow.

## Standalone backend

The backend can be started without JavaFX for API development:

```bash
mvn spring-boot:run \
  -Dspring-boot.run.main-class=application.backend.BackendApplication
```

It listens on `http://localhost:8080` unless `SERVER_PORT` is set.

## Verification

Compile all production and verification sources:

```bash
mvn clean test
```

Run the end-to-end Spring Boot API checks. It uses H2 in PostgreSQL compatibility mode locally and a real PostgreSQL
service in GitHub Actions:

```bash
mvn exec:java \
  -Dexec.mainClass=application.VerificationHarness \
  -Dexec.classpathScope=test
```

Run all simulation engine checks:

```bash
mvn exec:java \
  -Dexec.mainClass=application.AlgorithmVerificationHarness \
  -Dexec.classpathScope=test
```

Run the lesson-by-lesson semantic checks. This verifies the real invariant for each of the 54 DSA lessons, including
exact graph results, data-structure shape, range aggregates, string matches, and final algorithm state:

```bash
mvn exec:java \
  -Dexec.mainClass=application.DsaSemanticVerificationHarness \
  -Dexec.classpathScope=test
```

Run the JavaFX network-flow checks on Linux through Xvfb:

```bash
xvfb-run --auto-servernum mvn exec:java \
  -Dexec.mainClass=application.UiVerificationHarness \
  -Dexec.classpathScope=test
```

GitHub Actions repeats the build, PostgreSQL/JWT API flow, simulation checks, and JavaFX/FXML checks on Ubuntu.
