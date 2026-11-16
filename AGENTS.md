# team-management — Agent Instructions

## Engineering rules (owner-agreed)

- **Pre-release migrations (decided 2026-11-07):** the app ships ONE baseline
  migration, `V1__init_schema.sql`, EXTENDED in place by later features while
  the app is unpublished (`mvnw clean` after touching it). Separate
  `V2+` files may exist only after the first release creates deployed history.
- **Postgres aborted-transaction rule (proven 2026-11-07 by the concurrent
  first-join race spec):** after any failed statement, a Postgres transaction
  is dead — statements issued inside it fail ("current transaction is
  aborted"). Constraint-race recovery therefore NEVER happens mid-transaction:
  either the adapter ends the flow with the translated 409, or the recovery
  read/write runs as its OWN unit outside the wrapped tx. Identity account
  provisioning and the `CurrentPlayerProfile` seam deliberately run outside
  `tx.run(...)` for this reason (library ADR-0002 documents the same rule).

## Delivery gate (user-directed rule)

Do NOT commit, push, open a PR, merge, or check out an updated main until
the user says **"go-live"**. Until then: prepare changes in the working
tree, validate locally, and present them for review. On **"go-live"**: do
the whole delivery chain in order (commit → push → create PR → merge PR →
check out updated main).

**Input-preservation rule (user-directed, added 2026-11-15):** player input
is ALWAYS preserved on every page and app — record it when DESIGNING new
UI, not only when fixing bugs. Concretely: (a) a failed submit re-renders
the typed draft (flash-carried form state), never the stored state;
(b) NEVER ship multiple competing forms over the same visible state —
ONE form per visible state group whose ANY submit persists the FULL
staged state (profile page: details + photo; team header: name + logo),
validated ALL-OR-NOTHING so a bad part fails the whole submit. File
inputs are the inherent exception (browsers cannot repopulate them).
Integration tests upload into a per-JVM temp dir
(`PostgresIntegrationSpec`) — never the developer's `uploads/`.

**UI/UX gate (user-directed rule, added 2026-11-14):** for any UI change,
the ui-designer agent first produces a report/artifact with mockups and the
owner ACCEPTS it before implementation starts. No template/CSS/flow work
before that acceptance.

## Pre-review checklist (end of every change batch)

Before presenting a change batch for review, run and apply findings from:

1. **Clean code / SOLID / reusability** — no god methods (one reason per
   method, small collaborators instead), no rule duplication (extract a
   shared component), no dead parameters/keys, records in own files.
2. **Performance** — index-backed lookups, no accidental N+1 (batch the
   name/label lookups), bounded in-memory state, pagination counts only
   where the UI needs them.
3. **Security** — the public surface review (CSRF enforced even on
   permitAll POSTs, tokens opaque + only hashes stored, no IDs/PII in
   URLs, no internals in messages, no-store on fresh-clock pages,
   input bounds, untrusted-header honesty documented).
4. **Then** Spotless + unit + the nearest integration specs + full
   verify — only then present.

## Comment & code-hygiene rules (user-directed, added 2026-11-15 — apply
during refactoring and when writing ANY new code)

1. **Comments/javadoc are minimized**: javadoc on INTERFACES only (and
   where a non-obvious shortcut or an owner agreement needs recording —
   race semantics, aborted-transaction rules, security reasoning are
   agreements and stay). No narration comments restating the code.
2. **Always imports over qualified names** (inline `java.util.x.Y` in
   bodies is a defect); clean up dead code and unused imports during
   refactoring.
3. **Prefer named constants** (enum `.name()`, class constants) over
   hardcoded strings blended into logic (e.g. membership roles).
4. **Domain services carry the `Service` postfix** (`TeamViewsService`,
   `TeamPermissionsService`) — aggregates/configs stay as named.

## Status

Bootstrap phase complete (Boot 4.1.1 + Modulith 2.1.1, Java 26): `shared`
kernel + `identity` module, Flyway V1 (accounts), Spock/Groovy test harness.
Generic seams now come from `com.decoupledx:decoupledx-common` (see below).
Next: Phase 2 (team and player management). Reference project:
`../reservation-system` (ADRs, conventions).

## Quick start

```bash
source ~/tools/env.sh          # JDK 26 + Maven 3.9.16
(cd ../decoupledx-common && ./mvnw install)  # once per library update (CI: GitHub Packages)
./mvnw test                    # all tests (integration need Docker)
./mvnw test -Dtest=ModularitySpec          # module boundaries
./mvnw test -Dtest="!*IntegrationSpec,!ApplicationContextSmokeSpec,!PostgresMigrationSpec"  # unit only
./mvnw spotless:apply          # ALWAYS after editing Java (imports+format)
```

