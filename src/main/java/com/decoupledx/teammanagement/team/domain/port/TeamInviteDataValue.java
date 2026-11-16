package com.decoupledx.teammanagement.team.domain.port;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * A minted one-time team-join invite. Storage carries the SHA-256 HASH only
 * (decoupledx-common ADR-0003) — the raw token appeared exactly once, in the
 * minted link, and is unrecoverable by design.
 */
@Builder
public record TeamInviteDataValue(
        UUID id,
        UUID teamId,
        String tokenHash,
        UUID createdBy,
        Instant createdAt,
        Instant usedAt,
        UUID usedBy) {
}
