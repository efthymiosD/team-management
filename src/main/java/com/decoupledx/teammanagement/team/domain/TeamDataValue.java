package com.decoupledx.teammanagement.team.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * The team's persisted shape as spoken across the repository port (ADR-0001
 * rule 8 / rule 10): flat, immutable, public only for the module itself.
 */
@Builder
public record TeamDataValue(
        UUID id,
        String name,
        String logoPath,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