## Testing (Spock is the sole test-authoring framework)

- All specs live in `src/test/groovy/` (Groovy 5.1.3 + Spock 2.5-groovy-5.0
  via gmavenplus; surefire includes `**/*Spec.java`).
- Java helpers (not specs) live in `src/test/java/.../testinfra/`:
  `JwtSupport` (mock JWT request post-processors: `account(subject)`,
  `admin(subject)`), `WebUserSupport` (OIDC session users).
- `PostgresIntegrationSpec` (abstract base): ONE shared Testcontainers
  PostgreSQL per JVM, fixed clock `2026-09-01T10:00:00Z`, empty issuer (dev
  key — Keycloak never required), `jdbc` for raw-SQL assertions, `truncate(...)`
  helper per test.
- Conventions carried over: integration specs are black-box (HTTP API via
  MockMvc or raw SQL; NO direct domain-service calls); unit specs only for
  domain-model logic, none for services with mocked deps; concurrency specs
  expect exactly one success + 409s; persistence adapters translate BOTH
  constraint races (`DataIntegrityViolationException` 23P01 and
  `ConcurrencyFailureException` 40P01) to 409.
- Opt-in `@Tag("keycloak")` realm-contract spec excluded by default
  (`-Dsurefire.excludedGroups=` to run vs live Keycloak).
- Spotless covers `src/main/java` + `src/test/java` only — Groovy specs are
  not auto-formatted (keep them hand-tidy; optionally add a Spotless groovy
  section later).

## decoupledx-common (shared library)

- Generic seams (BusinessException + ErrorCode **contract**, event contract +
  `@OnDomainEvent`, TransactionRunner, JWT machinery, GlobalExceptionHandler,
  Clock, OpenAPI bearer scheme) come from `com.decoupledx:decoupledx-common`
  (repo `../decoupledx-common`), registered via Boot auto-configuration; every
  bean is `@ConditionalOnMissingBean` overridable.
- Local builds need `./mvnw install` once in the library repo; CI resolves the
  artifact from GitHub Packages (maven.pkg.github.com/efthymiosD/decoupledx-common)
  via the credentials in ci.yml's settings step.
- This app keeps: its own `ErrorCode` enum (implements the library contract),
  `SecurityConfig` (routing policy), `Money`-style domain value types — none of
  those are library material.
- Library ADR-0002 (verified against Spring 7 bytecode): exceptions crossing
  `run(...)` ALWAYS roll back; constraint races must be resolved INSIDE the
  work by persistence adapters (never let a DIVE cross the boundary).

## Module conventions (ADR carry-over — enforced by specs)

- Exactly one export per module: `adapter/api` (`@NamedInterface("api")`) —
  interface over use cases + DTO views; domain/adapters/internal wiring
  package-private. API views **delegate** to domain logic; api record
  components use api/shared/JDK types only. (Agreed 2026-11-14, slice 3.5:)
  the SHARED kernel has one additional sanctioned export —
  `shared/web` (`@NamedInterface("web")`, currently `Uploads`) — because
  upload storage is cross-cutting web infrastructure both the `team` module's
  REST adapters and the `webui` pages consume; keep that package logic-free.
- ADR-0001 layout: flat package-private `domain/`, public interfaces only in
  `domain/port`, one `XxxDomainConfig` owns domain beans, adapters own their
  configs, adapters never call adapters, domain returns immutable snapshots
  (`…Info`/`…DataValue`), controllers use domain ports not `adapter.api`,
  records in own top-level files (ADR-0003).
- Hexagonal direction + port-language records (ADR-0003, amended 2026-11-08):
  the `…DataValue` records live in `domain.port` (public immutable records —
  the data shape the ports speak), so `domain` imports NOTHING from
  `adapter` packages (persistence/web/scheduling); consuming other modules'
  `adapter.api` types is the one sanctioned cross-module edge (ADR-0002).
  Arch-enforced in `ModuleStructureSpec` (DataValue-records rule +
  domain-vs-adapters rule); entities stay in `adapter/persistence` and map
  to/from the records.
- ADR-0002: adapters never resolve/thread the logged-in account — ports expose
  implicit "me" variants; `identity` exports `CurrentAccountApi` +
  `AccountDirectoryApi`; the webui `CurrentUserAdvice` (when it exists) is the
  only presentation consumer of `CurrentAccountApi`.
