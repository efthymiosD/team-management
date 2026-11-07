package com.decoupledx.teammanagement.team.adapter.api;

import com.decoupledx.teammanagement.team.domain.port.TeamQueryService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Facade over the team query inbound port — the only bean other modules use
 * for this module.
 */
@RequiredArgsConstructor
class TeamApiImpl implements TeamApi {

    private final TeamQueryService teamQuery;

    @Override
    public TeamWithMembersInfo team(UUID teamId) {
        return teamQuery.get(teamId);
    }

    @Override
    public List<TeamInfo> myTeams() {
        return teamQuery.myTeams();
    }
}
