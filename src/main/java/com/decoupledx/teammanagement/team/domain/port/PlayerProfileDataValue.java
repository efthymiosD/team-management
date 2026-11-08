package com.decoupledx.teammanagement.team.domain.port;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

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