package com.decoupledx.teammanagement.team.adapter.web;

import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamInviteInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamMemberInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamWithMembersInfo;
import com.decoupledx.teammanagement.team.domain.port.CreateTeamService;
import com.decoupledx.teammanagement.team.domain.port.TeamInviteService;
import com.decoupledx.teammanagement.team.domain.port.TeamProfileManagementService;
import com.decoupledx.teammanagement.team.domain.port.TeamQueryService;
import com.decoupledx.teammanagement.team.domain.port.TeamRegistrationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pure delegation: request DTO → inbound port call (ADR-0002 — controllers
 * never resolve the acting account, the domain does).
 */
@RestController
@RequiredArgsConstructor
class TeamController {

    private final CreateTeamService createTeams;
    private final TeamQueryService teamQuery;
    private final TeamRegistrationService registrations;
    private final TeamProfileManagementService profileManagement;
    private final TeamInviteService invites;

    @PostMapping("/api/teams")
    @ResponseStatus(HttpStatus.CREATED)
    TeamWithMembersInfo create(@Valid @RequestBody TeamCreateRequest request) {
        return createTeams.create(request.name());
    }

    @GetMapping("/api/teams/{teamId}")
    TeamWithMembersInfo get(@PathVariable UUID teamId) {
        return teamQuery.get(teamId);
    }

    @PatchMapping("/api/teams/{teamId}")
    TeamWithMembersInfo rename(@PathVariable UUID teamId,
            @Valid @RequestBody TeamRenameRequest request) {
        return profileManagement.rename(teamId, request.name());
    }

    @PostMapping("/api/teams/{teamId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    TeamInviteInfo mintInvite(@PathVariable UUID teamId) {
        return invites.mint(teamId);
    }

    @GetMapping("/api/teams/{teamId}/invites")
    List<TeamInviteInfo> activeInvites(@PathVariable UUID teamId) {
        return invites.activeInvites(teamId);
    }

    @DeleteMapping("/api/teams/{teamId}/invites/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revokeInvite(@PathVariable UUID teamId, @PathVariable UUID inviteId) {
        invites.revoke(teamId, inviteId);
    }

    /**
     * One-time invite join: consumes the token atomically — a link works for
     * exactly one player (the old open join endpoint is gone).
     */
    @PostMapping("/api/teams/{teamId}/join/{token}")
    @ResponseStatus(HttpStatus.CREATED)
    TeamMemberInfo joinByInvite(@PathVariable UUID teamId, @PathVariable String token) {
        return invites.joinByInvite(teamId, token);
    }

    @PostMapping("/api/teams/{teamId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    PlayerProfileInfo registerPlayer(@PathVariable UUID teamId,
            @Valid @RequestBody PlayerProfileRegistrationRequest request) {
        return registrations.registerPlayer(teamId, request.fullName(), request.phone(),
                request.birthDate(), request.nationality());
    }
}
