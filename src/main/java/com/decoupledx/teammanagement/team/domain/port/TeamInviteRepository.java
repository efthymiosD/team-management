package com.decoupledx.teammanagement.team.domain.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for one-time team-join invites. The consume MUST be a single
 * atomic guarded UPDATE (decoupledx-common ADR-0003): exactly one parallel
 * caller sees a row matched, the loser surfaces the honest
 * INVITE_ALREADY_USED conflict.
 */
public interface TeamInviteRepository {

    /** Persists a minted invite (hash only — never the raw token). */
    TeamInviteDataValue save(TeamInviteDataValue invite);

    Optional<TeamInviteDataValue> find(UUID id);

    Optional<TeamInviteDataValue> findByTokenHash(String tokenHash);

    /** Active (unused) invites of a team, newest first. */
    java.util.List<TeamInviteDataValue> findActiveByTeam(UUID teamId);

    /**
     * Atomically consumes an ACTIVE invite: sets used_at/used_by and returns
     * true when THIS call won the race; false when the invite was already
     * used (or no longer exists — unknown is distinguished by the caller's
     * prior lookup).
     */
    boolean consume(String tokenHash, UUID usedByProfileId, java.time.Instant now);

    void delete(UUID id);
}
