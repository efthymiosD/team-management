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
-- `email` mirrors the IdP's (required at registration) address — the app's
-- unique player identifier, displayed read-only on the profile.
-- ---------------------------------------------------------------------------
CREATE TABLE accounts
(
    account_id   UUID         NOT NULL PRIMARY KEY,
    idp_subject  VARCHAR(128) NOT NULL UNIQUE,
    display_name VARCHAR(255) NULL,
    email        VARCHAR(255) NULL UNIQUE,
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
-- (one profile per account). The self-service contact set is a VALIDATED
-- phone number (required on save) plus an optional birthday; nationality is
-- an ISO 3166-1 alpha-2 country code (flag rendering is presentation-only).
-- photo_path is the uploaded photo's web path. claim_token_hash stores the
-- SHA-256 of the single-use claim token — the RAW token appears exactly once
-- (in the admin registration response / minted link) and never at rest
-- (decoupledx-common ADR-0003).
-- ---------------------------------------------------------------------------
CREATE TABLE player_profiles
(
    id              UUID         NOT NULL PRIMARY KEY,
    full_name       VARCHAR(200) NOT NULL,
    phone           VARCHAR(32)  NULL,
    birth_date      DATE         NULL,
    nationality     VARCHAR(2)   NULL,
    photo_path      TEXT         NULL,
    account_id      UUID         NULL,
    claim_token_hash VARCHAR(64) NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT player_profiles_name_length CHECK (char_length(full_name) BETWEEN 1 AND 200),
    CONSTRAINT player_profiles_nationality_check CHECK (nationality ~ '^[A-Z]{2}$'),
    CONSTRAINT player_profiles_phone_check CHECK (phone ~ '^\+[0-9][0-9 -]{5,18}[0-9]$')
);
CREATE UNIQUE INDEX player_profiles_account_unique ON player_profiles (account_id);
CREATE UNIQUE INDEX player_profiles_claim_token_unique ON player_profiles (claim_token_hash);

-- ---------------------------------------------------------------------------
-- One-time team join invites (owner decision 2026-11-15: the open join link
-- was exploitable). The RAW token appears exactly once — in the minted link;
-- storage holds the SHA-256 hash only (decoupledx-common ADR-0003). Single
-- use via an atomic guarded UPDATE; revocation = DELETE (no expiry).
-- ---------------------------------------------------------------------------
CREATE TABLE team_invites
(
    id         UUID        NOT NULL PRIMARY KEY,
    team_id    UUID        NOT NULL REFERENCES teams (id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_by UUID        NOT NULL REFERENCES player_profiles (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    used_at    TIMESTAMPTZ NULL,
    used_by    UUID        NULL REFERENCES player_profiles (id)
);
CREATE INDEX team_invites_team_idx ON team_invites (team_id);

-- ---------------------------------------------------------------------------
-- Memberships: the relationship between a team and a player profile, with the
-- member's role. Removal DELETES the row (participation/results reference the
-- profile, so history survives); the at-least-one-admin invariant is enforced
-- in the domain service under a locking read (and covered by a concurrency
-- spec). Unclaimed profiles removed from their last team are hard-deleted
-- together with their invitation (owner decision 2026-11-15).
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
