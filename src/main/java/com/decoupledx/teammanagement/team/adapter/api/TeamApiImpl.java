package com.decoupledx.teammanagement.team.adapter.api;

import com.decoupledx.teammanagement.team.domain.port.CreateTeamService;
import com.decoupledx.teammanagement.team.domain.port.TeamQueryService;
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

    @Override
    public TeamWithMembersInfo create(String name) {
        return createTeamService.create(name);
    }

    @Override
    public TeamWithMembersInfo team(UUID teamId) {
        return teamQuery.get(teamId);
    }

    @Override
    public List<TeamInfo> myTeams() {
        return teamQuery.myTeams();
    }
}
