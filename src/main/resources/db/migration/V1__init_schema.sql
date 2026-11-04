-- Team Management baseline schema (V1). Feature modules add their tables in
-- later migrations; the event_publication registry is the only auto-DDL table
-- (created by Spring Modulith, never by Flyway).

CREATE EXTENSION IF NOT EXISTS btree_gist;

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
