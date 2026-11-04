package com.decoupledx.teammanagement.identity.adapter.api;

import java.util.UUID;

/**
 * A provisioned account as seen by admin-facing views: the internal account
 * id plus the display name captured from the IdP (may be null for accounts
 * with no stored name).
 */
public record AccountEntry(UUID accountId, String displayName) {
}