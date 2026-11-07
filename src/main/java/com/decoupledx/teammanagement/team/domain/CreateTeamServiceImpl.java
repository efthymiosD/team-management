package com.decoupledx.teammanagement.team.domain;

import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.teammanagement.team.adapter.api.TeamWithMembersInfo;
import com.decoupledx.teammanagement.team.domain.port.CreateTeamService;
import com.decoupledx.teammanagement.team.domain.port.CurrentPlayerProfile;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamQueryService;
import com.decoupledx.teammanagement.team.domain.port.TeamRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class CreateTeamServiceImpl implements CreateTeamService {

    private final CurrentPlayerProfile me;
    private final TeamRepository teams;
    private final TeamMembershipRepository memberships;
    private final TeamQueryService teamQuery;
    private final Clock clock;
    private final TransactionRunner tx;

    @Override
    public TeamWithMembersInfo create(String name) {
        // Profile provisioning is its OWN idempotent unit — it must NOT run
        // inside the team tx: on the unique-race loser path the Postgres
        // transaction is aborted and recovery statements would be ignored.
        var profile = me.current();
        return tx.run(() -> {
            Team team = Team.create(name, clock.instant());
            // team-name unique-index race → 409 TEAM_NAME_TAKEN (adapter)
            TeamDataValue saved = teams.save(team.toData());
            TeamMembership membership = TeamMembership.create(saved.id(), profile.id(),
                    TeamMembershipRole.ADMIN, clock.instant());
            // membership PK race → 409 ALREADY_MEMBER (adapter, throws last —
            // nothing follows the failure in the aborted tx)
            memberships.save(membership.toData());
            return teamQuery.get(saved.id());
        });
    }
}
