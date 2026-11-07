# Team Management — Project Kickstart Plan

> **Status (updated 2026-11-06)**
>
> - **Phase 0 — reference inspection: DONE** → see `BOOTSTRAP-REPORT.md`.
> - **Phase 1 — bootstrap: DONE** → repo `github.com/efthymiosD/team-management`
>   (public, branch-protected), Spock harness 20/20 green, Flyway V1,
>   `identity` module + shared kernel, CI, Dockerfile, compose, Keycloak dev realm.
> - **Shared library: RELEASED** → the plan's originally-copied shared kernel
>   was extracted into `github.com/efthymiosD/decoupledx-common` **v0.1.0**
>   (GitHub Packages; tag-triggered publish; ErrorCode became a contract that
>   each app implements). The Reservation System consumes it too — PR #74 there.
> - **Pipeline hardening: DONE** → GPR credentials (`packages: read`) in CI,
>   Dockerfile GPR BuildKit secret mount + `apt-get upgrade` for the Trivy gate;
>   the reference repo's Release pipeline is green end to end (verify → build →
>   Trivy → GHCR promote).
> - **Decision changes from the original plan**:
>   photo/logo uploads are IN MVP scope (ADR-0002); §13's no-shared-library
>   recommendation was superseded once both apps carried identical seams.
> - **Next: Phase 2 — team and player management.**

---

## 1. Purpose and scope

Build a new, independently deployable **Team Management** application for teams that organize football matches and other team-based games.

The application is a separate product for a different target client than the existing Reservation System. It should use the same established technology stack, architectural decision records (ADRs), engineering rules, and development conventions where they remain applicable.

The initial objective is a production-minded MVP with team and player management, match participation voting, balanced team proposals, results, player rankings, and a simplified Man of the Match (MVP) election.

**Out of scope for the initial MVP:**
- Commercial plans, subscriptions, feature entitlements, and paid-addon packaging.
- Runtime integration with the Reservation System.
- Microservices or a separately deployed voting service.
- Redis unless measured requirements justify it.
- Goals, assists, and advanced player-performance metrics beyond the initial statistics.
- Browser E2E tests.
- Fully automated video/computer-vision player statistics.

## 2. Architectural decisions

### 2.1 Application architecture

- Build an **independent Spring Boot modular monolith** using **Spring Modulith**.
- Apply pragmatic **Domain-Driven Design (DDD)** and **Hexagonal Architecture**, following the Reservation System's existing ADRs and rules.
- Start with one Maven project, one deployable application, and one PostgreSQL database.
- Use explicit Spring Modulith module APIs and events. Keep module dependencies intentional and verify boundaries with architecture tests.
- Keep domain models and domain logic independent of Spring and persistence frameworks wherever practical.
- Use application services as inbound use cases, thin controllers, and outbound ports where meaningful infrastructure boundaries exist. Avoid speculative interfaces and unnecessary abstraction layers.
- Do not split modules into microservices or separate deployable applications just because they are separate business capabilities.

### 2.2 Relationship with the Reservation System

The new application has its own:
- Repository and release lifecycle.
- PostgreSQL database and migrations.
- Configuration and secrets.
- CI/CD pipeline and container image.
- Application runtime and deployment.

Do not share the Reservation System database, repositories, internal entities, or domain model. There is no runtime integration with it in the MVP.

Treat the Reservation System as an **architectural reference**, not a runtime dependency. Before implementation, inspect its ADRs, build configuration, security conventions, module boundaries, testing patterns, Docker workflow, and deployment conventions.

Reuse established conventions where appropriate. Extract shared code only when it is stable, well understood, and has a real second consumer. Avoid creating a large generic `common` library. Potential future candidates include framework-light utilities, reusable test support, stable error contracts, and narrowly scoped infrastructure configuration. Keep reservation-specific rules and team/match-specific domain models separate.

### 2.3 Technology stack

- Java and Spring Boot versions aligned with the Reservation System's ADRs/build configuration.
- Spring Modulith.
- PostgreSQL as the authoritative store.
- Spring Data JPA and the existing migration convention (verify the existing project before selecting or copying the migration tool).
- Existing identity-provider and security conventions, adapted to the new product's needs.
- Same frontend approach, responsive design conventions, Docker conventions, and operational standards as the Reservation System where applicable.
- No Redis in the initial MVP.
- No microservices in the initial MVP.