- ADR-0004 (events): contracts in decoupledx-common (`com.decoupledx.common.event`:
  `DomainEvent`, `DomainEventPublisher` port, `DomainEventListener`,
  `OnDomainEvent` composed annotation, `SpringDomainEventPublisher` — the
  one Modulith seam, auto-registered); payloads = records in the publishing-or-REACTIVE module's
  `adapter/api` (cycle-break rule); one logic-free `adapter/listeners/` class
  per module with composed `@OnDomainEvent` methods; events carry the actor
  explicitly; reactions idempotent. Registry: starter-jdbc, completion-mode
  DELETE, republish-on-restart; `event_publication` auto-DDL = the one
  non-Flyway table.
- `shared/` = app-owned shared kernel (the `ErrorCode` enum implementing the
  library error contract, `SecurityConfig` routing policy). Domain purity +
  record placement + module structure are spec-enforced (`architecture/`
  package). Generic seams come from `com.decoupledx:decoupledx-common`
  (see the decoupledx-common section above); "Shared" remains a Modulith
  module exactly as before.

## Identity

- `accounts` table: app-owned `AccountId` (UUID) ↔ `idp_subject` (Keycloak
  `sub`), auto-provisioned on first authenticated request; display names
  captured at login.
- Realm `team-management` (dev: compose, admin/admin, users from
  `keycloak/realm-export.json`); clients `team-management-api` (subject +
  audience mappers) and `team-management-web` (roles mapper). Realm roles:
  ADMIN (platform admin) + MEMBER (plain user). Team-admin is DOMAIN state,
  never a JWT role.

## Build & test quirks (Java 26 / Boot 4 / Testcontainers 2.x)

- JDK 26 renders `ISO_LOCAL_TIME` with seconds — matters for JSON-body
  assertions.
- Lombok processor is NOT implicit on JDK 25+ — `annotationProcessorPaths`
  in pom must stay.
- Boot 4: `@AutoConfigureMockMvc` lives in `spring-boot-webmvc-test`; Jackson
  is **3** (`tools.jackson.databind.ObjectMapper`); flyway needs
  `flyway-database-postgresql`.
- Testcontainers 2.x: `@ServiceConnection` + `PostgreSQLContainer` broken —
  static `{ POSTGRES.start() }` + `@DynamicPropertySource` (the base spec
  handles this).
- `spock-bom` must NOT be imported: it pins an older junit-bom that would
  downgrade Boot's managed JUnit 6. Spock deps carry explicit versions.
- Groovy is pinned to 5.1.x (the JDK-26-tested line) via `<groovy.version>`;
  the Spock variant `groovy-5.0` runs on any Groovy 5.x.
- Docker (Rancher Desktop) required for integration tests; socket fix as in
  the Reservation System README. Local Postgres alternative: `DB_URL`,
  `DB_USERNAME`, `DB_PASSWORD` (default
  `jdbc:postgresql://localhost:5433/team_management`).

## Dev-stack ports (agreed — sibling apps run side by side)

- team-management: Postgres **5433**, Keycloak **8082**, app **8080**.
- reservation-system owns Postgres **5432** + Keycloak **8081** — never
  publish team-management containers on those host ports.
- App port 8080 is the shared exception: run the second app with
  `--server.port=8083` (or similar) when both must be served at once.

## Photo/logo uploads + claiming (MVP scope, as-built slice 2)

