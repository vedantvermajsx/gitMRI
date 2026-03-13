# 🔬 RepoIntel — GitHub Repository Intelligence Engine

A full-stack static analysis platform for Java codebases. Clone any public GitHub
repository and get a deep engineering intelligence report: complexity hotspots,
dead code, bus factor, dependency graph, and a composite health score.

---

## Architecture

```
React Frontend (Vite + Tailwind + Recharts)
        │  REST API
Spring Boot Backend (Java 17)
        │
  ┌─────┴──────┐
  │  Pipeline  │
  │ ─────────  │
  │ RepoCloner (JGit)
  │ CodeParser (JavaParser)
  │ MetricsEngine (dead code)
  │ GitAnalyzer (hotspots, bus factor)
  │ DependencyAnalyzer (Maven/Gradle)
  │ ReportGenerator (health score)
  └─────────────┘
        │
    PostgreSQL
```

---

## Quick Start (Docker)

```bash
# Clone or download this repo
git clone <this-repo>
cd repo-intel

# Start all services
docker-compose up --build

# Open the dashboard
open http://localhost:3000
```

---

## Local Development

### Backend

**Prerequisites:** Java 17+, Maven 3.9+

```bash
cd backend

# Run with H2 in-memory database (no PostgreSQL needed)
mvn spring-boot:run

# Backend available at http://localhost:8080
# H2 console at http://localhost:8080/h2-console
```

To use PostgreSQL instead, edit `application.properties`:
```properties
# Comment out H2 lines, uncomment PostgreSQL lines
spring.datasource.url=jdbc:postgresql://localhost:5432/repointel
spring.datasource.username=repointel
spring.datasource.password=repointel
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.h2.console.enabled=false
```

### Frontend

**Prerequisites:** Node.js 18+

```bash
cd frontend
npm install
npm run dev

# Frontend available at http://localhost:3000
# API calls proxied to :8080 via Vite
```

---

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/analyze` | Start analysis `{ "repoUrl": "..." }` |
| GET | `/api/jobs/{jobId}/status` | Poll job progress |
| GET | `/api/jobs` | List all jobs |
| DELETE | `/api/jobs/{jobId}` | Delete a job |
| GET | `/api/reports/{jobId}` | Full report summary |
| GET | `/api/reports/{jobId}/complexity` | Method complexity list |
| GET | `/api/reports/{jobId}/deadcode` | Dead code findings |
| GET | `/api/reports/{jobId}/hotspots` | Git commit hotspots |
| GET | `/api/reports/{jobId}/contributors` | Contributor stats |
| GET | `/api/reports/{jobId}/dependencies` | Dependency graph |

---

## Health Score Formula

```
score = 100
  - penalty(% methods with CC > 10)     × 25    max -25
  - penalty(dead code ratio)             × 40    max -20
  - penalty(bus factor < 3)                      max -20
  - penalty(high hotspot count / files)  × 40    max -20
  - penalty(dependency count > 25/50)            max -15
```

---

## Metrics Explained

### Cyclomatic Complexity (CC)
McCabe's metric counting decision branches: `if`, `for`, `while`, `case`, `catch`, `&&`, `||`, ternary.
- 1–6: Simple ✅
- 7–11: Moderate ⚠️
- 12–19: Complex 🔶
- 20+: High Risk 🔴

### Dead Code
Conservative call-graph BFS from Spring/JUnit entry points.
- **HIGH**: Entire class unreferenced
- **MEDIUM**: Non-public unreachable method
- **LOW**: Public method not called in graph

### Hotspot Score
`normalize(commit_count) × normalize(avg_complexity)`
Files scoring > 0.7 are high-risk change targets.

### Bus Factor
Minimum number of contributors whose commits total ≥ 50%.
- ≤ 1: Critical 🔴
- 2: High Risk 🔶
- ≥ 3: Acceptable ✅

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, Vite, Tailwind CSS, Recharts |
| Backend | Spring Boot 3.2, Java 17 |
| Code Analysis | JavaParser 3.25 |
| Git Analysis | JGit 6.7 |
| Database | PostgreSQL / H2 (dev) |
| Migrations | Flyway |
| Container | Docker, Docker Compose |

---

## Project Structure

```
repo-intel/
├── backend/
│   ├── src/main/java/com/repointel/
│   │   ├── api/controller/          # REST controllers
│   │   │   ├── AnalysisController.java
│   │   │   └── ReportController.java
│   │   ├── api/dto/                 # Request/response DTOs
│   │   ├── config/                  # CORS, async config
│   │   ├── exception/               # Global error handling
│   │   ├── model/                   # JPA entities
│   │   ├── repository/              # Spring Data repos
│   │   └── service/                 # Business logic
│   │       ├── AnalysisOrchestrator.java  ← pipeline coordinator
│   │       ├── RepoCloner.java
│   │       ├── CodeParser.java
│   │       ├── MetricsEngine.java
│   │       ├── GitAnalyzer.java
│   │       ├── DependencyAnalyzer.java
│   │       ├── ReportGenerator.java
│   │       └── visitor/             # JavaParser AST visitors
│   └── src/main/resources/
│       ├── application.properties
│       └── db/migration/V1__initial_schema.sql
└── frontend/
    └── src/
        ├── api/client.js
        ├── pages/
        │   ├── Home.jsx
        │   ├── Report.jsx
        │   └── History.jsx
        └── components/
            ├── OverviewTab.jsx
            ├── ComplexityTab.jsx
            ├── DeadCodeTab.jsx
            ├── ContributorsTab.jsx
            └── DependenciesTab.jsx
```