Avoid changing Java/Spring versions just to start the new project; first establish the versions already used by the reference project and verify compatibility.

## 3. Testing strategy — Spock for every test layer

**Spock is the sole test-authoring framework for this project.** Test specifications should be written in Groovy using Spock, including domain unit tests, module integration tests, API integration tests, and architecture tests where practical.

Supporting libraries and frameworks remain allowed:
- `spock-core` and `spock-spring`.
- Spring Boot Test / Spring TestContext.
- Spring Modulith testing and module verification support.
- Testcontainers for PostgreSQL.
- MockMvc for HTTP/API integration tests.
- Spring Security Test for authorization testing.
- ArchUnit only if it adds value beyond Spring Modulith's verification.

Spring Boot and its testing ecosystem can be used with Spock through `spock-spring` and JUnit Platform integration. Pin compatible versions of Spring Boot, Spock, Groovy, and the JDK. Prove the setup with a minimal Spring context test and a PostgreSQL Testcontainers test before building out the domain.

### Test principles

1. **Domain unit tests:** fast, no Spring context, database, or infrastructure. Test rich domain models and business invariants.
2. **Module integration tests:** black-box first. Exercise a module through its public application/API boundary and use real Spring-managed components and PostgreSQL where relevant. Do not instantiate internal entities or repositories merely to arrange a test.
3. **HTTP/API integration tests:** use MockMvc and the real application context; verify status codes, authorization, persistence, and observable effects.
4. **Architecture tests:** verify module boundaries and allowed dependencies.
5. **Testcontainers:** use real PostgreSQL for migrations, constraints, transactions, and query behavior.
6. **No browser E2E tests initially.**
7. Use mocks/stubs selectively for true external boundaries or focused unit tests, not to mock away the module under test.

## 4. Proposed module boundaries

Start with a small number of cohesive Spring Modulith modules. These are logical modules within the same application, not separate Maven projects or microservices.

### `team`
Owns:
- Team creation and team details.
- Optional team logo.
- One or more team administrators.
- Player profiles.
- Team membership lifecycle.
- Self-registration and admin-managed registration.
- Profile claiming/account linking.

### `match`
Owns:
- Match lifecycle and association to a team.
- Match time and relevant game metadata.
- Eligible participants and participation/availability.
- Match completion and final participant snapshot.
- Match history.

A future integration can associate a match with a Reservation System booking through a documented external identifier/API, but this is out of scope for the MVP.

### `teamassignment`
Owns:
- Eligible-player input to team splitting.
- Proposed assignment of players to two sides.
- Balancing score and assignment algorithm.
- Historical pairing/side-assignment penalties.
- Manual adjustments and final assignment record.

Keep the balancing algorithm independently testable and as framework-independent as possible.

### `competition`
Owns:
- Confirmed match results.
- Win/draw/loss outcome and points.
- Player statistics and standings.
- Ranking calculations/read models.

### `voting`
A reusable, match-agnostic voting capability. Owns:
- Voting session lifecycle.
- Eligible voter and candidate sets (or references to immutable eligibility snapshots).
- Ballot submission, change/withdrawal policy, and duplicate prevention.
- Opening/closing timestamps and tallying.
- Voting visibility/privacy configuration.
- Quorum and tie policy where required.
- States such as `DRAFT`, `OPEN`, `CLOSED`, and `CANCELLED`.

The generic voting module must not contain football-specific rules.

### `matchrecognition`
Owns the Man of the Match (MVP) workflow:
- Eligibility rules for MVP candidates.
- Triggering the MVP vote after match statistics are finalized.
- Match-specific voting configuration.
- Recording and exposing the final recognition result.

It uses the generic `voting` module rather than implementing its own ballots.

### Supporting modules
- `identity`: account integration and authorization conventions, adapted from the reference application. Keep domain entities independent of JWT/identity-provider implementation types.
- `notification`: email/reminders where required by the MVP. Avoid overbuilding notification channels.

