package com.decoupledx.teammanagement.team.adapter.api;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * An invite as seen by the facade. rawToken is populated ONLY by mint() —
 * the one moment the raw value exists; listing/revoke flows never see it.
 */
@Builder
public record TeamInviteInfo(UUID inviteId, UUID teamId, Instant createdAt, Instant usedAt,
        String rawToken) {
}
