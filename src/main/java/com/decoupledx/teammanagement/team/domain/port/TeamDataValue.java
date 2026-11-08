package com.decoupledx.teammanagement.team.domain.port;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record TeamDataValue(
        UUID id,
        String name,
        String logoPath,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}