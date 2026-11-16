package com.decoupledx.teammanagement.team.adapter.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Facade over the team module's use-case and query ports — the only bean
 * other modules (incl. the webui) use for this module. All mutating methods
 * are "me"-scoped or admin-gated INSIDE the domain (ADR-0002).
 */
public interface TeamApi {

    TeamWithMembersInfo create(String name);

    TeamWithMembersInfo team(UUID teamId);

    /** The join page's identity hero (name + logo, no roster) — NOT gated. */
    TeamInfo joinPreview(UUID teamId);

    /** One member's profile in a team context (the member-profile page). */
    TeamMemberInfo memberProfile(UUID teamId, UUID profileId);

    List<TeamInfo> myTeams();

    /** Admin-only: mints a fresh ONE-TIME join invite (raw token included). */
    TeamInviteInfo mintInvite(UUID teamId);

    /** Admin-only: revokes an outstanding (unused) invite. */
    void revokeInvite(UUID teamId, UUID inviteId);

    /** Admin-only: the team's active (unused) invites — no raw tokens. */
    List<TeamInviteInfo> activeInvites(UUID teamId);

    /** Cheap liveness check for the join page's hero. */
    boolean inviteActive(UUID teamId, String rawToken);

    /**
     * Joins the acting account through a one-time invite (as PLAYER): the
     * token is consumed atomically — a link works for exactly one player.
     */
    TeamMemberInfo joinByInvite(UUID teamId, String rawToken);

    /** The acting account's own player profile (provisioned on first use). */
    PlayerProfileInfo myProfile();

    /** Whether the acting account's required info (a phone number) is saved. */
    boolean myProfileIsComplete();

    /** Replaces the acting account's own profile photo (web path under /uploads). */
    PlayerProfileInfo applyMyPhoto(String photoPath);

    /**
     * Self-service edit of the acting account's full profile: phone is
     * required + validated, birthday optional, nationality an ISO alpha-2
     * code.
     */
    PlayerProfileInfo applyMyProfile(String fullName, String phone, LocalDate birthDate,
            String nationality);

    /** The claim page's preview: the pending profile + its team identity. */
    ClaimPreviewInfo claimPreview(String rawToken);

    /** Claims the pending profile for the acting account (single use). */
    PlayerProfileInfo claim(String rawToken);

    /** Admin-only: registers a PENDING member (no e-mail; phone required). */
    PlayerProfileInfo registerMember(UUID teamId, String fullName, String phone,
            LocalDate birthDate, String nationality);

    /**
     * Admin-only: mints a FRESH claim link for a PENDING member — replacing
     * the previous one (one active link per pending member). The RAW token
     * rides the returned info once (ADR-0003).
     */
    PlayerProfileInfo regenerateClaimLink(UUID teamId, UUID profileId);

    /** Admin-only: renames the team. */
    TeamWithMembersInfo renameTeam(UUID teamId, String name);

    /** Admin-only: replaces the team logo (web path under /uploads). */
    TeamWithMembersInfo applyLogo(UUID teamId, String logoPath);

    /** Admin-only: promotes a member to team administrator. */
    TeamMemberInfo promote(UUID teamId, UUID profileId);

    /** Admin-only: demotes a team administrator (last admin protected). */
    TeamMemberInfo demote(UUID teamId, UUID profileId);

    /** Admin-only: removes a member from the team (last admin protected). */
    void removeMember(UUID teamId, UUID profileId);
}
