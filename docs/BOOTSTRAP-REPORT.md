# Team Management — Bootstrap Report (Phase 0)

Reference project inspected: `../reservation-system` (AGENTS.md, pom.xml, ADR-0001…0004,
source tree, test infra, CI/Docker conventions). Status: **for review — no code scaffolded yet.**

---

## 1. Architectural and engineering rules to carry over

### Module structure (ADR-0001, 11 rules — carry over verbatim)
- Flat, package-private `domain/`; only sub-package is `domain/port` with **public** inbound/outbound interfaces.
- One `XxxDomainConfig` (`@Configuration`, `@Bean`, no `@Service`) owns domain beans — the only Spring annotation in `domain`.
- Exactly one export per module: `adapter/api` with `@NamedInterface("api")`; API impls + config package-private.
- Adapters own their sub-package configs; **adapters never depend on adapters**; controller/adapter-in code drives `domain/port`, never `adapter.api`.
- Domain ports speak immutable snapshots (`…Info`, `…Command`, `…DataValue`); mapping lives in adapters; `…DataValue` public inside persistence only.
- Adapter classes package-private by default; **every record in its own top-level file** (ADR-0003), package-private unless cross-module.

### Identity placement (ADR-0002)
- Adapters never resolve/thread the logged-in account. Inbound ports expose **implicit "me" variants**; explicit-actor variants go through `adapter.api` for schedulers/audit/cross-module callers.
- `identity` exports `CurrentAccountApi` + `AccountDirectoryApi` (two interfaces, one `@NamedInterface("api")` package — established precedent). Sole presentation consumer: webui `CurrentUserAdvice`.

### Domain events (ADR-0004)
- Contracts in shared kernel root (`DomainEvent`, `DomainEventPublisher`, `DomainEventListener`), single platform seam `shared/event/SpringDomainEventPublisher`, composed `@OnDomainEvent` (platform annotation appears once).
- Payloads = records in the **publishing-or-reactive** module's `adapter/api` (cycle-break rule); one logic-free listener class per module in `adapter/listeners/`; reaction body = one use-case call, policy in domain services.
- Events carry the acting account explicitly (SecurityContext is gone after commit); every reaction tolerates redelivery.
- Registry: `spring-modulith-starter-jdbc`, completion-mode DELETE, republish-on-restart; `event_publication` auto-DDL is the only non-Flyway table.

### Testing (AGENTS.md conventions — now in Spock)
- Integration tests are **black-box**: MockMvc over HTTP or raw SQL at the DB boundary; no direct domain-service calls; unit tests only for domain-model logic (none for services with mocked deps).
- Concurrency tests: exactly one 201, rest 409; persistence adapters translate **both** constraint races (`DataIntegrityViolationException` 23P01, `ConcurrencyFailureException` 40P01) to 409.
- One shared PostgreSQL Testcontainers container per JVM; per-test truncate; fixed clock (`app.clock.fixed-instant`); integration tests never depend on live Keycloak (mock JWTs via `SecurityMockMvcRequestPostProcessors.jwt()`, empty issuer → dev-key fallback).
- Modulith verification spec (`ApplicationModules.of(...).verify()`) + domain-purity + record-placement arch checks.

### Build / ops / delivery
- Spotless after every Java edit; `mvn verify` as the single CI gate; PR-only flow with required check; Dependabot weekly, one PR at a time.
- Trivy-driven version pins (tomcat, Jackson 2 + Jackson 3 BOM) carried into the new pom.
- Dockerfile: multi-stage temurin, non-root `app` user, curl healthcheck, `/app/logs` owned.
- Flyway is the migration tool (Boot-managed `spring-boot-starter-flyway` + `flyway-database-postgresql`) — V1 init / V2+ feature migrations; re-importing history needs `flyway repair`.
- User-directed delivery gate (prepare in working tree, deliver only on "go-live") is assumed to govern the new repo too.
- i18n convention (message bundles + `WebMessages` facade + value-free i18n tests) — carry over, scope decided in §6.

---

## 2. Versions (verified)

