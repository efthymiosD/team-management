package com.decoupledx.teammanagement.team.domain;
import com.decoupledx.common.error.BusinessException;
import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.adapter.api.TeamWithMembersInfo;
import com.decoupledx.teammanagement.team.domain.port.TeamDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamProfileManagementService;
import com.decoupledx.teammanagement.team.domain.port.TeamQueryService;
import com.decoupledx.teammanagement.team.domain.port.TeamRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class TeamProfileManagementServiceImpl implements TeamProfileManagementService {

    private final TeamPermissions permissions;
    private final TeamRepository teams;
    private final TeamQueryService teamQuery;
    private final Clock clock;
    private final TransactionRunner tx;

    @Override
    public TeamWithMembersInfo rename(UUID teamId, String name) {
        return tx.run(() -> {
            permissions.requireAdmin(teamId);
            TeamDataValue existing = teams.find(teamId);
            if (existing == null) {
                throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
            }
            Team team = Team.from(existing);
            // team-name unique-index race → 409 TEAM_NAME_TAKEN (adapter)
            team.rename(name, clock.instant());
            teams.save(team.toData());
            return teamQuery.get(teamId);
        });
    }
}
