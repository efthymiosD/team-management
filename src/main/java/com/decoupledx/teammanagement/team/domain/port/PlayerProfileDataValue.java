package com.decoupledx.teammanagement.team.domain.port;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PlayerProfileDataValue(
        UUID id,
        String fullName,
        String phone,
        LocalDate birthDate,
        String nationality,
        String photoPath,
        UUID accountId,
        String claimTokenHash,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
