package com.decoupledx.teammanagement.team.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * A team membership's persisted shape (ADR-0001 rule 8/10). Role is the
 * membership-level state: ADMIN or PLAYER.
 */
@Builder
public record TeamMembershipDataValue(
        UUID teamId,
        UUID profileId,
        String role,
        Instant joinedAt,
        long version) {
}
