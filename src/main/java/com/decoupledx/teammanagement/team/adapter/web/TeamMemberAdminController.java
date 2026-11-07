package com.decoupledx.teammanagement.team.adapter.web;

import com.decoupledx.teammanagement.team.adapter.api.TeamMemberInfo;
import com.decoupledx.teammanagement.team.domain.port.TeamAdministrationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Pure delegation over the admin-side membership operations. */
@RestController
@RequiredArgsConstructor
class TeamMemberAdminController {

    private final TeamAdministrationService administrations;

    @PostMapping("/api/teams/{teamId}/members/{profileId}/promote")
    TeamMemberInfo promote(@PathVariable UUID teamId, @PathVariable UUID profileId) {
        return administrations.promote(teamId, profileId);
    }

    @PostMapping("/api/teams/{teamId}/members/{profileId}/demote")
    TeamMemberInfo demote(@PathVariable UUID teamId, @PathVariable UUID profileId) {
        return administrations.demote(teamId, profileId);
    }

    @DeleteMapping("/api/teams/{teamId}/members/{profileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable UUID teamId, @PathVariable UUID profileId) {
        administrations.removeMember(teamId, profileId);
    }
}
