# Team Management

Independent, production-minded MVP application for teams that organize football
matches and other team-based games: team and player management, match
participation voting, balanced team proposals, results, player rankings, and a
simplified Man of the Match election.

Architectural sibling of the [Reservation System] — same technology stack,
module conventions (ADR-0001…0004 semantics), and engineering rules, but a
separate product, repository, database, and deployment.

## Stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 26 |
| Framework | Spring Boot 4.1.1, Spring Modulith 2.1.1 |
| Architecture | Modular monolith — DDD + Hexagonal (see `docs/adr/`) |
| Database | PostgreSQL 17 + Flyway |
| Identity | Keycloak (realm `team-management`), OAuth2 login + JWT resource server |
| Tests | **Spock 2.5** (Groovy 5.1.3) for every layer, Testcontainers PostgreSQL |
| Build | Maven 3.9 (`./mvnw`), Spotless |

## Quick start

```bash
source ~/tools/env.sh          # JDK 26 + Maven 3.9.16
./mvnw verify                  # unit + integration tests (integration need Docker)
./mvnw test -Dtest=ModularitySpec           # module boundaries
./mvnw test -Dtest="!*IntegrationSpec,!ApplicationContextSmokeSpec,!PostgresMigrationSpec"  # unit only
./mvnw spotless:apply          # ALWAYS after editing Java (imports+format)

docker compose up -d           # postgres + keycloak (realm auto-imported, admin/admin)
./mvnw spring-boot:run         # app on :8080, swagger at /swagger-ui.html
```

Integration tests use Testcontainers and require a running Docker daemon
(Rancher Desktop locally; see the Reservation System README for the socket
setup). They never require a running Keycloak — mock JWTs are used instead.

## Module map

```
com.decoupledx.teammanagement
├── shared/          shared kernel (error contract, events, tx port, security) — no business logic
└── identity/        account provisioning, current-account resolution, directory (bootstrap scope)
```

Planned modules (bootstrap report §3): `team`, `match`, `teamassignment`,
`competition`, `voting`, `matchrecognition`, `notification`, plus a `webui`.

## Documentation

- `docs/BOOTSTRAP-REPORT.md` — Phase 0 findings, version matrix, module plan.
- `docs/adr/` — architecture decision records.
- `AGENTS.md` — conventions and quirks for coding agents (read before changes).

[Reservation System]: https://github.com/efthymiosD/reservation-system

## Repos

- This application: https://github.com/efthymiosD/team-management
- Shared library (`decoupledx-common`): https://github.com/efthymiosD/decoupledx-common — CI resolves `com.decoupledx:decoupledx-common` from GitHub Packages.