These are proposed boundaries. During bootstrap, validate them against existing ADRs and keep the initial implementation no more complex than necessary. For example, `teamassignment` may begin as a cohesive package within `match` if that fits the existing module conventions, while preserving a clean boundary for the algorithm.

## 5. Core domain model and identity

Do not equate a customer/authentication account with a player.

### Concepts

- **Account:** identity that can authenticate.
- **Player Profile:** person who participates in games; may initially exist without an account.
- **Team:** group that organizes matches.
- **Team Membership:** relationship between team and player, including role, status, and membership lifecycle.
- **Match:** a specific game.
- **Participation:** a player's opt-in/availability and final participation record for a match.
- **Team Assignment:** the side to which a player is assigned for a particular match.
- **Match Result:** confirmed score/outcome for the match.
- **Player Statistics / Ranking:** derived from confirmed results and, later, additional match events.
- **Voting Session / Ballot:** generic election concepts, separate from match-specific policy.

### Initial player profile fields

- Name.
- Contact details.
- Nationality.
- Optional photo.
- Optional linked account.
- Relevant timestamps/status as required by the existing conventions.

### Team and membership rules

- A team has a name and optionally a logo.
- A team can have multiple administrators.
- A team administrator must be an existing authenticated account linked to a player/account profile as appropriate.
- A player can register themselves to a team.
- A team administrator can register a player.
- Only team administrators can remove a team member.
- Define authorization for promoting/demoting admins and prevent accidental removal of the last administrator.
- Membership removal must not erase historical match participation, results, or rankings.
- Add database constraints and application validation for important invariants.

Use internal domain identifiers rather than leaking Keycloak/JWT claim types into domain models.

## 6. Functional requirements

### 6.1 Team management

- Create and update a team.
- Set team name and optional logo.
- Assign multiple team administrators.
- Register a player to a team either by self-registration or admin action.
- Support player profiles without accounts and a safe account-claiming/linking workflow.
- Allow removal only by team administrators.
- Enforce authorization on the server, not just in the UI.

### 6.2 Match creation and participation voting

- Announce/create a match for a team.
- A new match opens a participation vote visible to team members.
- Members can opt in and reverse their vote while voting is open.
- Default voting deadline: three days before match start, configurable at an appropriate application/team level.
- Use exact timestamps, not implicit midnight boundaries.
- Define behavior for matches created inside the default voting window (for example, open voting immediately and close at the match start, or require explicit handling).
- Persist the final participant snapshot when the match is locked/finalized.
- Prevent unauthorized or duplicate participation ballots and define what happens when membership changes during an open vote.

### 6.3 Team splitting

- Propose two dynamically formed sides from eligible opt-in players.
- Aim to balance the sides using ranking position and available performance data, including win-rate estimates.
- Encourage different teammate/opponent combinations across games.
- Permit an authorized user to review and adjust the proposal before it becomes final.
- Persist both the proposal and final assignment, including enough input/output information to audit or reproduce decisions.

Suggested initial algorithm:
1. Obtain eligible players.
2. Estimate player strength using ranking and available history.
3. Generate candidate splits.
4. Score candidates by side-strength imbalance.
5. Penalize repeated pairings and repeated side assignments.
6. Choose a good candidate and expose it as a proposal.
7. Allow manual changes and preserve the final outcome.

Do not use raw win rate as the only skill measure. Small sample sizes can produce misleading estimates, and win rate depends on teammates/opponents. Consider minimum sample thresholds or a smoothed estimate. A stored random seed may help reproduce and debug a proposal. Do not introduce machine learning for the MVP.

### 6.4 Match results and rankings

Initial points:
- Win: 3 points.
- Draw: 1 point.
- Loss: 0 points.

Record at least:
- Games played.
- Wins, draws, losses.
- Points.
- Win rate.

Rules:
- Only confirmed results update standings.
- Preserve the match result and the participant/team-assignment snapshot.
- Define handling of abandoned, postponed, cancelled, or disputed matches.
- Avoid making a mutable points counter the sole source of truth; derive rankings from confirmed results or maintain a rebuildable database projection.
- Keep competitive ranking (achievement) distinct from estimated player strength used for team balancing.
- Goals, assists, and other detailed statistics are deferred, but the model should not make their future addition unnecessarily difficult.

