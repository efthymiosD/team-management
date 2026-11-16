package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.adapter.api.TeamInviteInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamMemberInfo;
import java.util.List;
import java.util.UUID;

/**
 * One-time team-join invites (owner decision 2026-11-15: the open join link
 * was exploitable). Raw tokens exist ONLY at mint time — storage holds
 * hashes (decoupledx-common ADR-0003); the join consumes atomically, so a
 * link works for exactly one player.
 */
public interface TeamInviteService {

    /** Admin-only: mints a fresh one-time invite; the RAW token in the
     *  returned info is the only chance to build the join link. */
    TeamInviteInfo mint(UUID teamId);

    /** Admin-only: deletes an outstanding (unused) invite — revocation. */
    void revoke(UUID teamId, UUID inviteId);

    /** Active (unused) invites of the team, newest first — no raw tokens. */
    List<TeamInviteInfo> activeInvites(UUID teamId);

    /** Cheap liveness check for the join page: known, unused, team-matching. */
    boolean inviteActive(UUID teamId, String rawToken);

    /**
     * Joins the acting account's profile through a one-time invite: consumes
     * the token atomically inside the join transaction, so exactly one
     * parallel caller wins (the loser gets the honest INVITE_ALREADY_USED /
     * ALREADY_MEMBER conflict).
     */
    TeamMemberInfo joinByInvite(UUID teamId, String rawToken);
}
