package com.decoupledx.teammanagement.team.adapter.api;

import com.decoupledx.teammanagement.team.domain.port.CreateTeamService;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfilePhotoService;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileService;
import com.decoupledx.teammanagement.team.domain.port.ProfileClaimService;
import com.decoupledx.teammanagement.team.domain.port.TeamAdministrationService;
import com.decoupledx.teammanagement.team.domain.port.TeamInviteService;
import com.decoupledx.teammanagement.team.domain.port.TeamLogoService;
import com.decoupledx.teammanagement.team.domain.port.TeamProfileManagementService;
import com.decoupledx.teammanagement.team.domain.port.TeamQueryService;
import com.decoupledx.teammanagement.team.domain.port.TeamRegistrationService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Facade over the team module's use-case and query ports — the only bean
 * other modules (incl. the webui) use for this module.
 */
@RequiredArgsConstructor
class TeamApiImpl implements TeamApi {

    private final CreateTeamService createTeamService;
    private final TeamQueryService teamQuery;
    private final TeamRegistrationService registrations;
    private final PlayerProfilePhotoService photos;
    private final PlayerProfileService profiles;
    private final TeamLogoService logos;
    private final TeamAdministrationService administrations;
    private final TeamProfileManagementService profileManagement;
    private final TeamInviteService invites;
    private final ProfileClaimService claims;

    @Override
    public TeamWithMembersInfo create(String name) {
        return createTeamService.create(name);
    }

    @Override
    public TeamWithMembersInfo team(UUID teamId) {
        return teamQuery.get(teamId);
    }

    @Override
    public TeamInfo joinPreview(UUID teamId) {
        return teamQuery.joinPreview(teamId);
    }

    @Override
    public TeamMemberInfo memberProfile(UUID teamId, UUID profileId) {
        return teamQuery.memberProfile(teamId, profileId);
    }

    @Override
    public TeamWithMembersInfo renameTeam(UUID teamId, String name) {
        return profileManagement.rename(teamId, name);
    }

    @Override
    public ClaimPreviewInfo claimPreview(String rawToken) {
        return claims.preview(rawToken);
    }

    @Override
    public PlayerProfileInfo claim(String rawToken) {
        return claims.claim(rawToken);
    }

    @Override
    public PlayerProfileInfo registerMember(UUID teamId, String fullName, String phone,
            LocalDate birthDate, String nationality) {
        return registrations.registerPlayer(teamId, fullName, phone, birthDate, nationality);
    }

    @Override
    public PlayerProfileInfo regenerateClaimLink(UUID teamId, UUID profileId) {
        return claims.regenerateClaimLink(teamId, profileId);
    }

    @Override
    public List<TeamInfo> myTeams() {
        return teamQuery.myTeams();
    }

    @Override
    public TeamInviteInfo mintInvite(UUID teamId) {
        return invites.mint(teamId);
    }

    @Override
    public void revokeInvite(UUID teamId, UUID inviteId) {
        invites.revoke(teamId, inviteId);
    }

    @Override
    public List<TeamInviteInfo> activeInvites(UUID teamId) {
        return invites.activeInvites(teamId);
    }

    @Override
    public boolean inviteActive(UUID teamId, String rawToken) {
        return invites.inviteActive(teamId, rawToken);
    }

    @Override
    public TeamMemberInfo joinByInvite(UUID teamId, String rawToken) {
        return invites.joinByInvite(teamId, rawToken);
    }

    @Override
    public PlayerProfileInfo myProfile() {
        return registrations.myProfile();
    }

    @Override
    public boolean myProfileIsComplete() {
        return registrations.myProfileIsComplete();
    }

    @Override
    public PlayerProfileInfo applyMyPhoto(String photoPath) {
        return photos.applyMyPhoto(photoPath);
    }

    @Override
    public PlayerProfileInfo applyMyProfile(String fullName, String phone, LocalDate birthDate,
            String nationality) {
        return profiles.applyMyProfile(fullName, phone, birthDate, nationality);
    }

    @Override
    public TeamWithMembersInfo applyLogo(UUID teamId, String logoPath) {
        return logos.applyLogo(teamId, logoPath);
    }

    @Override
    public TeamMemberInfo promote(UUID teamId, UUID profileId) {
        return administrations.promote(teamId, profileId);
    }

    @Override
    public TeamMemberInfo demote(UUID teamId, UUID profileId) {
        return administrations.demote(teamId, profileId);
    }

    @Override
    public void removeMember(UUID teamId, UUID profileId) {
        administrations.removeMember(teamId, profileId);
    }
}
