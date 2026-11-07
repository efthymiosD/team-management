-- Team Management baseline schema (single migration — pre-release app, no
-- deployed history to evolve: all tables ship together and later features
-- EXTEND this file while the app stays unpublished). The event_publication
-- registry is the only auto-DDL table (created by Spring Modulith, never by
-- Flyway).

-- ---------------------------------------------------------------------------
-- Internal, application-owned account identity.
-- Decouples the domain's AccountId from the external IdP (Keycloak).
-- `idp_subject` binds the external identity (Keycloak `sub` claim) to the
-- stable internal UUID. Auto-provisioned on first authenticated request.
-- ---------------------------------------------------------------------------
CREATE TABLE accounts
(
    account_id   UUID         NOT NULL PRIMARY KEY,
    idp_subject  VARCHAR(128) NOT NULL UNIQUE,
    display_name VARCHAR(255) NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Teams: a group that organizes matches. Names are globally unique so an
-- invite can be spoken as "join <name>". Logo is an optional web path.
-- ---------------------------------------------------------------------------
CREATE TABLE teams
(
    id         UUID         NOT NULL PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    logo_path  TEXT         NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT teams_name_length CHECK (char_length(name) BETWEEN 1 AND 200)
);
CREATE UNIQUE INDEX teams_name_unique ON teams (name);

-- ---------------------------------------------------------------------------
-- Player profiles: a person who participates in games. A profile may exist
-- without an account (admin-created); account linking is optional and unique
-- (one profile per account). contact_details holds free-form contact info;
-- photo_path is the uploaded photo's web path.
-- ---------------------------------------------------------------------------
CREATE TABLE player_profiles
(
    id              UUID         NOT NULL PRIMARY KEY,
    full_name       VARCHAR(200) NOT NULL,
    contact_details TEXT         NULL,
    nationality     VARCHAR(100) NULL,
    photo_path      TEXT         NULL,
    account_id      UUID         NULL,
    claim_token     VARCHAR(64)  NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT player_profiles_name_length CHECK (char_length(full_name) BETWEEN 1 AND 200)
);
CREATE UNIQUE INDEX player_profiles_account_unique ON player_profiles (account_id);
CREATE UNIQUE INDEX player_profiles_claim_token_unique ON player_profiles (claim_token);

-- ---------------------------------------------------------------------------
-- Memberships: the relationship between a team and a player profile, with the
-- member's role. Removal DELETES the row (participation/results reference the
-- profile, so history survives); the at-least-one-admin invariant is enforced
-- in the domain service under a locking read (and covered by a concurrency
-- spec).
-- ---------------------------------------------------------------------------
CREATE TABLE team_memberships
(
    team_id    UUID        NOT NULL REFERENCES teams (id),
    profile_id UUID        NOT NULL REFERENCES player_profiles (id),
    role       VARCHAR(16) NOT NULL,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT team_membership_role_check CHECK (role IN ('ADMIN', 'PLAYER')),
    CONSTRAINT team_memberships_unique_per_team UNIQUE (team_id, profile_id)
);
CREATE INDEX team_memberships_profile_idx ON team_memberships (profile_id);