| Component | Version | Source / evidence |
|---|---|---|
| Java | **26** (local: 26.0.2.1) | reference pom `java.version=26`; local JDK |
| Maven | 3.9.16 (`~/tools/env.sh`) | local |
| Spring Boot | **4.1.1** (parent) | reference pom |
| Spring Modulith | **2.1.1** | reference pom |
| Tomcat pin | 11.0.25 (CVE-2026-65182/65905/68525) | reference pom — carry |
| Jackson pins | jackson2 2.22.4, jackson-bom 3.2.3 (Trivy gate) | reference pom — carry |
| Testcontainers | **2.0.5** BOM; `postgres:17-alpine`; static start + `@DynamicPropertySource` (`@ServiceConnection` broken in TC 2.x) | reference pom + testinfra |
| Flyway | Boot-managed + `flyway-database-postgresql` | reference |
| springdoc | 3.1.1 | reference pom |
| Lombok | 1.18.48 + explicit `annotationProcessorPaths` (not implicit on JDK 25+) | reference pom |
| **Spock** | **2.5-groovy-5.0** (GA, Oct 2026 — supersedes the 2.4-M7 milestone named during Phase 0) | `spock-core`/`spock-spring` with explicit versions; supports **Groovy 5, JUnit 6, Spring Framework 7, Boot 4**. Deliberately NOT imported as `spock-bom` (it pins an older junit-bom that would downgrade Boot's managed JUnit). |
| **Groovy** | **5.1.x** — JDK 26 is the decisive constraint: Groovy 5.0 is officially tested on JDK 11–25 (5.0.8+ reads JDK 26 classes via ASM bump), **Groovy 5.1 is tested on JDK 26/27 EA**. Spock's `-groovy-5.0` variant runs on any Groovy 5.x. Pin `groovy.version` in the pom; confirm Boot 4.1.1's managed version with `dependency:list` during bootstrap and override if < 5.1. | groovy-lang.org release notes 5.0/5.1 |
| Groovy build | `gmavenplus-plugin` **5.1.0** (2026-07; Groovy 5/6, honors compiler release) | Maven Central |
| Surefire | Boot-managed (picks Spock up as a JUnit Platform engine, same exclusion-group mechanism for opt-in Keycloak tests) | reference pattern |
| ArchUnit | **no separate pin** — arrives transitively via `spring-modulith-core`/`starter-test`; confirm at bootstrap; reuse for domain-purity/record-placement specs | reference uses it the same way |

Test deps: `spock-core`, `spock-spring` + existing `spring-boot-starter-test`, `spring-boot-webmvc-test` (`@AutoConfigureMockMvc` lives there in Boot 4), `spring-security-test`, `spring-modulith-starter-test`, `spring-boot-testcontainers`, `testcontainers-postgresql`.

Known quirks carried into the new project: JDK 26 renders `ISO_LOCAL_TIME` with seconds (JSON assertions); Spock 2.4-M7 is a milestone — check for a newer 2.4.x/2.5 at bootstrap time; Spotless config covers `src/test/java` only — Groovy sources under `src/test/groovy` start unformatted (add a Spotless `groovy` section later if desired).

---

## 3. Module boundaries and dependency diagram

Logical modules in **one** Maven project / one deployable (per plan §4), mapped onto the reference layout
(`domain/`, `domain/port/`, `XxxDomainConfig`, `adapter/api|persistence|web|listeners`):

```
                          webui (Thymeleaf; consumes module APIs only)
        ┌──────────┬───────────┬──────────────┬───────────────┬────────────┐
        ▼          ▼           ▼              ▼               ▼            
      team       match    competition    teamassignment   matchrecognition   
        │          │           │           │      │             │            
        ▼          ▼           │           │      └─────────────┼──► voting   
    identity ◄─── (me)        │           ▼                    │            │
        ▲                     │      reads match (participants) │            │
        │                     │      reads competition (strength)            │
        │                     ▼                                              │
        │                 reads match (result input, final snapshot)         │
        └── voting (generic; voters = references; no football rules) ◄───────┘

  notification: consumes events only (no API dependencies)
```

Synchronous API edges: `team → identity`; `match → team, identity`; `competition → match`;
`teamassignment → match, competition`; `matchrecognition → match, voting`; `voting → identity`.

Event edges (after-commit, idempotent reactions):
- `match` : `MatchAnnounced` → notification; `MatchFinalized` → competition (result entry unlocked);
  `MatchStatisticsFinalized` → matchrecognition (creates exactly one MVP vote).
- `voting` : `VotingSessionClosed` → matchrecognition (records final recognition).
- `team` : `MemberRemoved` → match/voting reaction (open-ballot policy — see §6).

### ADR conflict check — result: no hard conflicts, three resolved tensions
1. **Cycle-break rule exercised**: `competition` depends on `match` (reads the finalized participant snapshot), so `MatchFinalized` lives in `match.adapter.api` (publishing module; reactive module already depends on it). No inversion needed. If `match` ever needs competition data, the payload moves to `competition` per ADR-0004 rule 4.
2. **"Exactly one export" vs voting's genericity**: the generic `voting` module exports its session API through the same single `adapter.api` named interface — no additional export needed; football policy stays entirely in `matchrecognition`.
3. **`teamassignment` placement**: plan allows starting as a cohesive sub-module of `match`. The balancing algorithm must be framework-independent (pure domain code), which ADR-0001 supports natively. Recommendation: keep `teamassignment` a separate module from day one (its dependency direction — needs `competition` strength — differs from `match`'s, and merging would drag `competition` into `match`'s dependency set).

---

## 4. Shared abstractions — recommendation: **no extraction yet**

The new application **copies and renames** (Customer → Account) the stable shared-kernel pattern; it does **not** import a common library:

- Copy: `BusinessException`, `ErrorCode`, `TransactionRunner` + `SpringTransactionRunner`, the ADR-0004 event contract quartet + `@OnDomainEvent` + `SpringDomainEventPublisher`, `shared/security/*` (SecurityConfig, JwtRolesConverter, AudienceJwtValidator, ValidatingJwtDecoder, JwtDecoderConfig, KeycloakLogoutSuccessHandler, PromptLoginAuthorizationRequestResolver, RealmRoleOidcUserService), `ClockConfig`, `OpenApiConfig`, `GlobalExceptionHandler`, test-infra (`PostgresIntegrationTest` → `PostgresIntegrationSpec`, `JwtSupport`, `WebUserSupport`).
- Do **not** copy: `Money`, `ReservationPeriod` (reservation-specific value types).
- SUPERSEDED 2026-11-04: a genuine second consumer now exists, so per the
  established rule the stable seams WERE extracted into the shared library
  `com.decoupledx:decoupledx-common` (repo `../decoupledx-common`); both apps
  consume it via auto-configuration. This app keeps only its own `ErrorCode`
  enum (implements the library contract) and `SecurityConfig` (routing
  policy). The initial no-extraction recommendation applied to that moment
  (one consumer per class); the decision rule itself — real second consumer
  required — stayed in force until this extraction became justified.

---

## 5. Bootstrap implementation plan and test strategy

### Steps (Phase 1)
1. Create repo `team-management/`: git init, `.gitignore`, README, `docs/adr/` (0000 meta-ADR + "carries over reservation-system ADR-0001…0004" note), `AGENTS.md` adapted from the reference (delivery gate, checklist, conventions, quirks).
2. `pom.xml`: Boot 4.1.1 parent, all §2 pins, plus `spock-bom` import, `spock-core`/`spock-spring`, `groovy.version` property, `gmavenplus-plugin` (5.1.0) for `src/test/groovy`.
3. Package skeleton `com.decoupledx.teammanagement`: `shared/` copy + rename, `identity` module adapted to **Account**, `Application`, `application.yml` (+ prod profile, dev JWT pem), Flyway `V1__init_schema.sql` (baseline: identity + event registry conventions).
4. Spock harness in `src/test/groovy/`:
   - `ApplicationContextSmokeSpec` — context loads (proves Spock + `spock-spring` on Boot 4.1.1 / JUnit 6 / Java 26).
   - `PostgresIntegrationSpec` — base class: TC 2.x static container + `@DynamicPropertySource`, fixed clock, empty issuer, per-test truncate hook.
   - `PostgresMigrationSpec` — Flyway migrates a real PostgreSQL (proves migration tool + Testcontainers).
   - `ModularitySpec` — `ApplicationModules.of(...).verify()` (proves module detection/boundaries).
   - `DomainPuritySpec` — domain free of framework annotations.
5. Infra files: `Dockerfile`, `compose.yaml` (app + postgres + Keycloak dev realm `team-management`), `.github/workflows/ci.yml` (JDK 26 + Testcontainers, PR-trigger), `.env.example`.
6. Gate: `./mvnw verify` green with all specs; `spotless:apply` clean; then (and only then) Phase 2.

### Test strategy (Spock for every layer, per plan §3)
| Layer | Form |
|---|---|
| Domain unit | Spock specs, no Spring/DB — rich-model invariants (membership lifecycle, last-admin rule, voting state machine, balancing algorithm) |
| Module integration | Spock + `spock-spring`, black-box via module `adapter.api` / HTTP; real PostgreSQL via base spec |
| HTTP/API | Spock + MockMvc + mock JWT (`JwtSupport` trait/base), status + authorization + persistence effects |
| Architecture | `ModularitySpec`, `DomainPuritySpec`, record-placement spec |
| Concurrency | duplicate ballot / last-admin races: exactly one success, losers 409 |
| Opt-in | `@Tag("keycloak")` realm-contract spec, excluded by default like the reference |

---

## 6. Open domain decisions (blockers, with proposals)

1. **Global roles**: keep a single realm role `ROLE_ADMIN` (platform admin) like the reference, with **team-admin as domain state** (not a JWT role)? *Proposal: yes.* Realm name `team-management`, client `team-management-api`.
2. **Team creation authority**: any authenticated account may create a team (creator becomes first admin)? *Proposal: yes.*
3. **Admin promotion/demotion**: admins promote members; an admin may demote themselves only if another admin exists; removal of the last admin is rejected (DB constraint + application check).
4. **Profile claiming/linking**: admin creates unclaimed profiles; claiming = authenticated user links own account via per-profile invite token (pattern matches the reference's token-gated booking-request flow). Uniqueness: one linked account per profile per team.
5. **Match creation authority**: team admins create matches. Deadline config: per-team setting (default 3 days before start), stored in `team`, overridable per match.
6. **Match created inside the voting window**: open voting immediately and close at match start (never auto-close in the past).
7. **Membership change during open vote**: eligibility re-validated at close; ballots from departed members are voided; final participant snapshot is authoritative.
8. **Result confirmation**: team admins confirm results; corrections create a new confirmed result version and standings recompute from results (no mutable counters). Cancelled/abandoned/postponed matches produce no standings entries.
9. **MVP vote parameters**: voters = final participants; candidates = final participants; configurable duration (default 48 h); ballot change allowed while open; result visible after close to team members; **tie policy: shared recognition (multiple winners recorded, no runoff) for MVP**.
10. **Photo/logo uploads — IN SCOPE (user decision)**: player profile photos and team logos ship in the MVP, following the reference's proven upload convention: `image/jpeg|png|webp` allowlist, 5 MB per-file cap enforced application-side (multipart limits sized so the friendly error wins), filesystem storage under a configurable upload dir (`app.uploads.dir`, default `uploads/`, gitignored), served read-only from `/uploads/**`; storage constraints and who may view/replace photos decided in the team module design (Phase 2).
11. **i18n scope**: EN + PL + EL bundles from day one (cheap to carry the convention; content authored per-language later).
12. **Deployment target**: same QNAP/Tailscale pattern as the reference, new realm/ports/hostname — confirm.
13. **Coordinates**: groupId `com.decoupledx`, artifact `team-management`, base package `com.decoupledx.teammanagement` — confirm.
