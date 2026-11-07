package com.decoupledx.teammanagement.team.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * A player profile's persisted shape (ADR-0001 rule 8/10). account_id and
 * claim_token are null until linking/claiming happens; both are DB-unique.
 */
@Builder
public record PlayerProfileDataValue(
        UUID id,
        String fullName,
        String contactDetails,
        String nationality,
        String photoPath,
        UUID accountId,
        String claimToken,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
