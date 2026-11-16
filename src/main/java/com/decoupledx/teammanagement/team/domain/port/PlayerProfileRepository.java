package com.decoupledx.teammanagement.team.domain.port;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Outbound port for player-profile persistence. The profile/account unique
 * index and the claim-token unique index race as 409s inside the adapter.
 */
public interface PlayerProfileRepository {

    PlayerProfileDataValue save(PlayerProfileDataValue profile);

    PlayerProfileDataValue find(UUID profileId);

    PlayerProfileDataValue findByAccount(UUID accountId);

    PlayerProfileDataValue findByClaimTokenHash(String claimTokenHash);

    /**
     * Atomically consumes the claim token (single-use guard): clears the
     * hash when THIS call wins the race; false means the token was already
     * consumed (or gone) — the caller surfaces the honest 409.
     */
    boolean consumeClaimToken(UUID profileId, String tokenHash);

    /** Hard-deletes an unclaimed profile (owner decision: removing a pending
     *  member kills the row and with it the invitation). */
    void deleteById(UUID profileId);

    /** Batch lookup for assembling member views (no per-member queries). */
    List<PlayerProfileDataValue> findAllByIds(Collection<UUID> profileIds);
}