### 6.5 Man of the Match / MVP voting

For the MVP, use a simple vote instead of an advanced contribution-scoring algorithm.

Workflow:
1. An authorized scorer/admin submits match statistics.
2. Statistics are finalized.
3. The system creates an MVP voting session for that match.
4. Eligible voters select one eligible player.
5. Voting closes and votes are tallied.
6. The result is stored and exposed.

Requirements:
- Candidate eligibility should initially use the final participant list, not the presence of detailed stats.
- Vote creation must be idempotent: retries or duplicate events must not create duplicate MVP elections for the same match.
- Define voter eligibility, voting duration/deadline, whether voters can change their ballot, visibility/privacy, and tie behavior.
- A tie policy must be explicit: tied result, runoff, or another configured rule.

The MVP election should be independent of the match's 3/1/0 competition points.

### 6.6 Automated player statistics — future investigation

For the initial release, statistics can be entered by an authorized scorer/admin and finalized before MVP voting begins. Later collection options can include:
- Player/admin input with post-match confirmation.
- A live scoring interface with timestamps and corrections.
- Integration with an external scoring platform.
- Video/computer vision or wearables if the sport, hardware, budget, and privacy constraints justify them.

Do not make fully automated video-based contribution measurement a prerequisite for the MVP. Reliable player tracking and event attribution are materially harder than team balancing.

## 7. Redis decision

**Do not introduce Redis in the initial MVP.**

PostgreSQL is sufficient for the expected initial ranking workload. Use SQL queries or a database-backed, rebuildable ranking projection as needed.

Possible evolution:
1. Derive standings from confirmed match results.
2. Add indexes and optimize based on query plans.
3. Add a PostgreSQL ranking projection if calculations become expensive.
4. Add Redis as a disposable cache only if measurements show a real performance need.

Redis must not become the authoritative source of ranking points or match results. If introduced later, include it in local/deployment infrastructure and integration tests using Testcontainers as appropriate.

## 8. Optional commercial packaging and future integrations

Commercial packaging and integration with the Reservation System are deferred.

The architecture should not prevent future evolution, but avoid implementing speculative subscription abstractions now. When needed, feature entitlements should be enforced server-side and distinguished from user permissions.

Potential future integration mechanisms:
- Versioned REST APIs for independent applications.
- Explicit public module APIs for in-process consumers.
- Events for in-process workflows.
- If independent services later require reliable asynchronous delivery, consider an outbox and message broker.

Do not create remote calls between modules in the same application. Do not extract microservices until there is a concrete need such as independent deployment, a second consumer, scaling requirements, or a clear operational/team boundary.

## 9. Security, privacy, and operational considerations

- Follow the existing Reservation System's authentication, authorization, secret management, CORS, audit, and deployment ADRs where applicable.
- Keep account-provider identifiers at the identity boundary.
- Scope access to team and player data correctly.
- Enforce team-admin permissions server-side.
- Decide how contact details and optional photos are protected and who can view them.
- Define photo upload/storage constraints before implementing uploads.
- Audit sensitive admin actions and match-result corrections where consistent with existing conventions.
- Keep secrets out of source control and container images.
- Apply database migrations in CI and test them against PostgreSQL.
- Ensure voting closure and match-finalization workflows are safe under retries and concurrent requests.

## 10. Suggested repository structure

Adapt package names and resource conventions to the Reservation System's existing ADRs.

```text
team-management/
├── docs/
│   ├── adr/
│   ├── BOOTSTRAP-REPORT.md
│   └── PROJECT-PLAN.md          (this file)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── .../
│   │   │       ├── team/
│   │   │       ├── match/
│   │   │       ├── teamassignment/
│   │   │       ├── competition/
│   │   │       ├── voting/
│   │   │       ├── matchrecognition/
│   │   │       ├── identity/
│   │   │       └── notification/
│   │   └── resources/
│   └── test/
│       ├── groovy/
│       └── resources/
├── Dockerfile
├── compose.yaml
├── pom.xml
└── README.md
```

This is an illustrative layout, not a requirement to create one Maven submodule per business module. Keep the application as one Maven project unless the existing build conventions give a clear reason otherwise.

## 11. Kickstart roadmap