- Reference convention: `image/jpeg|png|webp` allowlist, 5 MB app-side cap,
  filesystem storage under `app.uploads.dir` (default `uploads/`, gitignored),
  served read-only from `/uploads/**` (StaticUploadsConfig maps the dir;
  SecurityConfig permitAll's it); multipart limits (6MB/12MB) sized so the
  friendly business error wins Tomcat's.
- Endpoints: `POST /api/teams/{teamId}/logo` (admin), `POST /api/players/me/photo`
  (own profile), `POST /api/players/me/profile` (self-service full edit:
  name editable, phone REQUIRED + validated — liberal international format
  `+[0-9][0-9 -]{5,18}[0-9]`, optional birthday ≤ today ≥ 1900, nationality
  = ISO 3166-1 alpha-2 code — emoji-flag rendering is presentation-only;
  the WEB form is ONE form whose every submit persists the full visible
  state (details + optional photo part) with all-or-nothing validation —
  never a half-applied profile; the REST photo endpoint
  `/api/players/me/photo` stays separate),
  `POST /api/claims` (invite-token claiming; single use, token cleared on
  success; one profile per account — DB unique indexes decide races as
  honest 409s; `ACCOUNT_ALREADY_PROFILED` blocks claiming when the account
  already provisioned a profile). Claim tokens are issued only in the admin
  registration response.
- Web UI (slice 3.5 review, approved mockups `docs/ui/slice35-review-mockups.html`):
  team page = identity header (logo/name/count/creation date) + players GRID
  (whole card links to `/teams/{teamId}/members/{profileId}`), member-profile
  view (phone rendered spaced + `tel:` link, admins-only; birthday + current
  age; cake 🎂 badge on today's birthdays), join page = identity hero only
  (no roster; uses the UN-GATED `joinPreview`), invite link shows the FULL
  absolute URL (`app.base-url`, default `http://localhost:8080`) with
  copy/share (progressive-enhancement JS only, `static/js/app.js`), profile
  page shows the IdP email READ-ONLY (accounts.email, mirrored from the
  `email` claim; Keycloak realm sets `registrationEmailAsUsername`), a
  profile-completeness GATE (`ProfileCompletionGate`) redirects incomplete
  profiles (no saved phone) to `/players/me` from every non-team page, and
  admins get an inline team RENAME form (`POST /teams/{id}/rename`; failed
  submits keep the typed draft).
- Team pages are TEAMMATES-ONLY (owner rule 2026-11-15): `TeamQueryService.get`
  rejects non-members (incl. removed former members) with NOT_TEAM_MEMBER —
  the join hero (`joinPreview`) is the one un-gated team view. Buttons are
  colored while eligible; grayed + `disabled` only for impossible actions
  (last-admin Remove/Demote); Remove is filled red, Promote/Replace-logo
  filled green.
- One-time invite links + direct member add (owner decisions 2026-11-15,
  approved mockups §12): the OPEN join link is DEAD — joining happens ONLY
  through a one-time invite (`team_invites`, `POST /api/teams/{id}/invites`
  admin-minted → `/join/{token}`, consumed by a single atomic guarded
  UPDATE; races → honest 409 `INVITE_ALREADY_USED`; revoke = DELETE, no
  expiry). Token storage is HASH-ONLY — decoupledx-common v0.2.0
  (`com.decoupledx.common.tokens`, ADR-0003): `OneTimeTokens.generate/hash`
  + `SingleUseTokenLedger`; the RAW token appears exactly once (minted link
  / registration response). Claim tokens are hashed too
  (`player_profiles.claim_token_hash`). Admins add PENDING members
  (name + phone required, no e-mail) whose claim link binds the claiming
  account; an account that already has a profile MERGES (their profile
  wins: memberships re-point, placeholder hard-deleted). Removing a pending
  member (unclaimed, last team) hard-deletes the profile row. Pending
  members are visible (clock badge + "Pending" pill on the grid, §13),
  cannot be PROMOTED (409 `MEMBER_PENDING` until claimed), and their claim
  links are REGENERABLE per member (`POST /teams/{teamId}/members/
  {profileId}/claim-link`, admin-gated): minting swaps the stored hash —
  one active link per pending member, previous link instantly dead; the
  claim-page deeplink is `/teams/{teamId}/members/{profileId}/claim/
  {token}` (path names the player; the random token is the sole
  authority — §13).
- Manual theme switch (approved mockup §11): server-read `theme` cookie
  (`auto|light|dark`, 1-year, `HttpOnly=false`, SameSite=Lax) renders
  `data-theme="light|dark"` on `<html>` at first paint; AUTO renders no
  attribute — `prefers-color-scheme` stays the fallback, and the
  `[data-theme]` CSS overrides sit at the END of team.css to win the
  cascade. `POST /theme` (permitAll, CSRF on, referer-path redirect,
  out-of-set values silently dropped) is the no-JS path; `static/js/app.js`
  swaps attribute + cookie in place. Keys: `theme.auto/light/dark`,
  `nav.theme`.
- Replacement never deletes the old file on the request path (ops reaps the
  uploads directory).

## Ops

- CI/CD conventions (mirroring reservation-system; agreed): PR-only `ci.yml`
  with the required `mvn verify` check; `packages: read` + GPR credentials so
  Actions resolves decoupledx-common; image builds hermetic via BuildKit
  secret `github_token` + `GPR_USERNAME` (never baked into layers); release
  pipelines publish versioned, immutable artifacts scanned before promotion.
- `./mvnw verify` is the single CI gate (PR-only, required check); Dockerfile
  multi-stage temurin, non-root `app` user, curl healthcheck; compose for dev
  (postgres + keycloak). Trivy-driven pins in the pom: tomcat 11.0.25,
  Jackson 2.22.4 / 3.2.3.
- Config keys: `app.security.*` (issuer, jwk-set-uri, audience, CORS,
  dev-public-key, end-session-uri), `app.clock.fixed-instant`,
  `app.uploads.dir`. Prod profile forbids the dev-key fallback
  (`allow-dev-key: false`).
