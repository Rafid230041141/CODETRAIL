# CodeTrail Phase 0 Design

This document fixes the architecture used by Phases 1 and 2. The implemented scope is intentionally limited to
Languages and DSA / Competitive Programming. Later topic families and Phase 3 analytics are outside this boundary.

## High-level architecture

```text
JavaFX desktop client
  FXML views and Spring-managed controllers
            |
            | Java HttpClient, JSON, Bearer JWT
            v
Spring Boot REST server
  Spring Security + services + Spring Data JPA
            |
            | Hibernate/JDBC
            v
PostgreSQL
```

`application.Main` starts JavaFX. During JavaFX initialization, the application starts the Spring Boot server on an
available loopback port and gives that address to `ApiClient`. The client still performs real HTTP requests; it does
not call repositories or services directly. `application.backend.BackendApplication` can also run as a standalone
server on port 8080.

### Login request flow

1. `AppController` asks `ApiClient` to send `POST /api/auth/login`.
2. `AuthController` validates the request and delegates to `AuthService`.
3. `UserAccountRepository` loads the account from PostgreSQL through JPA.
4. BCrypt verifies the password and `JwtTokenService` issues an eight-hour HS256 token.
5. The JavaFX client keeps the token in memory and adds it to later requests as `Authorization: Bearer ...`.
6. Spring Security validates the token and enforces `STUDENT` or `ADMIN` access before a protected controller runs.

## Database schema

| Entity | Important fields | Relationships and constraints |
| --- | --- | --- |
| `UserAccount` | username, display name, password hash, role, created time | unique username; role is `STUDENT` or `ADMIN` |
| `Topic` | slug, title, description, position, published | unique slug; owns modules |
| `CourseModule` | title, description, position | belongs to one topic; owns submodules |
| `Submodule` | title, position | belongs to one module; owns lessons |
| `Lesson` | slug, title, summary, Markdown body, example, position, published | unique slug; belongs to one submodule |
| `Simulation` | type, JSON configuration | optional one-to-one lesson simulation |
| `Enrollment` | enrolled time | unique user/topic pair |
| `LessonProgress` | completed, last viewed, completed time | unique user/lesson pair |
| `QuizQuestion` | prompt, four options, correct index, explanation | ordered questions belonging to a lesson |
| `QuizAttempt` | score, total, completion time | belongs to one user and one lesson |

JPA creates and updates the schema. `CurriculumSeeder` inserts the two Phase 2 trees, lesson bodies, simulations,
quizzes, and demo accounts entirely from Java when the topic table is empty.

## REST API contract

All paths use the `/api` prefix. JSON errors contain `timestamp`, `status`, `error`, `message`, and `path`.

| Method and path | Access | Request | Response |
| --- | --- | --- | --- |
| `POST /auth/register` | Public | username, displayName, password, role | token, role, user identity |
| `POST /auth/login` | Public | username, password | token, role, user identity |
| `GET /topics` | Authenticated | none | published topic summaries |
| `GET /topics/{id}/tree` | Authenticated | none | modules, submodules, lessons, completion flags |
| `GET /lessons/{id}` | Authenticated | none | Markdown body, example, quiz count, simulation |
| `POST /enrollments/{topicId}` | Authenticated | empty JSON | enrollment state |
| `GET /progress/me` | Authenticated | none | overall and per-topic progress |
| `PUT /progress/lessons/{id}` | Authenticated | completed boolean | updated progress summary |
| `GET /quizzes/{lessonId}` | Authenticated | none | questions and options without answers |
| `POST /quizzes/{lessonId}/attempts` | Authenticated | answer indexes | score and per-question feedback |
| `GET /admin/users` | Admin | none | users with completion and quiz totals |
| `GET/POST/PUT/DELETE /admin/content/topics` | Admin | filters or topic fields | topic list or mutation state |
| `GET/POST/PUT/DELETE /admin/content/modules` | Admin | topic filter or module fields | module list or mutation state |
| `GET/POST/PUT/DELETE /admin/content/submodules` | Admin | module filter or submodule fields | submodule list or mutation state |
| `GET/POST/PUT/DELETE /admin/content/lessons` | Admin | submodule filter or lesson fields | lesson list or mutation state |

## JavaFX wireframes

```text
LOGIN / REGISTER
+------------------------------------------------------+
| CodeTrail                                  Dark mode |
|------------------------------------------------------|
| Welcome back                                         |
| [username]                                           |
| [password]                                           |
| [Sign in]                                            |
| [New here? Create account]                           |
+------------------------------------------------------+

STUDENT
+--------------------------------------------------------------+
| CodeTrail  STUDENT                      user  theme  sign out |
+----------------------+---------------------------------------+
| Learning dashboard   | Topic cards / progress                 |
| Curriculum Tree      | or                                    |
|  Languages           | Lesson title                           |
|  DSA / CP             | Markdown, example, completion, quiz    |
|                      | Shared interactive simulation          |
+----------------------+---------------------------------------+

ADMIN
+--------------------------------------------------------------+
| CodeTrail  ADMIN                        user  theme  sign out |
|--------------------------------------------------------------|
| Student progress                            [Refresh]         |
| Username | Display name | Role | Complete | Quiz attempts    |
+--------------------------------------------------------------+
```

## Simulation architecture decision

Phase 2 uses one shared `SimulationView.fxml` and `SimulationController`. `SimulationEngine` converts each algorithm
configuration into immutable steps containing a message, highlighted pseudocode line, and visual cells. This keeps
playback, Previous, Next, Reset, styling, and dark mode consistent while allowing algorithm-specific state builders.

Implemented families are recursion, Merge/Quick sorting, binary search, binary search on answer, BFS, DFS, segment
tree, Fenwick tree, DSU, heap, Dijkstra, Prim, and Kruskal.