### Phase 0 — inspect the reference project
- Review Reservation System ADRs and coding-agent instructions.
- Record Java, Spring Boot, Spring Modulith, database, migration, identity, and build-tool versions.
- Inspect module naming, package structure, error handling, API conventions, auditing, security, logging, and Docker/CI practices.
- Identify proven code worth extracting versus conventions that should simply be copied.
- Confirm frontend approach and local development/deployment conventions.
- Do not assume a shared library is required.

### Phase 1 — bootstrap
- Create the new repository and baseline documentation.
- Initialize Spring Boot + Spring Modulith with compatible versions.
- Configure PostgreSQL, migrations, security foundations, health checks, configuration and container workflow.
- Configure Spock/Groovy and `spock-spring`.
- Add a minimal Spock Spring context test and a PostgreSQL Testcontainers test.
- Verify Spring Modulith module detection and architecture verification.
- Ensure the project builds and tests in CI before adding domain features.

### Phase 2 — team and player management
- Implement teams, player profiles, memberships, multiple admins, self-registration, admin registration, and profile/account linking.
- Add authorization and persistence constraints.
- Add domain unit tests and black-box module/API integration tests.

### Phase 3 — matches and participation voting
- Implement match lifecycle, eligible members, opt-in/reversible voting, deadlines, and final participant snapshots.
- Add idempotency and concurrency protections.
- Test deadline and membership-change edge cases.

### Phase 4 — balanced team proposals
- Implement a pure/testable balancing algorithm.
- Use ranking/history inputs, balance scoring, and pairing penalties.
- Persist proposals and final assignments.
- Add deterministic/data-driven Spock tests for balance quality and edge cases.

### Phase 5 — results, ranking, and MVP election
- Record and confirm match results.
- Calculate points and basic player statistics.
- Finalize match statistics and trigger exactly one MVP vote.
- Implement vote tallying and explicit tie behavior.
- Test retries, duplicate ballots, corrections, and result finalization.

### Phase 6 — hardening and MVP readiness
- Verify module boundaries and all supported API permissions.
- Test migrations, database constraints, transactions, and key workflows with PostgreSQL Testcontainers.
- Verify logs, health checks, configuration, secrets, and container startup.
- Document local setup, test commands, deployment, and known limitations.
- Defer browser E2E tests, Redis, commercial packaging, and external Reservation System integration.

## 12. Definition of done for the initial MVP

- The application builds and runs independently.
- All test specifications are authored in Spock.
- Domain rules have fast unit tests.
- Core workflows have black-box Spring/module integration tests and HTTP integration tests.
- PostgreSQL migrations and constraints are tested against a real PostgreSQL instance via Testcontainers.
- Spring Modulith verifies the intended module boundaries.
- Teams support multiple administrators and player profiles with or without linked accounts.
- Team admins alone can remove members, without deleting historical records.
- Members can opt in/out before the configured participation deadline.
- Eligible players can be split into balanced proposed sides, with final assignments preserved.
- Confirmed results produce the 3/1/0 points and initial player statistics.
- Finalized match statistics trigger one MVP election, with explicit voting and tie rules.
- Redis, browser E2E tests, subscription packaging, and Reservation System integration are not required.
- Setup, configuration, testing, and deployment instructions are documented.

## 13. First task for the coding agent (consumed 2026-10-28)

**Do not implement all business features immediately.** First inspect the Reservation System repository and its ADRs, then produce a short bootstrap report containing:

1. Architectural and engineering rules to carry over.
2. Exact Java/Spring/Modulith versions and compatible Spock/Groovy dependencies.
3. Proposed module dependency diagram and any conflicts with existing ADRs.
4. Shared abstractions worth extracting now, with justification; otherwise explicitly recommend no extraction yet.
5. Bootstrap implementation steps and test strategy.
6. Open domain decisions that block implementation.

After reviewing that report, bootstrap the application and prove the build, Spock/Spring integration, PostgreSQL Testcontainers, and module verification work before implementing the business modules.

> Status: §13 was fully executed on 2026-10-28 — `BOOTSTRAP-REPORT.md` + the
> bootstrap (Phase 1) followed it, plus the decisions recorded in `docs/adr/`.
