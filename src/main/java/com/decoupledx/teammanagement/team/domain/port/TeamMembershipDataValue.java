package com.decoupledx.teammanagement.team.domain.port;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record TeamMembershipDataValue(
        UUID teamId,
        UUID profileId,
        String role,
        Instant joinedAt,
        long version) {
}